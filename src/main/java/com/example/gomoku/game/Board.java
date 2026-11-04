package com.example.gomoku.game;

import java.util.ArrayList;
import java.util.List;

/**
 * 15x15 棋盘，坐标 x=列、y=行，范围 0..14。
 * 无禁手（自由五子棋），五连及以上判胜。
 */
public class Board {

    public static final int SIZE = 15;

    private final Stone[][] cells = new Stone[SIZE][SIZE];

    public Board() {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                cells[y][x] = Stone.EMPTY;
            }
        }
    }

    public static boolean inBounds(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    public Stone get(int x, int y) {
        if (!inBounds(x, y)) {
            throw new IllegalArgumentException("坐标越界: " + x + "," + y);
        }
        return cells[y][x];
    }

    public boolean isEmpty(int x, int y) {
        return get(x, y) == Stone.EMPTY;
    }

    public void place(int x, int y, Stone stone) {
        if (!inBounds(x, y)) {
            throw new IllegalArgumentException("坐标越界: " + x + "," + y);
        }
        if (stone == Stone.EMPTY) {
            throw new IllegalArgumentException("不能放置空子");
        }
        if (cells[y][x] != Stone.EMPTY) {
            throw new IllegalStateException("该位置已有棋子");
        }
        cells[y][x] = stone;
    }

    public boolean isFull() {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (cells[y][x] == Stone.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean hasFive(int x, int y) {
        Stone stone = get(x, y);
        if (stone == Stone.EMPTY) {
            return false;
        }
        return countDirection(x, y, 1, 0) >= 5
                || countDirection(x, y, 0, 1) >= 5
                || countDirection(x, y, 1, 1) >= 5
                || countDirection(x, y, 1, -1) >= 5;
    }

    /**
     * 返回经过 (x,y) 的连续五子（或更多）坐标，按方向从一端到另一端排序。
     * 未成五时返回空列表。
     */
    public List<int[]> winningLine(int x, int y) {
        Stone stone = get(x, y);
        if (stone == Stone.EMPTY) {
            return List.of();
        }
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] direction : directions) {
            int dx = direction[0];
            int dy = direction[1];
            int startX = x;
            int startY = y;
            while (inBounds(startX - dx, startY - dy)
                    && get(startX - dx, startY - dy) == stone) {
                startX -= dx;
                startY -= dy;
            }
            List<int[]> line = new ArrayList<>();
            int currentX = startX;
            int currentY = startY;
            while (inBounds(currentX, currentY) && get(currentX, currentY) == stone) {
                line.add(new int[]{currentX, currentY});
                currentX += dx;
                currentY += dy;
            }
            if (line.size() >= 5) {
                return line;
            }
        }
        return List.of();
    }

    private int countDirection(int x, int y, int dx, int dy) {
        Stone stone = get(x, y);
        int count = 1;
        count += countSide(x, y, dx, dy, stone);
        count += countSide(x, y, -dx, -dy, stone);
        return count;
    }

    private int countSide(int x, int y, int dx, int dy, Stone stone) {
        int count = 0;
        int nx = x + dx;
        int ny = y + dy;
        while (inBounds(nx, ny) && get(nx, ny) == stone) {
            count++;
            nx += dx;
            ny += dy;
        }
        return count;
    }
}
