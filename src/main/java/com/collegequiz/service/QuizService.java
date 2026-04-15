package com.collegequiz.service;

import com.collegequiz.dto.AnswerResponse;
import com.collegequiz.dto.QuizStateResponse;
import com.collegequiz.model.Question;
import com.collegequiz.model.QuestionView;
import com.collegequiz.model.QuizAttempt;
import com.collegequiz.model.QuizSessionState;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizService {
    private static final String SESSION_KEY = "COLLEGE_QUIZ_STATE";
    private static final int DEFAULT_ATTEMPT_SIZE = 10;
    private static final int RECENT_ATTEMPTS_TO_AVOID = 2;

    private final List<Question> questionBank;
    private final Map<Integer, Question> questionsById;

    public QuizService() {
        this.questionBank = List.copyOf(buildQuestionBank());
        this.questionsById = questionBank.stream()
                .collect(Collectors.toMap(Question::id, Function.identity()));
    }

    public synchronized QuizStateResponse startNewAttempt(HttpSession session) {
        return startNewAttempt(session, DEFAULT_ATTEMPT_SIZE);
    }

    public synchronized QuizStateResponse startNewAttempt(HttpSession session, int requestedSize) {
        QuizSessionState state = getOrCreateState(session);
        int size = Math.max(1, Math.min(requestedSize, questionBank.size()));
        List<Integer> selectedIds = selectQuestionIds(state, size);

        QuizAttempt attempt = new QuizAttempt(selectedIds);
        state.setActiveAttempt(attempt);
        state.addRecentAttempt(Set.copyOf(selectedIds), RECENT_ATTEMPTS_TO_AVOID);

        return buildQuizStateResponse(attempt, "New attempt started. Stay focused.");
    }

    public synchronized QuizStateResponse getCurrentQuestion(HttpSession session) {
        QuizSessionState state = getOrCreateState(session);
        if (state.getActiveAttempt() == null) {
            return startNewAttempt(session);
        }
        return buildQuizStateResponse(state.getActiveAttempt(), "Choose the best answer.");
    }

    public synchronized AnswerResponse answerQuestion(HttpSession session, int selectedIndex) {
        QuizSessionState state = getOrCreateState(session);
        if (state.getActiveAttempt() == null) {
            startNewAttempt(session);
        }

        QuizAttempt attempt = getOrCreateState(session).getActiveAttempt();
        if (attempt.isFinished()) {
            return new AnswerResponse(false, -1, attempt.getScore(), attempt.getAnswered(),
                    "This attempt is already complete. Start a new one.", true);
        }

        Question currentQuestion = questionsById.get(attempt.currentQuestionId());
        boolean validSelection = selectedIndex >= 0 && selectedIndex < currentQuestion.options().size();
        boolean correct = validSelection && selectedIndex == currentQuestion.correctIndex();

        if (correct) {
            attempt.incrementScore();
        }

        attempt.incrementAnswered();
        attempt.advance();

        String message = correct ? "Correct answer." : "Not quite. Keep moving.";
        if (attempt.isFinished()) {
            message = "Attempt complete.";
        }

        return new AnswerResponse(
                correct,
                currentQuestion.correctIndex(),
                attempt.getScore(),
                attempt.getAnswered(),
                message,
                attempt.isFinished()
        );
    }

    public synchronized QuizStateResponse skipQuestion(HttpSession session) {
        QuizSessionState state = getOrCreateState(session);
        if (state.getActiveAttempt() == null) {
            return startNewAttempt(session);
        }

        QuizAttempt attempt = state.getActiveAttempt();
        if (!attempt.isFinished()) {
            attempt.advance();
        }

        String message = attempt.isFinished() ? "Attempt complete." : "Question skipped.";
        return buildQuizStateResponse(attempt, message);
    }

    private QuizSessionState getOrCreateState(HttpSession session) {
        Object raw = session.getAttribute(SESSION_KEY);
        if (raw instanceof QuizSessionState existingState) {
            return existingState;
        }

        QuizSessionState state = new QuizSessionState();
        session.setAttribute(SESSION_KEY, state);
        return state;
    }

    private QuizStateResponse buildQuizStateResponse(QuizAttempt attempt, String message) {
        if (attempt == null) {
            return new QuizStateResponse("College Quiz", true, 0, 0, 0, 0, null, "Start a new attempt.");
        }

        if (attempt.isFinished()) {
            return new QuizStateResponse(
                    "College Quiz",
                    true,
                    attempt.getScore(),
                    attempt.getAnswered(),
                    attempt.getTotalQuestions(),
                    attempt.getTotalQuestions(),
                    null,
                    "Attempt complete. Final score: " + attempt.getScore() + "/" + attempt.getTotalQuestions()
            );
        }

        Question current = questionsById.get(attempt.currentQuestionId());
        QuestionView questionView = new QuestionView(current.id(), current.prompt(), current.options());

        return new QuizStateResponse(
                "College Quiz",
                false,
                attempt.getScore(),
                attempt.getAnswered(),
                attempt.getCurrentIndex() + 1,
                attempt.getTotalQuestions(),
                questionView,
                message
        );
    }

    private List<Integer> selectQuestionIds(QuizSessionState state, int size) {
        Set<Integer> excluded = state.unionOfRecentQuestionIds();

        List<Integer> eligibleIds = questionBank.stream()
                .map(Question::id)
                .filter(id -> !excluded.contains(id))
                .collect(Collectors.toCollection(ArrayList::new));

        if (eligibleIds.size() < size) {
            eligibleIds = questionBank.stream()
                    .map(Question::id)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        Collections.shuffle(eligibleIds);
        return new ArrayList<>(eligibleIds.subList(0, size));
    }

    private List<Question> buildQuestionBank() {
        return List.of(
                new Question(1, "If f(x)=x^3-4x, what is f'(2)?", List.of("4", "8", "10", "12"), 1),
                new Question(2, "What is the determinant of [[1,2],[3,4]]?", List.of("2", "-1", "-2", "4"), 2),
                new Question(3, "The limit of (1 + 1/n)^n as n approaches infinity is:", List.of("1", "e", "Infinity", "0"), 1),
                new Question(4, "What is the value of integral 2x dx from 0 to 3?", List.of("6", "7", "8", "9"), 3),
                new Question(5, "Expected value of a fair six-sided die is:", List.of("3", "4", "3.5", "2.5"), 2),
                new Question(6, "Newton's second law is represented by:", List.of("F = ma", "E = mc^2", "pV = nRT", "v = u + at"), 0),
                new Question(7, "The SI unit of electrical resistance is:", List.of("Volt", "Ohm", "Watt", "Ampere"), 1),
                new Question(8, "A solution with pH 7 is:", List.of("Acidic", "Basic", "Neutral", "Buffer"), 2),
                new Question(9, "Avogadro's number is approximately:", List.of("6.022 x 10^23", "3.0 x 10^8", "9.8", "1.6 x 10^-19"), 0),
                new Question(10, "The ideal gas equation is:", List.of("V = IR", "F = ma", "Q = mcT", "PV = nRT"), 3),
                new Question(11, "Time complexity of binary search on sorted data is:", List.of("O(n)", "O(log n)", "O(n log n)", "O(1)"), 1),
                new Question(12, "Which data structure follows LIFO order?", List.of("Queue", "Heap", "Stack", "Graph"), 2),
                new Question(13, "Which SQL command permanently removes a table?", List.of("DROP TABLE", "DELETE ROW", "CLEAR", "REMOVE"), 0),
                new Question(14, "Which protocol is reliable and connection-oriented?", List.of("UDP", "TCP", "ICMP", "ARP"), 1),
                new Question(15, "A process waiting for CPU execution is in which state?", List.of("Blocked", "Running", "Terminated", "Ready"), 3),
                new Question(16, "Every recursive function must include a:", List.of("Loop", "Global variable", "Base case", "Pointer"), 2),
                new Question(17, "HTTP status code 404 means:", List.of("Unauthorized", "Not Found", "Server Error", "Created"), 1),
                new Question(18, "In OOP, abstraction means:", List.of("Hiding implementation details", "Copying objects", "Increasing memory use", "Avoiding methods"), 0),
                new Question(19, "Opportunity cost is:", List.of("Total revenue", "Fixed cost", "Next best alternative forgone", "Tax amount"), 2),
                new Question(20, "Price elasticity of demand is calculated as:", List.of("Change in price divided by income", "Income divided by quantity", "Total cost divided by output", "% change in quantity demanded / % change in price"), 3),
                new Question(21, "Standard deviation measures:", List.of("Average value", "Spread around mean", "Skewness only", "Sample size"), 1),
                new Question(22, "The accounting equation is:", List.of("Revenue = Cost + Profit", "Assets = Revenue - Expenses", "Assets = Liabilities + Equity", "Cash = Assets - Debt"), 2),
                new Question(23, "The value of i^4 is:", List.of("1", "-1", "i", "-i"), 0),
                new Question(24, "If two vectors have dot product zero, they are:", List.of("Parallel", "Equal", "Orthogonal", "Collinear"), 2),
                new Question(25, "For ideal projectile motion, maximum range occurs at:", List.of("30 degrees", "45 degrees", "60 degrees", "90 degrees"), 1),
                new Question(26, "Speed of light in vacuum is approximately:", List.of("3.0 x 10^6 m/s", "3.0 x 10^7 m/s", "3.0 x 10^5 km/s", "3.0 x 10^8 m/s"), 3),
                new Question(27, "First law of thermodynamics can be written as:", List.of("Q = mL", "PV = nRT", "Delta U = Q - W", "F = qE"), 2),
                new Question(28, "The molecular formula of benzene is:", List.of("C2H6", "C6H6", "C6H12", "CH4"), 1),
                new Question(29, "Oxidation is defined as:", List.of("Loss of electrons", "Gain of neutrons", "Loss of protons", "Gain of ions"), 0),
                new Question(30, "Molarity is defined as:", List.of("Moles of solvent per liter", "Grams of solute per liter", "Moles of solute per liter of solution", "Moles of solution per kilogram"), 2),
                new Question(31, "Largest eigenvalue of matrix [[2,0],[0,5]] is:", List.of("2", "3", "4", "5"), 3),
                new Question(32, "Derivative of sin(x) is:", List.of("cos(x)", "-cos(x)", "tan(x)", "sec^2(x)"), 0),
                new Question(33, "Integral of 1/x with respect to x is:", List.of("1/(x^2) + C", "ln|x| + C", "e^x + C", "x + C"), 1),
                new Question(34, "If events A and B are independent, then:", List.of("P(A U B) = P(A) + P(B)", "P(A|B) = 0", "P(A n B) = P(A)P(B)", "P(A) = P(B)"), 2),
                new Question(35, "The correlation coefficient r always lies between:", List.of("0 and 1", "-1 and 1", "-10 and 10", "1 and infinity"), 1),
                new Question(36, "If discount rate increases, present value typically:", List.of("Increases", "Stays fixed", "Becomes zero", "Decreases"), 3),
                new Question(37, "Compound interest amount after t years (annual compounding) is:", List.of("A = P(1 + r)^t", "A = Prt", "A = P + rt", "A = P/r"), 0),
                new Question(38, "If marginal cost is below average cost, average cost will:", List.of("Increase", "Decrease", "Stay unchanged", "Become negative"), 1),
                new Question(39, "If MPC = 0.8, Keynesian spending multiplier is:", List.of("2", "4", "5", "8"), 2),
                new Question(40, "A primary key in relational databases must be:", List.of("Sorted", "Encrypted", "Indexed only", "Unique and non-null"), 3),
                new Question(41, "Which is one of Coffman's necessary conditions for deadlock?", List.of("Preemption", "Circular wait", "Infinite memory", "Single thread"), 1),
                new Question(42, "Two nested loops each running n times have complexity:", List.of("O(n)", "O(log n)", "O(n^2)", "O(n^3)"), 2),
                new Question(43, "An IPv4 address has how many bits?", List.of("32", "64", "48", "128"), 0),
                new Question(44, "If p-value < alpha, the correct statistical decision is to:", List.of("Accept null hypothesis", "Reject null hypothesis", "Increase alpha", "Collect no data"), 1),
                new Question(45, "Adding a catalyst to a reaction at equilibrium typically:", List.of("Increases K", "Decreases K", "Sets K to 1", "Does not change K"), 3),
                new Question(46, "Derivative of e^(3x) is:", List.of("e^(3x)", "3x e^(3x)", "3e^(3x)", "e^x"), 2),
                new Question(47, "The rank of an m x n matrix cannot exceed:", List.of("m + n", "m x n", "min(m, n)", "max(m, n) + 1"), 2),
                new Question(48, "For a binary random variable, Shannon entropy is maximum when p =", List.of("0.25", "0.5", "0.75", "1"), 1),
                new Question(49, "For a first-order chemical reaction, half-life is:", List.of("Independent of initial concentration", "Directly proportional to initial concentration", "Always zero", "Dependent on pressure only"), 0),
                new Question(50, "In Java, a method declared in an interface (without default/static/private) is:", List.of("Protected and final", "Package-private", "Private and abstract", "Public and abstract"), 3)
        );
    }
}
