package com.ecommerce.project.service;

import com.ecommerce.project.payload.AnalyticsResponse;
import com.ecommerce.project.repositries.OrderRepository;
import com.ecommerce.project.repositries.ProductRepositry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    @Autowired
    private ProductRepositry productRepositry;
    @Autowired
    private OrderRepository orderRepository;

    @Override
    public AnalyticsResponse getAnalylicData() {
        AnalyticsResponse response = new AnalyticsResponse();
        long productCount = productRepositry.count();
        long totalOrder = orderRepository.count();
        Double totaRevenue = orderRepository.getTotalRevenue();

        response.setProductCount(String.valueOf(productCount));
        response.setTotalRevenue(String.valueOf(totaRevenue != null ? totaRevenue : 0));
        response.setTotalOrders(String.valueOf(totalOrder));
        return response;
    }
}
