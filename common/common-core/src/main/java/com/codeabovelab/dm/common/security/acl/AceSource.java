

package com.codeabovelab.dm.common.security.acl;

import com.codeabovelab.dm.common.security.dto.PermissionData;
import com.codeabovelab.dm.common.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.springframework.security.acls.model.*;
import org.springframework.util.Assert;

/**
 * Source foe access control entry. It cannot be used as {@link AccessControlEntry } because has null Acl
 */
@Data
public class AceSource implements AuditableAccessControlEntry {

    public static class Builder extends AbstractBuilder<Builder> {

        @Override
        public AceSource build() {
            return new AceSource(this);
        }
    }

    @Data
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = Builder.class)
    public abstract static class AbstractBuilder<T> implements AuditableAccessControlEntry {
        protected String id;
        protected TenantSid sid;
        protected boolean granting;
        protected PermissionData permission;
        protected boolean auditFailure = false;
        protected boolean auditSuccess = false;

        @SuppressWarnings("unchecked")
        protected T thiz() {
            return (T) this;
        }

        public T id(String id) {
            setId(id);
            return thiz();
        }

        @Override
        public String getId() {
            return id;
        }

        public T sid(TenantSid sid) {
            setSid(sid);
            return thiz();
        }

        public T granting(boolean granting) {
            setGranting(granting);
            return thiz();
        }

        public T permission(Permission permission) {
            setPermission(PermissionData.from(permission));
            return thiz();
        }

        public T auditFailure(boolean auditFailure) {
            setAuditFailure(auditFailure);
            return thiz();
        }

        public T auditSuccess(boolean auditSuccess) {
            setAuditSuccess(auditSuccess);
            return thiz();
        }

        @Override
        public Acl getAcl() {
            // as planned
            return null;
        }

        /**
         * copy field values from specified entity
         * @param entry
         * @return
         */
        public T from(AccessControlEntry entry) {
            this.id = StringUtils.valueOf(entry.getId());
            this.sid = TenantSid.from(entry.getSid());
            this.granting = entry.isGranting();
            this.permission = PermissionData.from(entry.getPermission());
            if(entry instanceof AuditableAccessControlEntry) {
                AuditableAccessControlEntry ae = (AuditableAccessControlEntry) entry;
                this.auditFailure = ae.isAuditFailure();
                this.auditSuccess = ae.isAuditSuccess();
            }
            return thiz();
        }

        public abstract AceSource build();
    }

    protected final String id;
    protected final TenantSid sid;
    protected final boolean granting;
    protected final PermissionData permission;
    protected final boolean auditFailure;
    protected final boolean auditSuccess;

    @JsonCreator
    protected AceSource(AbstractBuilder<?> b) {
        Assert.notNull(b.sid, "Sid required");
        Assert.notNull(b.permission, "Permission required");
        this.id = b.id;
        // we must not check for null tenant, because it does not allow
        // create object accessible for all tenants
        this.sid = b.sid;
        this.permission = b.permission;
        this.granting = b.granting;
        this.auditSuccess = b.auditSuccess;
        this.auditFailure = b.auditFailure;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonIgnore
    @Override
    public Acl getAcl() {
        //as planned
        return null;
    }
}
