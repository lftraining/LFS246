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
public class ProductBrokerTrigger {
	
	private static Logger LOGGER = LoggerFactory.getLogger(ProductBrokerTrigger.class);
	
	@Autowired
	private ProductDetailsRepository productDetailsRepository;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private KafkaTemplate<String, CloudEvent> kafkaTemplate;
	
	
	private static String TOPIC_NAME_BT = "create-order";
	
	@Bean
	public Consumer<Message<Object>> blockBT(){
		return this::doBlockProductBT;
	}
	
	private void doBlockProductBT(Message<Object> msg) {
		OrderDetails orderDetails = null;
		if(msg.getPayload() != null && !"".equals(msg.getPayload().toString())){
			
			try {
				orderDetails = mapper.readValue(msg.getPayload().toString(), OrderDetails.class);
			} catch (JsonProcessingException e) {
				LOGGER.error("Error Occured in mapping :: ", e);
			} 
		}
		if(orderDetails != null) {
			LOGGER.info("product-service-es :: Order Recieved BT:: " + orderDetails);
			LOGGER.info("product-service-es :: Order Status BT:: " +  orderDetails.getOrderStatus());
	        ProductDetails productDetails = productDetailsRepository.findById(orderDetails.getProdId()).orElseThrow();
	        if (ProductOrderStatus.NEW.toString().equalsIgnoreCase(orderDetails.getOrderStatus())) {
	            productDetails.setProductBlocked(productDetails.getProductBlocked() + orderDetails.getProdCount());
	            productDetails.setProductAvailable(productDetails.getProductAvailable() - orderDetails.getProdCount());
	            orderDetails.setOrderStatus(ProductOrderStatus.PRODUCT_CONFIRMED.toString());
	            productDetailsRepository.save(productDetails);
	            try {
					kafkaTemplate.send(TOPIC_NAME_BT, this.generateCloudEvent(orderDetails));
				} catch (JsonProcessingException e) {
					LOGGER.error("Error Occurred :: ", e);
				}
	        } else if (ProductOrderStatus.CONFIRMED.toString().equalsIgnoreCase(orderDetails.getOrderStatus())) {
	            productDetails.setProductBlocked(productDetails.getProductBlocked() - orderDetails.getProdCount());
	            productDetailsRepository.save(productDetails);
	        }
	        
	        LOGGER.info("product-service-es :: order details BT :: " + productDetails);
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
