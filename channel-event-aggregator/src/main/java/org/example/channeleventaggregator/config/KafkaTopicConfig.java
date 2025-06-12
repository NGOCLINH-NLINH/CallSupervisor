package org.example.channeleventaggregator.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic channelEventAggregatorInputTopic() {
        return new NewTopic("channel-event-aggregator-input1", 2, (short) 1);
    }

    @Bean
    public NewTopic channelEventAggregatorOutputTopic() {
        return new NewTopic("channel-event-aggregator-output", 2, (short) 1);
    }
}