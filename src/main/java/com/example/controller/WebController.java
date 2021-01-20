package com.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class WebController {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private String str;
    
    @RequestMapping("increment")
    public void increment() {

        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        if (lock) {
            try {
                String s = redisTemplate.opsForValue().get("num");
                if (s != null) {
                    int num = Integer.parseInt(s);
                    num ++;
                    redisTemplate.opsForValue().set("num", Integer.toString(num));
                }
            } finally {
                // lua 防止一个线程超时已经取消锁，另一个线程建立新的锁后，又重复删去别人的锁
                String lua = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class), Arrays.asList("lock"), uuid);

                /*不采用脚本的删锁方式，事务
                while (true) {
                    redisTemplate.watch("lock");
                    if (redisTemplate.opsForValue().get("lock").equals(uuid)) {
                        redisTemplate.setEnableTransactionSupport(true);
                        redisTemplate.multi();
                        redisTemplate.delete("lock");
                        List<Object> exec = redisTemplate.exec();
                        if (exec == null) {
                            continue;
                        }
                    }
                    redisTemplate.unwatch();
                    break;
                }*/
            }
            
        }
        else {
            increment();
        }

        
    }


    /**
     * 可重入锁
     */
    @RequestMapping("lock")
    public void lock() {
        RLock lock = redissonClient.getLock("lock");
        try {
            lock.lock(3, TimeUnit.SECONDS);
            System.out.println("lock...");
            String s = redisTemplate.opsForValue().get("num");
            if (s != null) {
                int num = Integer.parseInt(s);
                num ++;
                redisTemplate.opsForValue().set("num", Integer.toString(num));
            }
        } finally {
            if (lock.isHeldByCurrentThread() && lock.isLocked()) {
                lock.unlock();
            }
            System.out.println("unlock...");
        }
        
    }
    
    
    
    
    @RequestMapping("read")
    public String read() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("readwrite_lock");
        RLock rLock = lock.readLock();
        rLock.lock();
        String a = str;
        rLock.unlock();
        return a;
    }

    @RequestMapping("write")
    public String write() throws InterruptedException {
        RReadWriteLock lock = redissonClient.getReadWriteLock("readwrite_lock");
        RLock rLock = lock.writeLock();
        rLock.lock();
        TimeUnit.SECONDS.sleep(10);
        str = UUID.randomUUID().toString();
        rLock.unlock();
        return str;
    }
    
    @RequestMapping("semaphore")
    public String semaphore() {
        RSemaphore semaphore = redissonClient.getSemaphore("semaphore");
        try {
            semaphore.acquire();
            System.out.println(Thread.currentThread().getName()+"抢占");
            
            TimeUnit.SECONDS.sleep(10);
            System.out.println(Thread.currentThread().getName()+"离开");
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
        return "OK";
    }


    @RequestMapping("count")
    public String count() {
        RCountDownLatch countDownLatch = redissonClient.getCountDownLatch("count");
        countDownLatch.trySetCount(3);
        try {
            countDownLatch.countDown();
            System.out.println(Thread.currentThread().getName()+" in");

            countDownLatch.await();
            System.out.println("finish");
        } catch (Exception e){
            e.printStackTrace();
        }
        return "OK";
    }
}
