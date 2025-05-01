package com.sismics.docs.rest.resource;

import com.google.common.base.Strings;
import com.sismics.docs.core.constant.Constants;
import com.sismics.docs.core.dao.UserDao;
import com.sismics.docs.core.dao.UserRegistrationDao;
import com.sismics.docs.core.dao.criteria.UserRegistrationCriteria;
import com.sismics.docs.core.dao.dto.UserRegistrationDto;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.model.jpa.UserRegistration;
import com.sismics.docs.core.util.jpa.PaginatedList;
import com.sismics.docs.core.util.jpa.SortCriteria;
import com.sismics.docs.rest.constant.BaseFunction;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.rest.util.ValidationUtil;
import com.sismics.security.IPrincipal;
import com.sismics.security.UserPrincipal;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User registration request REST resources.
 * 
 * @author [your name]
 */
@Path("/user/registration")
public class UserRegistrationResource extends BaseResource {
    
    /**
     * Submit a new user registration request.
     * 
     * @api {put} /user/registration Submit a new user registration request
     * @apiName PutUserRegistration
     * @apiGroup UserRegistration
     * @apiParam {String{3..50}} username Username
     * @apiParam {String{8..50}} password Password
     * @apiParam {String{1..100}} email E-mail
     * @apiParam {String{0..500}} message Optional message to administrators
     * @apiSuccess {String} status Status OK
     * @apiError (client) ForbiddenError Access denied or already logged in
     * @apiError (client) ValidationError Validation error
     * @apiError (client) AlreadyExistingUsername Username already taken
     * @apiError (client) RegistrationPending Registration already pending for this username
     * @apiError (server) UnknownError Unknown server error
     * @apiPermission none
     * @apiVersion 1.5.0
     * 
     * @param username Username
     * @param password Password
     * @param email Email
     * @param message Optional message
     * @return Response
     */
    @PUT
    public Response register(
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("email") String email,
            @FormParam("message") String message) {
        
        // Check if user is already logged in
        if (authenticate()) {
            // throw new ForbiddenClientException("Cannot register while logged in");
            throw new ForbiddenClientException();
        }
        
        // Validate the input data
        username = ValidationUtil.validateLength(username, "username", 3, 50);
        ValidationUtil.validateUsername(username, "username");
        password = ValidationUtil.validateLength(password, "password", 8, 50);
        email = ValidationUtil.validateLength(email, "email", 1, 100);
        ValidationUtil.validateEmail(email, "email");
        message = ValidationUtil.validateLength(message, "message", 0, 500, true);
        
        // Check if the username is already used
        UserDao userDao = new UserDao();
        User user = userDao.getActiveByUsername(username);
        if (user != null) {
            throw new ClientException("AlreadyExistingUsername", "Username already used");
        }
        
        // Check if registration is already pending for this username
        UserRegistrationDao userRegistrationDao = new UserRegistrationDao();
        UserRegistration existingRegistration = userRegistrationDao.getByUsername(username);
        if (existingRegistration != null) {
            throw new ClientException("RegistrationPending", "Registration already pending for this username");
        }
        
        // Create the registration request
        UserRegistration userRegistration = new UserRegistration();
        userRegistration.setUsername(username);
        userRegistration.setPassword(password); // Note: password will be hashed when creating the actual user
        userRegistration.setEmail(email);
        userRegistration.setMessage(message);
        
        try {
            userRegistrationDao.create(userRegistration);
        } catch (Exception e) {
            throw new ServerException("UnknownError", "Unknown server error", e);
        }
        
        // Always return OK
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("status", "ok");
        return Response.ok().entity(response.build()).build();
    }
    
    /**
     * Returns the list of all registration requests.
     * 
     * @api {get} /user/registration Get registration requests
     * @apiName GetUserRegistration
     * @apiGroup UserRegistration
     * @apiParam {Number} offset Offset
     * @apiParam {Number} limit Limit
     * @apiParam {String} [status] Filter by status (PENDING, APPROVED, REJECTED)
     * @apiParam {String} [search] Search query
     * @apiParam {Number} sort_column Column index to sort on
     * @apiParam {Boolean} asc If true, sort in ascending order
     * @apiSuccess {Number} total Total number of registration requests
     * @apiSuccess {Object[]} registrations List of registration requests
     * @apiSuccess {String} registrations.id ID
     * @apiSuccess {String} registrations.username Username
     * @apiSuccess {String} registrations.email E-mail
     * @apiSuccess {String} registrations.status Status (PENDING, APPROVED, REJECTED)
     * @apiSuccess {Number} registrations.create_date Create date (timestamp)
     * @apiSuccess {Number} [registrations.approval_date] Approval date (timestamp)
     * @apiSuccess {String} [registrations.approved_by_username] Admin username who approved/rejected the request
     * @apiSuccess {String} [registrations.message] Optional message
     * @apiError (client) ForbiddenError Access denied
     * @apiPermission admin
     * @apiVersion 1.5.0
     * 
     * @param offset Page offset
     * @param limit Page limit
     * @param status Filter by status
     * @param search Search query
     * @param sortColumn Sort column
     * @param asc If true, ascending sort
     * @return Response
     */
    @GET
    public Response list(
            @QueryParam("offset") Integer offset,
            @QueryParam("limit") Integer limit,
            @QueryParam("status") String status,
            @QueryParam("search") String search,
            @QueryParam("sort_column") Integer sortColumn,
            @QueryParam("asc") Boolean asc) {
        
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);
        
        // Initialize the criteria
        UserRegistrationCriteria criteria = new UserRegistrationCriteria();
        criteria.setStatus(status);
        criteria.setSearch(search);
        
        // Initialize the sort criteria
        SortCriteria sortCriteria = new SortCriteria(sortColumn, asc);
        
        // Get the requests
        PaginatedList<UserRegistrationDto> paginatedList;
        UserRegistrationDao userRegistrationDao = new UserRegistrationDao();
        try {
            paginatedList = userRegistrationDao.findByCriteria(
                    criteria, sortCriteria, limit == null ? 50 : limit, offset == null ? 0 : offset);
        } catch (Exception e) {
            throw new ServerException("SearchError", "Error searching for registration requests", e);
        }
        
        // Build the response
        JsonObjectBuilder response = Json.createObjectBuilder();
        response.add("total", paginatedList.getResultCount());
        
        JsonArrayBuilder registrations = Json.createArrayBuilder();
        for (UserRegistrationDto userRegistrationDto : paginatedList.getResultList()) {
            JsonObjectBuilder registration = Json.createObjectBuilder()
                    .add("id", userRegistrationDto.getId())
                    .add("username", userRegistrationDto.getUsername())
                    .add("email", userRegistrationDto.getEmail())
                    .add("status", userRegistrationDto.getStatus())
                    .add("create_date", userRegistrationDto.getCreateTimestamp());
            
            if (userRegistrationDto.getApprovalTimestamp() != null) {
                registration.add("approval_date", userRegistrationDto.getApprovalTimestamp());
            }
            
            if (!Strings.isNullOrEmpty(userRegistrationDto.getApprovedByUsername())) {
                registration.add("approved_by_username", userRegistrationDto.getApprovedByUsername());
            }
            
            if (!Strings.isNullOrEmpty(userRegistrationDto.getMessage())) {
                registration.add("message", userRegistrationDto.getMessage());
            }
            
            registrations.add(registration);
        }
        
        response.add("registrations", registrations);
        
        return Response.ok().entity(response.build()).build();
    }
    
    /**
     * Returns a registration request.
     * 
     * @api {get} /user/registration/:id Get a registration request
     * @apiName GetUserRegistrationId
     * @apiGroup UserRegistration
     * @apiParam {String} id Registration request ID
     * @apiSuccess {String} id ID
     * @apiSuccess {String} username Username
     * @apiSuccess {String} email E-mail
     * @apiSuccess {String} status Status (PENDING, APPROVED, REJECTED)
     * @apiSuccess {Number} create_date Create date (timestamp)
     * @apiSuccess {Number} [approval_date] Approval date (timestamp)
     * @apiSuccess {String} [approved_by_username] Admin username who approved/rejected the request
     * @apiSuccess {String} [message] Optional message
     * @apiError (client) ForbiddenError Access denied
     * @apiError (client) RegistrationNotFound Registration request not found
     * @apiPermission admin
     * @apiVersion 1.5.0
     * 
     * @param id Registration request ID
     * @return Response
     */
    @GET
    @Path("{id: [a-z0-9\\-]+}")
    public Response get(@PathParam("id") String id) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);
        
        // Get the registration request
        UserRegistrationDao userRegistrationDao = new UserRegistrationDao();
        UserRegistration userRegistration = userRegistrationDao.getById(id);
        if (userRegistration == null) {
            throw new ClientException("RegistrationNotFound", "Registration request not found");
        }
        
        // Get admin username if available
        String approvedByUsername = null;
        if (userRegistration.getApprovedBy() != null) {
            UserDao userDao = new UserDao();
            User approvedByUser = userDao.getById(userRegistration.getApprovedBy());
            if (approvedByUser != null) {
                approvedByUsername = approvedByUser.getUsername();
            }
        }
        
        // Build the response
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("id", userRegistration.getId())
                .add("username", userRegistration.getUsername())
                .add("email", userRegistration.getEmail())
                .add("status", userRegistration.getStatus())
                .add("create_date", userRegistration.getCreateDate().getTime());
        
        if (userRegistration.getApprovalDate() != null) {
            response.add("approval_date", userRegistration.getApprovalDate().getTime());
        }
        
        if (approvedByUsername != null) {
            response.add("approved_by_username", approvedByUsername);
        }
        
        if (userRegistration.getMessage() != null) {
            response.add("message", userRegistration.getMessage());
        }
        
        return Response.ok().entity(response.build()).build();
    }
    
    /**
     * Approves a registration request.
     * 
     * @api {post} /user/registration/:id/approve Approve a registration request
     * @apiName PostUserRegistrationApprove
     * @apiGroup UserRegistration
     * @apiParam {String} id Registration request ID
     * @apiParam {String{0..500}} [message] Optional message to send to the user
     * @apiParam {Number} [storage_quota] Storage quota (in bytes) to assign to the new user
     * @apiSuccess {String} status Status OK
     * @apiError (client) ForbiddenError Access denied
     * @apiError (client) RegistrationNotFound Registration request not found
     * @apiError (client) AlreadyProcessed Registration request already processed
     * @apiError (client) ValidationError Validation error
     * @apiError (server) UserCreationError Error creating the user
     * @apiPermission admin
     * @apiVersion 1.5.0
     * 
     * @param id Registration request ID
     * @param message Optional message
     * @param storageQuotaStr Storage quota
     * @return Response
     */
    @POST
    @Path("{id: [a-z0-9\\-]+}/approve")
    public Response approve(
            @PathParam("id") String id,
            @FormParam("message") String message,
            @FormParam("storage_quota") String storageQuotaStr) {
        
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);
        
        // Validate input data
        message = ValidationUtil.validateLength(message, "message", 0, 500, true);
        Long storageQuota = null;
        if (!Strings.isNullOrEmpty(storageQuotaStr)) {
            storageQuota = ValidationUtil.validateLong(storageQuotaStr, "storage_quota");
        }
        
        // Get the registration request
        UserRegistrationDao userRegistrationDao = new UserRegistrationDao();
        UserRegistration userRegistration = userRegistrationDao.getById(id);
        if (userRegistration == null) {
            throw new ClientException("RegistrationNotFound", "Registration request not found");
        }
        
        // Check if already processed
        if (!"PENDING".equals(userRegistration.getStatus())) {
            throw new ClientException("AlreadyProcessed", "Registration request already processed");
        }
        
        // Update the registration request
        userRegistration.setStatus("APPROVED");
        userRegistration.setApprovalDate(new Date());
        userRegistration.setApprovedBy(principal.getId());
        if (message != null) {
            userRegistration.setMessage(message);
        }
        userRegistrationDao.update(userRegistration);
        
        // Create the user
        UserDao userDao = new UserDao();
        User user = new User();
        user.setRoleId(Constants.DEFAULT_USER_ROLE);
        user.setUsername(userRegistration.getUsername()); // Password will be hashed by UserDao.create()
        user.setPassword(userRegistration.getPassword());
        user.setEmail(userRegistration.getEmail());
        user.setStorageQuota(storageQuota != null ? storageQuota : Constants.DEFAULT_USER_STORAGE_QUOTA);
        user.setOnboarding(true);
        
        try {
            userDao.create(user, principal.getId());
        } catch (Exception e) {
            if ("AlreadyExistingUsername".equals(e.getMessage())) {
                throw new ClientException("AlreadyExistingUsername", "Username already used", e);
            } else {
                throw new ServerException("UserCreationError", "Error creating the user", e);
            }
        }
        
        // Always return OK
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("status", "ok");
        return Response.ok().entity(response.build()).build();
    }
    
    /**
     * Rejects a registration request.
     * 
     * @api {post} /user/registration/:id/reject Reject a registration request
     * @apiName PostUserRegistrationReject
     * @apiGroup UserRegistration
     * @apiParam {String} id Registration request ID
     * @apiParam {String{0..500}} [message] Optional reason for rejection
     * @apiSuccess {String} status Status OK
     * @apiError (client) ForbiddenError Access denied
     * @apiError (client) RegistrationNotFound Registration request not found
     * @apiError (client) AlreadyProcessed Registration request already processed
     * @apiError (client) ValidationError Validation error
     * @apiPermission admin
     * @apiVersion 1.5.0
     * 
     * @param id Registration request ID
     * @param message Optional message
     * @return Response
     */
    @POST
    @Path("{id: [a-z0-9\\-]+}/reject")
    public Response reject(
            @PathParam("id") String id,
            @FormParam("message") String message) {
        
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);
        
        // Validate input data
        message = ValidationUtil.validateLength(message, "message", 0, 500, true);
        
        // Get the registration request
        UserRegistrationDao userRegistrationDao = new UserRegistrationDao();
        UserRegistration userRegistration = userRegistrationDao.getById(id);
        if (userRegistration == null) {
            throw new ClientException("RegistrationNotFound", "Registration request not found");
        }
        
        // Check if already processed
        if (!"PENDING".equals(userRegistration.getStatus())) {
            throw new ClientException("AlreadyProcessed", "Registration request already processed");
        }
        
        // Update the registration request
        userRegistration.setStatus("REJECTED");
        userRegistration.setApprovalDate(new Date());
        userRegistration.setApprovedBy(principal.getId());
        if (message != null) {
            userRegistration.setMessage(message);
        }
        userRegistrationDao.update(userRegistration);
        
        // Always return OK
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("status", "ok");
        return Response.ok().entity(response.build()).build();
    }
}