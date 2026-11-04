package com.example.gomoku.game;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {

    private final Long matchId;
    private final Long blackId;
    private final Long whiteId;
    private final Board board = new Board();
    private final List<Move> moves = new ArrayList<>();
    private final Set<Long> connected = ConcurrentHashMap.newKeySet();
    private final Object lock = new Object();

    private int seq;
    private Stone turn = Stone.BLACK;
    private GameMatchStatus status = GameMatchStatus.WAITING;
    private GameResult result;
    private Long winnerId;
    private Instant turnDeadline;

    public GameRoom(Long matchId, Long blackId, Long whiteId) {
        this.matchId = matchId;
        this.blackId = blackId;
        this.whiteId = whiteId;
    }

    public boolean isPlayer(Long userId) {
        return blackId.equals(userId) || whiteId.equals(userId);
    }

    public Stone colorOf(Long userId) {
        if (blackId.equals(userId)) {
            return Stone.BLACK;
        }
        if (whiteId.equals(userId)) {
            return Stone.WHITE;
        }
        throw new IllegalArgumentException("不是本局玩家: " + userId);
    }

    public Long opponentOf(Long userId) {
        if (blackId.equals(userId)) {
            return whiteId;
        }
        if (whiteId.equals(userId)) {
            return blackId;
        }
        throw new IllegalArgumentException("不是本局玩家: " + userId);
    }

    public Long playerOf(Stone color) {
        return color == Stone.BLACK ? blackId : whiteId;
    }

    public void connect(Long userId) {
        connected.add(userId);
    }

    public void disconnect(Long userId) {
        connected.remove(userId);
    }

    public boolean bothConnected() {
        return connected.contains(blackId) && connected.contains(whiteId);
    }

    public Long getMatchId() {
        return matchId;
    }

    public Long getBlackId() {
        return blackId;
    }

    public Long getWhiteId() {
        return whiteId;
    }

    public Board getBoard() {
        return board;
    }

    public List<Move> getMoves() {
        return moves;
    }

    public Object getLock() {
        return lock;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public Stone getTurn() {
        return turn;
    }

    public void setTurn(Stone turn) {
        this.turn = turn;
    }

    public GameMatchStatus getStatus() {
        return status;
    }

    public void setStatus(GameMatchStatus status) {
        this.status = status;
    }

    public GameResult getResult() {
        return result;
    }

    public void setResult(GameResult result) {
        this.result = result;
    }

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
    }

    public Instant getTurnDeadline() {
        return turnDeadline;
    }

    public void setTurnDeadline(Instant turnDeadline) {
        this.turnDeadline = turnDeadline;
    }
}
