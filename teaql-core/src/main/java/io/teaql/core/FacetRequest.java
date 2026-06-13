package io.teaql.core;

/**
 * take `order list` add `status` facet as an example
 * <p>
 * Q.orders().facetOfStatus("facetOfStatus", Q.orderStatuses().countOrders(), true/false)
 *
 * @author tianbo
 * @since 2026/5/18
 */
public class FacetRequest {

    /**
     * the facetName,  like "facetOfStatus"
     */
    private String facetName;

    /**
     * the relation name for facet,  `status` in the example
     */
    private String relationName;

    /**
     * the search request for facet, `Q.orderStatuses().countOrders()` in the example
     */
    private SearchRequest request;


    /**
     * whether merge sub request criteria, true/false in the example
     */
    private boolean mergeCriteria;

    public String getFacetName() {
        return facetName;
    }

    public void setFacetName(String facetName) {
        this.facetName = facetName;
    }

    public SearchRequest getRequest() {
        return request;
    }

    public void setRequest(SearchRequest request) {
        this.request = request;
    }

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    public boolean isMergeCriteria() {
        return mergeCriteria;
    }

    public void setMergeCriteria(boolean mergeCriteria) {
        this.mergeCriteria = mergeCriteria;
    }
}
