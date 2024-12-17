package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.craftinginterpreters.lox.TokenType.*;

public class Parser {

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Optional<Expr> parse() {
        try {
            return Optional.of(expression());
        } catch (ParseError error) {
            return Optional.empty();
        }
    }

    private Expr expression() {
        return equality();
    }

    private Expr equality() {
        return leftAssociativeBinarySeries(this::comparison, BANG_EQUAL, EQUAL_EQUAL);
    }

    private Expr leftAssociativeBinarySeries(Supplier<Expr> operandFactory, TokenType... operatorTypes) {
        var expression = operandFactory.get();

        while (match(operatorTypes)) {
            var operator = previous();
            var right = operandFactory.get();
            expression = new Expr.Binary(expression, operator, right);
        }

        return expression;
    }

    private Expr comparison() {
        return leftAssociativeBinarySeries(this::term, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    private Expr term() {
        return leftAssociativeBinarySeries(this::factor, MINUS, PLUS);
    }

    private Expr factor() {
        return leftAssociativeBinarySeries(this::unary, SLASH, STAR);
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            var operator = previous();
            var right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal());
        }

        if (match(LEFT_PAREN)) {
            var expression = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expression);
        }

        throw error(peek(), "Expect expression");
    }

    // TODO: maybe not advance for better readability?
    private boolean match(TokenType... types) {
        for (var type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        var terminalTypes = Set.of(CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN);

        while (!isAtEnd()) {
            if (previous().type() == SEMICOLON) {
                return;
            }

            if (terminalTypes.contains(peek().type())) {
                return;
            }

            advance();
        }
    }

    private static class ParseError extends RuntimeException {
    }
}
