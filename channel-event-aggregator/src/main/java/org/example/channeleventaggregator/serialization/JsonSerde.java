package org.example.channeleventaggregator.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Map;

public class JsonSerde<T> implements Serde<T> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Class<T> targetType;

    public JsonSerde(Class<T> targetType) {
        this.targetType = targetType;
    }

    @Override
    public void close() {}

    @Override
    public Serializer<T> serializer() {
        return new Serializer<T>() {
            @Override
            public byte[] serialize(String topic, T data) {
                if (data == null) {
                    return null; // Kafka interprets null as a tombstone for KTables
                }
                try {
                    return objectMapper.writeValueAsBytes(data); // Convert Java object to JSON byte array
                } catch (IOException e) {
                    throw new RuntimeException("Failed to serialize object to JSON", e);
                }
            }

            @Override
            public void close() {}
        };
    }

    @Override
    public Deserializer<T> deserializer() {
        return new Deserializer<T>() {
            @Override
            public T deserialize(String topic, byte[] data) {
                if (data == null) {
                    return null; // Interpret null byte array as null object
                }
                try {
                    return objectMapper.readValue(data, targetType); // Convert JSON byte array to Java object
                } catch (IOException e) {
                    throw new RuntimeException("Failed to deserialize JSON to object", e);
                }
            }

            @Override
            public void close() {}
        };
    }
}
