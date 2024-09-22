package com.knative.es.customer;

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
import com.knative.es.customer.model.CustomerDetails;
import com.knative.es.customer.model.CustomerOrderStatus;
import com.knative.es.customer.order.OrderDetails;
import com.knative.es.customer.repository.CustomerDetailsRepository;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

@Configuration
public class CustomerBrokerTrigger {
	
	private static Logger LOGGER = LoggerFactory.getLogger(CustomerBrokerTrigger.class);
	
	@Autowired
	private CustomerDetailsRepository customerDetailsRepository;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private KafkaTemplate<String, CloudEvent> kafkaTemplate;
	
	
	private static String TOPIC_NAME_BT = "create-order";
	
	@Bean
	public Consumer<Message<Object>> blockAmountBT(){
		return this::doBlockAmountBT;
	}
	
	private void doBlockAmountBT(Message<Object> msg) {
		OrderDetails orderDetails = null;
		if(msg.getPayload() != null && !"".equals(msg.getPayload().toString())){	
			try {
				orderDetails = mapper.readValue(msg.getPayload().toString(), OrderDetails.class);
			} catch (JsonProcessingException e) {
				LOGGER.error("Error Occured in mapping :: ", e);
			} 
		}
		if(orderDetails != null) {
			LOGGER.info("customer-service-es :: Order Details BT :: " + orderDetails);
			LOGGER.info("customer-service-es :: Order Status BT :: " +  orderDetails.getOrderStatus());
	        CustomerDetails customerDetails = customerDetailsRepository.findById(orderDetails.getCustId()).orElseThrow();
	        if (CustomerOrderStatus.NEW.toString().equalsIgnoreCase(orderDetails.getOrderStatus())) {
	            customerDetails.setWalletAmountBlocked(customerDetails.getWalletAmountBlocked() + orderDetails.getAmount());
	            customerDetails.setWalletAmount(customerDetails.getWalletAmount() - orderDetails.getAmount());
	            orderDetails.setOrderStatus(CustomerOrderStatus.CUSTOMER_CONFIRMED.toString());
	            customerDetailsRepository.save(customerDetails);
	            try {
					kafkaTemplate.send(TOPIC_NAME_BT, this.generateCloudEvent(orderDetails));
				} catch (JsonProcessingException e) {
					LOGGER.error("Error Occourred :: ", e);
				}
	        } else if (CustomerOrderStatus.CONFIRMED.toString().equalsIgnoreCase(orderDetails.getOrderStatus())) {
	            customerDetails.setWalletAmountBlocked(customerDetails.getWalletAmountBlocked() - orderDetails.getAmount());
	            customerDetailsRepository.save(customerDetails);
	        }
	        
	        LOGGER.info("Customer-service-es :: Order details BT :: " + customerDetails);
        }
	}
	
	/**
	 * Method used to generate cloud events
	 * @param orderDetails
	 * @return
	 * @throws JsonProcessingException
	 */
	private CloudEvent generateCloudEvent(OrderDetails orderDetails) throws JsonProcessingException {
		return CloudEventBuilder.v1().withData(mapper.writeValueAsBytes(orderDetails))
				.withSource(URI.create("http://localhost"))
                .withId(orderDetails.getId().toString())
                .withType(orderDetails.getOrderStatus())
                .withSubject("call-order")
                .build();
	}

}
