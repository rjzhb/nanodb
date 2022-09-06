package edu.caltech.test.nanodb.framework;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;


/** Helper operations to implement concurrent test cases. */
public class Concurrent {

    public static void assertConcurrent(String message,
            List<? extends Runnable> runnables, int maxTimeoutSeconds)
            throws InterruptedException {

        int numThreads = runnables.size();
        Queue<Throwable> exceptions = new ConcurrentLinkedQueue<>();
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        try {
            CountDownLatch allThreadsReady = new CountDownLatch(numThreads);
            CountDownLatch afterInitBlocker = new CountDownLatch(1);
            CountDownLatch finished = new CountDownLatch(numThreads);
            for (Runnable r : runnables) {
                threadPool.submit( () -> {
                    // Wait for all threads to be ready to run.
                    allThreadsReady.countDown();
                    try {
                        afterInitBlocker.await();
                        r.run();
                    } catch (final Throwable e) {
                        exceptions.add(e);
                    } finally {
                        finished.countDown();
                    }
                });
            }

            // Wait until all threads are ready to run.
            Assert.assertTrue(allThreadsReady.await(runnables.size() * 10,
                TimeUnit.MILLISECONDS),
                message + ":  Timeout while initializing threads.  Perform " +
                "slow initialization before passing Runnables to " +
                "assertConcurrent()");

            // Start off all the test runners at the same time.
            afterInitBlocker.countDown();

            // Wait for the "finished" count to hit 0, which indicates that
            // all threads have completed their test.
            Assert.assertTrue(finished.await(maxTimeoutSeconds, TimeUnit.SECONDS),
                message + ":  Timeout!  More than" + maxTimeoutSeconds + "seconds");
        }
        finally {
            threadPool.shutdownNow();
        }

        if (!exceptions.isEmpty()) {
            PrintWriter out = new PrintWriter(new StringWriter());
            for (Throwable t : exceptions)
                t.printStackTrace(out);

            Assert.fail(message + ":  Failed with " + exceptions.size() +
                " exception(s):\n" + out.toString());
        }
    }
}
