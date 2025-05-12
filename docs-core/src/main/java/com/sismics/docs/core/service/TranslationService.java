package com.sismics.docs.core.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sismics.docs.core.constant.ConfigType;
import com.sismics.docs.core.dao.FileDao;
import com.sismics.docs.core.dao.UserDao;
import com.sismics.docs.core.model.jpa.File;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.util.ConfigUtil;
import com.sismics.docs.core.util.DirectoryUtil;
import com.sismics.docs.core.util.EncryptionUtil;
/**
 * Translation service.
 *
 * @author [Your Name]
 */
public class TranslationService {
    
    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);
    
    /**
     * Youdao API URLs.
     */
    private static final String YOUDAO_URL_UPLOAD = "https://openapi.youdao.com/file_trans/upload";
    private static final String YOUDAO_URL_QUERY = "https://openapi.youdao.com/file_trans/query";
    private static final String YOUDAO_URL_DOWNLOAD = "https://openapi.youdao.com/file_trans/download";
    
    /**
     * Upload a document for translation.
     * 
     * @param fileId File ID to translate
     * @param sourceLanguage Source language code
     * @param targetLanguage Target language code
     * @param userId User ID initiating the translation
     * @return Flow number for tracking the translation
     */
    public String uploadForTranslation(String fileId, String sourceLanguage, String targetLanguage, String userId) {
    try {
        // 获取文件
        FileDao fileDao = new FileDao();
        File file = fileDao.getActiveById(fileId);
        if (file == null) {
            log.error("File not found: {}", fileId);
            return null;
        }
        
        // 获取用户信息以获取私钥
        UserDao userDao = new UserDao();
        User user = userDao.getById(userId);
        if (user == null) {
            log.error("User not found: {}", userId);
            return null;
        }
        
        // 解密文件到临时文件
        Path encryptedFile = DirectoryUtil.getStorageDirectory().resolve(file.getId());
        Path decryptedFile = EncryptionUtil.decryptFile(encryptedFile, user.getPrivateKey());
        
        // 读取解密后的文件内容
        byte[] decryptedContent = java.nio.file.Files.readAllBytes(decryptedFile);
        
        try {
            // 编码为Base64
            String fileBase64 = Base64.getEncoder().encodeToString(decryptedContent);
            
            // 获取API凭证
            String appKey = ConfigUtil.getConfigStringValue(ConfigType.YOUDAO_APP_KEY);
            String appSecret = ConfigUtil.getConfigStringValue(ConfigType.YOUDAO_APP_SECRET);
            
            if (StringUtils.isBlank(appKey) || StringUtils.isBlank(appSecret)) {
                log.error("Youdao API credentials not configured");
                return null;
            }
            
            // 准备请求参数
            Map<String, String> params = new HashMap<>();
            String salt = UUID.randomUUID().toString();
            String curtime = String.valueOf(System.currentTimeMillis() / 1000);
            String signStr = appKey + truncate(fileBase64) + salt + curtime + appSecret;
            String sign = getDigest(signStr);
            
            // 确定文件类型
            String fileType = getFileTypeFromMimetype(file.getMimeType());
            if (fileType == null) {
                log.error("Unsupported file type for translation: {}", file.getMimeType());
                return null;
            }
            
            params.put("q", fileBase64);
            params.put("fileName", file.getName());
            params.put("fileType", fileType);
            params.put("langFrom", sourceLanguage);
            params.put("langTo", targetLanguage);
            params.put("appKey", appKey);
            params.put("salt", salt);
            params.put("curtime", curtime);
            params.put("sign", sign);
            params.put("docType", "json");
            params.put("signType", "v3");
            
            // 调用有道API
            log.info("Uploading file {} ({} bytes) for translation from {} to {}", 
                     file.getName(), decryptedContent.length, sourceLanguage, targetLanguage);
            String response = sendPostRequest(YOUDAO_URL_UPLOAD, params);
            
            // 解析响应
            Map<String, Object> responseMap = parseJsonResponse(response);
            if (responseMap.containsKey("errorCode") && "0".equals(responseMap.get("errorCode"))) {
                String flowNumber = (String) responseMap.get("flownumber");
                log.info("File upload successful, flow number: {}", flowNumber);
                return flowNumber;
            } else {
                log.error("Error uploading file for translation: {}", response);
                return null;
            }
        } finally {
            // 删除临时文件
            try {
                Files.deleteIfExists(decryptedFile);
            } catch (java.io.IOException e) {
                log.warn("Could not delete temporary file: {}", decryptedFile, e);
            }
        }
    } catch (Exception e) {
        log.error("Error uploading document for translation", e);
        return null;
    }
}
    /**
     * Check the status of a translation job.
     * 
     * @param flowNumber Flow number from upload response
     * @return Status information map
     */
    public Map<String, Object> checkTranslationStatus(String flowNumber) {
        try {
            // Get API credentials from configuration
            String appKey = ConfigUtil.getConfigStringValue(ConfigType.YOUDAO_APP_KEY);
            String appSecret = ConfigUtil.getConfigStringValue(ConfigType.YOUDAO_APP_SECRET);
            
            if (StringUtils.isBlank(appKey) || StringUtils.isBlank(appSecret)) {
                log.error("Youdao API credentials not configured");
                return null;
            }
            
            // Prepare request parameters
            Map<String, String> params = new HashMap<>();
            String salt = UUID.randomUUID().toString();
            String curtime = String.valueOf(System.currentTimeMillis() / 1000);
            String signStr = appKey + truncate(flowNumber) + salt + curtime + appSecret;
            String sign = getDigest(signStr);
            
            params.put("flownumber", flowNumber);
            params.put("appKey", appKey);
            params.put("salt", salt);
            params.put("curtime", curtime);
            params.put("sign", sign);
            params.put("docType", "json");
            params.put("signType", "v3");
            
            // Call Youdao API
            String response = sendPostRequest(YOUDAO_URL_QUERY, params);
            
            // Parse response
            return parseJsonResponse(response);
            
        } catch (Exception e) {
            log.error("Error checking translation status", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Download a translated document.
     * 
     * @param flowNumber Flow number from upload response
     * @param downloadFileType File type to download
     * @return Byte array of the translated document
     */
    public byte[] downloadTranslatedDocument(String flowNumber, String downloadFileType) {
        try {
            // Get API credentials from configuration
            String appKey = ConfigUtil.getConfigStringValue(ConfigType.YOUDAO_APP_KEY);
            String appSecret = ConfigUtil.getConfigStringValue(ConfigType.YOUDAO_APP_SECRET);
            
            if (StringUtils.isBlank(appKey) || StringUtils.isBlank(appSecret)) {
                log.error("Youdao API credentials not configured");
                return null;
            }
            
            // Prepare request parameters
            Map<String, String> params = new HashMap<>();
            String salt = UUID.randomUUID().toString();
            String curtime = String.valueOf(System.currentTimeMillis() / 1000);
            String signStr = appKey + truncate(flowNumber) + salt + curtime + appSecret;
            String sign = getDigest(signStr);
            
            params.put("flownumber", flowNumber);
            params.put("downloadFileType", downloadFileType);
            params.put("appKey", appKey);
            params.put("salt", salt);
            params.put("curtime", curtime);
            params.put("sign", sign);
            params.put("docType", "json");
            params.put("signType", "v3");
            
            // Call Youdao API
            return sendPostRequestForFile(YOUDAO_URL_DOWNLOAD, params);
            
        } catch (Exception e) {
            log.error("Error downloading translated document", e);
            return null;
        }
    }
    
    /**
     * Get file type for Youdao API from MIME type.
     * 
     * @param mimeType MIME type
     * @return File type for Youdao API
     */
    private String getFileTypeFromMimetype(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        
        if (mimeType.contains("word") || mimeType.contains("docx") || mimeType.contains("doc")) {
            return "docx";
        } else if (mimeType.contains("pdf")) {
            return "pdf";
        } else if (mimeType.contains("powerpoint") || mimeType.contains("ppt")) {
            return "pptx";
        } else if (mimeType.contains("excel") || mimeType.contains("xlsx")) {
            return "xlsx";
        } else if (mimeType.contains("jpg") || mimeType.contains("jpeg")) {
            return "jpg";
        } else if (mimeType.contains("png")) {
            return "png";
        } else if (mimeType.contains("bmp")) {
            return "bmp";
        }
        
        return null;
    }
    
    /**
     * Send POST request to API.
     * 
     * @param url API URL
     * @param params Request parameters
     * @return API response as string
     */
    private String sendPostRequest(String url, Map<String, String> params) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (postData.length() != 0) {
                postData.append('&');
            }
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
        }
        
        try (OutputStream os = con.getOutputStream()) {
            os.write(postData.toString().getBytes(StandardCharsets.UTF_8));
        }
        
        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            return response.toString();
        } else {
            log.error("POST request failed with response code: {}", responseCode);
            return null;
        }
    }
    
    /**
     * Send POST request to API and get file response.
     * 
     * @param url API URL
     * @param params Request parameters
     * @return API response as byte array
     */
    private byte[] sendPostRequestForFile(String url, Map<String, String> params) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (postData.length() != 0) {
                postData.append('&');
            }
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
        }
        
        try (OutputStream os = con.getOutputStream()) {
            os.write(postData.toString().getBytes(StandardCharsets.UTF_8));
        }
        
        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Check if response is JSON (error) or file
            String contentType = con.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                // It's an error response
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                log.error("Error response from download API: {}", response.toString());
                return null;
            } else {
                // It's a file
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                java.io.InputStream is = con.getInputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                is.close();
                return baos.toByteArray();
            }
        } else {
            log.error("POST request failed with response code: {}", responseCode);
            return null;
        }
    }
    
    /**
     * Parse JSON response to Map.
     * 
     * @param jsonString JSON string
     * @return Map of JSON data
     */
    private Map<String, Object> parseJsonResponse(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Simple JSON parsing without external libraries
        Map<String, Object> result = new HashMap<>();
        try {
            // Remove { and }
            String trimmed = jsonString.trim();
            if (trimmed.startsWith("{")) {
                trimmed = trimmed.substring(1);
            }
            if (trimmed.endsWith("}")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            
            // Split by commas not inside quotes
            String[] pairs = trimmed.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    
                    // Remove quotes
                    if (key.startsWith("\"") && key.endsWith("\"")) {
                        key = key.substring(1, key.length() - 1);
                    }
                    
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                        result.put(key, value);
                    } else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                        result.put(key, Boolean.parseBoolean(value));
                    } else {
                        try {
                            result.put(key, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            result.put(key, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
        }
        
        return result;
    }
    
    /**
     * Create SHA-256 digest.
     * 
     * @param string Input string
     * @return Digest
     */
    private String getDigest(String string) {
        if (string == null) {
            return null;
        }
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        byte[] btInput = string.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest mdInst = MessageDigest.getInstance("SHA-256");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    
    /**
     * Truncate string for signing.
     * 
     * @param q Input string
     * @return Truncated string
     */
    private String truncate(String q) {
        if (q == null) {
            return null;
        }
        int len = q.length();
        return len <= 20 ? q : (q.substring(0, 10) + len + q.substring(len - 10, len));
    }
}