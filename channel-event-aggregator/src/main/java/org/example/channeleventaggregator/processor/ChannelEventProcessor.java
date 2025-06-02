package org.example.channeleventaggregator.processor;


import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.example.channeleventaggregator.serialization.JsonSerde;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
//import org.example.channeleventaggregator.model.CallEvent;
import org.example.commonmodel.model.CallEvent;
import org.example.channeleventaggregator.model.ChannelEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Component
public class ChannelEventProcessor {
    @Bean
    public Function<KStream<String, CallEvent>, KStream<String, ChannelEvent>> processChannelEvents() {
        return kzCallEventStream -> {
            Materialized<String, ChannelEvent, KeyValueStore<Bytes, byte[]>> materialized =
                    Materialized.<String, ChannelEvent, KeyValueStore<Bytes, byte[]>>as("channel-events-store")
                            .withKeySerde(Serdes.String())
                            .withValueSerde(new JsonSerde<>(ChannelEvent.class))
                            .withCachingDisabled();

            KTable<String, ChannelEvent> kzCallTable = kzCallEventStream
                    .flatMapValues(this::flatMapForChannelDestroyEvent)
                    .groupByKey(Grouped.with(Serdes.String(), new JsonSerde<>(CallEvent.class)))
                    .aggregate(
                            ChannelEvent::new,
                            (key, callEvent, aggregated) -> aggregate(callEvent, aggregated),
                            materialized
                    );

            return kzCallTable.toStream()
                    .filter((key, value) -> value != null && value.getCallId() != null)
                    .peek((key, value) -> System.out.println("Aggregated ChannelEvent: " + key + " -> " + value.getState() + ", Queue: " + value.getQueueId() + ", VC#: " + value.getVcNumber()));
        };
    }

    private ChannelEvent aggregate(CallEvent callEvt, ChannelEvent aggregated) {
        if (callEvt.isDestroyEvent()) {
            aggregated.with(callEvt); // Update DESTROY event before deleting
            return null; // Return null to delete from table
        }
        return aggregated.with(callEvt);
    }

    private List<CallEvent> flatMapForChannelDestroyEvent(CallEvent value) {
        if (value.isDestroyEvent()) {
            return Arrays.asList(value);
        }
        return Arrays.asList(value);
    }
}
