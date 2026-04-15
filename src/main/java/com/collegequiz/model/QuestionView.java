package com.collegequiz.model;

import java.util.List;

public record QuestionView(int id, String prompt, List<String> options) {
}
