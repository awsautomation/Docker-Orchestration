
package com.codeabovelab.dm.cluman.security;

import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.security.TenantGrantedAuthoritySid;
import com.codeabovelab.dm.common.security.acl.ExtPermissionGrantingStrategy;
import com.codeabovelab.dm.common.security.dto.PermissionData;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

/**
 */
public class AccessContext {
    private final AclService aclService;
    private final ExtPermissionGrantingStrategy pgs;
    private final List<Sid> sids;
    private final Authentication authentication;

    AccessContext(AccessContextFactory factory) {
        this.authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Sid> sids;
        if(this.authentication == null) {
            throw new AccessDeniedException("No credentials in context.");
        } else {
            sids = factory.sidStrategy.getSids(authentication);
        }
        this.aclService = factory.aclService;
        this.pgs = factory.pgs;
        this.sids = sids;
    }

    /**
     * Check access for specified object
     * @param o
     * @param perms
     * @return
     */
    public boolean isGranted(ObjectIdentity o, Permission ... perms) {
        Assert.notNull(o, "Secured object is null");
        if (isAdminFor(o)) {
            return true;
        }
        try {
            Acl acl = aclService.readAclById(o);
            return acl.isGranted(Arrays.asList(perms), sids, false);
        } catch (NotFoundException e) {
            return false;
        }
    }

    public void assertGranted(ObjectIdentity oid, Permission ... perms) {
        boolean granted = isGranted(oid, perms);
        if(!granted) {
            throw new AccessDeniedException("Access to " + oid + " with " + Arrays.toString(perms)  + " is denied.");
        }
    }

    private boolean isAdminFor(ObjectIdentity o) {
        final String role = Authorities.adminOf(o.getType());
        final String objectTenant = MultiTenancySupport.getTenant(o);
        for(Sid sid: sids) {
            if(!(sid instanceof TenantGrantedAuthoritySid)) {
                continue;
            }
            TenantGrantedAuthoritySid authoritySid = (TenantGrantedAuthoritySid) sid;
            //TODO we need retrieve tenant hierarchy
//            String sidTenant = authoritySid.getTenant();
//            if(sidTenant != null && !Objects.equals(sidTenant, objectTenant)) {
//                continue;
//            }
            String authority = authoritySid.getGrantedAuthority();
            if(Authorities.ADMIN_ROLE.equals(authority)) {
                //grant by admin authority
                return true;
            }
            if(role.equals(authority)) {
                //grant by type admin authority
                return true;
            }
        }
        return false;
    }

    public PermissionData getPermission(ObjectIdentity oid) {
        Assert.notNull(oid, "Secured object is null");
        if(isAdminFor(oid)) {
            return PermissionData.ALL;
        }
        try {
            Acl realAcl = aclService.readAclById(oid);
            return pgs.getPermission(realAcl, sids);
        } catch (NotFoundException e) {
            return PermissionData.NONE;
        }
    }

    boolean isActual() {
        return getActualAuthIfNew() == null;
    }

    private Authentication getActualAuthIfNew() {
        SecurityContext currContext = SecurityContextHolder.getContext();
        Authentication currAuth = currContext.getAuthentication();
        // something may change authentication and we can not use '==', so we need compare only principal and his authorities
        // it cause by spring which can save context into http session and change its authentication in different threads
        if(this.authentication.getPrincipal().equals(currAuth.getPrincipal()) &&
           this.authentication.getAuthorities().equals(currAuth.getAuthorities())) {
            return null;
        }
        return currAuth;
    }

    /**
     * Check that current authentication is complies for context authentication.
     */
    void assertActual() {
        Authentication currAuth = getActualAuthIfNew();
        if(currAuth != null) {
            throw new IllegalStateException("AccessContext is for " + this.authentication
              + " but current is " + currAuth);
        }
    }
}
