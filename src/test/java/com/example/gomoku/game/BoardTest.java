package com.example.gomoku.game;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardTest {

    @Test
    void detectsHorizontalFive() {
        Board board = new Board();
        for (int x = 3; x <= 7; x++) {
            board.place(x, 5, Stone.BLACK);
        }
        assertThat(board.hasFive(3, 5)).isTrue();
        assertThat(board.hasFive(5, 5)).isTrue();
        assertThat(board.hasFive(7, 5)).isTrue();
    }

    @Test
    void detectsVerticalFive() {
        Board board = new Board();
        for (int y = 2; y <= 6; y++) {
            board.place(8, y, Stone.WHITE);
        }
        assertThat(board.hasFive(8, 2)).isTrue();
        assertThat(board.hasFive(8, 4)).isTrue();
    }

    @Test
    void detectsDiagonalFive() {
        Board board = new Board();
        for (int i = 0; i < 5; i++) {
            board.place(3 + i, 3 + i, Stone.BLACK);
        }
        assertThat(board.hasFive(5, 5)).isTrue();
    }

    @Test
    void detectsAntiDiagonalFive() {
        Board board = new Board();
        for (int i = 0; i < 5; i++) {
            board.place(4 + i, 10 - i, Stone.WHITE);
        }
        assertThat(board.hasFive(6, 8)).isTrue();
    }

    @Test
    void fourIsNotAWin() {
        Board board = new Board();
        for (int x = 0; x < 4; x++) {
            board.place(x, 0, Stone.BLACK);
        }
        assertThat(board.hasFive(1, 0)).isFalse();
    }

    @Test
    void sixInARowStillWins() {
        Board board = new Board();
        for (int x = 0; x < 6; x++) {
            board.place(x, 1, Stone.BLACK);
        }
        assertThat(board.hasFive(3, 1)).isTrue();
    }

    @Test
    void winAtBoardEdges() {
        Board board = new Board();
        for (int y = 10; y < 15; y++) {
            board.place(14, y, Stone.BLACK);
        }
        assertThat(board.hasFive(14, 12)).isTrue();
    }

    @Test
    void rejectsOccupiedAndOutOfBounds() {
        Board board = new Board();
        board.place(7, 7, Stone.BLACK);
        assertThatThrownBy(() -> board.place(7, 7, Stone.WHITE))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> board.place(15, 0, Stone.BLACK))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> board.place(0, -1, Stone.BLACK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reportsFullBoard() {
        Board board = new Board();
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                board.place(x, y, Stone.BLACK);
            }
        }
        assertThat(board.isFull()).isTrue();
    }

    @Test
    void emptyCellHasNoFive() {
        Board board = new Board();
        assertThat(board.hasFive(0, 0)).isFalse();
    }

    @Test
    void returnsWinningLineInOrder() {
        Board board = new Board();
        for (int x = 3; x <= 7; x++) {
            board.place(x, 5, Stone.BLACK);
        }

        var line = board.winningLine(5, 5);

        assertThat(line).hasSize(5);
        assertThat(line.get(0)).containsExactly(3, 5);
        assertThat(line.get(4)).containsExactly(7, 5);
    }

    @Test
    void returnsWinningLineForDiagonal() {
        Board board = new Board();
        for (int i = 0; i < 5; i++) {
            board.place(3 + i, 3 + i, Stone.WHITE);
        }

        var line = board.winningLine(5, 5);

        assertThat(line).hasSize(5);
        assertThat(line.get(0)).containsExactly(3, 3);
        assertThat(line.get(4)).containsExactly(7, 7);
    }

    @Test
    void returnsEmptyWinningLineWhenNoWin() {
        Board board = new Board();
        for (int x = 0; x < 4; x++) {
            board.place(x, 0, Stone.BLACK);
        }

        assertThat(board.winningLine(1, 0)).isEmpty();
    }
}
