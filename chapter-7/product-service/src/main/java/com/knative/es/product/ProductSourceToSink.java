package com.knative.es.product;

import java.net.URI;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knative.es.product.model.ProductDetails;
import com.knative.es.product.model.ProductOrderStatus;
import com.knative.es.product.order.OrderDetails;
import com.knative.es.product.repository.ProductDetailsRepository;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

@Configuration
public class ProductSourceToSink {
	private static Logger LOGGER = LoggerFactory.getLogger(ProductSourceToSink.class);
	
	@Autowired
	private ProductDetailsRepository productDetailsRepository;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private KafkaTemplate<String, CloudEvent> kafkaTemplate;
	
	private static String TOPIC_NAME = "place-order";
	
	@Bean
	public Consumer<Message<OrderDetails>> block(){
		return this::doBlockProduct;
	}
		
	private void doBlockProduct(Message<OrderDetails> msg) {
		OrderDetails orderDetails = msg != null ? msg.getPayload() : null;
		LOGGER.info("product-service-es :: Order Recieved :: " + orderDetails);
		LOGGER.info("product-service-es :: Order Status :: " +  orderDetails.getOrderStatus());
        if(orderDetails != null) {
	        ProductDetails productDetails = productDetailsRepository.findById(orderDetails.getProdId()).orElseThrow();
	        if (ProductOrderStatus.NEW.toString().equalsIgnoreCase(orderDetails.getOrderStatus())) {
	            productDetails.setProductBlocked(productDetails.getProductBlocked() + orderDetails.getProdCount());
	            productDetails.setProductAvailable(productDetails.getProductAvailable() - orderDetails.getProdCount());
	            orderDetails.setOrderStatus(ProductOrderStatus.PRODUCT_CONFIRMED.toString());
	            productDetailsRepository.save(productDetails);
	            try {
					kafkaTemplate.send(TOPIC_NAME, this.generateCloudEvent(orderDetails));
				} catch (JsonProcessingException e) {
					LOGGER.error("Error Occurred :: ", e);
				}
	        } else if (ProductOrderStatus.CONFIRMED.toString().equalsIgnoreCase(orderDetails.getOrderStatus())) {
	            productDetails.setProductBlocked(productDetails.getProductBlocked() - orderDetails.getProdCount());
	            productDetailsRepository.save(productDetails);
	        }
	        
	        LOGGER.info("product-service-es :: order details :: " + productDetails);
        }
    }
	
	private CloudEvent generateCloudEvent(OrderDetails orderDetails) throws JsonProcessingException {
		
		return CloudEventBuilder.v1().withData(mapper.writeValueAsBytes(orderDetails))
				.withSource(URI.create("http://localhost"))
                .withId(orderDetails.getId().toString())
                .withType(orderDetails.getOrderStatus())
                .withSubject("call-order")
                .build();
	}
}
