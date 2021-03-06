
package com.codeabovelab.dm.cluman.model;

import com.codeabovelab.dm.common.json.JtEnumLower;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;

/**
 * Event of node changes. Usually clean different caches.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public final class NodeEvent extends Event implements WithCluster, WithAction {

    @JtEnumLower
    public enum Action {
        ONLINE, OFFLINE,
        CREATE, UPDATE, DELETE,
        /**
         * This event raise before node delete. You can cancel this through call {@link NodeEvent#cancel()}.
         */
        PRE_DELETE(true),
        /**
         * This event raise before node delete. You can cancel this through call {@link NodeEvent#cancel()}.
         */
        PRE_UPDATE(true);
        boolean pre;

        Action() {
            this(false);
        }

        Action(boolean pre) {
            this.pre = pre;
        }

        public boolean isPre() {
            return pre;
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Builder extends Event.Builder<Builder, NodeEvent> {

        private NodeInfo old;
        private NodeInfo current;
        private Action action;
        private Runnable canceller;

        public Builder old(NodeInfo old) {
            setOld(old);
            return this;
        }

        public Builder current(NodeInfo node) {
            setCurrent(node);
            return this;
        }

        /**
         * @see StandardActions
         * @param action
         * @return
         */
        public Builder action(Action action) {
            setAction(action);
            return this;
        }

        /**
         * callback for cancel. Applicable for {@link Action#PRE_UPDATE }
         * @param canceller
         * @return this
         */
        public Builder canceller(Runnable canceller) {
            setCanceller(canceller);
            return this;
        }

        @Override
        public NodeEvent build() {
            Assert.isTrue(this.current != null || this.old != null, "Old and current values is null.");
            return new NodeEvent(this);
        }
    }

    /**
     * Id of message bus
     */
    public static final String BUS = "bus.cluman.node";
    private final NodeInfo old;
    private final NodeInfo current;
    private final Action action;
    private final Runnable canceller;

    @JsonCreator
    public NodeEvent(Builder b) {
        super(b);
        this.old = b.old;
        this.current = b.current;
        this.action = b.action;
        this.canceller = b.canceller;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Method leaved for backward capability and must never return null.
     * @return current, or if it null then old, node info.
     */
    public NodeInfo getNode() {
        NodeInfo ni = this.getCurrent();
        if(ni == null) {
            // old code does not has 'old', therefore current never been null
            ni = this.getOld();
        }
        return ni;
    }

    @Override
    public String getCluster() {
        return getNode().getCluster();
    }

    /**
     * Invoke {@link  Builder#getCanceller()} when it exists, otherwise do nothing.
     */
    public void cancel() {
        if(this.canceller != null) {
            this.canceller.run();
        }
    }
}
