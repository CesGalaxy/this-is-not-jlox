package dev.cesarc.tinj.lang;

import dev.cesarc.tinj.RuntimeError;
import dev.cesarc.tinj.token.Token;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an instance of a class in the language.
 */
public class LangInstance {
    /**
     * The class of the instance.
     */
    private LangClass langClass;

    /**
     * The fields of the instance.
     */
    private final Map<String, Object> fields = new HashMap<>();

    /**
     * Constructs an instance of the given class.
     *
     * @param langClass The class of the instance.
     */
    LangInstance(LangClass langClass) {
        this.langClass = langClass;
    }

    public Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        LangFunction method = langClass.findMethod(name.lexeme);
        if (method != null) return method.bind(this);

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    public void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    /**
     * Returns the class of the instance.
     *
     * @return The class of the instance.
     */
    @Override
    public String toString() {
        return langClass.name + " instance";
    }
}
