package com.collegequiz.model;

import java.util.List;

public class QuizAttempt {
    private final List<Integer> questionIds;
    private int currentIndex;
    private int score;
    private int answered;

    public QuizAttempt(List<Integer> questionIds) {
        this.questionIds = questionIds;
        this.currentIndex = 0;
        this.score = 0;
        this.answered = 0;
    }

    public List<Integer> getQuestionIds() {
        return questionIds;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getScore() {
        return score;
    }

    public int getAnswered() {
        return answered;
    }

    public int getTotalQuestions() {
        return questionIds.size();
    }

    public boolean isFinished() {
        return currentIndex >= questionIds.size();
    }

    public int currentQuestionId() {
        if (isFinished()) {
            throw new IllegalStateException("No active question in a finished attempt.");
        }
        return questionIds.get(currentIndex);
    }

    public void advance() {
        if (!isFinished()) {
            currentIndex++;
        }
    }

    public void incrementScore() {
        score++;
    }

    public void incrementAnswered() {
        answered++;
    }
}
