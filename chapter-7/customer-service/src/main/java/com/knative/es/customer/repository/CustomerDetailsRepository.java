package com.knative.es.customer.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.knative.es.customer.model.CustomerDetails;

@Repository
public interface CustomerDetailsRepository extends CrudRepository<CustomerDetails, Integer>{

}
