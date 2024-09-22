package com.knative.customer;

import java.util.function.Consumer;

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

import com.knative.customer.model.CustomerDetails;
import com.knative.customer.model.CustomerOrderStatus;
import com.knative.customer.order.OrderDetails;
import com.knative.customer.repository.CustomerDetailsRepository;

@SpringBootApplication
public class CustomerApplication {
	
	private static Logger LOGGER = LoggerFactory.getLogger(CustomerApplication.class);
	
	@Autowired
	Environment env;
	
	@Autowired
	private CustomerDetailsRepository customerDetailsRepository;
	
	private WebClient webClient = WebClient.create();
		

	public static void main(String[] args) {
		SpringApplication.run(CustomerApplication.class, args);
	}
	
	@Bean
	public Consumer<OrderDetails> blockAmount(){
		return this::doBlockAmount;
	}
	
			
	private void doBlockAmount(OrderDetails orderDetails) {
		
		LOGGER.info("Customer-service :: Order Details :: " +orderDetails.toString());
		LOGGER.info("Customer-service :: Order Status :: " +  orderDetails.getOrderStatus());
		if(orderDetails != null) {
	        CustomerDetails customerDetails = customerDetailsRepository.findById(orderDetails.getCustId()).orElseThrow();
	        if (CustomerOrderStatus.NEW.toString().equalsIgnoreCase(orderDetails.getOrderStatus())) {
	            customerDetails.setWalletAmountBlocked(customerDetails.getWalletAmountBlocked() + orderDetails.getAmount());
	            customerDetails.setWalletAmount(customerDetails.getWalletAmount() - orderDetails.getAmount());
	            orderDetails.setOrderStatus(CustomerOrderStatus.CUSTOMER_CONFIRMED.toString());
	            customerDetailsRepository.save(customerDetails);
	            this.callOrder(orderDetails);
	        } else if (CustomerOrderStatus.CONFIRMED.toString().equalsIgnoreCase(orderDetails.getOrderStatus())) {
	            customerDetails.setWalletAmountBlocked(customerDetails.getWalletAmountBlocked() - orderDetails.getAmount());
	            customerDetailsRepository.save(customerDetails);
	        }
	        
	        LOGGER.info("Customer-service :: Order details :: " + customerDetails);
        }
	}
	
	private ResponseEntity<Void> callOrder(OrderDetails orderDetails) {
		return new ResponseEntity<Void>(webClient.post().uri(env.getProperty("order-service-ep") + "/orders/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(orderDetails))
				.retrieve()
				.bodyToMono(Void.class)
				.block(), HttpStatus.OK);
	}
	
}
