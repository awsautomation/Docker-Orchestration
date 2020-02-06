

package com.codeabovelab.dm.cluman.ui.configuration;

import com.codeabovelab.dm.cluman.ds.SwarmAdapterConfiguration;
import com.codeabovelab.dm.cluman.security.AccessContextFactory;
import com.codeabovelab.dm.cluman.security.AccessContextFilter;
import com.codeabovelab.dm.common.security.SecurityUtils;
import com.codeabovelab.dm.common.security.SuccessAuthProcessor;
import com.codeabovelab.dm.common.security.token.TokenValidator;
import com.codeabovelab.dm.common.security.token.TokenValidatorConfiguration;
import com.codeabovelab.dm.gateway.auth.UserCompositeAuthProvider;
import com.codeabovelab.dm.gateway.token.RequestTokenHeaderRequestMatcher;
import com.codeabovelab.dm.gateway.token.TokenAuthFilterConfigurer;
import com.codeabovelab.dm.gateway.token.TokenAuthProvider;
import com.codeabovelab.dm.gateway.token.TokenServiceConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.util.StringUtils;

import java.io.File;

/**
 * Configures servlet container
 */
@Import({WebMvcAutoConfiguration.class, SwarmAdapterConfiguration.class, TokenServiceConfiguration.class, TokenValidatorConfiguration.class})
@ComponentScan(basePackageClasses = {UserCompositeAuthProvider.class})
@Configuration
@Slf4j
public class ServletContainerConfiguration {

    /**
     * Disable csrf
     * Allows anonymous request
     *
     * @return
     */
    @Bean
    @Autowired
    WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
        return new WebSecurityConfigurerAdapterImpl();
    }

    @Value("${dm.https.keystore:}")
    private String keystoreFile;
    @Value("${dm.https.keystore.password:}")
    private String keystorePass;
    @Value("${dm.https.port:8762}")
    private int tlsPort;

    @Value("${dm.security.basic.enabled:true}")
    private boolean basicAuthEnable;

    /**
     * adds SSL connector to Tomcat
     *
     * @return
     */
    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
        if (StringUtils.hasText(keystoreFile)) {
            tomcat.addAdditionalTomcatConnectors(createSslConnector());
        }
        return tomcat;
    }

    /**
     * Configures ssl connector
     *
     * @return
     */
    Connector createSslConnector() {
        log.info("About to start ssl connector at port {} with {} keystoreFile", tlsPort, keystoreFile);
        final String absoluteKeystoreFile = new File(keystoreFile).getAbsolutePath();

        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setPort(tlsPort);
        connector.setSecure(true);
        connector.setScheme("https");

        Http11NioProtocol proto = (Http11NioProtocol) connector.getProtocolHandler();
        proto.setSSLEnabled(true);
        proto.setKeystoreFile(absoluteKeystoreFile);
        proto.setKeystorePass(keystorePass);
        proto.setKeystoreType("PKCS12");
        proto.setSslProtocol("TLSv1.2");
        proto.setKeyAlias("tomcat");
        return connector;
    }

    class WebSecurityConfigurerAdapterImpl extends WebSecurityConfigurerAdapter {

        @Autowired
        private TokenValidator tokenValidator;
        @Autowired
        private UserDetailsService userDetailsService;
        @Autowired
        private SuccessAuthProcessor authProcessor;
        @Autowired
        private AuthenticationProvider provider;
        @Autowired
        private AccessContextFactory aclContextFactory;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            final String uiPrefix = "/ui/";
            final String loginUrl = uiPrefix + "login.html";

            TokenAuthFilterConfigurer<HttpSecurity> tokenFilterConfigurer =
                    new TokenAuthFilterConfigurer<>(new RequestTokenHeaderRequestMatcher(),
                            new TokenAuthProvider(tokenValidator, userDetailsService, authProcessor));
            http.csrf().disable()
                    .authenticationProvider(provider).userDetailsService(userDetailsService)
                    .anonymous().principal(SecurityUtils.USER_ANONYMOUS).and()
                    .authorizeRequests().antMatchers(uiPrefix + "/token/login").permitAll()
                    .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()//allow CORS option calls
                    .antMatchers(uiPrefix + "**").authenticated()
                    .and().headers().cacheControl().disable()
                    .and().formLogin().loginPage(loginUrl).permitAll().defaultSuccessUrl(uiPrefix)
                    .and().logout().logoutUrl(uiPrefix + "logout").logoutSuccessUrl(loginUrl)
                    .and().apply(tokenFilterConfigurer);
//                enable after testing
//                        .and().sessionManagement()
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

            // X-Frame-Options
            http.headers()
              .frameOptions().sameOrigin();

            http.addFilterAfter(new AccessContextFilter(aclContextFactory), SwitchUserFilter.class);

            //we use basic in testing and scripts
            if (basicAuthEnable) {
                http.httpBasic();
            }

        }
    }
}
