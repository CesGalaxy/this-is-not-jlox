package dev.cesarc.tinj.lang;

import dev.cesarc.tinj.Environment;
import dev.cesarc.tinj.Interpreter;
import dev.cesarc.tinj.ReturnValue;
import dev.cesarc.tinj.syntax.nodes.Stmt;

import java.util.List;

public class LangFunction implements LangCallable {
    /// The function declaration
    private final Stmt.Function declaration;

    /// The environment in which the function was defined
    private final Environment closure;

    /// Create a new function with the given declaration and closure
    public LangFunction(Stmt.Function declaration, Environment closure) {
        this.closure = closure;
        this.declaration = declaration;
    }

    LangFunction bind(LangInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LangFunction(declaration, environment);
    }

    /// Get the number of parameters the function requires
    @Override
    public int arity() {
        return declaration.params.size();
    }

    /// Call the function with the given arguments
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);

        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeStmt(declaration.body, environment);
        } catch (ReturnValue returnValue) {
            return returnValue.value;
        }

        return null;
    }

//    @Override
//    public String toString() {
//        return super.toString();
//    }

    // fun fact(n) { var x = 1; for (var i = 1; i <= n; i = i + 1) x = x * i; print x; }

    // fun fib(n) if (n <= 1) return n; else return fib(n - 2) + fib(n - 1);
    // for (var i = 0; i < 20; i = i + 1) print fib(i);
}
