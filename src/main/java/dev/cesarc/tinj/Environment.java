package dev.cesarc.tinj;

import dev.cesarc.tinj.token.Token;

import java.util.HashMap;
import java.util.Map;

/// A virtual space for storing variables
public class Environment {
    /// The parent environment, if any
    /// @see Environment#Environment(Environment)
    final Environment enclosing;

    /// The values stored in this environment
    private final Map<String, Object> values = new HashMap<>();

    /// Create a new environment with no parent
    Environment() {
        enclosing = null;
    }

    /// Create a new environment with the provided parent
    /// @param enclosing The parent environment
    /// @see Environment#enclosing
    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    /// Get the value of a variable in this environment (or parent)
    /// @param name The name of the variable
    /// @return The value of the variable
    /// @throws RuntimeError If the variable does not exist
    Object get(Token name) throws RuntimeError {
        // Try to get the variable in the current environment
        if (values.containsKey(name.lexeme)) return values.get(name.lexeme);

        // If there's a parent environment, try to get the variable there
        if (enclosing != null) return enclosing.get(name);

        // If the variable doesn't exist, throw an error
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    /// Assign a value to an existing variable in this environment (or parent)
    /// @param name The name of the variable
    /// @param value The value to assign to the variable
    /// @throws RuntimeError If the variable does not exist
    void assign(Token name, Object value) throws RuntimeError {
        // Try to get the variable in the current environment
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        // If there's a parent environment, try to assign the variable there
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        // If the variable doesn't exist, throw an error
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    /// Define a new variable in this environment
    public void define(String name, Object value) {
        values.put(name, value);
    }
}
