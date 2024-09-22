package com.knative.product;

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

import com.knative.product.model.ProductDetails;
import com.knative.product.model.ProductOrderStatus;
import com.knative.product.order.OrderDetails;
import com.knative.product.repository.ProductDetailsRepository;


@SpringBootApplication
public class ProductApplication {
	
	private static Logger LOGGER = LoggerFactory.getLogger(ProductApplication.class);
	
	private WebClient webClient = WebClient.create();
	
	@Autowired
	private Environment env;
	
	@Autowired
	private ProductDetailsRepository productDetailsRepository;
	
	public static void main(String[] args) {
		SpringApplication.run(ProductApplication.class, args);
	}
	
	@Bean
	public Consumer<OrderDetails> block(){
		return this::doBlockProduct;
	}
			
	private void doBlockProduct(OrderDetails orderDetails) {
		LOGGER.info("product-service :: Order Recieved :: " + orderDetails.toString());
		LOGGER.info("product-service :: Order Status :: " +  orderDetails.getOrderStatus());
        if(orderDetails != null) {
	        ProductDetails productDetails = productDetailsRepository.findById(orderDetails.getProdId()).orElseThrow();
	        if (ProductOrderStatus.NEW.toString().equalsIgnoreCase(orderDetails.getOrderStatus())) {
	            productDetails.setProductBlocked(productDetails.getProductBlocked() + orderDetails.getProdCount());
	            productDetails.setProductAvailable(productDetails.getProductAvailable() - orderDetails.getProdCount());
	            orderDetails.setOrderStatus(ProductOrderStatus.PRODUCT_CONFIRMED.toString());
	            productDetailsRepository.save(productDetails);
	            this.callOrder(orderDetails);
	        } else if (ProductOrderStatus.CONFIRMED.toString().equalsIgnoreCase(orderDetails.getOrderStatus())) {
	            productDetails.setProductBlocked(productDetails.getProductBlocked() - orderDetails.getProdCount());
	            productDetailsRepository.save(productDetails);
	        }    
	        LOGGER.info("product-service :: order details :: " + productDetails);
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
