package custom.application;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.valve.DistributedLock;
import org.tinystruct.valve.Lock;
import org.tinystruct.valve.Watcher;

import java.util.logging.Logger;

public class lock extends AbstractApplication {
    private volatile static int tickets = 100;
    private static final Logger logger = Logger.getLogger(lock.class.getName());

    @Override
    public void init() {
        this.setAction("test", "test");
    }

    @Override
    public String version() {
        return null;
    }

    public void test(){
        for (int i = 0; i < 20; i++) {
            new Thread(new ticket(), "Window #" + i).start();
        }
    }

    class ticket implements Runnable {
        private final Lock lock;

        public ticket() {
            lock = new DistributedLock("TICKET-LOCK-".getBytes());
        }

        @Override
        public void run() {
            while (tickets > 0) {
                try {
                    if (lock != null) {
                        lock.lock();

                        // TODO
                        logger.info(Thread.currentThread().getName() + " is selling #" + (tickets--) + " with Lock#" + lock.id());
                    }
                } catch (ApplicationException e) {
                    e.printStackTrace();
                } finally {
                    if (lock != null) {
                        try {
                            lock.unlock();
                        } catch (ApplicationException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}