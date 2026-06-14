package io.teaql.core.utils;

public class StrBuilder implements Appendable, java.io.Serializable, CharSequence {
    private final StringBuilder delegate;

    public StrBuilder() {
        this.delegate = new StringBuilder();
    }

    public StrBuilder(int capacity) {
        if (capacity < 0) {
            throw new NegativeArraySizeException("Negative capacity: " + capacity);
        }
        this.delegate = new StringBuilder(capacity);
    }

    public StrBuilder(StringBuilder delegate) {
        this.delegate = delegate;
    }

    public StrBuilder append(Object obj) {
        delegate.append(obj);
        return this;
    }

    @Override
    public StrBuilder append(CharSequence csq) {
        delegate.append(csq);
        return this;
    }

    @Override
    public StrBuilder append(CharSequence csq, int start, int end) {
        delegate.append(csq, start, end);
        return this;
    }

    @Override
    public StrBuilder append(char c) {
        delegate.append(c);
        return this;
    }

    @Override
    public int length() {
        return delegate.length();
    }

    @Override
    public char charAt(int index) {
        return delegate.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return delegate.subSequence(start, end);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    public StrBuilder clear() {
        delegate.setLength(0);
        return this;
    }
}
