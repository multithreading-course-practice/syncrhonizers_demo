package org.example.stage1;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.Semaphore;

// https://gist.github.com/pedrominicz/1d976280c13e2900fa16aa5c7f2dfebf
// This program illustrates the use of semaphores to synchronize access to a
// buffer shared between multiple threads. Semaphores are a weird
// synchronization define and I believe they can be best understood by example.
//
// A semaphore is an atomic counter offering two methods: `acquire` and
// `release`. `acquire` decreases the counter if it is greater than zero,
// otherwise it blocks the current thread until the counter increases and it
// can do so. `release` increases the counter.
//
// It is simple to see that a semaphore can be used to implement a mutex lock.
// Initialize it to 1, each thread should `acquire` and subsequently `release`
// the mutex exactly once every time. Every time the semaphore is acquired its
// counter immediately becomes zero, ensuring only one thread at the time can
// `acquire` it.
//
// This mutex-like behaviour is used in this example.
@Log4j2
class Buffer {

    private static final int SIZE = 5;

    private final Object[] buffer = new Object[SIZE];

    private int in = 0;

    private int out = 0;

    private final Semaphore mutex = new Semaphore(1);

    // This semaphore counts how many empty slots exist in the buffer at any
    // given time.
    private final Semaphore empty = new Semaphore(SIZE);

    // This semaphore counts how many non-empty slots exist in the buffer at
    // any given time.
    private final Semaphore full = new Semaphore(0);

    // The `insert` and `remove` methods execute in a mutually exclisive manner
    // due to the `mutex` semaphore.
    public void insert(final Object object) throws InterruptedException {
        // Decrease the empty slot counter. Note that the order of the
        // `acquire`s is significant to avoid a deadlock.
        empty.acquire();
        mutex.acquire();

        buffer[in] = object;

        log.info("write {}", object);

        in = (in + 1) % SIZE;

        mutex.release();
        // Increase the full slot counter.
        full.release();
    }

    public Object remove() throws InterruptedException {
        // Decrease the full slot counter.
        full.acquire();
        mutex.acquire();

        final Object object = buffer[out];
        log.info("read {}", object);
        out = (out + 1) % SIZE;

        mutex.release();
        // Increase the empty slot counter.
        empty.release();

        return object;
    }

}


public class CyclicBuffer {

    public static void main(String[] args) {
        final Buffer buffer = new Buffer();

        // Producer thread. The only noticeable curiosity about it is that is
        // makes use of anonymous classes.
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; true; ++i) {
                    try {
                        // `insert` may throw `InterruptedException` which must
                        // be caught.
                        //
                        // Note that the produces is way faster than the
                        // consumer because the latter calls `sleep`. However,
                        // the `insert` method blocks if the buffer is full.
                        buffer.insert(i);
                    } catch (InterruptedException ie) {
                        // Empty.
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // Both `remove` and `sleep` may throw
                        // `InterruptedException` which must be caught.
                        System.out.println((Integer) buffer.remove());

                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        // Empty.
                    }
                }
            }
        }).start();
    }

}
