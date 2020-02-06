
package com.codeabovelab.dm.cluman.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable class which save map of job parameters, also it mut be serializable to JSON. <p/>
 * Parameters must be one of value-type (like String, Integer & etc.), array or collection.
 * @see JobDescription
 */
@Data
public final class JobParameters {

    @Data
    public static class Builder {
        private String type;
        /**
         * @see JobInfo#getTitle()
         */
        private String title;
        /**
         * String cron-like expression. Based on {@link org.springframework.scheduling.support.CronSequenceGenerator}
         */
        private String schedule;
        private final Map<String, Object> parameters = new HashMap<>();

        public Builder type(String type) {
            setType(type);
            return this;
        }

        public Builder title(String title) {
            setTitle(title);
            return this;
        }

        public Builder parameter(String key, Object value) {
            Assert.notNull(key, "key is null");
            Assert.notNull(value, "value is null");
            parameters.put(key, value);
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            setParameters(parameters);
            return this;
        }

        @JsonDeserialize(contentUsing = JobParametersDeserializer.class)
        public void setParameters(Map<String, Object> parameters) {
            this.parameters.clear();
            if(parameters != null) {
                this.parameters.putAll(parameters);
            }
        }

        /**
         * String cron-like expression. Based on {@link org.springframework.scheduling.support.CronSequenceGenerator}
         * @param schedule
         * @return
         */
        public Builder schedule(String schedule) {
            setSchedule(schedule);
            return this;
        }

        public JobParameters build() {
            return new JobParameters(this);
        }
    }

    private final String type;
    /**
     * @see JobInfo#getTitle()
     */
    private final String title;
    /**
     * String cron-like expression. Based on {@link org.springframework.scheduling.support.CronSequenceGenerator}
     */
    private final String schedule;
    private final Map<String, Object> parameters;

    @JsonCreator
    public JobParameters(Builder b) {
        this.type = b.type;
        Assert.hasText(this.type, "Job type must have text");
        this.title = b.title;
        this.schedule = b.schedule;
        //ConcurrentMap in any case doesn't allow nulls
        this.parameters = ImmutableMap.copyOf(b.parameters);
    }

    public static Builder builder() {
        return new Builder();
    }
}
