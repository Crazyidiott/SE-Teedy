package com.sismics.docs.core.dao.criteria;

/**
 * User registration criteria.
 *
 * @author [your name]
 */
public class UserRegistrationCriteria {
    /**
     * Search query.
     */
    private String search;
    
    /**
     * Registration status filter.
     */
    private String status;
    
    public String getSearch() {
        return search;
    }

    public UserRegistrationCriteria setSearch(String search) {
        this.search = search;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public UserRegistrationCriteria setStatus(String status) {
        this.status = status;
        return this;
    }
}