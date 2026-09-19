package io.teaql.core.businessid;

/** Generated entities adapt their strongly typed Business ID field to this slot. */
public interface BusinessIdSlot {
    String currentValue();

    boolean newAggregate();

    void assignCanonicalValue(String value);
}
