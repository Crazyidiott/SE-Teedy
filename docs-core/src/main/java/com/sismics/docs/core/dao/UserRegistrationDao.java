package com.sismics.docs.core.dao;

import com.google.common.base.Strings;
import com.sismics.docs.core.constant.Constants;
import com.sismics.docs.core.dao.criteria.UserRegistrationCriteria;
import com.sismics.docs.core.dao.dto.UserRegistrationDto;
import com.sismics.docs.core.model.jpa.UserRegistration;
import com.sismics.docs.core.util.jpa.PaginatedList;
import com.sismics.docs.core.util.jpa.PaginatedLists;
import com.sismics.docs.core.util.jpa.QueryParam;
import com.sismics.docs.core.util.jpa.SortCriteria;
import com.sismics.util.context.ThreadLocalContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.util.*;

/**
 * User registration DAO.
 * 
 * @author [your name]
 */
public class UserRegistrationDao {
    /**
     * Creates a new user registration request.
     * 
     * @param userRegistration User registration
     * @return New ID
     */
    public String create(UserRegistration userRegistration) {
        // Create the UUID
        userRegistration.setId(UUID.randomUUID().toString());
        
        // Create the registration date
        userRegistration.setCreateDate(new Date());
        
        // Set initial status
        userRegistration.setStatus("PENDING");
        
        // Create the registration request
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        em.persist(userRegistration);
        
        return userRegistration.getId();
    }
    
    /**
     * Updates a user registration request.
     * 
     * @param userRegistration User registration to update
     * @return Updated user registration
     */
    public UserRegistration update(UserRegistration userRegistration) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        
        // Get the registration request
        Query q = em.createQuery("select r from UserRegistration r where r.id = :id");
        q.setParameter("id", userRegistration.getId());
        UserRegistration registrationFromDb = (UserRegistration) q.getSingleResult();
        
        // Update the registration request
        registrationFromDb.setApprovalDate(userRegistration.getApprovalDate());
        registrationFromDb.setApprovedBy(userRegistration.getApprovedBy());
        registrationFromDb.setStatus(userRegistration.getStatus());
        registrationFromDb.setMessage(userRegistration.getMessage());
        
        return registrationFromDb;
    }
    
    /**
     * Gets a user registration request by ID.
     * 
     * @param id User registration ID
     * @return User registration
     */
    public UserRegistration getById(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        try {
            Query q = em.createQuery("select r from UserRegistration r where r.id = :id");
            q.setParameter("id", id);
            return (UserRegistration) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Gets a user registration request by username.
     * 
     * @param username Username
     * @return User registration
     */
    public UserRegistration getByUsername(String username) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        try {
            Query q = em.createQuery("select r from UserRegistration r where r.username = :username and r.status = 'PENDING'");
            q.setParameter("username", username);
            return (UserRegistration) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Finds user registration requests by criteria.
     * 
     * @param criteria Search criteria
     * @param sortCriteria Sort criteria
     * @return List of user registration requests
     */
    public List<UserRegistrationDto> findByCriteria(UserRegistrationCriteria criteria, SortCriteria sortCriteria) {
        Map<String, Object> parameterMap = new HashMap<>();
        List<String> criteriaList = new ArrayList<>();
        
        StringBuilder sb = new StringBuilder("select r.REG_ID_C, r.REG_USERNAME_C, r.REG_EMAIL_C, r.REG_CREATEDATE_D, " +
                "r.REG_APPROVALDATE_D, r.REG_APPROVEDBY_C, r.REG_STATUS_C, r.REG_MESSAGE_C, u.USE_USERNAME_C ");
        
        sb.append(" from T_USER_REGISTRATION r ");
        sb.append(" left join T_USER u on u.USE_ID_C = r.REG_APPROVEDBY_C ");
        
        // Add search criteria
        if (!Strings.isNullOrEmpty(criteria.getSearch())) {
            criteriaList.add("(r.REG_USERNAME_C like :search or r.REG_EMAIL_C like :search)");
            parameterMap.put("search", "%" + criteria.getSearch() + "%");
        }
        if (criteria.getStatus() != null) {
            criteriaList.add("r.REG_STATUS_C = :status");
            parameterMap.put("status", criteria.getStatus());
        }
        
        // Add the search criteria
        if (!criteriaList.isEmpty()) {
            sb.append(" where ");
            sb.append(String.join(" and ", criteriaList));
        }
        
        // Add order
        if (sortCriteria != null) {
            String sortColumn;
            switch (sortCriteria.getColumn()) {
                case 1:
                    sortColumn = "r.REG_CREATEDATE_D";
                    break;
                case 2:
                    sortColumn = "r.REG_USERNAME_C";
                    break;
                case 3:
                    sortColumn = "r.REG_EMAIL_C";
                    break;
                case 4:
                    sortColumn = "r.REG_STATUS_C";
                    break;
                default:
                    sortColumn = "r.REG_CREATEDATE_D";
                    break;
            }
            sb.append(" order by ").append(sortColumn).append(" ").append(sortCriteria.isAsc() ? "asc" : "desc");
        } else {
            sb.append(" order by r.REG_CREATEDATE_D desc");
        }
        
        // Build the query
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query q = em.createNativeQuery(sb.toString());
        
        // Bind parameters
        for (Map.Entry<String, Object> param : parameterMap.entrySet()) {
            q.setParameter(param.getKey(), param.getValue());
        }
        
        // Execute the query
        @SuppressWarnings("unchecked")
        List<Object[]> l = q.getResultList();
        
        // Assemble results
        List<UserRegistrationDto> registrationDtoList = new ArrayList<>();
        for (Object[] o : l) {
            int i = 0;
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setId((String) o[i++]);
            dto.setUsername((String) o[i++]);
            dto.setEmail((String) o[i++]);
            dto.setCreateTimestamp(((Date) o[i++]).getTime());
            Date approvalDate = (Date) o[i++];
            if (approvalDate != null) {
                dto.setApprovalTimestamp(approvalDate.getTime());
            }
            dto.setApprovedBy((String) o[i++]);
            dto.setStatus((String) o[i++]);
            dto.setMessage((String) o[i++]);
            dto.setApprovedByUsername((String) o[i]);
            registrationDtoList.add(dto);
        }
        
        return registrationDtoList;
    }
    
    /**
     * Returns a paginated list of user registration requests.
     * 
     * @param criteria Search criteria
     * @param sortCriteria Sort criteria
     * @param limit Page size
     * @param offset Page offset
     * @return Paginated list of user registration requests
     */
    public PaginatedList<UserRegistrationDto> findByCriteria(
            UserRegistrationCriteria criteria, 
            SortCriteria sortCriteria, 
            int limit, 
            int offset) {
        PaginatedList<UserRegistrationDto> paginatedList = new PaginatedList<>(limit, offset);
        
        Map<String, Object> parameterMap = new HashMap<>();
        List<String> criteriaList = new ArrayList<>();
        
        StringBuilder sb = new StringBuilder("select r.REG_ID_C, r.REG_USERNAME_C, r.REG_EMAIL_C, r.REG_CREATEDATE_D, " +
                "r.REG_APPROVALDATE_D, r.REG_APPROVEDBY_C, r.REG_STATUS_C, r.REG_MESSAGE_C, u.USE_USERNAME_C ");
        
        sb.append(" from T_USER_REGISTRATION r ");
        sb.append(" left join T_USER u on u.USE_ID_C = r.REG_APPROVEDBY_C ");
        
        // Add search criteria
        if (!Strings.isNullOrEmpty(criteria.getSearch())) {
            criteriaList.add("(r.REG_USERNAME_C like :search or r.REG_EMAIL_C like :search)");
            parameterMap.put("search", "%" + criteria.getSearch() + "%");
        }
        if (criteria.getStatus() != null) {
            criteriaList.add("r.REG_STATUS_C = :status");
            parameterMap.put("status", criteria.getStatus());
        }
        
        // Add the search criteria
        if (!criteriaList.isEmpty()) {
            sb.append(" where ");
            sb.append(String.join(" and ", criteriaList));
        }
        
        // Count query
        StringBuilder sbCount = new StringBuilder("select count(r.REG_ID_C) as c");
        sbCount.append(" from T_USER_REGISTRATION r ");
        sbCount.append(" left join T_USER u on u.USE_ID_C = r.REG_APPROVEDBY_C ");
        if (!criteriaList.isEmpty()) {
            sbCount.append(" where ");
            sbCount.append(String.join(" and ", criteriaList));
        }
        
        // Build the count query
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query qCount = em.createNativeQuery(sbCount.toString());
        
        // Bind parameters
        for (Map.Entry<String, Object> param : parameterMap.entrySet()) {
            qCount.setParameter(param.getKey(), param.getValue());
        }
        
        // Execute the count query
        Number count = (Number) qCount.getSingleResult();
        paginatedList.setResultCount(count.intValue()); // Changed from longValue() to intValue()
        
        // Sort criteria
        String sortColumn;
        if (sortCriteria != null) {
            switch (sortCriteria.getColumn()) {
                case 1:
                    sortColumn = "r.REG_CREATEDATE_D";
                    break;
                case 2:
                    sortColumn = "r.REG_USERNAME_C";
                    break;
                case 3:
                    sortColumn = "r.REG_EMAIL_C";
                    break;
                case 4:
                    sortColumn = "r.REG_STATUS_C";
                    break;
                default:
                    sortColumn = "r.REG_CREATEDATE_D";
                    break;
            }
            sb.append(" order by ").append(sortColumn).append(" ").append(sortCriteria.isAsc() ? "asc" : "desc");
        } else {
            sb.append(" order by r.REG_CREATEDATE_D desc");
        }
        
        // Build the paginated query
        Query q = em.createNativeQuery(sb.toString());
        
        // Bind parameters
        for (Map.Entry<String, Object> param : parameterMap.entrySet()) {
            q.setParameter(param.getKey(), param.getValue());
        }
        
        // Pagination
        q.setFirstResult(offset); // Use offset parameter directly
        q.setMaxResults(limit);   // Use limit parameter directly
        
        // Execute the query
        @SuppressWarnings("unchecked")
        List<Object[]> l = q.getResultList();
        
        // Assemble results
        List<UserRegistrationDto> registrationDtoList = new ArrayList<>();
        for (Object[] o : l) {
            int i = 0;
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setId((String) o[i++]);
            dto.setUsername((String) o[i++]);
            dto.setEmail((String) o[i++]);
            dto.setCreateTimestamp(((Date) o[i++]).getTime());
            Date approvalDate = (Date) o[i++];
            if (approvalDate != null) {
                dto.setApprovalTimestamp(approvalDate.getTime());
            }
            dto.setApprovedBy((String) o[i++]);
            dto.setStatus((String) o[i++]);
            dto.setMessage((String) o[i++]);
            dto.setApprovedByUsername((String) o[i]);
            registrationDtoList.add(dto);
        }
        
        paginatedList.setResultList(registrationDtoList);
        return paginatedList;
    }
}