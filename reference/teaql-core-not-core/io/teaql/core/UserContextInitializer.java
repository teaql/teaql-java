package io.teaql.core;

public interface UserContextInitializer {

    boolean support(Object request);

    void init(UserContext userContext, Object request);
}
