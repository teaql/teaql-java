package io.teaql.coreservice;

public interface TransactionCallback<T> {
    T doInTransaction();
}
