package com.knative.customer.order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class OrderDetails {
	private Integer id;
	private Integer custId;
	private int amount;
    private String orderStatus;
}
