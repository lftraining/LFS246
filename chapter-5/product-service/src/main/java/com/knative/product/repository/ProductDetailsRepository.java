package com.knative.product.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.knative.product.model.ProductDetails;

@Repository
public interface ProductDetailsRepository extends CrudRepository<ProductDetails, Integer> {

}
