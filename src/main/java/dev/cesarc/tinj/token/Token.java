package dev.cesarc.tinj.token;

/// The smallest unit of the language, it represents a part of the source code
public class Token {
    /// The type of the token
    public final TokenType type;

    /// A copy of the part of the source code that the token represents
    public final String lexeme;

    /// A literal (provided in the lexeme) that can extend the token details
    public final Object literal;

    /// The line in which the token was found
    public final int line;

    /// Create a new token with the given type, lexeme, literal and line
    /// @param type The type of the token
    /// @param lexeme The part of the source code that the token represents
    /// @param literal A literal that can extend the token details
    /// @param line The line in which the token was found
    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return "[" + lexeme + " >> " + type + ":" + literal + "]";
    }
}
