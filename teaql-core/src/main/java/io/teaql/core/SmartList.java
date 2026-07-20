package io.teaql.core;

import java.util.ArrayList;
import java.util.HashMap;
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

public class SmartList<T extends Entity> implements Iterable<T> {
    List<T> data = new ArrayList<T>();

    List<AggregationResult> aggregationResults = new ArrayList<>();

    Map<String, SmartList> facets = new HashMap<>();

    public SmartList() {
    }

    public SmartList(List<T> data) {
        if (data != null) {
            this.data.addAll(data);
        }
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
        return identityMap(Entity::getId);
    }

    public <R> Map<R, List<T>> groupBy(Function<T, R> key) {
        return CollStreamUtil.groupByKey(data, key, false);
    }

    public void add(T pValue) {
        data.add(pValue);
    }

    public void set(int index, T pValue) {
        data.set(index, pValue);
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> pData) {
        data = pData;
    }

    public void addAggregationResult(UserContext userContext, AggregationResult aggregationResult) {
        aggregationResults.add(aggregationResult);
    }

    public List<AggregationResult> getAggregationResults() {
        return aggregationResults;
    }

    public void setAggregationResults(List<AggregationResult> pAggregationResults) {
        aggregationResults = pAggregationResults;
    }

    public int size() {
        return data.size();
    }

    public T get(int index) {
        return data.get(index);
    }

    public SmartList<T> save(UserContext userContext) {
        userContext.saveGraph(this);
        return this;
    }


    public int getTotalCount() {
        if (ObjectUtil.isEmpty(aggregationResults)) {
            return size();
        }
        Map<String, Object> numberProps = aggregationNumberProperties();
        if (numberProps.isEmpty()) {
            return size();
        }
        Object count = numberProps.get(TeaQLConstants.ROOT_LIST_PARAMETER_NAME);
        if (count instanceof Number intCount) {
            return intCount.intValue();
        }
        throw new IllegalStateException("Number prop is expected a number, but it is now a " + count.getClass().getSimpleName());

        //return aggregationResults.get(0).toInt();
    }

    public Map<String, Object> aggregationProperties(Class<?> clazz) {
        if (ObjectUtil.isEmpty(aggregationResults)) {
            return MapUtil.empty();
        }
        Map<String, Object> result = MapUtil.createMap(HashMap.class);
        getAggregationResults().forEach(aggregationResult -> {

            //Map s=aggregationResult.toSimpleMap();
            List<Map<String, Object>> resultList = aggregationResult.valueList();

            resultList.forEach(map -> {
                map.entrySet().forEach(stringObjectEntry -> {

                    if (clazz.isAssignableFrom(stringObjectEntry.getValue().getClass())) {
                        result.put(stringObjectEntry.getKey(), stringObjectEntry.getValue());
                    }

                });
            });

        });
        return result;
    }

    public void addFacet(String name, SmartList facet) {
        facets.put(name, facet);
    }

    public Map<String, SmartList> getFacets() {
        return facets;
    }

    public void setFacets(Map<String, SmartList> facets) {
        this.facets = facets;
    }

    public SmartList getFacet(String name) {
        return facets.get(name);
    }

    public SmartList removeFacet(String name) {
        return facets.remove(name);
    }

    public void clearFacets() {
        facets.clear();
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
        return data.removeIf(filter);
    }
}
