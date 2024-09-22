package com.knative.es.order.util;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knative.es.order.model.OrderDetails;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;


@Component
public class OrderUtil {
	
	@Autowired
	private ObjectMapper mapper;
	
	public CloudEvent generateCloudEvent(OrderDetails orderDetails) throws JsonProcessingException {
		
		CloudEvent event = CloudEventBuilder.v1().withData(mapper.writeValueAsBytes(orderDetails))
		.withSource(URI.create("http://localhost"))
        .withId(orderDetails.getId().toString())
        .withType(orderDetails.getOrderStatus())
        .build();
		return event;
	}
	
		
	public CloudEvent generateCloudEvent(OrderDetails orderDetails, String subject) throws JsonProcessingException {
		
		CloudEvent event = CloudEventBuilder.v1().withData(mapper.writeValueAsBytes(orderDetails))
		.withSource(URI.create("http://localhost"))
        .withId(orderDetails.getId().toString())
        .withType(orderDetails.getOrderStatus())
        .withSubject(subject)
        .build();
		return event;
	}

}
