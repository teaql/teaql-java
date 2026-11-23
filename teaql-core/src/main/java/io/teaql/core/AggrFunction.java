package io.teaql.core;

public enum AggrFunction implements PropertyFunction {
    SELF,
    MIN,
    MAX,
    AVG,
    COUNT,
    COUNT_DISTINCT,
    SUM,
    GBK,
    STDDEV,
    STDDEV_POP,
    VAR_SAMP,
    VAR_POP,
    BIT_AND,
    BIT_OR,
    BIT_XOR,
}
