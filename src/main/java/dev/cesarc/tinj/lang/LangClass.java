package dev.cesarc.tinj.lang;

import dev.cesarc.tinj.Interpreter;

import java.util.List;
import java.util.Map;

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
     * The methods of the class.
     */
    private final Map<String, LangFunction> methods;

    /**
     * Constructs a LangClass with the given name.
     *
     * @param name The name of the class.
     * @param methods The methods of the class.
     */
    public LangClass(String name, Map<String, LangFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    /**
     * Finds a method with the given name in the class.
     *
     * @param name The name of the method to find.
     * @return The method with the given name, or null if it doesn't exist.
     */
    LangFunction findMethod(String name) {
        if (methods.containsKey(name) && !name.equals(this.name)) return methods.get(name);
        return null;
    }

    LangFunction getInitializer() {
        return findMethod(name);
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
        // Create a new empty instance of the class
        LangInstance instance = new LangInstance(this);

        // Find the initializer method and call it (if it's defined) with the given arguments and the new instance bound
        // The constructor is named as the class itself
        LangFunction initializer = getInitializer();
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    /**
     * Returns the number of arguments the class constructor expects.
     *
     * @return The number of arguments the class constructor expects.
     */
    @Override
    public int arity() {
        LangFunction initializer = getInitializer();
        if (initializer == null) return 0;
        return initializer.arity();
    }
}
