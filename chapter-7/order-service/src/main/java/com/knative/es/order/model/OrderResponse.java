package com.knative.es.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OrderResponse {
	
	private String status;
	private String statusMessage;
}
