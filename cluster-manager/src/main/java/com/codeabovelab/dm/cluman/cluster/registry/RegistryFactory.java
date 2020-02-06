
package com.codeabovelab.dm.cluman.cluster.registry;

import com.codeabovelab.dm.cluman.cluster.registry.aws.*;
import com.codeabovelab.dm.cluman.cluster.registry.model.*;
import com.codeabovelab.dm.cluman.utils.HttpUserAgentInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
@Slf4j
public class RegistryFactory implements DisposableBean {

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${dm.registry.connect.to:10000}")
    private Integer connectTimeOut;

    @Value("${dm.registry.read.to:20000}")
    private Integer readTimeOut = 10000;

    @Value("${dm.registry.dockerhub.search.url:https://registry.hub.docker.com}")
    private String dockerSearchHubUrl = "https://registry.hub.docker.com";

    @Value("${dm.registry.dockerhub.url:https://registry-1.docker.io}")
    private String dockerHubUrl = "https://registry-1.docker.io";

    @Value("${dm.registry.search.cacheMinutes:10}")
    private long searchCacheMinutes;

    @Autowired
    private AwsService awsService;

    private final ScheduledExecutorService scheduledExecutorService;
    private final Map<Class<?>, RegistryFactoryAdapter> adapters;

    @Autowired
    public RegistryFactory(AwsRegistryFactoryAdapter awsRegistryFactoryAdapter) {
        this.adapters = ImmutableMap.of(
          AwsRegistryConfig.class, awsRegistryFactoryAdapter,
          HubRegistryConfig.class, new RegistryFactoryAdapter<HubRegistryConfig>() {
              @Override
              public RegistryService create(RegistryFactory factory, HubRegistryConfig config) {
                  return createHubRegistryService(config);
              }

              @Override
              public void complete(HubRegistryConfig config) {
                  if(config.getName() != null) {
                      return;
                  }
                  config.setName(config.getUsername());
              }
          },
          PrivateRegistryConfig.class, new RegistryFactoryAdapter<PrivateRegistryConfig>() {
              @Override
              public RegistryService create(RegistryFactory factory, PrivateRegistryConfig config) {
                  return RegistryServiceImpl.builder()
                    .adapter(new PrivateRegistryAdapter(config, RegistryFactory.this::restTemplate))
                    .searchConfig(getSearchIndexDefaultConfig())
                    .build();
              }

              @Override
              public void complete(PrivateRegistryConfig config) {
                  if(config.getName() != null) {
                      return;
                  }
                  String name = RegistryUtils.getNameByUrl(config.getUrl());
                  config.setName(name);
              }
          }
        );
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(getClass().getSimpleName() + "-scheduled-%d")
                .build());
    }

    public RestTemplate restTemplate(RegistryAuthAdapter registryAuthAdapter) {
        RestTemplate restTemplate = new RestTemplate();
        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        SimpleClientHttpRequestFactory rf =
                (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        rf.setReadTimeout(readTimeOut);
        rf.setConnectTimeout(connectTimeOut);

        restTemplate.setInterceptors(ImmutableList.of(
          new RegistryAuthInterceptor(registryAuthAdapter),
          HttpUserAgentInterceptor.getDefault()
        ));

        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jsonConverter = (MappingJackson2HttpMessageConverter) converter;
                jsonConverter.setObjectMapper(objectMapper);
                //note that this content-type will be placed in Acceptable header
                jsonConverter.setSupportedMediaTypes(Arrays.asList(
                  new MediaType("application", "json", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET),
                  new MediaType("application", "*+json", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET),
                  new MediaType("application", "*+prettyjws", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET),
                  // it need for blobs (with json) from registry
                  new MediaType("application", "octet-stream", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET)
                ));
            }
        }
        return restTemplate;
    }


    DockerHubRegistry createHubRegistryService(HubRegistryConfig config) {
        DockerHubRegistryImpl registryService = DockerHubRegistryImpl.builder()
                .adapter(new HubRegistryAdapter(config, this::restTemplate, dockerHubUrl))
                .build();
        return new DockerHubRegistryServiceWrapper(registryService, config.getUsername());

    }

    DockerHubRegistry createPublicHubRegistryService(HubRegistryConfig config) {
        PublicDockerHubRegistryImpl registryService = PublicDockerHubRegistryImpl.builder()
                .adapter(new HubRegistryAdapter(config, this::restTemplate, dockerHubUrl))
                .dockerHubSearchRegistryUrl(dockerSearchHubUrl)
                .build();
        return registryService;

    }

    @Override
    public void destroy() throws Exception {
        this.scheduledExecutorService.shutdownNow();
    }

    public <T extends RegistryConfig> RegistryService createRegistryService(T config) {
        complete(config);
        RegistryFactoryAdapter<T> adapter = getTypeAdapter(config);
        return adapter.create(this, config);
    }

    public <T extends RegistryConfig> void complete(T config) {
        RegistryFactoryAdapter<T> adapter = getTypeAdapter(config);
        adapter.complete(config);
    }

    @SuppressWarnings("unchecked")
    private <T extends RegistryConfig> RegistryFactoryAdapter<T> getTypeAdapter(T config) {
        Class<?> type = config.getClass();
        RegistryFactoryAdapter<RegistryConfig> adapter = adapters.get(type);
        Assert.notNull(adapter, "can not find adapter for " + type);
        return (RegistryFactoryAdapter<T>) adapter;
    }

    public SearchIndex.Config getSearchIndexDefaultConfig() {
        SearchIndex.Config config = new SearchIndex.Config();
        config.setScheduledExecutorService(scheduledExecutorService);
        config.setCacheMinutes(this.searchCacheMinutes);
        return config;
    }

}
