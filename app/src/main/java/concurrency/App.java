package concurrency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import static java.lang.System.currentTimeMillis;

public class App {
    public static void main(String[] args) throws InterruptedException {

        getPart1();
        getPart2();
        getPerformancePart3();
        getMultiThreadPart4();
        getMultiThreadPart4Atomic();

    }

    public static void getPart1() throws InterruptedException {
        Runnable runnableTask = () -> {
            String page = getWebpage("https://www.google.com");
            System.out.println(page);
            System.out.println(Thread.currentThread().getName());
        };
        System.out.println("Thread has been started");
        Thread myThread = new Thread(runnableTask);
        myThread.start();
        myThread.join();
        System.out.println("Thread has been completed");

    }

    public static void getPart2(){
        ExecutorService pool = Executors.newFixedThreadPool(5);
        Runnable runnableTask1 = () -> {
            String page = getWebpage("https://www.google.com");
            System.out.println(page);
            System.out.println(Thread.currentThread().getName());
        };

        Runnable runnableTask2 = () -> {
            String page = getWebpage("https://corndeladmin.github.io/SoftwareDeveloperCourseMaterialPages/java/ongoing-training/async/reading.html");
            System.out.println(page);
            System.out.println(Thread.currentThread().getName());
        };

        pool.execute(runnableTask1);
        pool.execute(runnableTask2);
        pool.execute(runnableTask1);
        pool.execute(runnableTask2);
        pool.execute(runnableTask1);
        pool.shutdown();

    }

    public static void getMultiThreadPart4() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 10_000; i++) {
            //CounterWithIssue counterA = new CounterWithIssue();
            SynchCounter counterA = new SynchCounter();

            CompletableFuture<Void> increment1 = CompletableFuture.runAsync(counterA::increment, pool);
            CompletableFuture<Void> increment2 = CompletableFuture.runAsync(counterA::increment, pool);

            CompletableFuture<Void> all = CompletableFuture.<Integer>allOf(increment1, increment2);
            all.thenApply((v) -> {
                if (counterA.get() != 2) {
                    System.out.println("Incorrect counter value: " + Integer.toString(counterA.get()));
                }

                return null;
            });
        }

        waitForThreadPoolShutdown(pool);
    }

    public static void getMultiThreadPart4Atomic() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 10_000; i++) {
            // CounterWithIssue counterA = new CounterWithIssue();
            SafeCounterWithoutLock counterA = new SafeCounterWithoutLock();

            CompletableFuture<Void> increment1 = CompletableFuture.runAsync(counterA::increment, pool);
            CompletableFuture<Void> increment2 = CompletableFuture.runAsync(counterA::increment, pool);

            CompletableFuture<Void> all = CompletableFuture.<Integer>allOf(increment1, increment2);
            all.thenApply((v) -> {
                if (counterA.get() != 2) {
                    System.out.println("Incorrect counter value: " + Integer.toString(counterA.get()));
                }

                return null;
            });
        }

        waitForThreadPoolShutdown(pool);
    }



    private static void waitForThreadPoolShutdown(ExecutorService pool) throws InterruptedException {
        pool.shutdownNow();
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            System.err.println("Pool did not complete within 10 seconds");
            pool.shutdownNow();
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("Pool did not terminate");
            }
        }
    }

    public static class CounterWithIssue {
       private  int val = 0;

        public  void increment() {
            val += 1;
        }

        public int get() {
            return val;
        }
    }

    public static class SynchCounter {
        private volatile int val = 0;

        public synchronized void increment() {
            val += 1;
        }

        public synchronized int get() {
            return val;
        }
    }


    public static class SafeCounterWithoutLock {
        private final AtomicInteger counter = new AtomicInteger(0);

        int get() {
            return counter.get();
        }

        void increment() {
            counter.incrementAndGet();
        }
    }


    private static String getWebpage(String url1) {
        URL url = null;
        try {
            url = new URL(url1);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            con.setRequestMethod("GET");
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        }

        try {
            int status = con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);

            }
            in.close();
            con.disconnect();
            return content.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void getPerformancePart3() {
        long n = 100000L;
        long startTime = currentTimeMillis();
        countProbablePrimesNoParallel(n);
        long endTime = currentTimeMillis();
        long time = endTime - startTime;
        System.out.println("No Parallel:" + time);

        long startTime2 = currentTimeMillis();
        countProbablePrimesParallel(n);
        long endTime2 = currentTimeMillis();
        long time2 = endTime2 - startTime2;
        System.out.println("Parallel:" + time2);
    }


        static long countProbablePrimesParallel(long n) {
        long count = LongStream.rangeClosed(2, n)
                .mapToObj(BigInteger::valueOf)
                .parallel() // request parallel processing
                .filter((i) -> i.isProbablePrime(50))
                .count();

        return count;
    }

    static long countProbablePrimesNoParallel(long n) {
        long count = LongStream.rangeClosed(2, n)
                .mapToObj(BigInteger::valueOf)
                .filter((i) -> i.isProbablePrime(50))
                .count();

        return count;
    }












}
