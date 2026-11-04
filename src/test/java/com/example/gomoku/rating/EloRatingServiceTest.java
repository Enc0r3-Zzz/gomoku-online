package com.example.gomoku.rating;

import com.example.gomoku.game.GameResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EloRatingServiceTest {

    private final EloRatingService service = new EloRatingService();

    @Test
    void equalRatingsGiveSixteenPoints() {
        RatingService.RatingChange change = service.calculate(1200, 1200, 0, 0, GameResult.BLACK_WIN);

        assertThat(change.deltaBlack()).isEqualTo(16);
        assertThat(change.deltaWhite()).isEqualTo(-16);
        assertThat(change.newBlack()).isEqualTo(1216);
        assertThat(change.newWhite()).isEqualTo(1184);
    }

    @Test
    void drawBetweenEqualsChangesNothing() {
        RatingService.RatingChange change = service.calculate(1200, 1200, 0, 0, GameResult.DRAW);

        assertThat(change.deltaBlack()).isZero();
        assertThat(change.deltaWhite()).isZero();
    }

    @Test
    void establishedPlayersUseLowerKFactor() {
        RatingService.RatingChange change = service.calculate(1200, 1200, 40, 40, GameResult.BLACK_WIN);

        assertThat(change.deltaBlack()).isEqualTo(8);
        assertThat(change.deltaWhite()).isEqualTo(-8);
    }

    @Test
    void underdogGainsMoreThanFavorite() {
        RatingService.RatingChange change = service.calculate(1000, 1400, 0, 0, GameResult.BLACK_WIN);

        assertThat(change.deltaBlack()).isGreaterThan(16);
        assertThat(change.deltaWhite()).isLessThan(-16);
    }

    @Test
    void ratingNeverDropsBelowFloor() {
        RatingService.RatingChange change = service.calculate(100, 2000, 40, 40, GameResult.WHITE_WIN);

        assertThat(change.newBlack()).isEqualTo(100);
        assertThat(change.deltaBlack()).isZero();
    }

    @Test
    void abortDoesNotChangeRatings() {
        RatingService.RatingChange change = service.calculate(1300, 1100, 10, 10, GameResult.ABORT);

        assertThat(change.deltaBlack()).isZero();
        assertThat(change.deltaWhite()).isZero();
        assertThat(change.newBlack()).isEqualTo(1300);
        assertThat(change.newWhite()).isEqualTo(1100);
    }
}
