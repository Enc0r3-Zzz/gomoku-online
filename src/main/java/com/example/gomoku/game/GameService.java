package com.example.gomoku.game;

import com.example.gomoku.chat.ChatMessage;
import com.example.gomoku.chat.ChatService;
import com.example.gomoku.common.ApiException;
import com.example.gomoku.common.ErrorCode;
import com.example.gomoku.user.User;
import com.example.gomoku.user.UserRepository;
import com.example.gomoku.ws.WsSessionRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    public static final int TURN_SECONDS = 30;

    private final GameMatchRepository gameMatchRepository;
    private final MoveRepository moveRepository;
    private final UserRepository userRepository;
    private final WsSessionRegistry sessionRegistry;
    private final ChatService chatService;
    private final Map<Long, GameRoom> rooms = new ConcurrentHashMap<>();

    @Value("${gomoku.game.disconnect-grace-seconds:60}")
    private long disconnectGraceSeconds;

    public GameService(GameMatchRepository gameMatchRepository,
                       MoveRepository moveRepository,
                       UserRepository userRepository,
                       WsSessionRegistry sessionRegistry,
                       ChatService chatService) {
        this.gameMatchRepository = gameMatchRepository;
        this.moveRepository = moveRepository;
        this.userRepository = userRepository;
        this.sessionRegistry = sessionRegistry;
        this.chatService = chatService;
    }

    @Transactional
    public GameMatch createMatch(Long blackId, Long whiteId) {
        GameMatch match = new GameMatch();
        match.setBlackId(blackId);
        match.setWhiteId(whiteId);
        match.setStatus(GameMatchStatus.WAITING);
        match.setStartedAt(LocalDateTime.now());
        return gameMatchRepository.save(match);
    }

    @Transactional(readOnly = true)
    public GameMatch getMatch(Long matchId) {
        return gameMatchRepository.findById(matchId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "对局不存在"));
    }

    @Transactional
    public GameRoom joinRoom(Long matchId, Long userId) {
        GameMatch match = getMatch(matchId);
        if (!match.getBlackId().equals(userId) && !match.getWhiteId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "不是本局玩家");
        }

        GameRoom room = rooms.computeIfAbsent(matchId, id -> loadRoom(match));
        boolean reconnected = room.isDisconnected(userId);
        room.connect(userId);
        room.markConnected(userId);

        boolean started = false;
        if (match.getStatus() == GameMatchStatus.WAITING && room.bothConnected()) {
            match.setStatus(GameMatchStatus.PLAYING);
            gameMatchRepository.save(match);
            synchronized (room.getLock()) {
                room.setStatus(GameMatchStatus.PLAYING);
                room.setTurnDeadline(Instant.now().plusSeconds(TURN_SECONDS));
            }
            started = true;
        }
        if (room.getStatus() == GameMatchStatus.PLAYING && room.getTurnDeadline() == null) {
            room.setTurnDeadline(Instant.now().plusSeconds(TURN_SECONDS));
        }

        sendState(room, userId);
        if (started) {
            sendState(room, room.opponentOf(userId));
        }
        if (reconnected) {
            Long opponentId = room.opponentOf(userId);
            afterCommit(() -> sessionRegistry.send(opponentId, "OPPONENT_BACK", Map.of("matchId", matchId)));
        }
        return room;
    }

    @Transactional
    public void move(Long matchId, Long userId, int x, int y, int seq) {
        GameRoom room = rooms.get(matchId);
        if (room == null) {
            throw new ApiException(ErrorCode.NOT_IN_GAME, "尚未加入对局");
        }

        synchronized (room.getLock()) {
            if (room.getStatus() != GameMatchStatus.PLAYING) {
                throw new ApiException(ErrorCode.GAME_OVER, "对局已结束");
            }
            if (!room.isPlayer(userId)) {
                throw new ApiException(ErrorCode.FORBIDDEN, "不是本局玩家");
            }
            Stone color = room.colorOf(userId);
            if (room.getTurn() != color) {
                throw new ApiException(ErrorCode.NOT_YOUR_TURN, "还没轮到你");
            }
            if (seq != room.getSeq() + 1) {
                throw new ApiException(ErrorCode.SEQ_MISMATCH, "落子序号不匹配");
            }
            if (!Board.inBounds(x, y) || !room.getBoard().isEmpty(x, y)) {
                throw new ApiException(ErrorCode.INVALID_MOVE, "落点非法");
            }

            room.getBoard().place(x, y, color);
            Move move = new Move();
            move.setMatchId(matchId);
            move.setSeq(seq);
            move.setPlayerId(userId);
            move.setX(x);
            move.setY(y);
            move.setCreatedAt(LocalDateTime.now());
            moveRepository.save(move);
            room.getMoves().add(move);
            room.setSeq(seq);

            boolean win = room.getBoard().hasFive(x, y);
            boolean full = !win && room.getBoard().isFull();

            if (win) {
                broadcastMove(room, seq, x, y, userId, null, null);
                finish(room, color == Stone.BLACK ? GameResult.BLACK_WIN : GameResult.WHITE_WIN, userId);
            } else if (full) {
                broadcastMove(room, seq, x, y, userId, null, null);
                finish(room, GameResult.DRAW, null);
            } else {
                room.setTurn(color.opposite());
                room.setTurnDeadline(Instant.now().plusSeconds(TURN_SECONDS));
                broadcastMove(room, seq, x, y, userId, room.getTurn(), room.getTurnDeadline());
            }
        }
    }

    @Transactional
    public void resign(Long matchId, Long userId) {
        GameRoom room = rooms.get(matchId);
        if (room == null) {
            throw new ApiException(ErrorCode.NOT_IN_GAME, "尚未加入对局");
        }
        synchronized (room.getLock()) {
            if (room.getStatus() != GameMatchStatus.PLAYING) {
                return;
            }
            Long winnerId = room.opponentOf(userId);
            GameResult result = winnerId.equals(room.getBlackId())
                    ? GameResult.BLACK_WIN
                    : GameResult.WHITE_WIN;
            finish(room, result, winnerId);
        }
    }

    public void disconnect(Long userId) {
        for (GameRoom room : rooms.values()) {
            if (room.isPlayer(userId) && room.getStatus() == GameMatchStatus.PLAYING) {
                room.disconnect(userId);
                room.markDisconnected(userId);
                sessionRegistry.send(room.opponentOf(userId), "OPPONENT_LEFT", Map.of(
                        "matchId", room.getMatchId(),
                        "graceSeconds", disconnectGraceSeconds));
            }
        }
    }

    @Transactional
    public void chat(Long matchId, Long userId, String text) {
        GameRoom room = rooms.get(matchId);
        if (room == null) {
            throw new ApiException(ErrorCode.NOT_IN_GAME, "尚未加入对局");
        }
        if (!room.isPlayer(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "不是本局玩家");
        }
        if (room.getStatus() == GameMatchStatus.FINISHED) {
            throw new ApiException(ErrorCode.GAME_OVER, "对局已结束");
        }

        ChatMessage message = chatService.send(matchId, userId, text);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("senderId", userId);
        data.put("text", message.getContent());
        data.put("at", message.getCreatedAt().toString());
        afterCommit(() -> {
            sessionRegistry.send(room.getBlackId(), "CHAT", data);
            sessionRegistry.send(room.getWhiteId(), "CHAT", data);
        });
    }

    @Transactional
    public void timeoutExpired() {
        Instant now = Instant.now();
        for (GameRoom room : rooms.values()) {
            synchronized (room.getLock()) {
                if (room.getStatus() != GameMatchStatus.PLAYING) {
                    continue;
                }
                if (room.getTurnDeadline() != null && now.isAfter(room.getTurnDeadline())) {
                    Long currentPlayer = room.playerOf(room.getTurn());
                    Long winnerId = room.opponentOf(currentPlayer);
                    GameResult result = winnerId.equals(room.getBlackId())
                            ? GameResult.BLACK_WIN
                            : GameResult.WHITE_WIN;
                    finish(room, result, winnerId);
                    continue;
                }

                Instant blackDown = room.disconnectedAt(room.getBlackId());
                Instant whiteDown = room.disconnectedAt(room.getWhiteId());
                boolean blackExpired = blackDown != null
                        && Duration.between(blackDown, now).getSeconds() >= disconnectGraceSeconds;
                boolean whiteExpired = whiteDown != null
                        && Duration.between(whiteDown, now).getSeconds() >= disconnectGraceSeconds;

                if (blackExpired && whiteExpired) {
                    finish(room, GameResult.ABORT, null);
                    continue;
                }
                if (blackExpired || whiteExpired) {
                    Long loserId = blackExpired ? room.getBlackId() : room.getWhiteId();
                    Long winnerId = room.opponentOf(loserId);
                    GameResult result = winnerId.equals(room.getBlackId())
                            ? GameResult.BLACK_WIN
                            : GameResult.WHITE_WIN;
                    finish(room, result, winnerId);
                }
            }
        }
    }

    private GameRoom loadRoom(GameMatch match) {
        GameRoom room = new GameRoom(match.getId(), match.getBlackId(), match.getWhiteId());
        room.setStatus(match.getStatus());
        room.setResult(match.getResult());
        room.setWinnerId(match.getWinnerId());

        List<Move> moves = moveRepository.findByMatchIdOrderBySeqAsc(match.getId());
        for (Move move : moves) {
            room.getBoard().place(move.getX(), move.getY(), room.colorOf(move.getPlayerId()));
            room.getMoves().add(move);
        }
        room.setSeq(moves.size());
        room.setTurn(moves.size() % 2 == 0 ? Stone.BLACK : Stone.WHITE);
        return room;
    }

    private void finish(GameRoom room, GameResult result, Long winnerId) {
        GameMatch match = getMatch(room.getMatchId());
        if (match.getStatus() == GameMatchStatus.FINISHED) {
            return;
        }
        match.setStatus(GameMatchStatus.FINISHED);
        match.setResult(result);
        match.setWinnerId(winnerId);
        match.setEndedAt(LocalDateTime.now());
        gameMatchRepository.save(match);

        User black = userRepository.findById(room.getBlackId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "玩家不存在"));
        User white = userRepository.findById(room.getWhiteId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "玩家不存在"));

        if (result == GameResult.ABORT) {
            // 双方都掉线，直接中止，不计胜负
        } else if (result == GameResult.DRAW) {
            black.setDraws(black.getDraws() + 1);
            white.setDraws(white.getDraws() + 1);
        } else if (winnerId != null && winnerId.equals(black.getId())) {
            black.setWins(black.getWins() + 1);
            white.setLosses(white.getLosses() + 1);
        } else {
            white.setWins(white.getWins() + 1);
            black.setLosses(black.getLosses() + 1);
        }
        userRepository.save(black);
        userRepository.save(white);

        room.setStatus(GameMatchStatus.FINISHED);
        room.setResult(result);
        room.setWinnerId(winnerId);
        room.setTurnDeadline(null);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("matchId", room.getMatchId());
        payload.put("result", result.name());
        payload.put("winnerId", winnerId);
        afterCommit(() -> {
            sessionRegistry.send(room.getBlackId(), "GAME_OVER", payload);
            sessionRegistry.send(room.getWhiteId(), "GAME_OVER", payload);
        });
    }

    private void sendState(GameRoom room, Long userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("matchId", room.getMatchId());
        data.put("blackId", room.getBlackId());
        data.put("whiteId", room.getWhiteId());
        data.put("yourColor", room.colorOf(userId).name());
        data.put("turn", room.getTurn().name());
        data.put("seq", room.getSeq());
        data.put("status", room.getStatus().name());
        data.put("deadline", room.getTurnDeadline() == null ? null : room.getTurnDeadline().toEpochMilli());
        data.put("result", room.getResult() == null ? null : room.getResult().name());
        data.put("winnerId", room.getWinnerId());
        data.put("moves", room.getMoves().stream().map(move -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("seq", move.getSeq());
            item.put("x", move.getX());
            item.put("y", move.getY());
            item.put("playerId", move.getPlayerId());
            return item;
        }).toList());
        data.put("chats", chatService.recent(room.getMatchId()).stream().map(message -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("senderId", message.getSenderId());
            item.put("text", message.getContent());
            item.put("at", message.getCreatedAt().toString());
            return item;
        }).toList());
        afterCommit(() -> sessionRegistry.send(userId, "STATE", data));
    }

    private void broadcastMove(GameRoom room, int seq, int x, int y, Long playerId,
                               Stone nextTurn, Instant deadline) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("matchId", room.getMatchId());
        data.put("seq", seq);
        data.put("x", x);
        data.put("y", y);
        data.put("playerId", playerId);
        data.put("nextTurn", nextTurn == null ? null : nextTurn.name());
        data.put("deadline", deadline == null ? null : deadline.toEpochMilli());
        afterCommit(() -> {
            sessionRegistry.send(room.getBlackId(), "MOVE", data);
            sessionRegistry.send(room.getWhiteId(), "MOVE", data);
        });
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
