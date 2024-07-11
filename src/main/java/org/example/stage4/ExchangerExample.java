package org.example.stage4;

import lombok.extern.log4j.Log4j2;

import java.util.Scanner;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.System.exit;

@Log4j2
public class ExchangerExample {

    public static void main(String[] args) {
        AppenderUnit appA = new AppenderUnit("A");
        AppenderUnit appB = new AppenderUnit("B");
        AppenderUnit appC = new AppenderUnit("C");

        ConsumerUnit consumerUnit = new ConsumerUnit();

        appA.output = appB.input;
        appB.output = appC.input;
        appC.output = consumerUnit.input;

        ExecutorService pool = Executors.newFixedThreadPool(4);
        pool.execute(appA);
        pool.execute(appB);
        pool.execute(appC);
        pool.execute(consumerUnit);

        String cmd = "";
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                cmd = scanner.nextLine();
                if ("exit".equalsIgnoreCase(cmd)) {
                    break;
                }
                try {
                    appA.input.exchange(cmd);
                } catch (InterruptedException e) {
                    //
                }
            }
        }
        log.info("Exit");
        pool.shutdown();
        exit(0);
    }


    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {

        }
    }

    public static class AppenderUnit extends WorkerUnit {
        private final String append;

        public AppenderUnit(String append) {
            this.append = append;
        }

        @Override
        protected String process(String in) {
            log.info("Work on append {} to {}", append, in);
            return in + append;
        }
    }

    public static class ConsumerUnit extends WorkerUnit {

        @Override
        protected String process(String in) {
            log.info("Consumed: {}", in);
            return "";
        }
    }

    public static abstract class WorkerUnit implements Runnable {
        final Exchanger<String> input = new Exchanger<>();

        final int delay = ThreadLocalRandom.current().nextInt(1000) + 500;

        public Exchanger<String> output;


        @Override
        public void run() {
            String in = null;
            while (true) {
                try {
                    in = input.exchange(null);
                } catch (InterruptedException e) {
                    //
                }
                String res = process(in);
                sleep(delay);
                if (output != null) {
                    String response = res;
                    while (response != null) {
                        log.info("Try exchange {}", res);
                        try {
                            response = output.exchange(res);
                        } catch (InterruptedException e) {
                            //
                        }
                        log.info("Exchanged {}", response);
                    }
                }
            }
        }

        protected abstract String process(String in);
    }
}
