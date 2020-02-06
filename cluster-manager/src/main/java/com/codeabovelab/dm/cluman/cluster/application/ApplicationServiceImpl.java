

package com.codeabovelab.dm.cluman.cluster.application;

import com.codeabovelab.dm.cluman.cluster.compose.ComposeExecutor;
import com.codeabovelab.dm.cluman.cluster.compose.ComposeResult;
import com.codeabovelab.dm.cluman.cluster.compose.model.ApplicationEvent;
import com.codeabovelab.dm.cluman.cluster.compose.model.ComposeArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.StopContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.source.ContainerSourceFactory;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.kv.KvUtils;
import com.codeabovelab.dm.common.kv.mapping.KvMap;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.mb.MessageBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

@Service
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private final KeyValueStorage keyValueStorage;
    private final DiscoveryStorage discoveryStorage;
    private final String appPrefix;
    private final ComposeExecutor composeExecutor;

    private final MessageBus<ApplicationEvent> applicationBus;
    private final ContainerSourceFactory sourceService;
    private final KvMap<ApplicationImpl> map;

    @Autowired
    public ApplicationServiceImpl(KvMapperFactory mapper,
                                  DiscoveryStorage discoveryStorage,
                                  ComposeExecutor composeExecutor,
                                  ContainerSourceFactory sourceService,
                                  @Qualifier(ApplicationEvent.BUS) MessageBus<ApplicationEvent> applicationBus) {
        this.keyValueStorage = mapper.getStorage();
        this.appPrefix = keyValueStorage.getPrefix() + "/applications/";
        this.map = KvMap.builder(ApplicationImpl.class)
          .mapper(mapper)
          .path(this.appPrefix)
          .build();
        this.discoveryStorage = discoveryStorage;
        this.composeExecutor = composeExecutor;
        this.applicationBus = applicationBus;
        this.sourceService = sourceService;
    }

    @Override
    public List<Application> getApplications(String cluster) {
        List<String> appKeys = keyValueStorage.list(appPrefix + cluster);
        if(appKeys == null) {
            return Collections.emptyList();
        }
        List<Application> apps = new ArrayList<>();
        appKeys.forEach((k) -> {
            String name = KvUtils.suffix(appPrefix, k);
            // name has 'cluster/appName'
            ApplicationImpl app = map.get(name);
            apps.add(app);
        });
        return apps;
    }

    private ApplicationImpl readApplication(String cluster, String appId) {
        return map.get(buildKey(cluster, appId));
    }

    @Override
    public CreateApplicationResult deployCompose(ComposeArg composeArg) throws Exception {

        NodesGroup service = discoveryStorage.getCluster(composeArg.getClusterName());
        return upCompose(composeArg, service);
    }

    private CreateApplicationResult upCompose(ComposeArg composeArg, NodesGroup service) throws Exception {
        log.debug("about to launch {} at {}", composeArg, service.getName());
        fireStartEvent(composeArg);
        ComposeResult composeResult = composeExecutor.up(composeArg, service.getDocker());
        log.info("result of {} : {}", composeArg, composeResult);
        fireEndEvent(composeResult, composeArg);
        List<ContainerDetails> containerDetails = firstNonNull(composeResult.getContainerDetails(), Collections.emptyList());

        ApplicationImpl application = ApplicationImpl.builder()
                .creatingDate(new Date())
                .initFile(composeArg.getFile().getCanonicalPath())
                .name(composeArg.getAppName())
                .cluster(composeArg.getClusterName())
                .containers(Collections.unmodifiableList(containerDetails.stream()
                        .map(ContainerDetails::getId).collect(Collectors.toList()))).build();

        addApplication(application);
        CreateApplicationResult createApplicationResult = new CreateApplicationResult();
        createApplicationResult.setApplication(application);
        createApplicationResult.setCode(composeResult.getResultCode());
        return createApplicationResult;

    }

    @Override
    public void startApplication(String cluster, String id) throws Exception {
        NodesGroup service = discoveryStorage.getCluster(cluster);
        Application application = getApplication(cluster, id);

        if (application.getInitFile() != null) {
            // starting using compose, also checks new versions
            ComposeArg composeArg = ComposeArg.builder().appName(id).file(new File(application.getInitFile()))
                    .runUpdate(true).clusterName(cluster).build();
            upCompose(composeArg, service);
        } else {
            // starting manually
            ContainersManager containers = service.getContainers();
            application.getContainers().forEach(containers::startContainer);
        }
    }

    private void fireStartEvent(ComposeArg composeArg) {
        ApplicationEvent.Builder ae = ApplicationEvent.builder();
        ae.setAction("starting");
        ae.setSeverity(Severity.INFO);
        ae.setApplicationName(composeArg.getAppName());
        ae.setFileName(composeArg.getFile().getName());
        ae.setClusterName(composeArg.getClusterName());
        applicationBus.accept(ae.build());
    }

    private void fireEndEvent(ComposeResult composeResult, ComposeArg composeArg) {
        ApplicationEvent.Builder ae = ApplicationEvent.builder();
        ae.setAction("started");
        ae.setSeverity(composeResult.getResultCode() == ResultCode.OK ? Severity.INFO : Severity.ERROR);
        ae.setApplicationName(composeResult.getAppName());
        ae.setFileName(composeArg.getFile().getName());
        ae.setClusterName(composeArg.getClusterName());
        if (!CollectionUtils.isEmpty(composeResult.getContainerDetails())) {
            ae.setContainers(composeResult.getContainerDetails().stream().map(ContainerDetails::getId).collect(Collectors.toList()));
        }
        applicationBus.accept(ae.build());
    }

    @Override
    public void stopApplication(String cluster, String id) {
        Application application = getApplication(cluster, id);

        ContainersManager service = getService(cluster);
        application.getContainers().forEach(c -> service.stopContainer(StopContainerArg.builder().id(c).build()));
    }

    private ContainersManager getService(String cluster) {
        NodesGroup service = discoveryStorage.getCluster(cluster);
        return service.getContainers();
    }

    @Override
    public Application getApplication(String cluster, String appId) {
        ApplicationImpl applicationInstance = readApplication(cluster, appId);
        ExtendedAssert.notFound(applicationInstance, "application was not found " + appId);
        ContainersManager service = getService(cluster);
        ApplicationImpl.Builder clone = ApplicationImpl.builder().from(applicationInstance);
        List<String> existedContainers = applicationInstance.getContainers().stream()
                .filter(c -> service.getContainer(c) != null).collect(Collectors.toList());
        return clone.containers(existedContainers).build();
    }

    @Override
    public void addApplication(Application application) {
        Assert.notNull(application, "application can't be null");
        String appName = application.getName();
        ExtendedAssert.matchId(appName, "application name");

        ContainersManager service = getService(application.getCluster());
        List<String> containers = application.getContainers();
        List<String> existedContainers = containers.stream().filter(c -> service.getContainer(c) != null).collect(Collectors.toList());
        Assert.isTrue(!CollectionUtils.isEmpty(existedContainers), "Application doesn't have containers " + application);
        String key = buildKey(application.getCluster(), appName);
        map.put(key, ApplicationImpl.from(application));
    }

    @Override
    public void removeApplication(String cluster, String id) {
        log.info("about to remove application: {}, in cluster: {}", id, cluster);
        Application application = getApplication(cluster, id);
        DockerService service = discoveryStorage.getService(application.getCluster());
        composeExecutor.rm(application, service);
        map.remove(buildKey(cluster, id));

    }

    @Override
    public ApplicationSource getSource(String cluster, String appId) {
        Application application = getApplication(cluster, appId);
        ContainersManager service = getService(cluster);
        ApplicationSource src = new ApplicationSource();
        src.setName(application.getName());
        application.getContainers().stream()
            .map(c -> {
                ContainerDetails cd = service.getContainer(c);
                ContainerSource conf = new ContainerSource();
                sourceService.toSource(cd, conf);
                conf.setApplication(appId);
                conf.setCluster(cluster);
                conf.getLabels().put(APP_LABEL, appId);
                return conf;
            })
            .forEach(src.getContainers()::add);
        return src;
    }

    @Override
    public File getInitComposeFile(String cluster, String appId) {

        ApplicationImpl applicationInstance = readApplication(cluster, appId);
        ExtendedAssert.notFound(applicationInstance, "application was not found " + appId);
        File file = new File(applicationInstance.getInitFile());
        ExtendedAssert.notFound(file.exists(), "can't find file for " + appId);
        return file;
    }

    private String buildKey(String cluster, String id) {
        return cluster + "/" + id;
    }

}
