
package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.ExtendedUserDetailsImpl;
import com.codeabovelab.dm.common.utils.Sugar;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UiUserUpdate extends UiUserBase {
    private final List<UiRoleUpdate> roles = new ArrayList<>();


    public void toBuilder(ExtendedUserDetailsImpl.Builder builder) {
        Sugar.setIfNotNull(builder::setTenant, getTenant());
        Sugar.setIfNotNull(builder::setEmail, getEmail());
        Sugar.setIfNotNull(builder::setTitle, getTitle());
        String password = getPassword();
        if(password != null && !PWD_STUB.equals(password)) {
            builder.setPassword(password);
        }
        Sugar.setIfNotNull(builder::setUsername, getUser());
        Sugar.setIfNotNull(builder::setAccountNonExpired, getAccountNonExpired());
        Sugar.setIfNotNull(builder::setCredentialsNonExpired, getCredentialsNonExpired());
        Sugar.setIfNotNull(builder::setAccountNonLocked, getAccountNonLocked());
        Sugar.setIfNotNull(builder::setEnabled, getEnabled());
        updateRoles(roles, builder);
    }

    public static void updateRoles(List<UiRoleUpdate> updatedRoles, ExtendedUserDetailsImpl.Builder builder) {
        Collection<GrantedAuthority> existed = builder.getAuthorities();
        for(UiRoleUpdate updateRole: updatedRoles) {
            GrantedAuthority updateGa = Authorities.fromName(updateRole.getName(), updateRole.getTenant());
            if(updateRole.isDelete()) {
                boolean res = existed.remove(updateGa);
                ExtendedAssert.badRequest(res, "Can not delete role : {0}, is not exists.", updateGa);
            } else {
                boolean res = existed.add(updateGa);
                ExtendedAssert.badRequest(res, "Can not add role : {0}, is already exists.", updateGa);
            }
        }
    }
}
