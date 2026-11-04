package com.example.gomoku.game;

public enum Stone {
    EMPTY,
    BLACK,
    WHITE;

    public Stone opposite() {
        return switch (this) {
            case BLACK -> WHITE;
            case WHITE -> BLACK;
            case EMPTY -> EMPTY;
        };
    }
}
