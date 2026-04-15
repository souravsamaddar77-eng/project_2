package com.collegequiz.model;

import java.util.List;

public record Question(int id, String prompt, List<String> options, int correctIndex) {
}
