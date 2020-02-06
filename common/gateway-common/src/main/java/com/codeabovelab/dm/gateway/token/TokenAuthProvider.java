
package com.codeabovelab.dm.gateway.token;

import com.codeabovelab.dm.common.security.SuccessAuthProcessor;
import com.codeabovelab.dm.common.security.token.TokenData;
import com.codeabovelab.dm.common.security.token.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * Implementation of credentials provider for PreAuthenticatedAuthenticationToken
 */
public class TokenAuthProvider implements AuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TokenAuthProvider.class);

    private final TokenValidator tokenValidator;
    private final UserDetailsService userDetailsService;
    private final SuccessAuthProcessor authProcessor;

    @Autowired
    public TokenAuthProvider(TokenValidator tokenValidator,
                             UserDetailsService userDetailsService,
                             SuccessAuthProcessor authProcessor) {
        this.tokenValidator = tokenValidator;
        this.userDetailsService = userDetailsService;
        this.authProcessor = authProcessor;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        final TokenData tokenData = fetchToken(authentication);
        if (tokenData != null) {
            final UserDetails userDetails = userDetailsService.loadUserByUsername(tokenData.getUserName());
            LOG.debug("Token {} is valid; userDetails is {}", tokenData, userDetails);
            return authProcessor.createSuccessAuth(authentication, userDetails);
        } else {
            throw new UsernameNotFoundException("User not found" + authentication.getCredentials());
        }
    }

    protected TokenData fetchToken(Authentication authentication) {
        String principal = (String) authentication.getPrincipal();
        if (principal == null) {
            LOG.warn("principal wasn't passed");
            return null;
        }
        return tokenValidator.verifyToken(principal);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(PreAuthenticatedAuthenticationToken.class);
    }
}
