package com.knative.customer.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "customer_details")
@Getter
@Setter
@NoArgsConstructor
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
