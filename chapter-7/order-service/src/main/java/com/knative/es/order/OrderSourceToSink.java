package com.knative.es.order;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.knative.es.order.model.OrderDetails;
import com.knative.es.order.model.OrderResponse;
import com.knative.es.order.model.OrderStatus;
import com.knative.es.order.repository.OrderDetailsRepository;
import com.knative.es.order.util.OrderUtil;

import io.cloudevents.CloudEvent;

@Configuration
public class OrderSourceToSink {
	private static Logger LOGGER = LoggerFactory.getLogger(OrderSourceToSink.class);
	
	@Autowired
	private KafkaTemplate<String, CloudEvent> kafkaTemplate;
	
	@Autowired
	private OrderUtil orderUtil;
	
	@Autowired
	private OrderDetailsRepository orderDetailsRepository;
	
	private static String TOPIC_NAME_PRODUCT = "block-products";
	
	private static String TOPIC_NAME_CUSTOMER = "block-customers";
	
	/**
	 * Function used to take initial order from customer
	 * @param order
	 * @return
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	@Bean
	public Function<OrderDetails, ResponseEntity<OrderResponse>> place() throws JsonMappingException, JsonProcessingException {
		return this :: doPlace;
	}
	
	private ResponseEntity<OrderResponse> doPlace(OrderDetails order){
		LOGGER.info("order-service-es :: Order Request Recieved :: " + order.toString());
		order.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus() : OrderStatus.NEW.toString());
		String orderStatus = order.getOrderStatus();
		
		LOGGER.info("order-service-es :: Order Status in Request :: " + orderStatus);
		CloudEvent event = null;
		try {
			event = orderUtil.generateCloudEvent(order);
		} catch (JsonProcessingException e) {
			LOGGER.error("Error in creating order :: ", e);
		}
		
		LOGGER.info("order-service-es :: Order Cloud Event Generated :: " + event);
		
		if(OrderStatus.NEW.toString().equalsIgnoreCase(orderStatus)) {
			orderDetailsRepository.save(order);
			kafkaTemplate.send(TOPIC_NAME_PRODUCT, event);
			kafkaTemplate.send(TOPIC_NAME_CUSTOMER, event);
			return new ResponseEntity<>(OrderResponse.builder().status(OrderStatus.IN_PROGRESS.toString())
					   .statusMessage("Request Processed Successfully")
					   .build(), HttpStatus.ACCEPTED);
		}
		
		return new ResponseEntity<>(OrderResponse.builder().status(OrderStatus.IN_PROGRESS.toString())
				   .statusMessage("Request Processed Successfully")
				   .build(), HttpStatus.BAD_REQUEST);
	}
	
	
	@Bean
	public Consumer<Message<OrderDetails>> confirm(){
		return this::doConfirmOrder;
	}
	
	private void doConfirmOrder(Message<OrderDetails> msg) {
		OrderDetails orderDetails = msg.getPayload();
		LOGGER.info("order-service-es :: Message Recieved  :: "+ orderDetails);
		LOGGER.info("order-service-es :: Order Status  :: "+ orderDetails.getOrderStatus());
		Optional<OrderDetails> order = orderDetailsRepository.findById(orderDetails.getId());
		
		if(order.isPresent()) {
			
			//In-progress Order
			String orderStatus = orderDetails.getOrderStatus();	
			if(OrderStatus.CUSTOMER_CONFIRMED.toString().equalsIgnoreCase(orderStatus)) {
				order.get().setOrderStatus(OrderStatus.IN_PROGRESS.toString());
				order.get().setCustomerOrderStatus(orderStatus);
				order.get().setProductOrderStatus(order.get().getProductOrderStatus());
				orderDetailsRepository.save(order.get());
			}else if(OrderStatus.PRODUCT_CONFIRMED.toString().equalsIgnoreCase(orderStatus)) {
				order.get().setOrderStatus(OrderStatus.IN_PROGRESS.toString());
				order.get().setProductOrderStatus(orderStatus);
				order.get().setCustomerOrderStatus(order.get().getCustomerOrderStatus());
				orderDetailsRepository.save(order.get());
			}
			order = orderDetailsRepository.findById(orderDetails.getId());
			if(OrderStatus.CUSTOMER_CONFIRMED.toString().equalsIgnoreCase(order.get().getCustomerOrderStatus()) &&
					OrderStatus.PRODUCT_CONFIRMED.toString().equalsIgnoreCase(order.get().getProductOrderStatus())) {
				order.get().setOrderStatus(OrderStatus.CONFIRMED.toString());
				orderDetailsRepository.save(order.get());
				LOGGER.info("Final Order Status :: " + order.get());
				try {
					kafkaTemplate.send(TOPIC_NAME_CUSTOMER, orderUtil.generateCloudEvent(order.get(), "call-customer"));
					kafkaTemplate.send(TOPIC_NAME_PRODUCT, orderUtil.generateCloudEvent(order.get(), "call-product"));
				} catch (JsonProcessingException e) {
					LOGGER.error("Error Occurred :: ", e);
				}
				
			}
		}
	}
	
}
