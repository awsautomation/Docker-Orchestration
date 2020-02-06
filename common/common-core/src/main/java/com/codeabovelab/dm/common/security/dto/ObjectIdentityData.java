

package com.codeabovelab.dm.common.security.dto;

import com.codeabovelab.dm.common.security.acl.AclUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.ObjectIdentity;

import java.io.Serializable;

/**
 * An implementation based on {@link ObjectIdentityImpl} and add some metadata for properly serialization <p/>
 * note that original implementation use simple 'equals' and 'hashcode', which does not allow correct
 * comparing with other implementations of ObjectIdentity interface. <p/>
 * Also sometime it must be a key for json object value, therefore we must supports serialization of it into string.
 */
public final class ObjectIdentityData extends ObjectIdentityImpl {

    //we keep JsonCreator for backward capability
    @JsonCreator
    public ObjectIdentityData(@JsonProperty("type") String type,
                              @JsonProperty("identifier") Serializable identifier) {
        super(type, validateId(identifier));
    }

    private static Serializable validateId(Serializable id) {
        if(AclUtils.isSupportedId(id)) {
            return id;
        }
        throw new IllegalArgumentException("Unsupported type of identifier: " + id.getClass());
    }

    @Override
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public Serializable getIdentifier() {
        return super.getIdentifier();
    }

    public static ObjectIdentityData from(ObjectIdentity objectIdentity) {
        if(objectIdentity == null || objectIdentity instanceof ObjectIdentityData) {
            return (ObjectIdentityData) objectIdentity;
        }
        return new ObjectIdentityData(objectIdentity.getType(), objectIdentity.getIdentifier());
    }

    @JsonCreator
    public static ObjectIdentityData fromString(String id) {
        return AclUtils.fromId(id);
    }

    @JsonValue
    public String asString() {
        return AclUtils.toId(this);
    }
}
