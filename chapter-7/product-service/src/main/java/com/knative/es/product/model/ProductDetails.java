package com.knative.es.product.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "product_details")
@Setter
@Getter
@ToString
public class ProductDetails {
	
	@Id
	private Integer id;
	
	@Column (name = "product_name")
	private String prodName;
	
	@Column (name = "product_available")
	private Integer productAvailable;
	
	@Column (name = "product_blocked")
	private Integer productBlocked;
	
}
