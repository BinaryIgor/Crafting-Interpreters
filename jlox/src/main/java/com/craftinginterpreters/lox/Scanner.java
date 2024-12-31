package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;
import static java.util.Map.entry;

public class Scanner {

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
        entry("and", AND),
        entry("class", CLASS),
        entry("else", ELSE),
        entry("false", FALSE),
        entry("for", FOR),
        entry("fun", FUN),
        entry("if", IF),
        entry("nil", NIL),
        entry("or", OR),
        entry("print", PRINT),
        entry("return", RETURN),
        entry("super", SUPER),
        entry("this", THIS),
        entry("true", TRUE),
        entry("var", VAR),
        entry("while", WHILE),
        entry("break", BREAK),
        entry("continue", CONTINUE)
    );
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int line = 1;
    private int start = 0;
    private int current = 0;

    public Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        var unexpectedCharacters = new LinkedHashMap<Integer, List<Character>>();

        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme
            start = current;
            var result = scanToken();
            if (result.error) {
                unexpectedCharacters.computeIfAbsent(line, k -> new ArrayList<>()).add(result.character);
            }
        }

        unexpectedCharacters.forEach((line, unexpected) -> {
            Lox.error(line, "Unexpected characters: " + unexpected);
        });

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private ScanTokenResult scanToken() {
        var c = advance();
        var error = false;
        switch (c) {
            case '(' -> addToken(LEFT_PAREN);
            case ')' -> addToken(RIGHT_PAREN);
            case '{' -> addToken(LEFT_BRACE);
            case '}' -> addToken(RIGHT_BRACE);
            case '[' -> addToken(LEFT_BRACKET);
            case ']' -> addToken(RIGHT_BRACKET);
            case ',' -> addToken(COMMA);
            case '.' -> addToken(DOT);
            case '-' -> addToken(MINUS);
            case '+' -> addToken(PLUS);
            case ';' -> addToken(SEMICOLON);
            case ':' -> addToken(COLON);
            case '*' -> addToken(STAR);
            case '?' -> addToken(QUESTION_MARK);
            case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
            case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);
            case '>' -> addToken(match('=') ? GREATER_EQUAL : GREATER);
            case '/' -> {
                if (match('/')) {
                    singleLineComment();
                } else if (match('*')) {
                    error = multiLineComment();
                } else {
                    addToken(SLASH);
                }
            }
            case ' ', '\r', '\t' -> {
                // Ignore whitespaces
            }
            case '\n' -> line++;
            case '"' -> string();
            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    error = true;
                }
            }
        }

        return new ScanTokenResult(c, error);
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        var text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }
        if (source.charAt(current) != expected) {
            return false;
        }
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private void singleLineComment() {
        // A comment goes until the end of the line
        while (peek() != '\n' && !isAtEnd()) {
            advance();
        }
    }

    private boolean multiLineComment() {
        while (peek() != '*' && !isAtEnd()) {
            advance();
            if (peek() == '\n') {
                line++;
            }
        }
        // Consume '*' character
        if (!isAtEnd()) {
            advance();
        }
        if (peek() != '/') {
            return true;
        }
        // Consume '/' character
        advance();
        return false;
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // The closing "
        advance();
        // Trim the surrounding quotes
        var value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void number() {
        while (isDigit(peek())) {
            advance();
        }
        // Look for a fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) {
                advance();
            }
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private char peekNext() {
        var next = current + 1;
        if (next >= source.length()) {
            return '\0';
        }
        return source.charAt(next);
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        var text = source.substring(start, current);
        var type = KEYWORDS.getOrDefault(text, IDENTIFIER);

        addToken(type);
    }

    private record ScanTokenResult(char character, boolean error) {
    }
}
