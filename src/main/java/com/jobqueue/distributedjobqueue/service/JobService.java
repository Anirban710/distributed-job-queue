package com.jobqueue.distributedjobqueue.service;

import com.jobqueue.distributedjobqueue.model.Job;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
public class JobService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public JobService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public Job createJob(Job job) {
        try {
            job.setId(UUID.randomUUID().toString());
            job.setStatus("PENDING");
            job.setRetryCount(0);

            String jobJson = objectMapper.writeValueAsString(job);

            // Push to queue
            redisTemplate.opsForList().leftPush("jobQueue", jobJson);

            // 🔥 Store job for tracking
            redisTemplate.opsForValue().set("job:" + job.getId(), jobJson);

            Long size = redisTemplate.opsForList().size("jobQueue");
            System.out.println("Queue size AFTER push: " + size);

            return job;
        } catch (Exception e) {
            throw new RuntimeException("Error creating job", e);
        }
    }
}