

package com.codeabovelab.dm.common.security;

import com.codeabovelab.dm.common.security.acl.TenantSid;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

/**
 * Some constants methods and constants for multi tenancy support
 */
public class MultiTenancySupport {
    /**
     * uses in cases where tenantId retrieved from null or incorrect objects
     * @see #isNoTenant(Object)
     */
    public static final String NO_TENANT = null;
    public static final String ANONYMOUS_TENANT = "anonymous_tenant";
    public static final String ROOT_TENANT = "root";

    /**
     * retrieve tenantId from object if it instance of {@link OwnedByTenant}, otherwise return {@link  #NO_TENANT}
     * @param object
     * @return 
     */
    public static String getTenant(Object object) {
        if(object instanceof OwnedByTenant) {
            return ((OwnedByTenant)object).getTenant();
        }
        return NO_TENANT;
    }

    /**
     * Fix null tenant for principals and validate.
     * @param sid
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends TenantSid> T fixTenant(T sid) {
        if(sid == null) {
            return sid;
        }
        final String tenant = sid.getTenant();
        if(sid instanceof GrantedAuthoritySid && tenant == null) {
            return sid;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ExtendedUserDetails eud = (ExtendedUserDetails) auth.getPrincipal();
        final String authTenant = eud.getTenant();
        if(authTenant.equals(tenant)) {
            return sid;
        }
        if(tenant == null) {
            return (T) TenantPrincipalSid.from((PrincipalSid) sid);
        }
        if(!ROOT_TENANT.equals(authTenant)) {
            // we must check tenancy access through TenantHierarchy, but now we does not have any full tenancy support
            throw new IllegalArgumentException("Sid " + sid + " has incorrect tenant: " + tenant + " it allow only for root tenant.");
        }
        return sid;
    }

    /**
     * Test that specified object does not has any tenant.
     * @see #NO_TENANT
     * @param o object with tenant
     * @return true if tenant is null
     */
    public static boolean isNoTenant(Object o) {
        return Objects.equals(NO_TENANT, getTenant(o));
    }
}
