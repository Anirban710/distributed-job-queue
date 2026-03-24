package com.jobqueue.distributedjobqueue.controller;

import com.jobqueue.distributedjobqueue.model.Job;
import com.jobqueue.distributedjobqueue.service.JobService;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;
    private final RedisTemplate<String, String> redisTemplate;

    public JobController(JobService jobService,
                         RedisTemplate<String, String> redisTemplate) {
        this.jobService = jobService;
        this.redisTemplate = redisTemplate;
    }

    // POST
    @PostMapping
    public Job createJob(@RequestBody Job job) {
        return jobService.createJob(job);
    }

    // GET job status
    @GetMapping("/{id}")
    public String getJob(@PathVariable String id) {

        String jobJson = redisTemplate.opsForValue().get("job:" + id);

        if (jobJson == null) {
            throw new RuntimeException("Job not found");
        }

        return jobJson; // return JSON directly
    }
}