package dev.cesarc.tinj.lang;

import dev.cesarc.tinj.Interpreter;

import java.util.List;

public interface LangCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}
