
package com.codeabovelab.dm.cluman.cluster.registry.model;

import lombok.Data;
import org.springframework.http.HttpHeaders;

/**
 */
public interface RegistryAuthAdapter {
    void handle(AuthContext ctx);

    @Data
    class AuthContext {
        private final HttpHeaders requestHeaders;
        private final HttpHeaders responseHeaders;
        /**
         * "Www-Authenticate" request header value
         */
        private final String authenticate;
    }
}
