package com.knative.es.customer.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import io.cloudevents.CloudEvent;
import io.cloudevents.kafka.CloudEventSerializer;


@Configuration
public class KafkaConfig {
	
	@Value("${spring.kafka.bootstrap-servers}")
	private String kafkaBootstrapServer;
	
	@Bean
    public ProducerFactory<String, CloudEvent> producerFactory(){
		
        Map<String,Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServer);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CloudEventSerializer.class);
        return new DefaultKafkaProducerFactory<String, CloudEvent>(config);
    }
	
	@Bean
    public KafkaTemplate<String, CloudEvent> kafkaTemplate(){
        return new KafkaTemplate<String, CloudEvent>(producerFactory());
    }
}
