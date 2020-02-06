
package com.codeabovelab.dm.cluman.cluster.registry;

import com.codeabovelab.dm.cluman.cluster.registry.model.HubRegistryConfig;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryConfig;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.mb.MessageBuses;
import com.codeabovelab.dm.common.utils.SSLUtil;
import com.google.common.base.MoreObjects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EnableConfigurationProperties(RegistriesProperties.class)
@Configuration
@ComponentScan
public class RegistriesConfiguration {

    @Autowired
    private RegistryFactory registryFactory;

    @Value("${dm.ssl.check:true}")
    private Boolean checkSsl;

    @Bean
    @Lazy
    @Autowired
    RegistryRepository registryService(KvMapperFactory factory,
                                       RegistriesProperties regProps,
                                       @Qualifier(RegistryEvent.BUS) MessageBus<RegistryEvent> messageBus) {

        if (!checkSsl) {
            SSLUtil.disable();
        }

        HubRegistryConfig defaultRegistryConf = new HubRegistryConfig();
        defaultRegistryConf.setName(DockerHubRegistry.DEFAULT_NAME);
        defaultRegistryConf.setTitle("Docker Hub");
        defaultRegistryConf.setEditable(false);
        defaultRegistryConf.setReadOnly(true);
        DockerHubRegistry defaultRegistry = registryFactory.createPublicHubRegistryService(defaultRegistryConf);
        RegistryRepository registryRepository = new RegistryRepository(factory, defaultRegistry, registryFactory, messageBus);

        List<RegistryConfig> args = new ArrayList<>();
        args.addAll(MoreObjects.firstNonNull(regProps.getPrivateRegistry(), Collections.emptyList()));
        args.addAll(MoreObjects.firstNonNull(regProps.getAwsRegistry(), Collections.emptyList()));
        args.addAll(MoreObjects.firstNonNull(regProps.getHubRegistry(), Collections.emptyList()));

        if(regProps.isSyncInit()) {
            registryRepository.init(args);
        } else {
            new Thread(() -> registryRepository.init(args), "registry-init").start();
        }


        return registryRepository;

    }

    @Bean(name = RegistryEvent.BUS)
    MessageBus<RegistryEvent> registryEventMessageBus() {
        return MessageBuses.create(RegistryEvent.BUS, RegistryEvent.class);
    }

}
