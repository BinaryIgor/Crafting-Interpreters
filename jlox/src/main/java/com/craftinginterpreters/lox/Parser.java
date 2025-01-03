package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.craftinginterpreters.lox.TokenType.*;

// Grammar is as follows:
// program             -> declaration* EOF
// declaration         -> classDeclaration | funDeclaration | varDeclaration | statement
// classDeclaration    -> "class" IDENTIFIER "{" function* "}"
// funDeclaration      -> "fun" function;
// function            -> IDENTIFIER "(" parameters? ")" block
// varDeclaration      -> "var" IDENTIFIER ( "=" expression )? ";"
// statement           -> expressionStatement | forStatement | ifStatement | printStatement | whileStatement
//                        | breakStatement | continueStatement | returnStatement | block
// forStatement        -> "for" "(" ( varDeclaration | expressionStatement | ";" ) expression? ";" expression? ")" statement;
// whileStatement      -> "while" "(" expression ")" statement
// breakStatement      -> "break" ";"
// continueStatement   -> "continue" ";"
// ifStatement         -> "if" "(" expression ")" statement ( "else" statement )?;
// returnStatement     -> "return" expression? ";"
// block               -> "{" declaration "}"
// expressionStatement -> expression ";"
// printStatement      -> "print" expression ";"
// expression          -> assignment
// assignment          -> IDENTIFIER ("[" NUMBER "]")? =" ( call "." )? assignment | logicOr
// logicOr             -> logicAnd ( "or" logicAnd)*
// logicAnd            -> ternary ( "and" ternary)*
// ternary             -> equality ( ? equality ( ? equality : equality )* : equality )*
// equality            -> comparison ( ( "!=" | "==" ) comparison )*
// comparison          -> term ( ( ">" | ">=" | "<" | "<=" ) term )*
// term                -> factor ( ( "-" | "+" ) factor )*
// factor              -> unary ( ( "/" | "*" ) unary )*
// unary               -> ( "!" | "-" ) unary | primary
// call                -> primary ( "(" arguments? ")" | "." IDENTIFIER )*
// arguments           -> expression ( "," expression )*
// elements            _> expression ( "," expression )*
// parameters          -> IDENTIFIER ("," IDENTIFIER)*
// primary             -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER | fun "(" parameters? ")" block | list
// list                -> "[" (elements)* "]"
// listElementGet      -> IDENTIFIER "[" NUMBER "]"

public class Parser {

    private static final int MAX_FUNCTION_ARGS = 255;
    private final List<Token> tokens;
    private int current = 0;
    private Expr enclosingLoopCondition;

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
            if (match(CLASS)) {
                return classDeclaration();
            }
            if (match(FUN)) {
                return funDeclaration("function");
            }
            if (match(VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        var name = consume(IDENTIFIER, "Expect class name");
        consume(LEFT_BRACE, "Expect '{' before class body");

        var methods = new ArrayList<Stmt.Function>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(funDeclaration("method"));
        }

        consume(RIGHT_BRACE, "Expect '}' after class body");

        return new Stmt.Class(name, methods);
    }

    private Stmt.Function funDeclaration(String kind) {
        var name = consume(IDENTIFIER, "Expect %s name".formatted(kind));
        var parametersAndBody = funParamsAndBody(kind);
        return new Stmt.Function(name, parametersAndBody.params, parametersAndBody.body);
    }

    private FunParamsAndBody funParamsAndBody(String kind) {
        consume(LEFT_PAREN, "Expect '(' after %s name".formatted(kind));

        var parameters = new ArrayList<Token>();
        if (!check(RIGHT_PAREN)) {
            do {
                parameters.add(consume(IDENTIFIER, "Expect parameter name"));
                if (parameters.size() >= MAX_FUNCTION_ARGS) {
                    error(peek(), "Can't have more than %d parameters".formatted(MAX_FUNCTION_ARGS));
                }
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters");

        consume(LEFT_BRACE, "Expect '{' before %s body".formatted(kind));

        return new FunParamsAndBody(parameters, block());
    }

    private Expr funExpression() {
        var parametersAndBody = funParamsAndBody("function");
        return new Expr.Function(parametersAndBody.params, parametersAndBody.body);
    }

    private Stmt varDeclaration() {
        var name = consume(IDENTIFIER, "Expect variable name");
        var initializer = match(EQUAL) ? expression() : null;
        consume(SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Variable(name, initializer);
    }

    private Stmt statement() {
        if (match(FOR)) {
            return forStatement();
        }
        if (match(IF)) {
            return ifStatement();
        }
        if (match(PRINT)) {
            return printStatement();
        }
        if (match(RETURN)) {
            return returnStatement();
        }
        if (match(WHILE)) {
            return whileStatement();
        }
        if (match(LEFT_BRACE)) {
            return blockStatement();
        }
        if (match(BREAK)) {
            return breakStatement();
        }
        if (match(CONTINUE)) {
            return continueStatement();
        }
        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        var condition = check(SEMICOLON) ? new Expr.Literal(true) : expression();
        consume(SEMICOLON, "Expect ';' after loop condition");

        var increment = !check(RIGHT_PAREN) ? expression() : null;
        consume(RIGHT_PAREN, "Expect ')' after a for clause");

        var body = whileBody(condition);
        var forLoopStep = increment == null ? null : new Stmt.Expression(increment);

        body = new Stmt.While(condition, body, forLoopStep);

        if (initializer != null) {
            body = new Stmt.Block(List.of(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'");
        var condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition");

        var thenBranch = statement();
        var elseBranch = match(ELSE) ? statement() : null;

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        var expression = expression();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Print(expression);
    }

    private Stmt returnStatement() {
        var keyword = previous();
        var value = check(SEMICOLON) ? null : expression();
        consume(SEMICOLON, "Expect ';' after return value");
        return new Stmt.Return(keyword, value);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'");
        var condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition");
        return new Stmt.While(condition, whileBody(condition), null);
    }

    private Stmt whileBody(Expr condition) {
        var previousEnclosingLoopCondition = enclosingLoopCondition;
        try {
            enclosingLoopCondition = condition;
            return statement();
        } finally {
            enclosingLoopCondition = previousEnclosingLoopCondition;
        }
    }

    private Stmt expressionStatement() {
        var expression = expression();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Expression(expression);
    }

    private Stmt breakStatement() {
        var keyword = previous();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Break(enclosingLoopCondition, keyword);
    }

    private Stmt continueStatement() {
        var keyword = previous();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Continue(enclosingLoopCondition, keyword);
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
        var expression = or();

        if (match(EQUAL)) {
            var equals = previous();
            var value = assignment();

            if (expression instanceof Expr.Variable variable) {
                return new Expr.Assignment(variable.name, value);
            }
            if (expression instanceof Expr.Get get) {
                return new Expr.Set(get.object, get.name, value);
            }

            // TODO: shouldn't be thrown?
            error(equals, "Invalid assignment target");
        }

        return expression;
    }

    private Expr or() {
        var expr = and();

        while (match(OR)) {
            var operator = previous();
            var right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        var expr = ternary();

        while (match(AND)) {
            var operator = previous();
            var right = ternary();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
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

        return call();
    }

    private Expr call() {
        var expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                var name = consume(IDENTIFIER, "Expect property name after '.'");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        var arguments = new ArrayList<Expr>();
        if (!check(RIGHT_PAREN)) {
            do {
                arguments.add(expression());
                if (arguments.size() >= MAX_FUNCTION_ARGS) {
                    error(peek(), "Can't have more than %d arguments".formatted(MAX_FUNCTION_ARGS));
                }
            } while (match(COMMA));
        }

        var paren = consume(RIGHT_PAREN, "Expect ')' after arguments");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(NIL)) {
            return new Expr.Literal(null);
        }

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal());
        }

        if (match(THIS)) {
            return new Expr.This(previous());
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            var expression = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expression);
        }

        if (match(LEFT_BRACKET)) {
            return list();
        }

        if (match(FUN)) {
            return funExpression();
        }

        throw error(peek(), "Expect expression");
    }

    private Expr list() {
        var elements = new ArrayList<Expr>();
        if (!check(RIGHT_BRACKET)) {
            do {
                elements.add(expression());
                if (elements.size() >= 1000) {
                    error(peek(), "Can't have more than %d elements in list expression".formatted(1000));
                }
            } while (match(COMMA));
        }

        consume(RIGHT_BRACKET, "Expect ']' after list expression");

        return new Expr.LoxList(elements);
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

    private record FunParamsAndBody(List<Token> params, List<Stmt> body) {
    }
}
