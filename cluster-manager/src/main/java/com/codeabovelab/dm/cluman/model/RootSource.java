
package com.codeabovelab.dm.cluman.model;

import com.codeabovelab.dm.common.json.JtToMap;
import com.codeabovelab.dm.common.utils.Cloneables;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Root entry for source file.
 */
@Data
public class RootSource implements Cloneable {
    /**
     * First an only one supported version
     */
    public static final String V_1_0 = "1.0";
    private String version = V_1_0;
    @JtToMap(key = "name")
    @Setter(AccessLevel.NONE)
    private List<ClusterSource> clusters = new ArrayList<>();

    @Override
    public RootSource clone() {
        try {
            RootSource clone = (RootSource) super.clone();
            clone.clusters = Cloneables.clone(clone.clusters);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
