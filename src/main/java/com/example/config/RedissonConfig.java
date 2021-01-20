package com.example.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        //config.useClusterServers().addNodeAddress("rediss://192.168.228.101:6379");
        config.useSingleServer().setAddress("redis://192.168.228.101:6379").setDatabase(0);
        return Redisson.create(config);
    }
    
}
