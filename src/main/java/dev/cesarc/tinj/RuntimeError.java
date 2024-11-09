package dev.cesarc.tinj;

import dev.cesarc.tinj.token.Token;

public class RuntimeError extends RuntimeException {
    final Token token;

    public RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
