package com.knative.order.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "order_details")
@Getter
@Setter
@NoArgsConstructor
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
