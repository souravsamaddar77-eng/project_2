package com.collegequiz.controller;

import com.collegequiz.dto.AnswerRequest;
import com.collegequiz.dto.AnswerResponse;
import com.collegequiz.dto.QuizStateResponse;
import com.collegequiz.service.QuizService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/start")
    public QuizStateResponse startAttempt(HttpSession session) {
        return quizService.startNewAttempt(session);
    }

    @GetMapping("/question")
    public QuizStateResponse getQuestion(HttpSession session) {
        return quizService.getCurrentQuestion(session);
    }

    @PostMapping("/answer")
    public AnswerResponse submitAnswer(HttpSession session, @RequestBody(required = false) AnswerRequest request) {
        int selectedIndex = -1;
        if (request != null) {
            Integer candidate = request.selectedIndex();
            if (candidate != null) {
                selectedIndex = candidate;
            }
        }
        return quizService.answerQuestion(session, selectedIndex);
    }

    @PostMapping("/skip")
    public QuizStateResponse skipQuestion(HttpSession session) {
        return quizService.skipQuestion(session);
    }
}
