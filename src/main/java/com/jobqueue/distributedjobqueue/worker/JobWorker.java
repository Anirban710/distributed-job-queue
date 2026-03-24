package com.jobqueue.distributedjobqueue.worker;

import com.jobqueue.distributedjobqueue.model.Job;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Component
public class JobWorker {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRY = 3;

    public JobWorker(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void startWorker() {
        new Thread(() -> {
            while (true) {
                try {
                    // 🔥 Blocking pop (wait max 5 seconds)
                    Object jobData = redisTemplate.opsForList()
                            .rightPop("jobQueue", 5, TimeUnit.SECONDS);

                    if (jobData != null) {

                        String jobJson = (String) jobData;

                        // 🔥 Convert JSON → Job object
                        Job job = objectMapper.readValue(jobJson, Job.class);

                        System.out.println("Processing job: " + job);

                        try {
                            // 🔥 Simulate random failure
                            if (Math.random() < 0.5) {
                                throw new RuntimeException("Simulated failure");
                            }

                            // ✅ SUCCESS CASE
                            job.setStatus("COMPLETED");

                            String updatedJob = objectMapper.writeValueAsString(job);

                            // 🔥 Update status in Redis
                            redisTemplate.opsForValue()
                                    .set("job:" + job.getId(), updatedJob);

                            System.out.println("Job completed: " + job);

                        } catch (Exception e) {

                            int retry = job.getRetryCount();

                            if (retry < MAX_RETRY) {

                                // 🔁 RETRY CASE
                                job.setRetryCount(retry + 1);
                                job.setStatus("RETRYING");

                                String updatedJob = objectMapper.writeValueAsString(job);

                                // 🔥 Update status in Redis
                                redisTemplate.opsForValue()
                                        .set("job:" + job.getId(), updatedJob);

                                // 🔁 Push back to queue
                                redisTemplate.opsForList()
                                        .leftPush("jobQueue", updatedJob);

                                System.out.println("Retrying job (" + (retry + 1) + "): " + job);

                            } else {

                                // ☠️ DLQ CASE
                                job.setStatus("FAILED");

                                String failedJob = objectMapper.writeValueAsString(job);

                                // 🔥 Update status in Redis
                                redisTemplate.opsForValue()
                                        .set("job:" + job.getId(), failedJob);

                                // 🔥 Move to Dead Letter Queue
                                redisTemplate.opsForList()
                                        .leftPush("deadLetterQueue", failedJob);

                                System.out.println("Moved to DLQ: " + job);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}