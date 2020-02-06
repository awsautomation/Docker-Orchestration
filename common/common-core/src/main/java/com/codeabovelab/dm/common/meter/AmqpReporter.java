

package com.codeabovelab.dm.common.meter;

import com.codahale.metrics.*;
import com.codahale.metrics.json.HealthCheckModule;
import com.codahale.metrics.json.MetricsModule;
import com.codeabovelab.dm.common.json.JacksonUtils;
import com.codeabovelab.dm.common.utils.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Metric reporter for rabbitmq
 */
@Slf4j
public class AmqpReporter extends ScheduledReporter {

    /**
     * Returns a new {@link Builder} for {@link AmqpReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link AmqpReporter}
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * A builder for {@link AmqpReporter} instances. Defaults to logging to {@code metrics}, converting rates
     * to events/second, converting durations to milliseconds, and not filtering metrics.
     */
    public static class Builder {
        private final MetricRegistry registry;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private ObjectFactory<ConnectionFactory> connectionFactoryProvider;
        private String routingKey;
        private String exchangeName;
        private Callback<MessageProperties> messagePropertiesCallback;
        private Callable<Exchange> exchangeFactory;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Set connection factory provider. Required.
         * @param connectionFactoryProvider
         * @return
         */
        public Builder connectionFactoryProvider(ObjectFactory<ConnectionFactory> connectionFactoryProvider) {
            this.connectionFactoryProvider = connectionFactoryProvider;
            return this;
        }

        /**
         * Set message routing key. Required.
         * @param routingKey
         * @return
         */
        public Builder routingKey(String routingKey) {
            this.routingKey = routingKey;
            return this;
        }

        /**
         * Set message exchangeName. Required.
         * @param exchange
         * @return
         */
        public Builder exchangeName(String exchange) {
            this.exchangeName = exchange;
            return this;
        }

        /**
         * Set callback which can be used for overriding default message properties.
         * @param messagePropertiesCallback allow null
         * @return
         */
        public Builder messagePropertiesCallback(Callback<MessageProperties> messagePropertiesCallback) {
            this.messagePropertiesCallback = messagePropertiesCallback;
            return this;
        }

        /**
         * Set exchangeName factory. If you ned to decalre exchangeName then you need to set appropriate factory.
         * @see AmqpUtils.ExchangeFactory
         * @param exchangeFactory
         * @return
         */
        public Builder exchangeFactory(Callable<Exchange> exchangeFactory) {
            this.exchangeFactory = exchangeFactory;
            return this;
        }

        /**
         * Builds a {@link AmqpReporter} with the given properties.
         *
         * @return a {@link AmqpReporter}
         */
        public AmqpReporter build() {
            return new AmqpReporter(this);
        }

    }

    private final ObjectMapper objectMapper = JacksonUtils.objectMapperBuilder();
    private final ObjectFactory<ConnectionFactory> connectionFactoryProvider;
    private final LazyInitializer<RabbitTemplate> templateFuture = new LazyInitializer<>(new Callable<RabbitTemplate>() {
        @Override
        public RabbitTemplate call() throws Exception {
            ConnectionFactory connectionFactory = connectionFactoryProvider.getObject();
            Assert.notNull(connectionFactory, "connectionFactory is null");

            if(exchangeFactory != null) {
                Exchange exchange = exchangeFactory.call();
                if(exchange != null) {
                    RabbitAdmin admin = new RabbitAdmin(connectionFactory);
                    admin.declareExchange(exchange);
                }
            }

            return new RabbitTemplate(connectionFactory);
        }
    });
    private final String routingKey;
    private final String exchangeName;
    private final Callback<MessageProperties> messagePropertiesCallback;
    private final Callable<Exchange> exchangeFactory;

    private AmqpReporter(Builder builder) {
        super(builder.registry, "amqp-reporter", builder.filter, builder.rateUnit, builder.durationUnit);
        this.routingKey = builder.routingKey;
        this.exchangeName = builder.exchangeName;
        Assert.hasText(this.exchangeName, "exchangeName is null or empty");
        this.exchangeFactory = builder.exchangeFactory;
        this.messagePropertiesCallback = builder.messagePropertiesCallback;


        this.connectionFactoryProvider = builder.connectionFactoryProvider;
        Assert.notNull(this.connectionFactoryProvider, "connectionFactoryProvider can't be null");

        objectMapper.registerModules(new HealthCheckModule(),
          new MetricsModule(builder.rateUnit, builder.durationUnit, false /* if enable histogram data will be serialized */));
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        RabbitTemplate rabbitTemplate = this.templateFuture.get();
        MetricReport report = new MetricReport();
        Map<String, Object> data = new HashMap<>();
        report.setMetrics(data);
        // we note that in MetricRegistry all data persisted into single map, and therefore it's keys unique per registry
        data.putAll(gauges);
        data.putAll(counters);
        data.putAll(histograms);
        data.putAll(meters);
        data.putAll(timers);
        try {
            MessageProperties messageProperties = new MessageProperties();
            processMessageProperties(messageProperties);
            // it's not good, but it simply
            report.setApplication(messageProperties.getAppId());
            report.setApplicationVersion(AppInfo.getApplicationVersion());
            report.setHost((String)messageProperties.getHeaders().get(MessageHeaders.HOST));

            byte[] body = objectMapper.writeValueAsBytes(report);
            Message message = new Message(body, messageProperties);
            rabbitTemplate.send(exchangeName, routingKey, message);
        } catch (JsonProcessingException e) {
            log.error("on serialize metrics report", e);
        }
    }

    private void processMessageProperties(MessageProperties messageProperties) {
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding("UTF-8");
        messageProperties.setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        messageProperties.setHeader(MessageHeaders.HOST, OSUtils.getHostName());
        messageProperties.setAppId(AppInfo.getApplicationName());
        if(this.messagePropertiesCallback != null) {
            this.messagePropertiesCallback.call(messageProperties);
        }
    }
}
