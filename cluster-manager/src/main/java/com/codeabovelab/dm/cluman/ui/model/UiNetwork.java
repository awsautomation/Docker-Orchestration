

package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.cluman.cluster.docker.model.Network;
import com.codeabovelab.dm.cluman.ds.container.ContainerRegistration;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UiNetwork extends UiNetworkBase {
    private String id;
    private Network.Scope scope;
    private final List<Container> containers = new ArrayList<>();
    private boolean attachable;

    @Data
    public static class Container {

        private String id;

        private String name;

        private String image;

        private String ipv4Address;

        private String ipv6Address;

    }


    public UiNetwork from(Network network, ContainerStorage cs) {
        super.from(network);
        this.setId(network.getId());
        this.setScope(network.getScope());
        this.setAttachable(isAttachable());

        Map<String, Network.EndpointResource> containers = network.getContainers();
        if(cs != null && containers != null) {
            containers.forEach((key, val) -> {
                ContainerRegistration cr = cs.getContainer(key);
                // not any endpoint is a container!
                if(cr == null) {
                    return;
                }
                Container c = new Container();
                c.setId(cr.getId());
                DockerContainer dc = cr.getContainer();
                c.setName(dc.getName());
                c.setImage(dc.getImage());
                c.setIpv4Address(val.getIpv4Address());
                String ipv6Address = val.getIpv6Address();
                if(StringUtils.hasText(ipv6Address)) {
                    c.setIpv6Address(ipv6Address);
                }
                getContainers().add(c);
            });
        }
        return this;
    }

}
