package io.teaql.data.dynamic;

public interface DynamicFieldsFacade {

    DynamicFieldsFacade withContext(Object userContext);

    DynamicFieldsFacade purpose(String purpose);

    DynamicFieldsFacade comment(String comment);

    OwnerBound owner(String ownerType, long ownerId);

    interface OwnerBound {
        StringFieldBound string(String fieldCode);
        NumberFieldBound number(String fieldCode);
        BoolFieldBound bool(String fieldCode);
        DynamicFieldValues readAll(DynamicFieldSelection selection);
    }

    interface StringFieldBound {
        void set(String value);
        String get();
    }

    interface NumberFieldBound {
        void set(Number value);
        Number get();
    }

    interface BoolFieldBound {
        void set(Boolean value);
        Boolean get();
    }
}
