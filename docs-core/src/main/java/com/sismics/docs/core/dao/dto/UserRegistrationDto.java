package com.sismics.docs.core.dao.dto;

/**
 * User registration DTO.
 *
 * @author [your name]
 */
public class UserRegistrationDto {
    /**
     * Registration ID.
     */
    private String id;
    
    /**
     * Username.
     */
    private String username;
    
    /**
     * Email.
     */
    private String email;
    
    /**
     * Create timestamp.
     */
    private Long createTimestamp;
    
    /**
     * Approval timestamp.
     */
    private Long approvalTimestamp;
    
    /**
     * Admin user ID who approved/rejected the request.
     */
    private String approvedBy;
    
    /**
     * Admin username who approved/rejected the request.
     */
    private String approvedByUsername;
    
    /**
     * Registration status.
     */
    private String status;
    
    /**
     * Optional message.
     */
    private String message;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getCreateTimestamp() {
        return createTimestamp;
    }

    public void setCreateTimestamp(Long createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    public Long getApprovalTimestamp() {
        return approvalTimestamp;
    }

    public void setApprovalTimestamp(Long approvalTimestamp) {
        this.approvalTimestamp = approvalTimestamp;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovedByUsername() {
        return approvedByUsername;
    }

    public void setApprovedByUsername(String approvedByUsername) {
        this.approvedByUsername = approvedByUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}