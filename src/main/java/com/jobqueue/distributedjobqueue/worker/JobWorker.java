package com.jobqueue.distributedjobqueue.worker;

import com.jobqueue.distributedjobqueue.model.Job;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Component
public class JobWorker {

    private final RedisTemplate<String, String> redisTemplate; // ✅ FIXED
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRY = 3;

    public JobWorker(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void startWorker() {
        new Thread(() -> {
            while (true) {
                try {
                    String jobJson = redisTemplate.opsForList()
                            .rightPop("jobQueue", 5, TimeUnit.SECONDS);

                    if (jobJson != null) {

                        Job job = objectMapper.readValue(jobJson, Job.class);

                        System.out.println("Processing job: " + job);

                        try {
                            // Simulate failure
                            if (Math.random() < 0.5) {
                                throw new RuntimeException("Simulated failure");
                            }

                            // ✅ SUCCESS
                            job.setStatus("COMPLETED");

                            String updatedJob = objectMapper.writeValueAsString(job);

                            redisTemplate.opsForValue()
                                    .set("job:" + job.getId(), updatedJob);

                            System.out.println("Job completed: " + job);

                        } catch (Exception e) {

                            int retry = job.getRetryCount();

                            if (retry < MAX_RETRY) {

                                // 🔁 RETRY
                                job.setRetryCount(retry + 1);
                                job.setStatus("RETRYING");

                                String updatedJob = objectMapper.writeValueAsString(job);

                                redisTemplate.opsForValue()
                                        .set("job:" + job.getId(), updatedJob);

                                redisTemplate.opsForList()
                                        .leftPush("jobQueue", updatedJob);

                                System.out.println("Retrying job (" + (retry + 1) + "): " + job);

                            } else {

                                // ☠️ DLQ
                                job.setStatus("FAILED");

                                String failedJob = objectMapper.writeValueAsString(job);

                                redisTemplate.opsForValue()
                                        .set("job:" + job.getId(), failedJob);

                                redisTemplate.opsForList()
                                        .leftPush("deadLetterQueue", failedJob);

                                System.out.println("Moved to DLQ: " + job);
                            }
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Worker error: " + e.getMessage());
                    try {
                        Thread.sleep(2000); // prevent spam
                    } catch (InterruptedException ignored) {}
                }
            }
        }).start();
    }
}