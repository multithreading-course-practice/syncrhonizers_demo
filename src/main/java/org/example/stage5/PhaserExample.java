package org.example.stage5;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

@Log4j2
public class PhaserExample {
    static Phaser phaser = new Phaser(1) {
        @Override
        protected boolean onAdvance(int phase, int registeredParties) {
            log.info("Advance to {}", phase);
            return super.onAdvance(phase, registeredParties);
        }
    };

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            phaser.register();
            pool.execute(() -> {
                int n = 0;
                while (n < 10) {
                    sleep(500);
                    log.info("Iteration: {}, phase: {}", ++n, phaser.getPhase());
                    phaser.arriveAndAwaitAdvance();
                }
            });
        }

        phaser.arriveAndDeregister();
        pool.shutdown();
    }

    static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            //
        }
    }

}
