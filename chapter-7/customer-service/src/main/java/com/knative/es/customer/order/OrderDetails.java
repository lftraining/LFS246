package com.knative.es.customer.order;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class OrderDetails {
	
	private Integer id;
	private Integer custId;
	private int amount;
    private String orderStatus;
}
