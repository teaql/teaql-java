package io.teaql.core;

public interface ResponseHolder {
    void setHeader(String name, String value);

    String getHeader(String name);
}
