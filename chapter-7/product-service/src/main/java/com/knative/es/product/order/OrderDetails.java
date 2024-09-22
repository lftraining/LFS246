package com.knative.es.product.order;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class OrderDetails {
	
	private Integer id;
	private Integer prodId;
	private int prodCount;
    private String orderStatus;
}
