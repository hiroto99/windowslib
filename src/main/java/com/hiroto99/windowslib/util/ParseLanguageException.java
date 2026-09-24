package com.hiroto99.windowslib.util;

import java.io.Serial;

public class ParseLanguageException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 7656822348558955165L;

    public ParseLanguageException(String message) {
        super(message);
    }
}
