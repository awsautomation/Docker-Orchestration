
package com.codeabovelab.dm.agent.security;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class SslServletContainerCustomizer implements EmbeddedServletContainerCustomizer {
    @Value("${dm.agent.server.names:}")
    private List<String> preconfiguredNames;
    @Value("${dm.agent.server.resolve}")
    private boolean resolve;
    @Value("${dm.agent.server.keystore:${dm.data.location}/keysore.jks}")
    private String keystore;
    @Value("${dm.agent.server.rootCert.keystore:classpath:/root.jks}")
    private String rootCertKeystore;
    @Value("${dm.agent.server.rootCert.storepass:storepass}")
    private String rootCertKeystorePass;
    @Value("${dm.agent.server.rootCert.keypass:keypass}")
    private String rootCertKeystoreKeyPass;
    @Value("${dm.agent.server.rootCert.alias:root-cert}")
    private String rootCertKeysoreAlias;

    @Autowired
    private ResourceLoader resourceLoader;

    @Override
    public void customize(ConfigurableEmbeddedServletContainer container) {
        KeystoreConfig cert = configureKeystore();
        if(cert == null) {
            log.debug("Ssl is not enabled due to no any configured keystore.");
            return;
        }
        String keystorePath = cert.getKeystore().getAbsolutePath();
        log.debug("Configure ssl with {} keystore.", keystorePath);
        Ssl ssl = new Ssl();
        ssl.setEnabled(true);
        ssl.setKeyStore(keystorePath);
        ssl.setKeyStorePassword(cert.getKeystorePassword());
        ssl.setKeyPassword(cert.getKeyPassword());
        container.setSsl(ssl);
    }

    private KeystoreConfig configureKeystore() {
        Set<String> names = new HashSet<>();
        if(this.preconfiguredNames != null) {
            for(String name: this.preconfiguredNames) {
                if(StringUtils.hasText(name)) {
                    names.add(name);
                }
            }
        }
        CertificateGenerator.gatherNames(names, this.resolve);
        X509CertificateHolder rootCert;
        PrivateKey rootKey;
        try {
            if(rootCertKeystore == null || rootCertKeystorePass == null) {
                log.warn("Keystore {} or its password {} is null, skip.", rootCertKeystore, rootCertKeystorePass);
                return null;
            }
            KeyStore ks = KeyStore.getInstance("JKS");
            Resource resource = resourceLoader.getResource(rootCertKeystore);
            if(!resource.exists()) {
                log.warn("Keystore {} is not an exists.", rootCertKeystore);
                return null;
            }
            try(InputStream is = resource.getInputStream()) {
                ks.load(is, rootCertKeystorePass.toCharArray());
            }
            rootKey = (PrivateKey) ks.getKey(rootCertKeysoreAlias, rootCertKeystoreKeyPass.toCharArray());
            Assert.notNull(rootKey, "Can not find " + rootCertKeysoreAlias + " in " + rootCertKeystore);
            Certificate[] certs = ks.getCertificateChain(rootCertKeysoreAlias);
            Assert.isTrue(certs != null && certs.length == 1, "Certificate chain of " + rootCertKeysoreAlias +
              " alias is null or has invalid count of certificates, wanted one.");
            rootCert = new X509CertificateHolder(certs[0].getEncoded());
        } catch (Exception e) {
            log.error("Can not load keystore with root certificate from {}", rootCertKeystore, e);
            return null;
        }
        try {
            File ksf = new File(keystore);
            ksf.getParentFile().mkdirs();
            return CertificateGenerator.constructCert(rootCert, rootKey, ksf, names);
        } catch (Exception e) {
            log.error("Can not generate cert.", e);
            return null;
        }
    }
}
