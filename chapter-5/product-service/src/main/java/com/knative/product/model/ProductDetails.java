package com.knative.product.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "product_details")
@Getter
@Setter
@NoArgsConstructor
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
