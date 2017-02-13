package org.wltea.analyzer.lucene;

import java.io.IOException;
import java.util.Vector;

/**
 * Created by liangyongxing on 2017/2/6.
 * 1分钟自动判断更新
 */
public class UpdateKeeper implements Runnable {
    static final long INTERVAL = 60000L;
    private static UpdateKeeper singleton;
    Vector<UpdateJob> filterFactorys;
    Thread worker;

    private UpdateKeeper() {
        this.filterFactorys = new Vector();

        this.worker = new Thread(this);
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public static UpdateKeeper getInstance() {
        if (singleton == null) {
            synchronized (UpdateKeeper.class) {
                if (singleton == null) {
                    singleton = new UpdateKeeper();
                    return singleton;
                }
            }
        }
        return singleton;
    }

    public void register(UpdateJob filterFactory) {
        this.filterFactorys.add(filterFactory);
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!this.filterFactorys.isEmpty()) {
                for (UpdateJob factory : this.filterFactorys) {
                    try {
                        factory.update();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public interface UpdateJob {
        void update() throws IOException;
    }
}
