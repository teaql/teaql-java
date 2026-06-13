package io.teaql.core.repository;

import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.MDC;

import io.teaql.core.utils.ThreadUtil;
import io.teaql.core.utils.ObjectUtil;

import io.teaql.core.BaseEntity;
import io.teaql.core.SearchRequest;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;

public class StreamEnhancer<T extends BaseEntity> implements Spliterator<T> {
    static ExecutorService executor = EnhancerThreadUtil.threadPoolExecutor();
    UserContext userContext;
    Spliterator<T> spliterator;
    SearchRequest<T> request;
    AbstractRepository<T> repository;
    int batch = 1000;
    SmartList<T> currentBatch = null;
    int nextIndex = 0;

    public <T extends BaseEntity> StreamEnhancer(
            UserContext ctx, AbstractRepository repository, Stream baseStream, SearchRequest request) {
        this.userContext = ctx;
        this.repository = repository;
        this.spliterator = baseStream.spliterator();
        this.request = request;
    }

    public <T extends BaseEntity> StreamEnhancer(
            UserContext ctx,
            AbstractRepository repository,
            Stream baseStream,
            SearchRequest request,
            int batch) {
        this.userContext = ctx;
        this.repository = repository;
        this.spliterator = baseStream.spliterator();
        this.request = request;
        this.batch = batch;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (ObjectUtil.isNotEmpty(currentBatch) && nextIndex < currentBatch.size()) {
            action.accept(currentBatch.get(nextIndex++));
            return true;
        }

        int i = 0;
        currentBatch = new SmartList<>();
        while (i < batch
                && spliterator.tryAdvance(
                e -> {
                    currentBatch.add(e);
                })) {
            i++;
        }

        repository.enhanceChildren(userContext, currentBatch, request);
        repository.enhanceRelations(userContext, currentBatch, request);

        if (ObjectUtil.isNotEmpty(currentBatch) && nextIndex < currentBatch.size()) {
            action.accept(currentBatch.get(nextIndex++));
            return true;
        }
        return false;
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return 16;
    }
}
