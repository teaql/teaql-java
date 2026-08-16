package io.teaql.cloud;

import java.util.List;

public interface CloudClient {
    void register(ServiceInstance instance);
    void deregister(ServiceInstance instance);
    List<ServiceInstance> getInstances(String serviceId);
    boolean isHealthy();
}
