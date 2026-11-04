package com.example.gomoku.ws;

import com.example.gomoku.game.GameMatch;
import com.example.gomoku.game.GameMatchRepository;
import com.example.gomoku.game.GameMatchStatus;
import com.example.gomoku.game.GameResult;
import com.example.gomoku.game.MoveRepository;
import com.example.gomoku.chat.ChatRepository;
import com.example.gomoku.match.MatchmakingService;
import com.example.gomoku.user.User;
import com.example.gomoku.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "gomoku.scheduling.enabled=false")
class GameWebSocketIntegrationTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" content=\"([^\"]+)\"");
    private static final Pattern TICKET = Pattern.compile("data-ws-ticket=\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameMatchRepository gameMatchRepository;

    @Autowired
    private MoveRepository moveRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MatchmakingService matchmakingService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User black;
    private User white;
    private Long matchId;
    private WebSocketSession blackSession;
    private WebSocketSession whiteSession;

    @BeforeEach
    void setUp() {
        String suffix = Long.toString(System.nanoTime(), 36);
        black = createUser("ws_black_" + suffix);
        white = createUser("ws_white_" + suffix);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (blackSession != null && blackSession.isOpen()) {
            blackSession.close();
        }
        if (whiteSession != null && whiteSession.isOpen()) {
            whiteSession.close();
        }
        if (matchId != null) {
            chatRepository.deleteAll(chatRepository.findTop50ByMatchIdOrderByIdDesc(matchId));
            moveRepository.deleteAll(moveRepository.findByMatchIdOrderBySeqAsc(matchId));
            gameMatchRepository.deleteById(matchId);
        }
        if (black != null) {
            userRepository.deleteById(black.getId());
        }
        if (white != null) {
            userRepository.deleteById(white.getId());
        }
    }

    @Test
    void twoPlayersMatchChatAndFinishGame() throws Exception {
        CookieManager blackCookies = new CookieManager();
        CookieManager whiteCookies = new CookieManager();
        String blackTicket = login(blackCookies, black.getUsername());
        String whiteTicket = login(whiteCookies, white.getUsername());

        TestHandler blackHandler = new TestHandler();
        TestHandler whiteHandler = new TestHandler();
        blackSession = connect(blackTicket, blackHandler);
        whiteSession = connect(whiteTicket, whiteHandler);

        send(blackSession, "QUEUE_JOIN", Map.of());
        send(whiteSession, "QUEUE_JOIN", Map.of());
        blackHandler.await("QUEUED", Duration.ofSeconds(5));
        whiteHandler.await("QUEUED", Duration.ofSeconds(5));

        matchmakingService.pair();

        JsonNode blackMatch = blackHandler.await("MATCH_FOUND", Duration.ofSeconds(5));
        JsonNode whiteMatch = whiteHandler.await("MATCH_FOUND", Duration.ofSeconds(5));
        matchId = blackMatch.path("data").path("matchId").asLong();
        assertThat(matchId).isPositive();
        assertThat(whiteMatch.path("data").path("matchId").asLong()).isEqualTo(matchId);

        String blackColor = blackMatch.path("data").path("color").asText();
        String whiteColor = whiteMatch.path("data").path("color").asText();
        assertThat(blackColor).isNotEqualTo(whiteColor);

        boolean blackIsFirst = "BLACK".equals(blackColor);
        WebSocketSession firstSession = blackIsFirst ? blackSession : whiteSession;
        WebSocketSession secondSession = blackIsFirst ? whiteSession : blackSession;
        TestHandler firstHandler = blackIsFirst ? blackHandler : whiteHandler;
        TestHandler secondHandler = blackIsFirst ? whiteHandler : blackHandler;

        send(blackSession, "RESUME", Map.of("matchId", matchId));
        send(whiteSession, "RESUME", Map.of("matchId", matchId));
        firstHandler.await("STATE", node -> "PLAYING".equals(node.path("data").path("status").asText()),
                Duration.ofSeconds(5));
        secondHandler.await("STATE", node -> "PLAYING".equals(node.path("data").path("status").asText()),
                Duration.ofSeconds(5));

        send(blackSession, "CHAT", Map.of("matchId", matchId, "text", "hello"));
        JsonNode chatFromBlack = blackHandler.await("CHAT", Duration.ofSeconds(5));
        JsonNode chatFromWhite = whiteHandler.await("CHAT", Duration.ofSeconds(5));
        assertThat(chatFromBlack.path("data").path("text").asText()).isEqualTo("hello");
        assertThat(chatFromWhite.path("data").path("text").asText()).isEqualTo("hello");
        assertThat(chatRepository.countByMatchId(matchId)).isEqualTo(1);

        play(firstSession, secondSession, firstHandler, secondHandler);

        JsonNode firstOver = firstHandler.await("GAME_OVER", Duration.ofSeconds(5));
        JsonNode secondOver = secondHandler.await("GAME_OVER", Duration.ofSeconds(5));
        assertThat(firstOver.path("data").path("result").asText()).isEqualTo("BLACK_WIN");
        assertThat(secondOver.path("data").path("result").asText()).isEqualTo("BLACK_WIN");

        GameMatch match = gameMatchRepository.findById(matchId).orElseThrow();
        assertThat(match.getStatus()).isEqualTo(GameMatchStatus.FINISHED);
        assertThat(match.getResult()).isEqualTo(GameResult.BLACK_WIN);
        assertThat(match.getWinnerId()).isEqualTo(match.getBlackId());
        assertThat(moveRepository.findByMatchIdOrderBySeqAsc(matchId)).hasSize(9);
        assertThat(userRepository.findById(match.getBlackId()).orElseThrow().getWins()).isEqualTo(1);
        assertThat(userRepository.findById(match.getWhiteId()).orElseThrow().getLosses()).isEqualTo(1);
    }

    private void play(WebSocketSession firstSession,
                      WebSocketSession secondSession,
                      TestHandler firstHandler,
                      TestHandler secondHandler) throws Exception {
        move(firstSession, firstHandler, secondHandler, 1, 7, 7);
        move(secondSession, firstHandler, secondHandler, 2, 0, 0);
        move(firstSession, firstHandler, secondHandler, 3, 8, 7);
        move(secondSession, firstHandler, secondHandler, 4, 0, 1);
        move(firstSession, firstHandler, secondHandler, 5, 9, 7);
        move(secondSession, firstHandler, secondHandler, 6, 0, 2);
        move(firstSession, firstHandler, secondHandler, 7, 10, 7);
        move(secondSession, firstHandler, secondHandler, 8, 0, 3);
        move(firstSession, firstHandler, secondHandler, 9, 11, 7);
    }

    private void move(WebSocketSession session,
                      TestHandler firstHandler,
                      TestHandler secondHandler,
                      int seq, int x, int y) throws Exception {
        send(session, "MOVE", Map.of("matchId", matchId, "x", x, "y", y, "seq", seq));
        firstHandler.await("MOVE", node -> node.path("data").path("seq").asInt() == seq, Duration.ofSeconds(5));
        secondHandler.await("MOVE", node -> node.path("data").path("seq").asInt() == seq, Duration.ofSeconds(5));
    }

    private void send(WebSocketSession session, String op, Map<String, ?> data) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("op", op, "data", data));
        session.sendMessage(new TextMessage(payload));
    }

    private WebSocketSession connect(String ticket, TestHandler handler) throws Exception {
        String uri = "ws://127.0.0.1:" + port + "/ws/game?ticket=" + encode(ticket);
        return new StandardWebSocketClient()
                .execute(handler, uri)
                .get(5, TimeUnit.SECONDS);
    }

    private String login(CookieManager cookies, String username) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpResponse<String> page = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        Matcher matcher = CSRF.matcher(page.body());
        assertThat(matcher.find()).isTrue();

        String form = "username=" + encode(username)
                + "&password=" + encode("gomoku123")
                + "&_csrf=" + encode(matcher.group(1));
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.body()).contains("准备开局");

        Matcher ticket = TICKET.matcher(response.body());
        assertThat(ticket.find()).isTrue();
        return ticket.group(1);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setNickname(username);
        user.setPasswordHash(passwordEncoder.encode("gomoku123"));
        user.setRating(1200);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private class TestHandler extends TextWebSocketHandler {

        private final BlockingQueue<JsonNode> messages = new LinkedBlockingQueue<>();

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            messages.add(objectMapper.readTree(message.getPayload()));
        }

        JsonNode await(String op, Duration timeout) throws InterruptedException {
            return await(op, node -> true, timeout);
        }

        JsonNode await(String op, Predicate<JsonNode> predicate, Duration timeout) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                JsonNode message = messages.poll(200, TimeUnit.MILLISECONDS);
                if (message == null) {
                    continue;
                }
                if (op.equals(message.path("op").asText()) && predicate.test(message)) {
                    return message;
                }
            }
            throw new AssertionError("timeout waiting for " + op);
        }
    }
}
