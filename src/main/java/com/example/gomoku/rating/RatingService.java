package com.example.gomoku.rating;

import com.example.gomoku.game.GameResult;

public interface RatingService {

    RatingChange calculate(int blackRating,
                           int whiteRating,
                           int blackGames,
                           int whiteGames,
                           GameResult result);

    record RatingChange(int deltaBlack, int deltaWhite, int newBlack, int newWhite) {
    }
}
