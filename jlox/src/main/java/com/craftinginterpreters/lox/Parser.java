package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.craftinginterpreters.lox.TokenType.*;

// Grammar is as follows:
// program             -> declaration* EOF
// declaration         -> varDeclaration | statement
// varDeclaration      -> "var" IDENTIFIER ( "=" expression )? ";"
// statement           -> expressionStatement | printStatement | block
// block               -> "{" declaration "}"
// expressionStatement -> expression ";"
// printStatement      -> "print" expression ";"
// expression          -> assignment
// assignment          -> IDENTIFIER "=" assignment | ternary
// ternary             -> equality ( ? equality ( ? equality : equality )* : equality )*
// equality            -> comparison ( ( "!==" | "==" ) comparison )*
// comparison          -> term ( ( ">" | ">=" | "<" | "<=" ) term )*
// term                -> factor ( ( "-" | "+" ) factor )*
// factor              -> unary ( ( "/" | "*" ) unary )*
// unary               -> ( "!" | "-" ) unary | primary
// primary             -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER

public class Parser {

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        var statements = new ArrayList<Stmt>();
        while (!isAtEnd()) {
            var statement = declaration();
            if (statement != null) {
                statements.add(statement);
            }
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            return match(VAR) ? varDeclaration() : statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        var name = consume(IDENTIFIER, "Expect variable name");
        var initializer = match(EQUAL) ? expression() : null;
        consume(SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Variable(name, initializer);
    }

    private Stmt statement() {
        if (match(PRINT)) {
            return printStatement();
        }
        if (match(LEFT_BRACE)) {
            return blockStatement();
        }
        return expressionStatement();
    }

    private Stmt printStatement() {
        var expression = expression();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Print(expression);
    }

    private Stmt expressionStatement() {
        var expression = expression();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Expression(expression);
    }

    private Stmt blockStatement() {
        return new Stmt.Block(block());
    }

    private List<Stmt> block() {
        var statements = new ArrayList<Stmt>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block");
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        var expression = ternary();

        if (match(EQUAL)) {
            var equals = previous();
            var value = assignment();

            if (expression instanceof Expr.Variable variable) {
                return new Expr.Assignment(variable.name, value);
            }

            // TODO: shouldn't be thrown?
            error(equals, "Invalid assignment target");
        }

        return expression;
    }

    private Expr ternary() {
        return ternary(equality());
    }

    // better error handling?
    private Expr ternary(Expr selector) {
        var expression = selector;
        var selectorLine = previous().line();

        while (match(QUESTION_MARK)) {
            var left = ternary(equality());

            if (!match(COLON)) {
                throw error(peek(), "Expect : in ternary expression");
            }

            var right = equality();
            expression = new Expr.Ternary(expression, left, right, selectorLine);
        }

        return expression;
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

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
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
