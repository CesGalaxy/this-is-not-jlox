package dev.cesarc.tinj.lang;

/**
 * Represents an instance of a class in the language.
 */
public class LangInstance {
    /**
     * The class of the instance.
     */
    private LangClass langClass;

    /**
     * Constructs an instance of the given class.
     *
     * @param langClass The class of the instance.
     */
    LangInstance(LangClass langClass) {
        this.langClass = langClass;
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
