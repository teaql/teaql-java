package io.teaql.core;

public class Slice {
    private int offset;
    private int size = 1000;

    public int getOffset() {
        return offset;
    }

    public void setOffset(int pOffset) {
        offset = pOffset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int pSize) {
        size = pSize;
    }
}
