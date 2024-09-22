package com.knative.es.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "order_details")
@Getter
@Setter
@ToString
public class OrderDetails {
	
	@Id
	private Integer id;
	
	@Column(name = "customer_id")
	private Integer custId;
	
	@Column(name = "product_id")
	private Integer prodId;
	
	@Column(name = "amount")
	private Integer amount;
	
	@Column(name = "product_count")
	private Integer prodCount;
	
	@Column(name = "product_order_status")
	private String productOrderStatus;
	
	@Column(name = "customer_order_status")
	private String customerOrderStatus;
	
	@Column(name = "order_status")
	private String orderStatus;
}
