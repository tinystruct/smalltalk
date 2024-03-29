package custom.application;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.valve.DistributedLock;
import org.tinystruct.valve.Lock;

import java.util.logging.Logger;

public class lock extends AbstractApplication {
    private static final Logger logger = Logger.getLogger(lock.class.getName());
    private volatile static int tickets = 100;

    @Override
    public void init() {
        this.setAction("test", "test");

        setTemplateRequired(false);
    }

    @Override
    public String version() {
        return null;
    }

    public void test() {
        for (int i = 0; i < 20; i++) {
            new Thread(new ticket(), "Window #" + i).start();
        }
    }

    static class ticket implements Runnable {
        private final Lock lock;

        public ticket() {
            lock = new DistributedLock();
        }

        @Override
        public void run() {
            while (tickets > 0) {
                try {
                    lock.lock();
                    if (tickets > 0)
                        // TODO
                        logger.info(Thread.currentThread().getName() + " is selling #" + (tickets--) + " with Lock#" + lock.id());
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}