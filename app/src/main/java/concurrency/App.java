package concurrency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class App {
    public static void main(String[] args) throws InterruptedException {

        ExecutorService pool = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 10_000; i++) {
            // CounterWithIssue counterA = new CounterWithIssue();
            // Counter counterA = new Counter();
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

        waitForThreadpoolShutdown(pool);
    }

    private static void waitForThreadpoolShutdown(ExecutorService pool) throws InterruptedException {
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

    public static class Counter {
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

}
