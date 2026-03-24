package com.jobqueue.distributedjobqueue.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Job implements Serializable {

    private String id;
    private String payload;
    private String status;
    private Integer retryCount;
}