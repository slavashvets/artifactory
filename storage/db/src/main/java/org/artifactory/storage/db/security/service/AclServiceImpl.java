/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.storage.db.security.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.artifactory.common.ConstantValues;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.security.*;
import org.artifactory.storage.DBEntityNotFoundException;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.security.dao.AclsDao;
import org.artifactory.storage.db.security.dao.PermissionTargetsDao;
import org.artifactory.storage.db.security.dao.UserGroupsDao;
import org.artifactory.storage.db.security.entity.Ace;
import org.artifactory.storage.db.security.entity.Acl;
import org.artifactory.storage.db.security.entity.Group;
import org.artifactory.storage.db.security.entity.PermissionTarget;
import org.artifactory.storage.security.service.AclStoreService;
import org.artifactory.util.AlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.artifactory.security.ArtifactoryPermission.DEPLOY;
import static org.artifactory.security.ArtifactoryPermission.READ;
import static org.artifactory.security.PermissionTargetInfo.*;

/**
 * Date: 9/3/12
 * Time: 4:12 PM
 *
 * @author freds
 */
@Service
public class AclServiceImpl implements AclStoreService {
    private static final Logger log = LoggerFactory.getLogger(AclServiceImpl.class);
    @Autowired
    private DbService dbService;
    @Autowired
    private AclsDao aclsDao;
    @Autowired
    private PermissionTargetsDao permTargetsDao;
    @Autowired
    private UserGroupsDao userGroupsDao;
    private AclsCache aclsCache;

    @PostConstruct
    private void init(){
        long timeout = ConstantValues.aclDirtyReadsTimeout.getLong();
        aclsCache = new AclsCache(timeout,new AclCacheLoader(aclsDao,userGroupsDao,permTargetsDao));
    }

    @Override
    public Collection<AclInfo> getAllAcls() {
        return getAclsMap().values();
    }

    @Override
    public void createAcl(AclInfo entity) {
        try {
            PermissionTargetInfo permTargetInfo = entity.getPermissionTarget();
            PermissionTarget dbPermTarget = permTargetsDao.findPermissionTarget(permTargetInfo.getName());
            if (dbPermTarget != null) {
                throw new AlreadyExistsException("Could not create ACL. Permission target already exist: " +
                        permTargetInfo.getName());
            }

            dbPermTarget = new PermissionTarget(dbService.nextId(), permTargetInfo.getName(),
                    permTargetInfo.getIncludes(), permTargetInfo.getExcludes());
            dbPermTarget.setRepoKeys(Sets.newHashSet(permTargetInfo.getRepoKeys()));
            permTargetsDao.createPermissionTarget(dbPermTarget);
            Acl acl = aclFromInfo(dbService.nextId(), entity, dbPermTarget.getPermTargetId());
            aclsDao.createAcl(acl);
        } catch (SQLException e) {
            throw new StorageException("Could not create ACL " + entity, e);
        } finally {
            aclsCache.promoteAclsDbVersion();
        }
    }

    @Override
    public void updateAcl(MutableAclInfo aclInfo) {
        PermissionTargetInfo permTargetInfo = aclInfo.getPermissionTarget();
        try {
            PermissionTarget dbPermTarget = permTargetsDao.findPermissionTarget(permTargetInfo.getName());
            if (dbPermTarget == null) {
                throw new DBEntityNotFoundException(
                        "Could not update ACL with non existent Permission Target " + aclInfo.getPermissionTarget());
            }
            long permTargetId = dbPermTarget.getPermTargetId();
            PermissionTarget newPermTarget = new PermissionTarget(permTargetId,
                    permTargetInfo.getName(), permTargetInfo.getIncludes(), permTargetInfo.getExcludes());
            newPermTarget.setRepoKeys(Sets.newHashSet(permTargetInfo.getRepoKeys()));
            permTargetsDao.updatePermissionTarget(newPermTarget);
            Acl dbAcl = aclsDao.findAclByPermissionTargetId(permTargetId);
            if (dbAcl == null) {
                throw new DBEntityNotFoundException("Could not update non existent ACL " + aclInfo);
            }
            Acl acl = aclFromInfo(dbAcl.getAclId(), aclInfo, permTargetId);
            aclsDao.updateAcl(acl);
        } catch (SQLException e) {
            throw new StorageException("Could not update ACL " + aclInfo, e);
        } finally {
            aclsCache.promoteAclsDbVersion();
        }
    }

    @Override
    public void deleteAcl(String permTargetName) {
        try {
            PermissionTarget permissionTarget = permTargetsDao.findPermissionTarget(permTargetName);
            if (permissionTarget == null) {
                // Already deleted
                return;
            }
            Acl acl = aclsDao.findAclByPermissionTargetId(permissionTarget.getPermTargetId());
            if (acl != null) {
                aclsDao.deleteAcl(acl.getAclId());
            } else {
                log.warn("ACL already deleted, but permission target was not!");
            }
            permTargetsDao.deletePermissionTarget(permissionTarget.getPermTargetId());
        } catch (SQLException e) {
            throw new StorageException("Could not delete ACL " + permTargetName, e);
        } finally {
            aclsCache.promoteAclsDbVersion();
        }
    }

    @Override
    public AclInfo getAcl(String permTargetName) {
        return getAclsMap().get(permTargetName);
    }

    @Override
    public boolean permissionTargetExists(String permTargetName) {
        return getAclsMap().containsKey(permTargetName);
    }

    @Override
    public boolean userHasPermissions(String username) {
        try {
            long userId = userGroupsDao.findUserIdByUsername(username);
            if (userId <= 0L) {
                // User not in DB yet
                return false;
            }
            return aclsDao.userHasAce(userId);
        } catch (SQLException e) {
            throw new StorageException("Cannot check if user " + username + " has permissions!", e);
        }
    }

    @Override
    public void removeAllUserAces(String username) {
        try {
            long userId = userGroupsDao.findUserIdByUsername(username);
            if (userId <= 0L) {
                // User does not exists
                return;
            }
            aclsDao.deleteAceForUser(userId);
        } catch (SQLException e) {
            throw new StorageException("Could not delete ACE for user " + username, e);
        } finally {
            aclsCache.promoteAclsDbVersion();
        }
    }

    @Override
    public void removeAllGroupAces(String groupName) {
        try {
            Group group = userGroupsDao.findGroupByName(groupName);
            if (group == null) {
                // Group does not exists
                return;
            }
            aclsDao.deleteAceForGroup(group.getGroupId());
        } catch (SQLException e) {
            throw new StorageException("Could not delete ACE for group " + groupName, e);
        } finally {
            aclsCache.promoteAclsDbVersion();
        }
    }

    @Override
    public void createDefaultSecurityEntities(UserInfo anonUser, GroupInfo readersGroup, String currentUsername) {
        if (!UserInfo.ANONYMOUS.equals(anonUser.getUsername())) {
            throw new IllegalArgumentException(
                    "Default anything permissions should be created for the anonymous user only");
        }

        // create or update read permissions on "anything"
        AclInfo anyAnyAcl = getAcl(ANY_PERMISSION_TARGET_NAME);
        Set<AceInfo> anyAnyAces = new HashSet<>(2);
        anyAnyAces.add(InfoFactoryHolder.get().createAce(
                anonUser.getUsername(), false, READ.getMask()));
        anyAnyAces.add(InfoFactoryHolder.get().createAce(
                readersGroup.getGroupName(), true, READ.getMask()));
        if (anyAnyAcl == null) {
            MutablePermissionTargetInfo anyAnyTarget = InfoFactoryHolder.get().createPermissionTarget(
                    ANY_PERMISSION_TARGET_NAME, Lists.newArrayList((ANY_REPO)));
            anyAnyTarget.setIncludesPattern(ANY_PATH);
            anyAnyAcl = InfoFactoryHolder.get().createAcl(anyAnyTarget, anyAnyAces, currentUsername);
            createAcl(anyAnyAcl);
        } else {
            MutableAclInfo acl = InfoFactoryHolder.get().createAcl(anyAnyAcl.getPermissionTarget());
            acl.setAces(anyAnyAces);
            acl.setUpdatedBy(currentUsername);
            updateAcl(acl);
        }

        // create or update read and deploy permissions on all remote repos
        AclInfo anyRemoteAcl = getAcl(ANY_REMOTE_PERMISSION_TARGET_NAME);
        HashSet<AceInfo> anyRemoteAces = new HashSet<>(2);
        anyRemoteAces.add(InfoFactoryHolder.get().createAce(
                anonUser.getUsername(), false,
                READ.getMask() | DEPLOY.getMask()));
        if (anyRemoteAcl == null) {
            MutablePermissionTargetInfo anyRemoteTarget = InfoFactoryHolder.get().createPermissionTarget(
                    ANY_REMOTE_PERMISSION_TARGET_NAME, new ArrayList<String>() {{
                add(ANY_REMOTE_REPO);
            }});
            anyRemoteTarget.setIncludesPattern(ANY_PATH);
            anyRemoteAcl = InfoFactoryHolder.get().createAcl(anyRemoteTarget, anyRemoteAces, currentUsername);
            createAcl(anyRemoteAcl);
        } else {
            MutableAclInfo acl = InfoFactoryHolder.get().createAcl(anyRemoteAcl.getPermissionTarget());
            acl.setAces(anyRemoteAces);
            acl.setUpdatedBy(currentUsername);
            updateAcl(acl);
        }
    }

    @Override
    public void deleteAllAcls() {
        try {
            aclsDao.deleteAllAcls();
            permTargetsDao.deleteAllPermissionTargets();
        } catch (SQLException e) {
            throw new StorageException("Could not delete all ACLs", e);
        } finally {
            aclsCache.promoteAclsDbVersion();
        }
    }

    @Override
    public int promoteAclsDbVersion() {
        return aclsCache.promoteAclsDbVersion();
    }

    private Acl aclFromInfo(long aclId, AclInfo aclInfo, long permTargetId) throws SQLException {
        Acl acl = new Acl(aclId, permTargetId, System.currentTimeMillis(),
                aclInfo.getUpdatedBy());
        Set<AceInfo> aces = aclInfo.getAces();
        HashSet<Ace> dbAces = new HashSet<>(aces.size());
        for (AceInfo ace : aces) {
            Ace dbAce = null;
            if (ace.isGroup()) {
                Group group = userGroupsDao.findGroupByName(ace.getPrincipal());
                if (group != null) {
                    dbAce = new Ace(dbService.nextId(), acl.getAclId(), ace.getMask(), 0, group.getGroupId());
                } else {
                    log.error("Got ACE entry for ACL " + aclInfo.getPermissionTarget().getName() +
                            " with a group " + ace.getPrincipal() + " that does not exist!");
                }
            } else {
                long userId = userGroupsDao.findUserIdByUsername(ace.getPrincipal());
                if (userId > 0L) {
                    dbAce = new Ace(dbService.nextId(), acl.getAclId(), ace.getMask(), userId, 0);
                } else {
                    log.error("Got ACE entry for ACL " + aclInfo.getPermissionTarget().getName() +
                            " with a user " + ace.getPrincipal() + " that does not exist!");
                }
            }
            if (dbAce != null) {
                dbAces.add(dbAce);
            }
        }
        acl.setAces(dbAces);
        return acl;
    }

    private Map<String, AclInfo> getAclsMap() {
        return aclsCache.get();
    }

    public static class AclsCache {
        private ReentrantLock cacheLock = new ReentrantLock();
        private long timeout;
        private final AclCacheLoader cacheLoader;
        private AtomicInteger aclsDbVersion = new AtomicInteger(
                1);  // promoted on each DB change (permission change/add/delete)
        private volatile int aclsMapVersion = 0; // promoted each time we load the map from DB
        private volatile Map<String, AclInfo> aclsMap;

        public AclsCache(long timeout,AclCacheLoader cacheLoader) {
            this.timeout = timeout;
            this.cacheLoader = cacheLoader;
        }

        /**
         * Call this method each permission update/change/delete in DB.
         */
        public int promoteAclsDbVersion() {
            return aclsDbVersion.incrementAndGet();
        }

        /**
         * Returns permissions map.
         */
        public Map<String, AclInfo> get() {
            Map<String, AclInfo> tempMap = aclsMap;
            if (aclsDbVersion.get() > aclsMapVersion) {
                // Need to update aclsMap (new version in aclsDbVersion).
                // Try to acquire acl lock
                log.debug("Attempting to acquire a lock on cacheLock");
                boolean lockAcquired = tryToWaitForAclLock();
                if ( ! lockAcquired) {
                    // Timeout occurred : Return the current aclMap without waiting to thew new map which is being reloaded.
                    log.debug("Acl lock timeout occurred returning current aclMap instead the one that is being loaded");
                    return aclsMap;
                }else {
                    log.debug("cacheLock lock has been acquired");
                    // Lock was successfully acquired, now we can start reloading the map
                    try {
                        tempMap = aclsMap;
                        //print only if debug is enabled to avoid performances degradation on large maps
                        if (log.isDebugEnabled()){
                            if (tempMap!=null && !tempMap.isEmpty()){
                                log.debug("Current aclsMap contains: "+tempMap.keySet());
                            } else {
                                log.debug("Current aclsMap is empty");
                            }
                        }
                        // Double check after cacheLoader synchronization.
                        if (aclsDbVersion.get() > aclsMapVersion) {
                            log.debug("aclsDbVersion verion '{}' is higher than aclsMapVersion: {}",
                                    aclsDbVersion.get(), aclsMapVersion);
                            // The map will be valid for version the current aclsDbVersion.
                            int startingVersion = aclsDbVersion.get();
                            tempMap = cacheLoader.call();
                            aclsMap = tempMap;
                            //print only if debug is enabled to avoid performances degradation on large maps
                            if (log.isDebugEnabled()) {
                                log.debug("aclsMap has been updated with: " + tempMap.keySet());
                            }
                            aclsMapVersion = startingVersion;
                        } else {
                            log.debug("Skipping aclMap update, newer version exist: aclsDbVersion verion is: '{}'" +
                                    " while aclsMapVersion version is: {}", aclsDbVersion.get(), aclsMapVersion);
                        }

                    } finally {
                        cacheLock.unlock();
                    }
                }
            }
            return tempMap;
        }

        private boolean tryToWaitForAclLock() {
            boolean acquireLock=false;
            try {
                acquireLock = cacheLock.tryLock(timeout, TimeUnit.MILLISECONDS);
                if( ! acquireLock && aclsMap==null){
                    log.debug("Blocking thread while acl map is being processed for the first time");
                    acquireLock=tryToWaitForAclLock();
                }
            } catch (InterruptedException e) {
                if( aclsMap==null){
                    log.debug("Blocking thread while acl map is being processed for the first time");
                    acquireLock=tryToWaitForAclLock();
                }
            }
            return acquireLock;
        }
    }

    public static class AclCacheLoader implements Callable<Map<String, AclInfo>> {

        private AclsDao aclsDao;
        private UserGroupsDao userGroupsDao;
        private PermissionTargetsDao permTargetsDao;

        public AclCacheLoader(AclsDao aclsDao,UserGroupsDao userGroupsDao,PermissionTargetsDao permissionTargetsDao) {
            this.aclsDao = aclsDao;
            this.userGroupsDao = userGroupsDao;
            this.permTargetsDao = permissionTargetsDao;
        }

        @Override
        public Map<String, AclInfo> call() {
            try {
                log.debug("Starting acl-reload-process");
                Map<Long, String> allUsernamePerIds = userGroupsDao.getAllUsernamePerIds();
                Map<Long, String> allGroupNamePerIds = userGroupsDao.getAllGroupNamePerIds();
                Map<Long, PermissionTarget> targetMap = permTargetsDao.getAllPermissionTargets();
                Collection<Acl> allAcls = aclsDao.getAllAcls();
                Map<String, AclInfo> result = Maps.newHashMapWithExpectedSize(allAcls.size());
                for (Acl acl : allAcls) {
                    PermissionTarget permTarget = targetMap.get(acl.getPermTargetId());
                    MutablePermissionTargetInfo permissionTarget = InfoFactoryHolder.get().createPermissionTarget(
                            permTarget.getName(), new ArrayList<>(permTarget.getRepoKeys()));
                    permissionTarget.setIncludes(permTarget.getIncludes());
                    permissionTarget.setExcludes(permTarget.getExcludes());
                    ImmutableSet<Ace> dbAces = acl.getAces();
                    HashSet<AceInfo> aces = new HashSet<>(dbAces.size());
                    for (Ace dbAce : dbAces) {
                        MutableAceInfo ace = null;
                        if (dbAce.isOnGroup()) {
                            String groupName = allGroupNamePerIds.get(dbAce.getGroupId());
                            if (groupName != null) {
                                ace = InfoFactoryHolder.get().createAce(groupName, true, dbAce.getMask());
                            }
                        } else {
                            String username = allUsernamePerIds.get(dbAce.getUserId());
                            if (username != null) {
                                ace = InfoFactoryHolder.get().createAce(username, false, dbAce.getMask());
                            }
                        }
                        if (ace != null) {
                            aces.add(ace);
                        }
                    }
                    result.put(permTarget.getName(),
                            InfoFactoryHolder.get().createAcl(permissionTarget, aces, acl.getLastModifiedBy()));
                }
                log.debug("Successfully finished acl-reload-process");
                return result;
            } catch (SQLException e) {
                throw new StorageException("Could not load all Access Control List from DB due to:" + e.getMessage(),
                        e);
            }
        }
    }
}
