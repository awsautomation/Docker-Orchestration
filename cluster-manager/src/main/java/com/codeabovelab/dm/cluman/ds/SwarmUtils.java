
package com.codeabovelab.dm.cluman.ds;

import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 */
@Slf4j
public class SwarmUtils {

    /**
     * Swarm place some info into containers labels, this may collide with some other options and we sometime msut clean it.
     */
    public static final String LABELS_PREFIX = "com.docker.swarm.";
    public static final String LABEL_CONSTRAINTS = LABELS_PREFIX + "constraints";
    public static final String LABEL_ID = LABELS_PREFIX + "id";
    public static final String NODE_EQ = "node==";
    public static final String LABEL_SERVICE_ID = "com.docker.swarm.service.id";
    private static final String PROP_NODES_UPDATE = "dm.nodeStorage.updateSeconds";
    public static final String EXPR_NODES_UPDATE = "${" + SwarmUtils.PROP_NODES_UPDATE + "}";
    // we use seconds in timeout, but spring require milliseconds
    public static final String EXPR_NODES_UPDATE_MS = "#{" + EXPR_NODES_UPDATE + "*1000}";

    private SwarmUtils() {
        throw new IllegalStateException("");
    }

    /**
     * Remove all swarm data from labels
     * @param labels
     */
    public static void clearLabels(Map<String, String> labels) {
        // swarm save some info to container labels, we must not copy it constraints (because it
        // conflicts with our) and some other data
        labels.keySet().removeIf(k -> k.startsWith(LABELS_PREFIX));
    }

    public static void clearConstraints(Map<String, String> labels) {
        labels.remove(LABEL_CONSTRAINTS);
    }

    /**
     * Restore source env (like constraints), which is stored into specific labels.
     * @param objectMapper
     * @param containerSource
     */
    public static void restoreEnv(ObjectMapper objectMapper, ContainerSource containerSource) {
        String constraintStr = containerSource.getLabels().get(LABEL_CONSTRAINTS);
        if(constraintStr == null) {
            return;
        }
        try {
            String[] constraints = objectMapper.readValue(constraintStr, String[].class);
            List<String> env = containerSource.getEnvironment();
            final String node = containerSource.getNode();
            for(String constraint: constraints) {
                if(node != null &&
                  constraint.startsWith(NODE_EQ) &&
                  constraint.regionMatches(NODE_EQ.length(), node, 0, node.length())) {
                    //the node eq constraint is granted by node name, we must skip this restoring
                    continue;
                }
                env.add("constraint:" + constraint);
            }
        } catch (Exception e) {
            log.error("Can not parse constraints '{}' of '{}'", constraintStr, containerSource, e);
        }
    }
}
