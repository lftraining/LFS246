package com.knative.es.order.repository ;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.knative.es.order.model.OrderDetails;

@Repository
public interface OrderDetailsRepository extends CrudRepository<OrderDetails, Integer>{

}
