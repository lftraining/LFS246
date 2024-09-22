package com.knative.product.order;

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
	private Integer prodId;
	private int prodCount;
    private String orderStatus;
}
