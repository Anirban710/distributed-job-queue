package com.jobqueue.distributedjobqueue.service;

import com.jobqueue.distributedjobqueue.model.Job;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
public class JobService {

    // ✅ FIXED: use <String, String>
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // ✅ FIXED constructor
    public JobService(RedisTemplate<String, String> redisTemplate) {
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

            // Store job for tracking
            redisTemplate.opsForValue().set("job:" + job.getId(), jobJson);

            Long size = redisTemplate.opsForList().size("jobQueue");
            System.out.println("Queue size AFTER push: " + size);

            return job;
        } catch (Exception e) {
            throw new RuntimeException("Error creating job", e);
        }
    }
}