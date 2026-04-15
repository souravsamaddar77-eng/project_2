const scoreValue = document.getElementById("scoreValue");
const progressValue = document.getElementById("progressValue");
const statusLine = document.getElementById("statusLine");
const questionText = document.getElementById("questionText");
const optionsGrid = document.getElementById("optionsGrid");
const skipBtn = document.getElementById("skipBtn");
const newAttemptBtn = document.getElementById("newAttemptBtn");

const isQuizPage = Boolean(
    scoreValue
    && progressValue
    && statusLine
    && questionText
    && optionsGrid
    && skipBtn
    && newAttemptBtn
);

let currentState = null;
let currentQuestion = null;
let isRequestInFlight = false;

async function apiRequest(path, method = "GET", body) {
    const options = {
        method,
        headers: {
            "Content-Type": "application/json"
        }
    };

    if (body !== undefined) {
        options.body = JSON.stringify(body);
    }

    const response = await fetch(path, options);
    if (!response.ok) {
        throw new Error("Request failed with status " + response.status);
    }

    return response.json();
}

function setBusyState(busy) {
    if (!isQuizPage) {
        return;
    }

    isRequestInFlight = busy;
    skipBtn.disabled = busy || (currentState ? currentState.attemptFinished : false);

    const optionButtons = document.querySelectorAll(".option-btn");
    optionButtons.forEach((button) => {
        button.disabled = busy;
    });
}

function updateHeaderScore(state) {
    if (!isQuizPage) {
        return;
    }

    scoreValue.textContent = "Score " + state.score + " / " + state.totalQuestions;

    if (state.attemptFinished) {
        progressValue.textContent = "Attempt Complete";
    } else {
        progressValue.textContent = "Question " + state.questionNumber + " / " + state.totalQuestions;
    }
}

function renderOptions(options) {
    if (!isQuizPage) {
        return;
    }

    optionsGrid.innerHTML = "";

    options.forEach((optionText, index) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "option-btn";
        button.textContent = optionText;
        button.style.setProperty("--i", String(index));
        button.addEventListener("click", () => submitAnswer(index, button));
        optionsGrid.appendChild(button);
    });
}

function renderFinishedState(state) {
    if (!isQuizPage) {
        return;
    }

    questionText.textContent = "Attempt Complete";
    optionsGrid.innerHTML = "";

    const summary = document.createElement("div");
    summary.className = "final-card";
    summary.innerHTML = "Final Score: <strong>" + state.score + " / " + state.totalQuestions + "</strong><br>"
        + "Answered: " + state.answered + " | Skipped: " + (state.totalQuestions - state.answered) + "<br>"
        + "Start a new attempt for a fresh set of questions.";

    optionsGrid.appendChild(summary);
    newAttemptBtn.classList.remove("hidden");
}

function renderState(state) {
    if (!isQuizPage) {
        return;
    }

    currentState = state;
    currentQuestion = state.question;

    updateHeaderScore(state);
    statusLine.textContent = state.message || "";

    if (state.attemptFinished || !state.question) {
        renderFinishedState(state);
        skipBtn.disabled = true;
        return;
    }

    newAttemptBtn.classList.add("hidden");
    questionText.textContent = state.question.prompt;
    renderOptions(state.question.options);
    skipBtn.disabled = false;
}

async function startAttempt() {
    if (!isQuizPage) {
        return;
    }

    if (isRequestInFlight) {
        return;
    }

    setBusyState(true);
    statusLine.textContent = "Loading a fresh set of questions...";

    try {
        const state = await apiRequest("/api/quiz/start", "POST");
        renderState(state);
    } catch (error) {
        statusLine.textContent = "Could not start the quiz. Please try again.";
        questionText.textContent = "Connection issue.";
        optionsGrid.innerHTML = "";
    } finally {
        setBusyState(false);
    }
}

async function refreshQuestion() {
    if (!isQuizPage) {
        return;
    }

    const state = await apiRequest("/api/quiz/question", "GET");
    renderState(state);
}

async function submitAnswer(selectedIndex, clickedButton) {
    if (!isQuizPage) {
        return;
    }

    if (isRequestInFlight || !currentQuestion) {
        return;
    }

    setBusyState(true);

    try {
        const result = await apiRequest("/api/quiz/answer", "POST", { selectedIndex });
        const buttons = document.querySelectorAll(".option-btn");

        if (result.correct) {
            clickedButton.classList.add("correct");
            statusLine.textContent = "Correct. " + result.message;
        } else {
            clickedButton.classList.add("incorrect");
            const correctText = currentQuestion.options[result.correctIndex];
            statusLine.textContent = "Incorrect. Correct answer: " + correctText;
        }

        setTimeout(async () => {
            try {
                await refreshQuestion();
            } catch (error) {
                statusLine.textContent = "Could not load next question.";
            } finally {
                setBusyState(false);
            }
        }, result.attemptFinished ? 550 : 420);
    } catch (error) {
        setBusyState(false);
        statusLine.textContent = "Answer could not be submitted. Try again.";
    }
}

async function skipCurrentQuestion() {
    if (!isQuizPage) {
        return;
    }

    if (isRequestInFlight) {
        return;
    }

    setBusyState(true);
    statusLine.textContent = "Skipping this question...";

    try {
        const state = await apiRequest("/api/quiz/skip", "POST");
        renderState(state);
    } catch (error) {
        statusLine.textContent = "Skip action failed. Try once more.";
    } finally {
        setBusyState(false);
    }
}

if (isQuizPage) {
    skipBtn.addEventListener("click", skipCurrentQuestion);
    newAttemptBtn.addEventListener("click", startAttempt);
    document.addEventListener("DOMContentLoaded", startAttempt);
}
