package com.sismics.docs.core.dao.jpa;

import com.sismics.docs.BaseTransactionalTest;
import com.sismics.docs.core.constant.MetadataType;
import com.sismics.docs.core.dao.AuthenticationTokenDao;
import com.sismics.docs.core.dao.MetadataDao;
import com.sismics.docs.core.dao.UserDao;
import com.sismics.docs.core.dao.criteria.MetadataCriteria;
import com.sismics.docs.core.dao.dto.MetadataDto;
import com.sismics.docs.core.model.jpa.AuthenticationToken;
import com.sismics.docs.core.model.jpa.Metadata;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.util.TransactionUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

/**
 * Tests for AuthenticationTokenDao and MetadataDao.
 */
public class TestDaoCoverage extends BaseTransactionalTest {
    @Test
    public void testAuthenticationTokenDao() throws Exception {
        // Create dependencies
        UserDao userDao = new UserDao();
        AuthenticationTokenDao tokenDao = new AuthenticationTokenDao();
        User user = createUser("testTokenUser");
        
        // Test create
        AuthenticationToken token = new AuthenticationToken();
        token.setUserId(user.getId());
        String tokenId = tokenDao.create(token);
        TransactionUtil.commit();
        
        // Test get
        AuthenticationToken createdToken = tokenDao.get(tokenId);
        Assert.assertNotNull(createdToken);
        Assert.assertEquals(user.getId(), createdToken.getUserId());
        
        // Test updateLastConnectionDate
        tokenDao.updateLastConnectionDate(tokenId);
        TransactionUtil.commit();
        
        // Test getByUserId
        List<AuthenticationToken> tokenList = tokenDao.getByUserId(user.getId());
        Assert.assertEquals(1, tokenList.size());
        
        // Test deleteByUserId
        tokenDao.deleteByUserId(user.getId(), "dummy_id");
        TransactionUtil.commit();
        Assert.assertEquals(0, tokenDao.getByUserId(user.getId()).size());
        
        // Test deleteOldSessionToken
        tokenDao.deleteOldSessionToken(user.getId());
        TransactionUtil.commit();
        
        // Test delete (success case)
        tokenDao.delete(tokenId);
        TransactionUtil.commit();
        
        // Test delete (failure case)
        try {
            tokenDao.delete("invalid_token_id");
            Assert.fail("Expected exception was not thrown");
        } catch (Exception e) {
            Assert.assertEquals("Token not found: invalid_token_id", e.getMessage());
        }
    }

    @Test
    public void testMetadataDao() throws Exception {
        // Create dependencies
        MetadataDao metadataDao = new MetadataDao();
        User user = createUser("testMetadataUser");
        
        // Test create
        Metadata metadata = new Metadata();
        metadata.setName("Test Metadata");
        metadata.setType(MetadataType.STRING);  // 使用正确的enum值
        String metadataId = metadataDao.create(metadata, user.getId());
        TransactionUtil.commit();
        
        // Test getActiveById
        Metadata createdMetadata = metadataDao.getActiveById(metadataId);
        Assert.assertNotNull(createdMetadata);
        Assert.assertEquals("Test Metadata", createdMetadata.getName());
        
        // Test update
        Metadata updateMetadata = new Metadata();
        updateMetadata.setId(metadataId);
        updateMetadata.setName("Updated Metadata");
        Metadata updatedMetadata = metadataDao.update(updateMetadata, user.getId());
        TransactionUtil.commit();
        Assert.assertEquals("Updated Metadata", updatedMetadata.getName());
        
        // Test findByCriteria
        List<MetadataDto> metadataList = metadataDao.findByCriteria(
            new MetadataCriteria(), null);
        Assert.assertFalse(metadataList.isEmpty());
        Assert.assertEquals(metadataId, metadataList.get(0).getId());
        
        // Test delete
        metadataDao.delete(metadataId, user.getId());
        TransactionUtil.commit();
        Assert.assertNull(metadataDao.getActiveById(metadataId));
    }

    @Override
    public User createUser(String username) throws Exception {
        UserDao userDao = new UserDao();
        User user = new User();
        user.setUsername(username);
        user.setPassword("12345678");
        user.setEmail(username + "@docs.com");
        user.setRoleId("admin");
        user.setStorageQuota(10L);
        
        // 假设系统管理员用户ID为"admin"，或者使用其他合适的创建者ID
        String creatorUserId = "admin"; 
        String userId = userDao.create(user, creatorUserId);
        
        // 设置返回对象的ID
        user.setId(userId);
        return user;
    }

}