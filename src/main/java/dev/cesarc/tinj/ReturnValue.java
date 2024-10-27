package dev.cesarc.tinj;

public class ReturnValue extends RuntimeException {
    public final Object value;

    ReturnValue(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
