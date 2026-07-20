package io.teaql.core.checker;

public class HashLocation extends ObjectLocation {
    private String member;

    public HashLocation(ObjectLocation pParent) {
        super(pParent);
    }

    public HashLocation(ObjectLocation pParent, String pMember) {
        super(pParent);
        member = pMember;
    }

    public String getMember() {
        return member;
    }

    @Override
    public String toString() {
        ObjectLocation parent = getParent();
        if (parent == null) {
            return member;
        }
        return parent + "." + member;
    }
}
