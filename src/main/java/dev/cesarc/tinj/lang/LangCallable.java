package dev.cesarc.tinj.lang;

import dev.cesarc.tinj.Interpreter;

import java.util.List;

/// Represents any type of value that can be called.
public interface LangCallable {
    /// The amount of arguments the function expects.
    int arity();

    /// Make a call with the given arguments.
    Object call(Interpreter interpreter, List<Object> arguments);
}
