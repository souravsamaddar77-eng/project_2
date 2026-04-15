package com.collegequiz.dto;

public record AnswerResponse(
        boolean correct,
        int correctIndex,
        int score,
        int answered,
        String message,
        boolean attemptFinished
) {
}
