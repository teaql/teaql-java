package io.teaql.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.teaql.core.utils.CollStreamUtil;
import io.teaql.core.utils.CollectionUtil;
import io.teaql.core.utils.MapUtil;
import io.teaql.core.utils.ObjectUtil;

public class SmartList<T> implements Iterable<T> {
    private static final Map<Class<?>, SmartList<?>> TYPED_EMPTY =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final SmartList<?> UNLOADED = new SmartList<>(true, false);

    List<T> data;

    List<AggregationResult> aggregationResults;
    private String pageCountAlias = TeaQLConstants.ROOT_LIST_PARAMETER_NAME;
    @FrameworkInternal("Named page total metadata, independent of physical count SQL")
    public void __internalSetPageCountAlias(String alias) { ensureMutable(); pageCountAlias = alias; }

    Map<String, SmartList<?>> facets;
    private Map<String, Object> summary;
    private boolean loaded = true;

    private final boolean immutable;

    public SmartList() {
        immutable = false;
        data = new ArrayList<>();
    }

    public SmartList(int expectedSize) {
        immutable = false;
        data = new ArrayList<>(Math.max(0, expectedSize));
    }

    public SmartList(List<T> data) {
        this(data == null ? 0 : data.size());
        if (data != null) {
            this.data.addAll(data);
            __internalShareLoadStates();
        }
    }

    private SmartList(boolean immutable, boolean loaded) {
        this.immutable = immutable;
        this.loaded = loaded;
        data = List.of();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> SmartList<T> empty(Class<? extends T> entityType) {
        if (entityType == null) throw new IllegalArgumentException("entityType must not be null");
        return (SmartList<T>) TYPED_EMPTY.computeIfAbsent(entityType, ignored -> new SmartList<>(true, true));
    }

    /** Unloaded relation marker; successful empty queries use a loaded empty list. */
    @SuppressWarnings("unchecked")
    public static <T> SmartList<T> unloaded() { return (SmartList<T>) UNLOADED; }

    public boolean isLoaded() { return loaded; }

    @FrameworkInternal
    public static <T extends Entity> SmartList<T> takeOwnership(List<T> data) {
        SmartList<T> result = new SmartList<>();
        result.data = data == null ? new ArrayList<>() : data;
        result.__internalShareLoadStates();
        return result;
    }

    public boolean isSharedEmpty() {
        return immutable;
    }

    @Override
    public Iterator<T> iterator() {
        return data.iterator();
    }

    public T first() {
        return CollectionUtil.getFirst(data);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Stream<T> stream() {
        return data.stream();
    }

    public <R> Map<R, T> identityMap(Function<T, R> key) {
        return CollStreamUtil.toIdentityMap(data, key);
    }

    public Map<Long, T> mapById() {
        Map<Long, T> result = new LinkedHashMap<>();
        for (T item : data) {
            Long id = entity(item).getId();
            if (id != null) result.put(id, item);
        }
        return result;
    }

    public List<Long> ids() { return toList(item -> entity(item).getId()); }
    public List<Long> versions() { return toList(item -> entity(item).getVersion()); }

    private static Entity entity(Object item) {
        if (item instanceof Entity entity) return entity;
        throw new IllegalStateException("Entity identity operations require entity rows");
    }

    public <R> Map<R, List<T>> groupBy(Function<T, R> key) {
        return CollStreamUtil.groupByKey(data, key, false);
    }

    public void add(T pValue) {
        ensureMutable();
        shareExistingState(pValue);
        data.add(pValue);
    }

    public void set(int index, T pValue) {
        ensureMutable();
        shareExistingState(pValue);
        data.set(index, pValue);
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> pData) {
        ensureMutable();
        data = pData;
        __internalShareLoadStates();
    }

    /** Group by exact availability, not by a union of requested fields. The temporary
     * index is discarded: a long-lived list does not retain historical COW states. */
    @FrameworkInternal("Hydration result finalization only")
    public void __internalShareLoadStates() {
        if (data == null || data.size() < 2) return;
        LoadState first = null;
        Map<LoadState, LoadState> shapes = null;
        // Indexed traversal avoids an iterator allocation on the normal ArrayList
        // result, while non-random-access lists retain linear traversal.
        Iterator<T> iterator = data instanceof java.util.RandomAccess ? null : data.iterator();
        int index = 0;
        while (iterator == null ? index < data.size() : iterator.hasNext()) {
            T entity = iterator == null ? data.get(index++) : iterator.next();
            if (!(entity instanceof BaseEntity base)) continue;
            LoadState state = base.__internalLoadState();
            if (first == null) { first = state; continue; }
            // Compiled hydration already supplies the same immutable snapshot.
            // No grouping index is needed unless another reference appears.
            if (state == first) continue;
            if (shapes == null) {
                shapes = new HashMap<>();
                shapes.put(first, first);
            }
            LoadState shared = shapes.putIfAbsent(state, state);
            if (shared != null && shared != state) base.__internalUseLoadState(shared);
        }
    }

    private void shareExistingState(T entity) {
        if (!(entity instanceof BaseEntity base) || data == null || data.isEmpty()) return;
        LoadState state = base.__internalLoadState();
        // Homogeneous hydration/append is the usual case and only checks the tail.
        T tail = data.get(data.size() - 1);
        if (tail instanceof BaseEntity previous && state.equals(previous.__internalLoadState())) {
            base.__internalUseLoadState(previous.__internalLoadState());
            return;
        }
        for (T existing : data) {
            if (existing instanceof BaseEntity previous && state.equals(previous.__internalLoadState())) {
                base.__internalUseLoadState(previous.__internalLoadState());
                return;
            }
        }
    }

    public void addAggregationResult(UserContext userContext, AggregationResult aggregationResult) {
        ensureMutable();
        mutableAggregationResults().add(aggregationResult);
    }

    public List<AggregationResult> getAggregationResults() {
        return mutableAggregationResults();
    }

    public void setAggregationResults(List<AggregationResult> pAggregationResults) {
        ensureMutable();
        aggregationResults = pAggregationResults;
    }

    public int size() {
        return data.size();
    }

    public T get(int index) {
        return data.get(index);
    }

    public T getOrNull(int index) { return index >= 0 && index < size() ? data.get(index) : null; }
    public T last() { return isEmpty() ? null : data.get(size() - 1); }

    public SmartList<T> save(UserContext userContext) {
        userContext.saveGraph(this);
        return this;
    }


    /** Exact source-query total, or null when no count was requested. */
    public Long getExactTotalCount() {
        Map<String, Object> properties = aggregationProperties();
        if (!properties.containsKey(pageCountAlias)) return null;
        Object count = properties.get(pageCountAlias);
        try {
            if (!(count instanceof Number)) throw new ArithmeticException();
            long exact = new java.math.BigDecimal(count.toString()).longValueExact();
            if (exact < 0) throw new ArithmeticException();
            return exact;
        } catch (ArithmeticException | NumberFormatException invalid) {
            throw new IllegalStateException("List count must be an exact non-negative long", invalid);
        }
    }

    public long getTotalCountOrSize() {
        Long exact = getExactTotalCount();
        return exact == null ? size() : exact;
    }

    /** @deprecated Use getExactTotalCount, or the explicitly named getTotalCountOrSize fallback. */
    @Deprecated
    public int getTotalCount() { return Math.toIntExact(getTotalCountOrSize()); }

    public Map<String, Object> aggregationProperties(Class<?> clazz) {
        if (ObjectUtil.isEmpty(aggregationResults)) {
            return MapUtil.empty();
        }
        Map<String, Object> result = MapUtil.createMap(HashMap.class);
        getAggregationResults().forEach(aggregationResult -> {
            if (aggregationResult.getData() == null) return;

            //Map s=aggregationResult.toSimpleMap();
            List<Map<String, Object>> resultList = aggregationResult.valueList();

            resultList.forEach(map -> {
                map.entrySet().forEach(stringObjectEntry -> {

                    if (stringObjectEntry.getValue() == null ? clazz == Object.class
                            : clazz.isInstance(stringObjectEntry.getValue())) {
                        result.put(stringObjectEntry.getKey(), stringObjectEntry.getValue());
                    }

                });
            });

        });
        return result;
    }

    public void addFacet(String name, SmartList<?> facet) {
        ensureMutable();
        mutableFacets().put(name, facet);
    }

    public Map<String, SmartList<?>> getFacets() {
        return mutableFacets();
    }

    public void setFacets(Map<String, ? extends SmartList<?>> facets) {
        ensureMutable();
        this.facets = facets == null ? null : new LinkedHashMap<>(facets);
    }

    public SmartList<?> getFacet(String name) {
        return facets == null ? null : facets.get(name);
    }

    public SmartList<?> removeFacet(String name) {
        ensureMutable();
        return facets == null ? null : facets.remove(name);
    }

    public void clearFacets() {
        ensureMutable();
        if (facets != null) facets.clear();
    }

    public Map<String, SmartList<?>> takeFacets() {
        ensureMutable();
        Map<String, SmartList<?>> result = facets == null ? new LinkedHashMap<>() : facets;
        facets = null;
        return result;
    }

    /** Summary population stays package-private; consumers receive a detached value tree. */
    void setSummary(Map<String, Object> values) {
        ensureMutable();
        summary = copyMap(values);
    }

    public Map<String, Object> getSummary() { return copyMap(summary); }

    private List<AggregationResult> mutableAggregationResults() {
        if (immutable) return List.of();
        if (aggregationResults == null) aggregationResults = new ArrayList<>();
        return aggregationResults;
    }

    private Map<String, SmartList<?>> mutableFacets() {
        if (immutable) return Map.of();
        if (facets == null) facets = new LinkedHashMap<>();
        return facets;
    }

    public Map<String, Object> aggregationProperties() {
        return aggregationProperties(Object.class);
    }

    public Map<String, Object> aggregationNumberProperties() {
        return aggregationProperties(Number.class);
    }


    public <R> List<R> toList(Function<T, R> function) {
        return CollStreamUtil.toList(data, function);
    }

    public <R> Set<R> toSet(Function<T, R> function) {
        return CollStreamUtil.toSet(data, function);
    }

    public <R> Map<R, T> toIdentityMap(Function<T, R> function) {
        return CollStreamUtil.toIdentityMap(data, function);
    }

    public boolean removeIf(Predicate<T> filter) {
        ensureMutable();
        return data.removeIf(filter);
    }

    /** One output row per source row, with an isolated query metadata snapshot. */
    public <R> SmartList<R> map(Function<? super T, ? extends R> mapper) {
        SmartList<R> result = new SmartList<>(toList(mapper::apply));
        result.loaded = loaded;
        result.pageCountAlias = pageCountAlias;
        result.summary = copyMap(summary);
        if (aggregationResults != null) {
            result.aggregationResults = new ArrayList<>();
            for (AggregationResult aggregation : aggregationResults) {
                AggregationResult copy = new AggregationResult();
                copy.setName(aggregation.getName());
                List<AggregationItem> items = new ArrayList<>();
                if (aggregation.getData() != null) for (AggregationItem item : aggregation.getData()) {
                    AggregationItem copied = new AggregationItem();
                    item.getValues().forEach((key, value) -> copied.addValue(key, copyValue(value)));
                    item.getDimensions().forEach((key, value) -> copied.addDimension(key, copyValue(value)));
                    items.add(copied);
                }
                copy.setData(items);
                result.aggregationResults.add(copy);
            }
        }
        if (facets != null) {
            result.facets = new LinkedHashMap<>();
            facets.forEach((name, facet) -> result.facets.put(name, facet.map(SmartList::copyValue)));
        }
        return result;
    }

    public SmartList<T> filter(Predicate<? super T> predicate) {
        SmartList<T> result = new SmartList<>(data.stream().filter(predicate).toList());
        result.loaded = loaded;
        return result;
    }

    public <R> SmartList<R> flatMap(Function<? super T, ? extends Iterable<R>> mapper) {
        SmartList<R> result = new SmartList<>();
        for (T item : data) for (R value : mapper.apply(item)) result.add(value);
        result.loaded = loaded;
        return result;
    }

    public <K> void mergeBy(Iterable<T> incoming, Function<? super T, K> key) {
        ensureMutable();
        Map<K, Integer> positions = new HashMap<>();
        for (int index = 0; index < size(); index++) positions.put(key.apply(data.get(index)), index);
        for (T item : incoming) {
            K itemKey = key.apply(item);
            Integer position = positions.get(itemKey);
            if (position != null) set(position, item);
            else { positions.put(itemKey, size()); add(item); }
        }
    }

    private static Map<String, Object> copyMap(Map<String, Object> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (values != null) values.forEach((key, value) -> result.put(key, copyValue(value)));
        return result;
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(key, copyValue(item)));
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            list.forEach(item -> copy.add(copyValue(item)));
            return copy;
        }
        if (value instanceof Set<?> set) {
            Set<Object> copy = new LinkedHashSet<>();
            set.forEach(item -> copy.add(copyValue(item)));
            return copy;
        }
        return value;
    }

    private void ensureMutable() {
        if (immutable) {
            throw new UnsupportedOperationException("A shared empty SmartList is immutable");
        }
    }
}
