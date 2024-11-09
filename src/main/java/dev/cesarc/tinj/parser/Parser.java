package dev.cesarc.tinj.parser;

import dev.cesarc.tinj.Main;
import dev.cesarc.tinj.syntax.nodes.Expr;
import dev.cesarc.tinj.syntax.nodes.Stmt;
import dev.cesarc.tinj.token.Token;
import dev.cesarc.tinj.token.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.cesarc.tinj.token.TokenType.*;

public class Parser {
    /// A runtime exception to indicate a parsing error
    private static class ParseError extends RuntimeException {}

    /// The list of tokens to parse
    private final List<Token> tokens;

    /// A cursor pointing to the index (at the token list) of current token being parsed
    private int current = 0;

    /**
     * Create a new parser with the provided list of tokens
     * @param tokens The tokens to parse
     * @see dev.cesarc.tinj.token.Token
     */
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Parse the list of tokens into a list of statements
     * @return The list of statements
     */
    public List<Stmt> parse() {
        // Create a list to store the parsed statements
        List<Stmt> statements = new ArrayList<>();

        // Parse the tokens into statements until the end of the token list
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(CLASS)) return classDeclaration();
        if (match(FUN)) return function("function");
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(BRACE_OPEN)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");
        consume(BRACE_OPEN, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(BRACE_CLOSE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(BRACE_CLOSE, "Expect '}' after class body.");

        return new Stmt.Class(name, methods);
    }

    private Stmt forStatement() {
        consume(PAREN_OPEN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) condition = expression();
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(PAREN_CLOSE)) increment = expression();
        consume(PAREN_CLOSE, "Expect ')' after for clauses.");

        Stmt body = statement();

        if (increment != null) body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) body = new Stmt.Block(Arrays.asList(initializer, body));

        return body;
    }

    private Stmt ifStatement() {
        // The primary expression already protects us from misinterpreting the branch as part of the condition
        Expr condition = primary();

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) value = expression();

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        Expr condition = primary();
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(PAREN_OPEN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(PAREN_CLOSE)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(
                        consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(PAREN_CLOSE, "Expect ')' after parameters.");

        Stmt body = statement();
        return new Stmt.Function(name, parameters, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(BRACE_CLOSE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(BRACE_CLOSE, "Expect '}' after block.");
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        // TODO: Make this a loop
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable var) {
                return new Expr.Assign(var.name, value);
            } else if (expr instanceof Expr.Get get) {
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(DASH, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, ASTERISK)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, DASH)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(PAREN_OPEN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(PAREN_CLOSE)) {
            do {
                if (arguments.size() >= 255) error(peek(), "Can't have more than 255 arguments.");
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(PAREN_CLOSE, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(IDENTIFIER)) return new Expr.Variable(previous());

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(PAREN_OPEN)) {
            Expr expr = expression();
            consume(PAREN_CLOSE, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect (primary) expression.");
    }

    /// If the current token is any of the provided types, advance the cursor and return true. Otherwise, false.
    /// @param types The types of tokens to match
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    /// Expect a token of the provided type, otherwise throw an error
    /// @param type The type of token to expect
    /// @param message The error message to display if the token is not of the expected type
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    /// Check if the current token is of the provided type
    /// @param type The type of token to check
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /// Move the cursor to the next token, if it's not at the end
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    /// Check if the cursor is pointing at the "End of File" token
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    /// Get the current token relative to the cursor position
    private Token peek() {
        return tokens.get(current);
    }

    /// Get the previous token relative to the current cursor position
    private Token previous() {
        return tokens.get(current - 1);
    }

    /// Report an error while parsing the tokens
    /// @param token The token where the error occurred
    /// @param message The error message
    private ParseError error(Token token, String message) {
        Main.error(token, message);
        return new ParseError();
    }

    /// Try to move thc cursor to the next statement if possible
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
