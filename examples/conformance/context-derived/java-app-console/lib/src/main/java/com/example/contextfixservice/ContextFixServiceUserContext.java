
package com.example.contextfixservice;

import io.teaql.core.UserContext;
public interface ContextFixServiceUserContext extends UserContext{
    public default String uniStr(String prefix){
        throw new RuntimeException("not implemented yet");
    }




}