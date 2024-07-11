package org.example.stage2;

import lombok.extern.log4j.Log4j2;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class CountDownLatchExample implements Runnable {


    private static CountDownLatch readySignal = new CountDownLatch(10);
    private static CountDownLatch startSignal = new CountDownLatch(1);

    private final int threadId;

    public CountDownLatchExample(int threadId) {
        this.threadId = threadId;
    }

    @Override
    public void run() {
        while (startSignal.getCount() > 0) {
            awaitLatch(startSignal);
        }
        sleep(500 + 100 * threadId);
        log.info("Loaded, next countDown()");
        readySignal.countDown();
    }

    private static void awaitLatch(CountDownLatch readyCount) {
        try {
            readyCount.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        int threadNum = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadNum);

        for (int i = 0; i < threadNum; i++) {
            final int threadId = i + 1;
            pool.execute(new CountDownLatchExample(threadId));
        }

        log.info("Some work");
        sleep(1000);
        log.info("Start loading");
        startSignal.countDown();

        while (readySignal.getCount() > 0) {
            awaitLatch(readySignal);
        }

        log.info("Load completed");
        pool.shutdown();
    }
}
