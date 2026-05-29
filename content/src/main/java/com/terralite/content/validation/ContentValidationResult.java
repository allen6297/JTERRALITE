package com.terralite.content.validation;

import java.util.List;
import java.util.Objects;

public record ContentValidationResult(List<ContentValidationIssue> issues) {
    public ContentValidationResult {
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public static ContentValidationResult valid() {
        return new ContentValidationResult(List.of());
    }

    public boolean isValid() {
        return issues.isEmpty();
    }

    public void requireValid() {
        if (!isValid()) {
            throw new ContentValidationException(this);
        }
    }

    public String summary() {
        if (isValid()) {
            return "Content validation passed";
        }
        return "Content validation failed with " + issues.size() + " issue(s)";
    }
}
