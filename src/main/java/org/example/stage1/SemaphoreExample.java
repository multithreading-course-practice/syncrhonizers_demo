package org.example.stage1;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Log4j2
public class SemaphoreExample implements Runnable {


    private static Semaphore semaphore = new Semaphore(10);

    private final int threadId;

    public SemaphoreExample(int threadId) {
        this.threadId = threadId;
    }

    @Override
    public void run() {
        boolean run = true;
        while (run) {
            if (!acquire()) continue;
            try {
                run = false;
                log.info("aquired " + threadId);
                sleep(1_000);
            } finally {
                log.info("released " + threadId);
                semaphore.release(threadId);
            }
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean acquire() {
        try {
            semaphore.acquire(threadId);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        int threadsNum = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadsNum);

        for (int i = 0; i < threadsNum; i++) {
            final int threadId = i + 1;
            pool.execute(new SemaphoreExample(threadId));
        }

        pool.shutdown();
    }
}
