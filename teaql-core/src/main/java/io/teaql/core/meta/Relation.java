package io.teaql.core.meta;

import java.util.HashMap;
import java.util.Map;

/**
 * special property
 */
public class Relation extends PropertyDescriptor {

    /**
     * reverse property
     */
    private PropertyDescriptor reverseProperty;

    /**
     * the relation keeper
     */
    private EntityDescriptor relationKeeper;

    public PropertyDescriptor getReverseProperty() {
        return reverseProperty;
    }

    public void setReverseProperty(PropertyDescriptor pReverseProperty) {
        reverseProperty = pReverseProperty;
    }

    public EntityDescriptor getRelationKeeper() {
        return relationKeeper;
    }

    public void setRelationKeeper(EntityDescriptor pRelationKeeper) {
        relationKeeper = pRelationKeeper;
    }

    @Override
    public Map<String, String> getAdditionalInfo() {
        Map<String, String> additionalInfo = super.getAdditionalInfo();
        if (relationKeeper != getOwner()) {
            return additionalInfo;
        }
        else {
            EntityDescriptor owner = getReverseProperty().getOwner();
            Map<String, String> parentAttributes = owner.getAdditionalInfo();
            Map<String, String> ret = new HashMap<>(parentAttributes);
            ret.putAll(additionalInfo);
            return ret;
        }
    }
}
