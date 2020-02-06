
package com.codeabovelab.dm.gateway.auth;

import com.codeabovelab.dm.common.security.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Order
public class ConfigurableUserDetailService implements UserIdentifiersDetailsService {

    private final Map<String, ExtendedUserDetails> detailsMap;

    public ConfigurableUserDetailService(Config config) {
        String rootTenant = MultiTenancySupport.ROOT_TENANT;
        ExtendedUserDetailsImpl admin = ExtendedUserDetailsImpl.builder()
          .username("admin")
          .title("Administrator")
          .password(config.getAdminPassword())
          .tenant(rootTenant)
          .addAuthority(new GrantedAuthorityImpl(Authorities.ADMIN_ROLE, rootTenant))
          .build();
        Map<String, ExtendedUserDetails> detailsMap = new HashMap<>();
        detailsMap.put(admin.getUsername(), admin);
        Map<String, UserConfig> users = config.getUser();
        if(users != null) {
            for(Map.Entry<String, UserConfig> e: users.entrySet()) {
                ExtendedUserDetailsImpl.Builder ub = ExtendedUserDetailsImpl.builder();
                parseUserName(ub, e, rootTenant);
                UserConfig uc = e.getValue();
                ub.setEmail(uc.getEmail());
                ub.setPassword(uc.getPassword());
                ub.setTitle(uc.getTitle());
                Set<String> roles = uc.getRoles();
                if(roles != null) {
                    for(String authority: roles) {
                        ub.addAuthority(parseAuthority(authority, ub.getTenant()));
                    }
                }
                ExtendedUserDetailsImpl details = ub.build();
                ExtendedUserDetails old = detailsMap.put(ub.getUsername(), details);
                if(old != null) {
                    log.warn("Override \n old={} with \n new={}", old, details);
                }
            }
        }
        this.detailsMap = Collections.unmodifiableMap(detailsMap);
    }

    private static GrantedAuthority parseAuthority(String token, String defaultTenant) {
        String[] arr = StringUtils.split(token, "@");
        String name;
        String tenant;
        if(arr == null) {
            name = token;
            tenant = defaultTenant;
        } else {
            name = arr[0];
            tenant = arr[1];
        }
        return Authorities.fromName(name, tenant);
    }

    private void parseUserName(ExtendedUserDetailsImpl.Builder ub, Map.Entry<String, UserConfig> e, String defaultTenant) {
        String key = e.getKey();
        String[] arr = StringUtils.split(key, "@");
        String username, tenant;
        if(arr == null) {
            username = key;
            tenant = e.getValue().getTenant();
        } else {
            username = arr[0];
            tenant = arr[1];
        }
        if(tenant == null) {
            tenant = defaultTenant;
        }
        ub.setUsername(username);
        ub.setTenant(tenant);
    }

    @Override
    public Collection<ExtendedUserDetails> getUsers() {
        return detailsMap.values();
    }

    @Override
    public ExtendedUserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        ExtendedUserDetails details = detailsMap.get(username);
        if(details == null) {
            throw new UsernameNotFoundException("'" + username + "' is not found");
        }
        return details;
    }

    @Override
    public ExtendedUserDetails loadUserByIdentifiers(UserIdentifiers identifiers) {
        return loadUserByUsername(identifiers.getUsername());
    }

    @ConfigurationProperties("dm.auth")
    @Data
    public static class Config {
        /**
         * # you can create password hash with below line:
         # read pwd && python -c "import bcrypt; print(bcrypt.hashpw(\"$pwd\", bcrypt.gensalt()))"
         # below hash is for 'password' password
         $2a$08$bFLBfYL8Eb6n71D/yvLyLu9QzxDWEPG0TTx3/LgfiwaKdhfyCEdVe
         */
        private String adminPassword = "$2a$08$bFLBfYL8Eb6n71D/yvLyLu9QzxDWEPG0TTx3/LgfiwaKdhfyCEdVe";
        private Map<String, UserConfig> user;
    }

    @Data
    public static class UserConfig {
        private String password;
        private String tenant;
        private String title;
        private String email;
        private Set<String> roles;
    }
}
