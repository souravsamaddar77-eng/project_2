package com.collegequiz.dto;

import com.collegequiz.model.QuestionView;

public record QuizStateResponse(
        String title,
        boolean attemptFinished,
        int score,
        int answered,
        int questionNumber,
        int totalQuestions,
        QuestionView question,
        String message
) {
}
