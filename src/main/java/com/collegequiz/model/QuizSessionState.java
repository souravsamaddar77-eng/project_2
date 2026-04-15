package com.collegequiz.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class QuizSessionState {
    private QuizAttempt activeAttempt;
    private final Deque<Set<Integer>> recentAttemptQuestionIds = new ArrayDeque<>();

    public QuizAttempt getActiveAttempt() {
        return activeAttempt;
    }

    public void setActiveAttempt(QuizAttempt activeAttempt) {
        this.activeAttempt = activeAttempt;
    }

    public void addRecentAttempt(Set<Integer> questionIds, int maxAttemptsToKeep) {
        recentAttemptQuestionIds.addLast(new HashSet<>(questionIds));
        while (recentAttemptQuestionIds.size() > maxAttemptsToKeep) {
            recentAttemptQuestionIds.removeFirst();
        }
    }

    public Set<Integer> unionOfRecentQuestionIds() {
        Set<Integer> union = new HashSet<>();
        for (Set<Integer> attemptIds : recentAttemptQuestionIds) {
            union.addAll(attemptIds);
        }
        return union;
    }
}
