package com.craftinginterpreters.lox;

public class RuntimeError extends RuntimeException {
    final int line;

    RuntimeError(int line, String message) {
        super(message);
        this.line = line;
    }

    RuntimeError(Token token, String message) {
        this(token.line(), message);
    }
}
