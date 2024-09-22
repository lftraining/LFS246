package com.knative.es.order;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knative.es.order.model.OrderDetails;
import com.knative.es.order.model.OrderResponse;
import com.knative.es.order.model.OrderStatus;
import com.knative.es.order.repository.OrderDetailsRepository;
import com.knative.es.order.util.OrderUtil;

import io.cloudevents.CloudEvent;

public class OrderChannelSubscription {
	private static Logger LOGGER = LoggerFactory.getLogger(OrderChannelSubscription.class);
	
	@Autowired
	private KafkaTemplate<String, CloudEvent> kafkaTemplate;
	
	@Autowired
	private OrderUtil orderUtil;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private OrderDetailsRepository orderDetailsRepository;
	
	private static String TOPIC_NAME_CS = "purchase-order";
	
	
	@Bean
	public Function<OrderDetails, ResponseEntity<OrderResponse>> purchase() throws JsonMappingException, JsonProcessingException {
		return this::doPurchaseOrder;
	}
	
	private ResponseEntity<OrderResponse> doPurchaseOrder(OrderDetails order){
		LOGGER.info("order-service-es :: Order Request Recieved CS :: " + order.toString());
		order.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus() : OrderStatus.NEW.toString());
		String orderStatus = order.getOrderStatus();
		LOGGER.info("order-service-es :: Order Status in CS Request :: " + orderStatus);
		if(OrderStatus.NEW.toString().equalsIgnoreCase(orderStatus)) {
			orderDetailsRepository.save(order);
			try {
				kafkaTemplate.send(TOPIC_NAME_CS, orderUtil.generateCloudEvent(order, "call-customer"));
				kafkaTemplate.send(TOPIC_NAME_CS, orderUtil.generateCloudEvent(order, "call-product"));
			}catch (JsonProcessingException e) {
				LOGGER.error("Error while pushing message in topic :: ", e);
			}
			return new ResponseEntity<>(OrderResponse.builder().status(OrderStatus.IN_PROGRESS.toString())
					   .statusMessage("Request Processed Successfully")
					   .build(), HttpStatus.ACCEPTED);
		}
		
		return new ResponseEntity<>(OrderResponse.builder().status(OrderStatus.IN_PROGRESS.toString())
				   .statusMessage("Request Processed Successfully")
				   .build(), HttpStatus.BAD_REQUEST);
	}
	
	@Bean
	public Consumer<Message<Object>> confirmOrderCS(){
		return this::doConfrimOrderCS;
	}
	
	private void doConfrimOrderCS(Message<Object> msg) {
		OrderDetails orderDetails = null;
		if(msg.getPayload() != null && !"".equals(msg.getPayload().toString())){
			try {
				orderDetails = mapper.readValue(msg.getPayload().toString(), OrderDetails.class);
			} catch (JsonProcessingException e) {
				LOGGER.error("Error Occured in mapping :: ", e);
			} 
		}
		
		if(orderDetails != null) {
			LOGGER.info("order-service-es :: Message Recieved CS :: "+ orderDetails);
			LOGGER.info("order-service-es :: Order Status CS :: "+ orderDetails.getOrderStatus());
			Optional<OrderDetails> order = orderDetailsRepository.findById(orderDetails.getId());
			
			if(order.isPresent()) {
				
				//In progress Order
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
				LOGGER.info("Order Status in DB CS :: " + order.get());
				if(OrderStatus.CUSTOMER_CONFIRMED.toString().equalsIgnoreCase(order.get().getCustomerOrderStatus()) &&
						OrderStatus.PRODUCT_CONFIRMED.toString().equalsIgnoreCase(order.get().getProductOrderStatus())) {
					order.get().setOrderStatus(OrderStatus.CONFIRMED.toString());
					orderDetailsRepository.save(order.get());
					LOGGER.info("Final Order Status CS :: " + order.get());
					try {
						kafkaTemplate.send(TOPIC_NAME_CS, orderUtil.generateCloudEvent(order.get(), "call-customer"));
						kafkaTemplate.send(TOPIC_NAME_CS, orderUtil.generateCloudEvent(order.get(), "call-product"));
					} catch (JsonProcessingException e) {
						LOGGER.error("Error Occurred :: ", e);
					}
					
				}
			}
		}
	}
}
