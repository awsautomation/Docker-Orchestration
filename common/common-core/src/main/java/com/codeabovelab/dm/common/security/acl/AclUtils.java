

package com.codeabovelab.dm.common.security.acl;

import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import com.google.common.collect.ImmutableMap;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.*;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 */
public class AclUtils {

    private static class TypeSupport {
        private final String id;
        private final Class<?> type;
        private final Function<String, Object> reader;

        TypeSupport(String id, Class<?> type, Function<String, Object> reader) {
            this.id = id;
            this.type = type;
            this.reader = reader;
        }
    }

    private static final Map<Object, TypeSupport> SUPPORTED_TYPES;
    static {
        final ImmutableMap.Builder<Object, TypeSupport> b = ImmutableMap.builder();
        TypeSupport[] ts = {
          new TypeSupport("s", String.class, a -> a),
          new TypeSupport("i", Integer.class, Integer::valueOf),
          new TypeSupport("l", Long.class, Long::valueOf)
        };
        for(TypeSupport t: ts) {
            b.put(t.id, t);
            b.put(t.type, t);
        }
        SUPPORTED_TYPES  = b.build();
    }

    public static boolean isSupportedId(Serializable id) {
        if(id == null) {
            return true;
        }
        Class<?> type = id.getClass();
        return SUPPORTED_TYPES.get(type) != null;
    }

    public static boolean isSidLoaded(List<Sid> loadedSids, List<Sid> sids) {
        // If loadedSides is null, this indicates all SIDs were loaded
        // Also return true if the caller didn't specify a SID to find
        if ((loadedSids == null) || (sids == null) || (sids.size() == 0)) {
            return true;
        }

        // This ACL applies to a SID subset only. Iterate to check it applies.
        for (Sid sid: sids) {
            boolean found = false;

            for (Sid loadedSid : loadedSids) {
                if (sid.equals(loadedSid)) {
                    // this SID is OK
                    found = true;

                    break; // out of loadedSids for loop
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    public static void buildEntries(Acl acl, Collection<?> from, Consumer<AccessControlEntry> to) {
        for(Object object: from) {
            AccessControlEntry ace;
            if(object instanceof AceSource) {
                ace = new AccessControlEntryImpl.Builder().from((AccessControlEntry) object).acl(acl).build();
            } else if(object instanceof AccessControlEntryImpl.Builder) {
                ace = ((AccessControlEntryImpl.Builder)object).acl(acl).build();
            } else if(object instanceof AccessControlEntry) {
                ace = (AccessControlEntry) object;
            } else {
                throw new IllegalArgumentException(object + " must be an instance of " + AccessControlEntry.class + " or it's builder");
            }
            to.accept(ace);
        }
    }

    public static String toId(ObjectIdentity object) {
        Assert.notNull(object, "ObjectIdentity is null");
        Object idsrc = object.getIdentifier();
        Assert.notNull(idsrc, "identifier is null");
        String type = object.getType();
        return toId(type, idsrc);
    }

    public static String toId(String type, Object id) {
        Assert.isTrue(type.indexOf(':') < 0, "Type contains ':'.");
        if(id == null || id instanceof String && ((String) id).isEmpty()) {
            return type + ":";
        }
        return type + ":" + getIdType(id) + ":" + id.toString();
    }

    private static String getIdType(Object id) {
        if(id == null) {
            return "";
        }
        Class<?> clazz = id.getClass();
        TypeSupport support = SUPPORTED_TYPES.get(clazz);
        Assert.notNull(support, "Unsupported id type: " + clazz);
        return support.id;
    }

    /**
     * Make id fot type of identity (ignore object.identifier).
     * @param object
     * @return
     */
    public static String toTypeId(ObjectIdentity object) {
        Assert.notNull(object, "ObjectIdentity is null");
        String type = object.getType();
        return toTypeId(type);
    }

    public static String toTypeId(String type) {
        return toId(type, null);
    }

    public static ObjectIdentityData fromId(String oid) {
        Assert.notNull(oid, "oid is null");
        int typeEnd = oid.indexOf(':');
        Assert.notNull(typeEnd < 1, "Bad string. Expect like: 'type:s:id'.");
        String type = oid.substring(0, typeEnd);
        String idType = "s";
        String idStr;
        String second = oid.substring(typeEnd + 1);
        int idTypeEnd = second.indexOf(':');
        if(idTypeEnd >= 0) {
            idStr = second.substring(idTypeEnd + 1);
            idType = second.substring(0, idTypeEnd);
        } else {
            idStr = second;
        }
        Object id = "";
        if(!idStr.isEmpty()) {
            TypeSupport typeSupport = SUPPORTED_TYPES.get(idType);
            Assert.notNull(typeSupport, "Unsupported id type:" + idType);
            id = typeSupport.reader.apply(idStr);
        }
        return new ObjectIdentityData(type, (Serializable) id);
    }

    /**
     * Test that specified acl contains specified user.
     * @param acl
     * @param username
     * @return
     */
    public static boolean isContainsUser(AclSource acl, String username) {
        Sid owner = acl.getOwner();
        if(isPrincipal(owner, username)) {
            return true;
        }
        for(AceSource ace: acl.getEntriesMap().values()) {
            if(isPrincipal(ace.getSid(), username)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPrincipal(Sid sid, String username) {
        return sid instanceof PrincipalSid &&
          username.equals(((PrincipalSid) sid).getPrincipal());
    }
}
