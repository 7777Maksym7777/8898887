import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Лабораторна робота №2.
 * Керування життєвим циклом потоків у мові Java.
 */
public class Lab2Threads {
    private static final AtomicReference<Thread> fibonacciThread =
            new AtomicReference<>();
    private static final CountDownLatch fibonacciStarted =
            new CountDownLatch(1);

    // Потік, який обчислює та виводить перші 10 чисел Фібоначчі.
    static class FibonacciTask implements Runnable {
        @Override
        public void run() {
            Thread current = Thread.currentThread();

            // Внутрішній потік створюється та запускається з потоку Fibonacci.
            Thread innerThread = new Thread(() -> {
                try {
                    System.out.println("[Внутрішній потік] Запущено. Засинаю на 1 секунду.");
                    Thread.sleep(1000);
                    System.out.println("[Внутрішній потік] Завершив роботу.");
                } catch (InterruptedException e) {
                    System.out.println("[Внутрішній потік] Перервано.");
                    Thread.currentThread().interrupt();
                }
            }, "Inner-Thread");
            innerThread.start();

            long first = 0;
            long second = 1;

            for (int i = 1; i <= 10; i++) {
                try {
                    // Затримка на 1 секунду перед друком кожного числа.
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("[Fibonacci] Потік перервано.");
                    current.interrupt();
                    break;
                }

                long value;
                if (i == 1) {
                    value = first;
                } else if (i == 2) {
                    value = second;
                } else {
                    value = first + second;
                    first = second;
                    second = value;
                }

                System.out.println("[Fibonacci] Число " + i + ": " + value);
                // Добровільне звільнення кванта процесорного часу.
                Thread.yield();
            }

            try {
                // Fibonacci не завершиться раніше за внутрішній потік.
                innerThread.join();
                System.out.println("[Fibonacci] Внутрішній потік завершився. "
                        + "Потік Fibonacci завершує роботу.");
            } catch (InterruptedException e) {
                System.out.println("[Fibonacci] Очікування внутрішнього потоку перервано.");
                current.interrupt();
            }
        }
    }

    // Службовий потік-демон відстежує стан потоку Fibonacci.
    static class StateMonitor implements Runnable {
        @Override
        public void run() {
            Thread worker = new Thread(new FibonacciTask(), "Fibonacci-Thread");
            worker.setPriority(Thread.NORM_PRIORITY + 2); // пріоритет 7 замість 5
            fibonacciThread.set(worker);
            worker.start();
            fibonacciStarted.countDown();
            System.out.println("[Демон] Запущено потік Fibonacci з пріоритетом "
                    + worker.getPriority() + ".");

            // Демон продовжує працювати та відстежувати стан Fibonacci.
            while (true) {
                System.out.println("[Демон] Стан потоку Fibonacci: "
                        + worker.getState());
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("[main] Головний потік розпочав роботу.");

        Thread monitorThread = new Thread(new StateMonitor(), "Daemon-Monitor");
        monitorThread.setDaemon(true);
        // Демон запускається в main, а всередині нього створюється Fibonacci.
        monitorThread.start();

        try {
            // Очікуємо, поки демон створить і запустить Fibonacci.
            fibonacciStarted.await();
            Thread worker = fibonacciThread.get();

            // Головний потік завершується лише після завершення Fibonacci.
            worker.join();

            System.out.println("[main] Потік Fibonacci завершений: "
                    + !worker.isAlive());
            System.out.println("[main] Потік-демон усе ще активний: "
                    + monitorThread.isAlive());
            System.out.println("[main] Головний потік завершує роботу.");
        } catch (InterruptedException e) {
            System.out.println("[main] Головний потік перервано.");
            Thread.currentThread().interrupt();
        }
    }
}
