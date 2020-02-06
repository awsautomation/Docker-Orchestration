
package com.codeabovelab.dm.cluman.cluster.docker.management;

import com.codeabovelab.dm.cluman.model.*;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.beans.ConstructorProperties;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @see DockerServiceInfoEvent
 */
public class DockerServiceEvent implements WithCluster, EventWithTime, WithSeverity {

    public static final String BUS = "bus.cluman.dockerservice";
    private final LocalDateTime time = LocalDateTime.now();
    private final String service;
    private final String node;
    private final String cluster;
    private final String action;
    private final Severity severity;

    public DockerServiceEvent(DockerService service, String action) {
        this(service.getId(), service.getNode(), service.getCluster(), action);
    }

    /**
     * Do not use this constructor, it only for deserialization.
     * @param service
     * @param node
     * @param cluster
     * @param action
     */
    @JsonCreator
    @ConstructorProperties({"service", "node", "cluster", "action"})
    public DockerServiceEvent(String service,
                              String node,
                              String cluster,
                              String action) {
        this.service = service;
        this.node = node;
        this.cluster = cluster;
        this.action = action;
        this.severity = StandardActions.toSeverity(action);
    }

    public String getAction() {
        return action;
    }

    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public long getTimeInMilliseconds() {
        return time.toEpochSecond(ZoneOffset.UTC);
    }

    public String getService() {
        return service;
    }

    @Override
    public String getCluster() {
        return cluster;
    }

    public String getNode() {
        return node;
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    public static DockerServiceEvent onServiceInfo(DockerService service, DockerServiceInfo serviceInfo) {
        return new DockerServiceInfoEvent(service.getId(), service.getNode(), service.getCluster(), serviceInfo);
    }

    public static class DockerServiceInfoEvent extends DockerServiceEvent {
        private final DockerServiceInfo info;

        @JsonCreator
        @ConstructorProperties({"service", "node", "cluster", "info"})
        public DockerServiceInfoEvent(String service,
                                      String node,
                                      String cluster,
                                      DockerServiceInfo info) {
            super(service, node, cluster, StandardActions.UPDATE);
            this.info = info;
        }

        public DockerServiceInfo getInfo() {
            return info;
        }
    }
}
