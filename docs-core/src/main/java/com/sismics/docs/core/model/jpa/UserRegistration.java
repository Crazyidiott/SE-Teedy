package com.sismics.docs.core.model.jpa;

import com.google.common.base.MoreObjects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;

/**
 * User registration request entity.
 * Used to store registration requests from guests that need admin approval.
 * 
 * @author [your name]
 */
@Entity
@Table(name = "T_USER_REGISTRATION")
public class UserRegistration implements Loggable {
    /**
     * Registration request ID.
     */
    @Id
    @Column(name = "REG_ID_C", length = 36)
    private String id;
    
    /**
     * Requested username.
     */
    @Column(name = "REG_USERNAME_C", nullable = false, length = 50)
    private String username;
    
    /**
     * Password (hashed).
     */
    @Column(name = "REG_PASSWORD_C", nullable = false, length = 60)
    private String password;
    
    /**
     * Email address.
     */
    @Column(name = "REG_EMAIL_C", nullable = false, length = 100)
    private String email;
    
    /**
     * Creation date of the registration request.
     */
    @Column(name = "REG_CREATEDATE_D", nullable = false)
    private Date createDate;
    
    /**
     * Approval/rejection date.
     */
    @Column(name = "REG_APPROVALDATE_D")
    private Date approvalDate;
    
    /**
     * Admin user ID who approved/rejected the request.
     */
    @Column(name = "REG_APPROVEDBY_C", length = 36)
    private String approvedBy;
    
    /**
     * Status of the registration request.
     * Possible values: PENDING, APPROVED, REJECTED
     */
    @Column(name = "REG_STATUS_C", nullable = false, length = 20)
    private String status;
    
    /**
     * Optional message from the user or admin.
     */
    @Column(name = "REG_MESSAGE_C", length = 500)
    private String message;
    
    public String getId() {
        return id;
    }

    public UserRegistration setId(String id) {
        this.id = id;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public UserRegistration setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UserRegistration setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserRegistration setEmail(String email) {
        this.email = email;
        return this;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public UserRegistration setCreateDate(Date createDate) {
        this.createDate = createDate;
        return this;
    }

    public Date getApprovalDate() {
        return approvalDate;
    }

    public UserRegistration setApprovalDate(Date approvalDate) {
        this.approvalDate = approvalDate;
        return this;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public UserRegistration setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public UserRegistration setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public UserRegistration setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public Date getDeleteDate() {
        // Registration requests don't have a delete date field
        // They are managed by status changes instead
        return null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("username", username)
                .add("email", email)
                .add("status", status)
                .toString();
    }

    @Override
    public String toMessage() {
        return username + " (" + status + ")";
    }
}