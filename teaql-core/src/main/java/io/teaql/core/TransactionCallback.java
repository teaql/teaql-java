package io.teaql.core;

public interface TransactionCallback<T> {
    T doInTransaction();
}
