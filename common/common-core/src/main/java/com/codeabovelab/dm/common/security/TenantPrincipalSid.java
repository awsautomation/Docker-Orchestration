

package com.codeabovelab.dm.common.security;

import com.codeabovelab.dm.common.security.acl.TenantSid;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

/**
 * {@link PrincipalSid} extension which implement {@link com.codeabovelab.dm.common.security.OwnedByTenant }
 */
@JsonTypeName("PRINCIPAL")
public class TenantPrincipalSid extends PrincipalSid implements TenantSid {
    private final String tenantId;

    @JsonCreator
    public TenantPrincipalSid(@JsonProperty("principal") String principal,
                              @JsonProperty("tenant") String tenant) {
        super(principal);
        this.tenantId = tenant;
        validate();
    }

    public TenantPrincipalSid(Authentication authentication) {
        super(authentication);
        this.tenantId = MultiTenancySupport.getTenant(authentication.getPrincipal());
        validate();
    }

    private void validate() {
        Assert.notNull(this.tenantId, "tenant of principal is null");
    }

    @Override
    public String getTenant() {
        return this.tenantId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof TenantPrincipalSid)) return false;
        if (!super.equals(o)) return false;

        final TenantPrincipalSid that = (TenantPrincipalSid) o;

        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TenantPrincipalSid[" + getPrincipal() + ":" + tenantId + ']';
    }

    /**
     * Make SID from user details
     * @param userDetails
     * @return
     */
    public static TenantPrincipalSid from(UserDetails userDetails) {
        return new TenantPrincipalSid(userDetails.getUsername(), MultiTenancySupport.getTenant(userDetails));
    }

    public static TenantPrincipalSid from(PrincipalSid sid) {
        return new TenantPrincipalSid(sid.getPrincipal(), MultiTenancySupport.getTenant(sid));
    }
}
