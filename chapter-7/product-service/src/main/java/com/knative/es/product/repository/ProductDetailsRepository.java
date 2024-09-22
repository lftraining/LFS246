package com.knative.es.product.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import com.knative.es.product.model.ProductDetails;

@Repository
public interface ProductDetailsRepository extends CrudRepository<ProductDetails, Integer> {

}
