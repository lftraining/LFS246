package com.knative.order;

import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import com.knative.order.model.OrderDetails;
import com.knative.order.model.OrderResponse;
import com.knative.order.model.OrderStatus;
import com.knative.order.repository.OrderDetailsRepository;


@SpringBootApplication
public class OrderApplication{
	
	private static Logger LOGGER = LoggerFactory.getLogger(OrderApplication.class);
	
	@Autowired
	private OrderDetailsRepository orderDetailsRepository;
	
	@Autowired
	private Environment env;
	
	private WebClient webClient = WebClient.create();
	
	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}
	
	
	@Bean
	public Function<OrderDetails, ResponseEntity<OrderResponse>> place(){
		return this::doPlaceOrder; 
	}
	
	private ResponseEntity<OrderResponse> doPlaceOrder(OrderDetails orderDetails) {
		LOGGER.info("order-service :: Order Request Recieved from revision :: " + env.getProperty("TARGET"));
		LOGGER.info("order-service :: Order Request Recieved :: " + orderDetails.toString());
		orderDetails.setOrderStatus(orderDetails.getOrderStatus() != null ? orderDetails.getOrderStatus() : OrderStatus.NEW.toString());
		String orderStatus = orderDetails.getOrderStatus();
		LOGGER.info("order-service :: Order Status in Request :: " + orderStatus);
		
		if(OrderStatus.NEW.toString().equalsIgnoreCase(orderStatus)) {
			orderDetailsRepository.save(orderDetails);
			this.callCustomer(orderDetails);
			
			this.callproduct(orderDetails);
			return new ResponseEntity<>(OrderResponse.builder().status("SUCCESS")
					   .statusMessage("Request Processed Successfully")
					   .build(), HttpStatus.ACCEPTED);
		}
		
		return new ResponseEntity<>(OrderResponse.builder().status(OrderStatus.IN_PROGRESS.toString())
							   .statusMessage("Request Processed Successfully")
							   .build(), HttpStatus.ACCEPTED);
	}
	
	@Bean
	public Function<OrderDetails, String> confirm(){
		return this::doConfirmOrder; 
	}
	
	private String doConfirmOrder(OrderDetails orderDetails) {
		LOGGER.info("order-service :: Final Order Confirmation  :: " + orderDetails.toString());
		LOGGER.info("order-service :: Order Status  :: " + orderDetails.getOrderStatus());
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
			if(OrderStatus.CUSTOMER_CONFIRMED.toString().equalsIgnoreCase(order.get().getCustomerOrderStatus()) &&
					OrderStatus.PRODUCT_CONFIRMED.toString().equalsIgnoreCase(order.get().getProductOrderStatus())) {
				order.get().setOrderStatus(OrderStatus.CONFIRMED.toString());
				orderDetailsRepository.save(order.get());
				this.callCustomer(order.get());
				this.callproduct(order.get());
				LOGGER.info("Final Order Status :: " + order.get());
			}
		}
		return HttpStatus.OK.toString();
	}
	
	private ResponseEntity<Void> callCustomer(OrderDetails orderDetails){
		return new ResponseEntity<Void>(webClient.post().uri(env.getProperty("customer-service-ep") + "/customers/blockAmount")
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(orderDetails))
				.retrieve()
				.bodyToMono(Void.class)
				.block(), HttpStatus.OK);
			
	}
	
	private ResponseEntity<Void> callproduct(OrderDetails orderDetails) {
		return new ResponseEntity<Void>(webClient.post().uri(env.getProperty("product-service-ep") + "/products/block")
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(orderDetails))
				.retrieve()
				.bodyToMono(Void.class)
				.block(), HttpStatus.OK);
	}

}
