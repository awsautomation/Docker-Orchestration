
package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.ds.container.ContainerRegistration;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.ui.UiUtils;
import com.codeabovelab.dm.common.utils.Comparables;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * UI representation for Container
 */
@Data
public class UiContainer implements Comparable<UiContainer>, UiContainerIface, WithUiPermission {
    /**
     * It mean that node is offline.
     */
    public static final String NO_NODE = "Node is offline";
    @NotNull protected String id;
    @NotNull protected String name;
    @NotNull protected String node;
    @NotNull protected String image;
    @NotNull protected String imageId;
    protected String application;
    protected String cluster;
    protected final List<String> command = new ArrayList<>();
    protected final List<Port> ports = new ArrayList<>();
    protected String status;
    protected DockerContainer.State state;
    protected Date created;
    protected boolean lock;
    protected String lockCause;
    protected final Map<String, String> labels = new HashMap<>();
    protected boolean run;
    private UiPermission permission;

    @Override
    public int compareTo(UiContainer o) {
        int comp = Comparables.compare(cluster, o.cluster);
        if(comp == 0) {
            comp = Comparables.compare(application, o.application);
        }
        if(comp == 0) {
            comp = Comparables.compare(node, o.node);
        }
        if(comp == 0) {
            comp = Comparables.compare(name, o.name);
        }
        if(comp == 0) {
            comp = Comparables.compare(image, o.image);
        }
        if(comp == 0) {
            comp = Comparables.compare(id, o.id);
        }
        return comp;
    }

    public static UiContainer from(DockerContainer container) {
        UiContainer uic = new UiContainer();
        return from(uic, container);
    }

    public static  UiContainer from(UiContainer uic, DockerContainer container) {
        fromBase(uic, container);
        uic.setNode(container.getNode());
        uic.setCreated(new Date(container.getCreated()));
        uic.getPorts().addAll(container.getPorts());
        String status = container.getStatus();
        uic.setStatus(status);
        uic.setState(container.getState());
        uic.setRun(container.isRun());
        // this is workaround, because docker use simply command representation in container,
        // for full you need use ContainerDetails
        String command = container.getCommand();
        if(command != null) {
            uic.getCommand().add(command);
        }
        return uic;
    }

    public static UiContainer fromBase(UiContainer uic, ContainerBaseIface container) {
        uic.setId(container.getId());
        uic.setName(container.getName());
        uic.setImage(container.getImage());
        uic.setImageId(container.getImageId());
        uic.getLabels().putAll(container.getLabels());
        UiUtils.resolveContainerLock(uic, container);
        return uic;
    }

    /**
     * Fill container data with some values from specified storages.
     * @param discoveryStorage
     * @param containerStorage
     */
    public void enrich(DiscoveryStorage discoveryStorage, ContainerStorage containerStorage) {
        //note that cluster can be virtual
        String node = getNode();
        if(node != null) {
            NodesGroup nodeCluster = discoveryStorage.getClusterForNode(node);
            if(nodeCluster != null) {
                setCluster(nodeCluster.getName());
            }
        }

        ContainerRegistration registration = containerStorage.getContainer(getId());
        if (registration != null && registration.getAdditionalLabels() != null) {
            getLabels().putAll(registration.getAdditionalLabels());
        }
    }

    /**
     * Override status when node is offline. Require filled {@link #getNode()} value.
     * @param uc container
     * @param nodeStorage storage
     */
    public static void resolveStatus(UiContainer uc, NodeStorage nodeStorage) {
        String node = uc.getNode();
        DockerService ds = node == null? null : nodeStorage.getNodeService(node);
        if(ds == null || !ds.isOnline()) {
            uc.setRun(false);
            uc.setState(null);// state is unknown
            uc.setStatus(UiContainer.NO_NODE);
        }
    }
}
