package com.knative.es.customer.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "customer_details")
@Getter
@Setter
@ToString
public class CustomerDetails {
	
	@Id
	private Integer id;
	
	@Column(name = "customer_name")
	private String custName;
	
	@Column(name = "wallet_amount")
	private Integer walletAmount;
	
	@Column(name = "wallet_amount_blocked")
	private Integer walletAmountBlocked;
	
}
