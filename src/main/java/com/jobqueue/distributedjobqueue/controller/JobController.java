package com.jobqueue.distributedjobqueue.controller;

import com.jobqueue.distributedjobqueue.model.Job;
import com.jobqueue.distributedjobqueue.service.JobService;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public JobController(JobService jobService,
                         RedisTemplate<String, Object> redisTemplate) {
        this.jobService = jobService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    // ✅ POST API (already exists)
    @PostMapping
    public Job createJob(@RequestBody Job job) {
        return jobService.createJob(job);
    }

    // 🔥 NEW: GET job status
    @GetMapping("/{id}")
    public Job getJob(@PathVariable String id) {

        try {
            Object jobData = redisTemplate.opsForValue().get("job:" + id);

            if (jobData == null) {
                throw new RuntimeException("Job not found");
            }

            String jobJson = (String) jobData;

            return objectMapper.readValue(jobJson, Job.class);

        } catch (Exception e) {
            throw new RuntimeException("Error fetching job", e);
        }
    }
}