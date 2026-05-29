package com.terralite.content.validation;

import java.util.Objects;

public record ContentValidationIssue(String code, String message) {
    public ContentValidationIssue {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Validation issue code cannot be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Validation issue message cannot be blank");
        }
    }

    public static ContentValidationIssue of(String code, String message) {
        return new ContentValidationIssue(
                Objects.requireNonNull(code, "code"),
                Objects.requireNonNull(message, "message")
        );
    }
}
