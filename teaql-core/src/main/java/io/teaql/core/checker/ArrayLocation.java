package io.teaql.core.checker;

import io.teaql.core.utils.StrUtil;

public class ArrayLocation extends ObjectLocation {

    private int index;

    public ArrayLocation(ObjectLocation pParent) {
        super(pParent);
    }

    public ArrayLocation(ObjectLocation pParent, int pIndex) {
        super(pParent);
        index = pIndex;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        ObjectLocation parent = getParent();
        String elementPath = StrUtil.format("[{}]", index);
        if (parent == null) {
            return elementPath;
        }
        return parent + elementPath;
    }
}
