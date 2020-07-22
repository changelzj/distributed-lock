package com.example.controller;

import com.example.entity.Cart;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@Slf4j
public class WebController2 {
    
    @Autowired
    private RedissonClient redissonClient;

    @RequestMapping("map")
    public void add() {
        RMap<Object, Object> map = redissonClient.getMap("cart");
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setNum(2);
        cart.setPrice(BigDecimal.valueOf(12.36));
        map.put("cart", cart);
        
        
    }
}
