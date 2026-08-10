# Bug: Entity constructor suppliers not registered in default SimpleEntityMetaFactory

## Description
When executing database queries (e.g. `Q.outboxEvents()...executeForList(ctx)`), the mapped rows are converted to Java entity instances. The mapping repository calls `descriptor.createEntity()`, which relies on `entitySupplier` to instantiate the classes:

```java
public Entity createEntity() {
    if (entitySupplier == null) {
        throw new IllegalStateException("No entity supplier registered for " + getType());
    }
    return entitySupplier.get();
}
```

The default `SimpleEntityMetaFactory` resolved by the registry contains metadata configurations but does not associate or register these constructor suppliers. As a result, query execution throws:
`java.lang.IllegalStateException: No entity supplier registered for OutboxEvent`

## Expected Behavior
Entity descriptors should automatically resolve their class constructors using reflection or have registrations automatically generated without requiring users to register constructor suppliers manually on boot.

## Suggested Solution
1. **Generator-side registration**: Update the code generator to emit constructor supplier registration statements inside the generated `EntityMetaRegistry.java` file (e.g., `entityDescriptor.setEntitySupplier(OutboxEvent::new)`).
2. **Reflection-based Fallback**: Update `EntityDescriptor` or `SimpleEntityMetaFactory` to automatically construct a default constructor supplier using reflection if `entitySupplier` is null:
   ```java
   if (entitySupplier == null && targetType != null) {
       entitySupplier = () -> {
           try {
               return targetType.getDeclaredConstructor().newInstance();
           } catch (Exception e) {
               throw new RuntimeException("Failed to instantiate " + targetType, e);
           }
       };
   }
   ```
