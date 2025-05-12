package com.sismics.docs.rest.resource;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sismics.docs.core.constant.ConfigType;
import com.sismics.docs.core.dao.FileDao;
import com.sismics.docs.core.model.jpa.File;
import com.sismics.docs.core.service.TranslationService;
import com.sismics.docs.core.util.ConfigUtil;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.exception.ServerException;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Translation REST resources.
 * 
 * @author [Your Name]
 */
@Path("/file/translate")
public class TranslationResource extends BaseResource {
    
    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(TranslationResource.class);
    
    /**
     * Start a translation job for a file.
     *
     * @api {post} /file/translate/start Start a file translation job
     * @apiName PostFileTranslateStart
     * @apiGroup File
     * @apiParam {String} id File ID
     * @apiParam {String} source_language Source language code
     * @apiParam {String} target_language Target language code
     * @apiSuccess {String} status Status OK
     * @apiSuccess {String} flow_number Translation job flow number
     * @apiError (client) ForbiddenError Access denied
     * @apiError (client) NotFound File not found
     * @apiError (client) ValidationError Validation error
     * @apiError (server) TranslationError Error starting translation
     * @apiPermission user
     * @apiVersion 1.5.0
     *
     * @param fileId File ID
     * @param sourceLanguage Source language code
     * @param targetLanguage Target language code
     * @return Response
     */
    @POST
    @Path("start")
    public Response startTranslation(
            @FormParam("id") String fileId,
            @FormParam("source_language") String sourceLanguage,
            @FormParam("target_language") String targetLanguage) {
        
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        
        // Validate input
        if (StringUtils.isEmpty(fileId)) {
            throw new ClientException("ValidationError", "File ID is required");
        }
        if (StringUtils.isEmpty(sourceLanguage)) {
            throw new ClientException("ValidationError", "Source language is required");
        }
        if (StringUtils.isEmpty(targetLanguage)) {
            throw new ClientException("ValidationError", "Target language is required");
        }
        
        // Check API configuration
        String appKey = ConfigUtil.getConfigStringValue(ConfigType.YOUDAO_APP_KEY);
        String appSecret = ConfigUtil.getConfigStringValue(ConfigType.YOUDAO_APP_SECRET);
        if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(appSecret)) {
            throw new ServerException("ConfigError", "Translation API not configured");
        }
        
        // Check if file exists and user has access
        FileDao fileDao = new FileDao();
        File file = fileDao.getActiveById(fileId);
        if (file == null) {
            throw new ClientException("NotFound", "File not found");
        }
        
        // Start translation job
        TranslationService translationService = new TranslationService();
        String flowNumber = translationService.uploadForTranslation(fileId, sourceLanguage, targetLanguage, principal.getId());
        
        if (flowNumber == null) {
            throw new ServerException("TranslationError", "Error starting translation job");
        }
        
        // Return response
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("status", "ok")
                .add("flow_number", flowNumber);
        
        return Response.ok().entity(response.build()).build();
    }
    
    /**
     * Check the status of a translation job.
     *
     * @api {get} /file/translate/status Check translation job status
     * @apiName GetFileTranslateStatus
     * @apiGroup File
     * @apiParam {String} flow_number Translation job flow number
     * @apiSuccess {String} status Job status
     * @apiSuccess {String} status_code Numeric status code
     * @apiSuccess {String} status_text Status description
     * @apiError (client) ForbiddenError Access denied
     * @apiError (client) ValidationError Validation error
     * @apiError (server) TranslationError Error checking translation status
     * @apiPermission user
     * @apiVersion 1.5.0
     *
     * @param flowNumber Translation job flow number
     * @return Response
     */
    @GET
    @Path("status")
    public Response checkStatus(@QueryParam("flow_number") String flowNumber) {
        
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        
        // Validate input
        if (StringUtils.isEmpty(flowNumber)) {
            throw new ClientException("ValidationError", "Flow number is required");
        }
        
        // Check status
        TranslationService translationService = new TranslationService();
        Map<String, Object> statusInfo = translationService.checkTranslationStatus(flowNumber);
        
        if (statusInfo == null || statusInfo.isEmpty()) {
            throw new ServerException("TranslationError", "Error checking translation status");
        }
        
        // Build response
        JsonObjectBuilder response = Json.createObjectBuilder();
        
        if ("0".equals(statusInfo.get("errorCode"))) {
            response.add("status", "ok");
            
            // Add status information
            if (statusInfo.containsKey("status")) {
                int statusCode = (Integer) statusInfo.get("status");
                response.add("status_code", statusCode);
                
                String statusText;
                switch (statusCode) {
                    case 1:
                        statusText = "Uploading";
                        break;
                    case 2:
                        statusText = "Converting";
                        break;
                    case 3:
                        statusText = "Translating";
                        break;
                    case 4:
                        statusText = "Completed";
                        break;
                    case 5:
                        statusText = "Generating";
                        break;
                    case -1:
                        statusText = "Upload failed";
                        break;
                    case -2:
                        statusText = "Conversion failed";
                        break;
                    case -3:
                        statusText = "Translation failed";
                        break;
                    case -4:
                        statusText = "Cancelled";
                        break;
                    case -5:
                        statusText = "Generation failed";
                        break;
                    case -10:
                        statusText = "Translation failed";
                        break;
                    case -11:
                        statusText = "File deleted";
                        break;
                    default:
                        statusText = "Unknown";
                        break;
                }
                
                response.add("status_text", statusText);
            }
            
            if (statusInfo.containsKey("statusString")) {
                response.add("status_message", (String) statusInfo.get("statusString"));
            }
        } else {
            response.add("status", "error");
            response.add("error_code", (String) statusInfo.get("errorCode"));
        }
        
        return Response.ok().entity(response.build()).build();
    }
    
    /**
     * Download a translated file.
     *
     * @api {get} /file/translate/download Download translated file
     * @apiName GetFileTranslateDownload
     * @apiGroup File
     * @apiParam {String} flow_number Translation job flow number
     * @apiParam {String} file_type File type (docx, pdf, etc.)
     * @apiParam {String} file_id Original file ID
     * @apiParam {String} target_language Target language code
     * @apiSuccess {Object} file The translated file
     * @apiError (client) ForbiddenError Access denied
     * @apiError (client) ValidationError Validation error
     * @apiError (server) TranslationError Error downloading translated file
     * @apiPermission user
     * @apiVersion 1.5.0
     *
     * @param flowNumber Translation job flow number
     * @param fileType File type
     * @param fileId Original file ID
     * @param targetLanguage Target language code
     * @return Response
     */
    @GET
    @Path("download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadTranslation(
            @QueryParam("flow_number") String flowNumber,
            @QueryParam("file_type") String fileType,
            @QueryParam("file_id") String fileId,
            @QueryParam("target_language") String targetLanguage) {
        
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        
        // Validate input
        if (StringUtils.isEmpty(flowNumber)) {
            throw new ClientException("ValidationError", "Flow number is required");
        }
        if (StringUtils.isEmpty(fileType)) {
            throw new ClientException("ValidationError", "File type is required");
        }
        if (StringUtils.isEmpty(fileId)) {
            throw new ClientException("ValidationError", "File ID is required");
        }
        
        // Get the original file to determine the name
        FileDao fileDao = new FileDao();
        File originalFile = fileDao.getActiveById(fileId);
        if (originalFile == null) {
            throw new ClientException("NotFound", "Original file not found");
        }
        
        // Generate a name for the translated file
        String originalName = originalFile.getName();
        String fileName = generateTranslatedFileName(originalName, targetLanguage);
        
        // Download the translated file
        TranslationService translationService = new TranslationService();
        byte[] fileData = translationService.downloadTranslatedDocument(flowNumber, fileType);
        
        if (fileData == null || fileData.length == 0) {
            throw new ServerException("TranslationError", "Error downloading translated file");
        }
        
        // Return the file
        return Response.ok(fileData)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();
    }
    
    /**
     * Get supported languages for translation.
     *
     * @api {get} /file/translate/languages Get supported languages
     * @apiName GetFileTranslateLanguages
     * @apiGroup File
     * @apiSuccess {Object[]} languages List of supported languages
     * @apiSuccess {String} languages.code Language code
     * @apiSuccess {String} languages.name Language name
     * @apiError (client) ForbiddenError Access denied
     * @apiPermission user
     * @apiVersion 1.5.0
     *
     * @return Response
     */
    @GET
    @Path("languages")
    public Response getSupportedLanguages() {
        
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        
        // Build language list
        JsonArrayBuilder languages = Json.createArrayBuilder();
        
        // Source languages
        languages.add(Json.createObjectBuilder()
                .add("code", "zh-CHS")
                .add("name", "Chinese")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "en")
                .add("name", "English")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "ja")
                .add("name", "Japanese")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "ko")
                .add("name", "Korean")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "ru")
                .add("name", "Russian")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "fr")
                .add("name", "French")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "es")
                .add("name", "Spanish")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "pt")
                .add("name", "Portuguese")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "de")
                .add("name", "German")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "it")
                .add("name", "Italian")
                .build());

        languages.add(Json.createObjectBuilder()
                .add("code", "th")
                .add("name", "Thai")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "vi")
                .add("name", "Vietnamese")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "id")
                .add("name", "Indonesian")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "ar")
                .add("name", "Arabic")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "nl")
                .add("name", "Dutch")
                .build());
        
        languages.add(Json.createObjectBuilder()
                .add("code", "hi")
                .add("name", "Hindi")
                .build());
        
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("languages", languages);
        
        return Response.ok().entity(response.build()).build();
    }
    
    /**
     * Generate a file name for translated file.
     * 
     * @param originalName Original file name
     * @param targetLanguage Target language code
     * @return Generated file name
     */
    private String generateTranslatedFileName(String originalName, String targetLanguage) {
        if (originalName == null) {
            return "translated_file";
        }
        
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            String nameWithoutExt = originalName.substring(0, dotIndex);
            String extension = originalName.substring(dotIndex);
            return nameWithoutExt + "_" + targetLanguage + extension;
        } else {
            return originalName + "_" + targetLanguage;
        }
    }
}