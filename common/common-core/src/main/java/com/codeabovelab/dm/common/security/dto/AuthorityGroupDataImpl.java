

package com.codeabovelab.dm.common.security.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 */
public final class AuthorityGroupDataImpl implements AuthorityGroupData {

    public static class Builder implements AuthorityGroupData {

        private String name;
        private String tenantId;
        private final Set<GrantedAuthority> authorities = new HashSet<>();

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getTenant() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Set<GrantedAuthority> getAuthorities() {
            return authorities;
        }

        public void setAuthorities(Set<GrantedAuthority> authorities) {
            this.authorities.clear();
            this.authorities.addAll(authorities);
        }

        public AuthorityGroupDataImpl build() {
            return new AuthorityGroupDataImpl(this);
        }
    }

    private final String name;
    private final String tenantId;
    private final Set<GrantedAuthority> authorities;

    @JsonCreator
    public AuthorityGroupDataImpl(Builder builder) {
        this.name = builder.name;
        this.tenantId = builder.tenantId;
        this.authorities = Collections.unmodifiableSet(new HashSet<>(builder.authorities));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTenant() {
        return tenantId;
    }

    @Override
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public Set<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorityGroupDataImpl that = (AuthorityGroupDataImpl) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(authorities, that.authorities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tenantId, authorities);
    }

    @Override
    public String toString() {
        return "AuthorityGroupData{" +
                "name='" + name + '\'' +
                ", tenantId=" + tenantId +
                ", authorities=" + authorities +
                '}';
    }
}
