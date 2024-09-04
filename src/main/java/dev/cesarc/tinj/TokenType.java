package dev.cesarc.tinj;

public enum TokenType {
    // Single-character tokens (open/close)
    PAREN_OPEN, PAREN_CLOSE,
    BRACE_OPEN, BRACE_CLOSE,

    // Single-character tokens
    COMMA, DOT,
    SEMICOLON,
    PLUS, DASH, ASTERISK, SLASH,

    // Characters that can be with an equal sign
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Literals
    IDENTIFIER,
    STRING,
    NUMBER,

    // Keywords
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    EOF
}
