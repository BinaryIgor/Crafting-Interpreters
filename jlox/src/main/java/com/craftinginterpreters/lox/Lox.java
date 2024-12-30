package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Lox {

    private static final Interpreter interpreter = new Interpreter();
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        var source = Files.readString(Paths.get(path));
        run(source, false);
        if (hadError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    private static void run(String source, boolean repl) {
        var scanner = new Scanner(source);
        var tokens = scanner.scanTokens();
        var parser = new Parser(tokens);
        var statements = parser.parse();
        // Stop if there was a syntax error
        if (hadError) return;

        var resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        if (hadError) return;

        if (repl && statements.size() == 1) {
            interpreter.interpretPrinting(statements.getFirst());
        } else {
            interpreter.interpret(statements);
        }
    }

    private static void runPrompt() throws IOException {
        var input = new InputStreamReader(System.in);
        var reader = new BufferedReader(input);

        while (true) {
            System.out.println("jlox> ");
            var line = reader.readLine();
            if (line == null) {
                break;
            }
            run(line, true);
            hadError = false;
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type() == TokenType.EOF) {
            report(token.line(), " at end ", message);
        } else {
            report(token.line(), " at '%s'".formatted(token.lexeme()), message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.printf("%s\n[line %d ]%n", error.getMessage(), error.line);
        hadRuntimeError = true;
    }
}
