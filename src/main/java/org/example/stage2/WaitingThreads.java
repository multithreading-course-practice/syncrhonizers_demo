package org.example.stage2;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.sleep;

@Log4j2
public class WaitingThreads {

    public static void main(String[] args) {

        Thread[] threads = new Thread[5];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                log.info("Thread started");
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                log.info("Thread finished");
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);

            }

        }
        log.info("Main finished");
    }
}

@Log4j2
class WaitingThreads2 {

    static CountDownLatch latch = new CountDownLatch(5);
    static ExecutorService executor = Executors.newFixedThreadPool(5);

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++){
            executor.submit(() -> {
                log.info("Thread started");
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                log.info("Thread finished");
                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Main finished");
        executor.shutdown();
    }
}
