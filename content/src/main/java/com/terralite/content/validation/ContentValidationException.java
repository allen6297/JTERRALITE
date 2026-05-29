package com.terralite.content.validation;

public final class ContentValidationException extends RuntimeException {
    private final ContentValidationResult result;

    public ContentValidationException(ContentValidationResult result) {
        super(result.summary());
        this.result = result;
    }

    public ContentValidationResult result() {
        return result;
    }
}
