package com.example.gomoku.rating;

import com.example.gomoku.game.GameResult;
import org.springframework.stereotype.Component;

@Component
public class EloRatingService implements RatingService {

    private static final int FLOOR = 100;
    private static final int K_NEW = 32;
    private static final int K_ESTABLISHED = 16;
    private static final int ESTABLISHED_GAMES = 30;

    @Override
    public RatingChange calculate(int blackRating,
                                  int whiteRating,
                                  int blackGames,
                                  int whiteGames,
                                  GameResult result) {
        if (result == GameResult.ABORT) {
            return new RatingChange(0, 0, blackRating, whiteRating);
        }

        double expectedBlack = 1.0 / (1.0 + Math.pow(10.0, (whiteRating - blackRating) / 400.0));
        double scoreBlack = switch (result) {
            case BLACK_WIN -> 1.0;
            case WHITE_WIN -> 0.0;
            case DRAW -> 0.5;
            case ABORT -> 0.0;
        };

        int kBlack = blackGames < ESTABLISHED_GAMES ? K_NEW : K_ESTABLISHED;
        int kWhite = whiteGames < ESTABLISHED_GAMES ? K_NEW : K_ESTABLISHED;

        int deltaBlack = (int) Math.round(kBlack * (scoreBlack - expectedBlack));
        int deltaWhite = (int) Math.round(kWhite * (expectedBlack - scoreBlack));

        int newBlack = Math.max(FLOOR, blackRating + deltaBlack);
        int newWhite = Math.max(FLOOR, whiteRating + deltaWhite);

        return new RatingChange(newBlack - blackRating, newWhite - whiteRating, newBlack, newWhite);
    }
}
