package io.teaql.core;

public class Slice {
    private int offset;
    private int size = 1000;
    // Execution semantics, not a query JSON property. Defaults are outer-page
    // policy; only an explicitly configured page can bound a nested population.
    private boolean explicitPage;

    public boolean hasExplicitPage() { return explicitPage; }

    public Slice copy() {
        Slice copy = new Slice();
        copy.offset = offset; copy.size = size; copy.explicitPage = explicitPage;
        return copy;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int pOffset) {
        offset = pOffset;
        explicitPage = true;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int pSize) {
        size = pSize;
        explicitPage = true;
    }
}
