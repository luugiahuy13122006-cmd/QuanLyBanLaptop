package com.laptopshop.service;

import com.laptopshop.entity.JobLock;
import com.laptopshop.repository.JobLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;

@Service
public class JobLockService {

    private final JobLockRepository jobLockRepository;

    public JobLockService(JobLockRepository jobLockRepository) {
        this.jobLockRepository = jobLockRepository;
    }

    @Transactional
    public boolean tryAcquire(String jobName, int lockSeconds) {
        JobLock lock = jobLockRepository.findByNameWithLock(jobName)
                .orElseGet(() -> jobLockRepository.saveAndFlush(new JobLock(jobName)));
        LocalDateTime now = LocalDateTime.now();
        if (lock.getLockedUntil() != null && lock.getLockedUntil().isAfter(now)) {
            return false;
        }
        lock.setLockedBy(resolveHost());
        lock.setLockedUntil(now.plusSeconds(lockSeconds));
        lock.setLastRunAt(now);
        jobLockRepository.save(lock);
        return true;
    }

    @Transactional
    public void release(String jobName) {
        jobLockRepository.findByNameWithLock(jobName).ifPresent(lock -> {
            lock.setLockedUntil(LocalDateTime.now().minusSeconds(1));
            jobLockRepository.save(lock);
        });
    }

    private static String resolveHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return Thread.currentThread().getName();
        }
    }
}
