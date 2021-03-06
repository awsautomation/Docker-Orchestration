
package com.codeabovelab.dm.common.kv;

import lombok.Data;

/**
 */
@Data
public class KvNode {
    /**
     * Index of node, usually used for CAS operations.
     */
    private final long index;
    private final String value;
    private final boolean directory;

    private KvNode(long index, String val, boolean dir) {
        this.index = index;
        this.value = val;
        this.directory = dir;
    }

    public static KvNode dir(long index) {
        return new KvNode(index, null, true);
    }

    public static KvNode leaf(long index, String val) {
        return new KvNode(index, val, false);
    }
}
