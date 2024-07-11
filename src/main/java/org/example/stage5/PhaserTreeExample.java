package org.example.stage5;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

@Log4j2
public class PhaserTreeExample {
    static Phaser phaserRoot = new Phaser(1) {
        @Override
        protected boolean onAdvance(int phase, int registeredParties) {
            log.info("Advance root to {}", phase);
            return super.onAdvance(phase, registeredParties);
        }
    };

    public static void main(String[] args) {
        Phaser[] leafs = new Phaser[3];
        for (int i = 0; i < leafs.length; i++) {
            int leafId = i;
            leafs[i] = new Phaser(phaserRoot) {
                @Override
                protected boolean onAdvance(int phase, int registeredParties) {
                    log.info("Advance leaf {} to {}", leafId, getPhase());
                    return super.onAdvance(phase, registeredParties);
                }
            };
        }

        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            int leafIndex = i;
            leafs[leafIndex].register();
            pool.execute(() -> {
                int n = 0;
                while (n < 10) {
                    sleep(500);
                    log.info("Iteration at {}: {}, {}", leafIndex, ++n, leafs[leafIndex].getPhase());
                    leafs[leafIndex].arriveAndAwaitAdvance();
                }
            });
        }

        phaserRoot.arriveAndDeregister();
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
