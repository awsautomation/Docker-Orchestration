
package com.codeabovelab.dm.cluman.cluster.docker;

import com.codeabovelab.dm.cluman.ds.swarm.Strategies;
import com.codeabovelab.dm.common.utils.Smelter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for docker service api config. <p/>
 * TODO rename to DockerConfig
 */
@EqualsAndHashCode
@Data
public class ClusterConfigImpl implements ClusterConfig {


    /**
     * Convert config to {@link  ClusterConfigImpl } or null
     * @param cc config or null
     * @return config or null
     */
    public static ClusterConfigImpl of(ClusterConfig cc) {
        if(cc instanceof ClusterConfigImpl || cc == null) {
            return (ClusterConfigImpl) cc;
        }
        return builder(cc).build();
    }

    /**
     * Constant for hold default values of config.
     */
    private static final ClusterConfigImpl DEFAULT = ClusterConfigImpl.builder().build();

    @Data
    public static class Builder implements ClusterConfig {


        /**
         * docker/swarm 'http[s]://host:port'
         */
        private String host;
        private int maxCountOfInstances = 1;
        private String dockerRestart;
        private String cluster;
        private Strategies strategy = Strategies.DEFAULT;
        /**
         * Time in seconds, which data was cached after last write.
         */
        private long cacheTimeAfterWrite = 10L;
        private int dockerTimeout = 5 * 60;
        /**
         * Name of registries
         * @return
         */
        private final List <String> registries = new ArrayList<>();

        public Builder from(ClusterConfig orig) {
            if(orig == null) {
                return this;
            }
            setHost(orig.getHost());
            setMaxCountOfInstances(orig.getMaxCountOfInstances());
            setDockerRestart(orig.getDockerRestart());
            setCluster(orig.getCluster());
            setRegistries(orig.getRegistries());
            setStrategy(orig.getStrategy());
            setCacheTimeAfterWrite(orig.getCacheTimeAfterWrite());
            setDockerTimeout(orig.getDockerTimeout());
            return this;
        }

        /**
         * Set only not default fields from specified config.
         * @param src
         */
        public Builder merge(ClusterConfig src) {
            if(src == null) {
                return this;
            }
            Smelter<ClusterConfig> s = new Smelter<>(src, DEFAULT);
            s.set(this::setHost, ClusterConfig::getHost);
            s.setInt(this::setMaxCountOfInstances, ClusterConfig::getMaxCountOfInstances);
            s.set(this::setDockerRestart, ClusterConfig::getDockerRestart);
            s.set(this::setCluster, ClusterConfig::getCluster);
            s.set(this::setRegistries, ClusterConfig::getRegistries);
            s.set(this::setStrategy, ClusterConfig::getStrategy);
            s.setLong(this::setCacheTimeAfterWrite, ClusterConfig::getCacheTimeAfterWrite);
            s.setInt(this::setDockerTimeout, ClusterConfig::getDockerTimeout);
            return this;
        }

        public Builder cluster(String cluster) {
            setCluster(cluster);
            return this;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Builder host(String host) {
            setHost(host);
            return this;
        }

        // remain only for capability for old serialized JSON data
        @Deprecated
        public void setHosts(List<String> hosts) {
            if(!CollectionUtils.isEmpty(hosts)) {
                setHost(hosts.get(0));
            }
            // we must not to do anything when 'hosts' is empty
        }

        public Builder maxCountOfInstances(int maxCountOfInstances) {
            setMaxCountOfInstances(maxCountOfInstances);
            return this;
        }

        public Builder dockerRestart(String dockerRestart) {
            setDockerRestart(dockerRestart);
            return this;
        }

        public Builder cacheTimeAfterWrite(long cacheTimeAfterWrite) {
            setCacheTimeAfterWrite(cacheTimeAfterWrite);
            return this;
        }

        public Builder addRegistry(String registry) {
            this.registries.add(registry);
            return this;
        }

        public Builder registries(List<String> registries) {
            setRegistries(registries);
            return this;
        }

        public Builder strategy(Strategies strategy) {
            this.strategy = strategy;
            return this;
        }

        public void setRegistries(List<String> registries) {
            this.registries.clear();
            this.registries.addAll(registries);
        }

        public Builder dockerTimeout(int dockerTimeout) {
            setDockerTimeout(dockerTimeout);
            return this;
        }

        public ClusterConfigImpl build() {
            return new ClusterConfigImpl(this);
        }
    }

    /**
     * docker/swarm 'host:port'
     */
    private final String host;
    private final int maxCountOfInstances;
    private final String dockerRestart;
    private final String cluster;
    /**
     * Time in seconds, which data was cached after last write.
     */
    private final long cacheTimeAfterWrite;
    private final int dockerTimeout;
    /**
     * Name of registries
     * @return
     */
    private final List <String> registries;
    private final Strategies strategy;

    @JsonCreator
    public ClusterConfigImpl(Builder builder) {
        this.host = builder.host;
        this.maxCountOfInstances = builder.maxCountOfInstances;
        this.strategy = builder.strategy;
        this.dockerRestart = builder.dockerRestart;
        this.cluster = builder.cluster;
        this.cacheTimeAfterWrite = builder.cacheTimeAfterWrite;
        this.dockerTimeout = builder.dockerTimeout;
        this.registries = ImmutableList.copyOf(builder.registries);
    }

    /**
     * Check that config is valid.
     * @return this
     */
    public ClusterConfigImpl validate() {
        Assert.hasText(this.host, "Hosts is empty or null");
        Assert.isTrue(this.host.contains(":"), "Hosts does not has port: " + this.host);
        return this;
    }

    public static Builder builder(ClusterConfig cc) {
        return builder().from(cc);
    }

    public static Builder builder() {
        return new Builder();
    }
}
