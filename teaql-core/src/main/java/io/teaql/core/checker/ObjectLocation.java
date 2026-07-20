package io.teaql.core.checker;

public class ObjectLocation {
    private ObjectLocation parent;

    public ObjectLocation(ObjectLocation pParent) {
        parent = pParent;
    }

    public static ObjectLocation hashRoot(String memberName) {
        return new HashLocation(null, memberName);
    }

    public static ObjectLocation arrayRoot(int index) {
        return new ArrayLocation(null, index);
    }

    public ObjectLocation getParent() {
        return parent;
    }

    public ObjectLocation member(String memberName) {
        return new HashLocation(this, memberName);
    }

    public ObjectLocation element(int index) {
        return new ArrayLocation(this, index);
    }

    public int getLevel() {
        if (getParent() == null) {
            return 1;
        }
        return getParent().getLevel() + 1;
    }

    public boolean isFirstLevel() {
        return getLevel() == 1;
    }

    public boolean isSecondLevel() {
        return getLevel() == 2;
    }

    public boolean isThirdLevel() {
        return getLevel() == 3;
    }
}
