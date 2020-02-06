

package com.codeabovelab.dm.common.meter;

import com.codahale.metrics.Metered;
import com.codahale.metrics.Sampling;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * some usual limit checkers
 */
public final class LimitCheckers {

    public static class Builder<T> {
        private long checkEveryTime;
        private TimeUnit checkEveryUnit;
        private Condition<T> limit;

        public Builder<T> checkEvery(long time, TimeUnit unit) {
            Assert.isTrue(time > 0, "time <= 0");
            Assert.notNull(unit, "TimeUnit can't be null");
            this.checkEveryTime = time;
            this.checkEveryUnit = unit;
            return this;
        }

        public Builder<T> limit(Condition<T> limit) {
            this.limit = limit;
            return this;
        }

        public ConfigurableChecker<T> build() {
            return new ConfigurableChecker<T>(this);
        }

        public long getPeriod() {
            return checkEveryUnit.toMillis(checkEveryTime);
        }
    }

    private static class ConfigurableChecker<T> extends BaseLimitChecker {

        private final Condition<T> limit;

        public ConfigurableChecker(Builder<T> b) {
            super(b.getPeriod());
            Assert.notNull(b.limit, "limit condition can't be null");
            this.limit = b.limit;
        }

        @Override
        @SuppressWarnings("unchecked")
        public LimitExcess check(LimitCheckContext context) {
            T metric = (T) context.getMetric();
            if(!this.limit.apply(metric)) {
                return null;
            }
            return LimitExcess.builder()
              .message("limit exceeded")
              .metric(context.getMetricId())
              .build();
        }
    }

    public static Builder<Metered> whenOneMinuteRateGreaterThan(final double value) {
        return LimitCheckers.<Metered>builder().limit(new OneMinuteRate(new GreaterThan<>(value)));
    }

    public static Builder<Sampling> whenMaxValueGreaterThan(final long value) {
        return LimitCheckers.<Sampling>builder().limit(new MaxSnapshotValue(new GreaterThan<>(value)));
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    private abstract static class MetricLimit<T, V extends Number> implements Condition<T> {
        private final Condition<V> condition;

        public MetricLimit(Condition<V> condition) {
            this.condition = condition;
            Assert.notNull(this.condition, "condition can't be null");
        }

        @Override
        public boolean apply(T val) {
            return this.condition.apply(getMetric(val));
        }

        abstract V getMetric(T metric);
    }

    private static class OneMinuteRate extends MetricLimit<Metered, Double> {

        public OneMinuteRate(Condition<Double> condition) {
            super(condition);
        }

        @Override
        Double getMetric(Metered metric) {
            return metric.getOneMinuteRate();
        }
    }

    /**
     * provide greatest value from snapshot by {@link com.codahale.metrics.Snapshot#get999thPercentile()}
     */
    private static class MaxSnapshotValue extends MetricLimit<Sampling, Long> {
        public MaxSnapshotValue(Condition<Long> condition) {
            super(condition);
        }

        @Override
        Long getMetric(Sampling metric) {
            return metric.getSnapshot().getMax();
        }
    }

    private interface Condition<T> {
        boolean apply(T val);
    }

    private static class GreaterThan<T extends Number> implements Condition<T> {
        private final T value;

        public GreaterThan(T value) {
            this.value = value;
        }

        @Override
        public boolean apply(T val) {
            return Double.compare(val.doubleValue(), this.value.doubleValue()) > 0;
        }
    }

}
