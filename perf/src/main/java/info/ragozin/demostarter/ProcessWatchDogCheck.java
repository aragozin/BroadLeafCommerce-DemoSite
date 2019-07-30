package info.ragozin.demostarter;

import java.io.File;

import org.junit.Test;

public class ProcessWatchDogCheck {

    @SuppressWarnings("unused")
    @Test
    public void start() throws InterruptedException {
        ProcessWatchDog watchdog = new ProcessWatchDog(new File("target/check.lg"));
        while (true) {
            Thread.sleep(1000);
        }
    }

    @Test
    public void check() throws InterruptedException {
        System.out.println(ProcessWatchDog.check(new File("target/check.lg")));
    }

    @Test
    public void check_hsql() throws InterruptedException {
        System.out.println(ProcessWatchDog.check(new File("../pids/hsqldb.lg")));
    }

    @Test
    public void check_storefront() throws InterruptedException {
        System.out.println(ProcessWatchDog.check(new File("../pids/storefront.lg")));
    }

    @Test
    public void kill() throws InterruptedException {
        ProcessWatchDog.kill(new File("target/check.lg"));
    }

}
