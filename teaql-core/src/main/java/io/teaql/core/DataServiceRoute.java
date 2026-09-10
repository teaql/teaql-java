package io.teaql.core;

/**
 * Type-safe data service route identifier.
 * Replaces magic strings for data service routing.
 */
public final class DataServiceRoute {
    
    // Built-in routes
    public static final DataServiceRoute DEFAULT = new DataServiceRoute("default");
    public static final DataServiceRoute SQL = new DataServiceRoute("sql");
    public static final DataServiceRoute MEMORY = new DataServiceRoute("memory");
    
    private final String name;
    
    public DataServiceRoute(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Route name cannot be null or blank");
        }
        this.name = name;
    }
    
    public String name() {
        return name;
    }
    
    /**
     * Create a tenant-specific route for multi-tenant scenarios.
     */
    public DataServiceRoute withTenant(String tenantId) {
        return new DataServiceRoute(name + "_" + tenantId);
    }
    
    /**
     * Create a slave route for read-write splitting.
     */
    public DataServiceRoute withSlave(int slaveIndex) {
        return new DataServiceRoute(name + "_slave_" + slaveIndex);
    }
    
    /**
     * Create a master route for read-write splitting.
     */
    public DataServiceRoute withMaster() {
        return new DataServiceRoute(name + "_master");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataServiceRoute that)) return false;
        return name.equals(that.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return "Route[" + name + "]";
    }
}
