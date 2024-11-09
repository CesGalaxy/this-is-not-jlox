package dev.cesarc.tinj.lang;

import dev.cesarc.tinj.Interpreter;

import java.util.List;

/**
 * Represents a class in the language.
 *
 * @see dev.cesarc.tinj.syntax.nodes.Stmt.Class
 */
public class LangClass implements LangCallable {
    /**
     * The name of the class.
     */
    final String name;

    /**
     * Constructs a LangClass with the given name.
     *
     * @param name The name of the class.
     */
    public LangClass(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the class.
     *
     * @return The name of the class.
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Calls the class constructor with the given arguments, returning a new instance of the class.
     *
     * @param interpreter The interpreter where the statement will run
     * @param arguments   The arguments to pass to the class constructor
     * @return A new instance of the class
     */
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LangInstance instance = new LangInstance(this);
        return instance;
    }

    /**
     * Returns the number of arguments the class constructor expects.
     *
     * @return The number of arguments the class constructor expects.
     */
    @Override
    public int arity() {
        return 0;
    }
}
