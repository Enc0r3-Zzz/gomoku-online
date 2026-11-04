package com.example.gomoku.ws;

import com.fasterxml.jackson.databind.JsonNode;

public record WsMessage(String op, Long seq, JsonNode data) {
}
