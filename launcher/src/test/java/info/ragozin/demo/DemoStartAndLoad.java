package info.ragozin.demo;

import org.junit.Test;

import info.ragozin.loadgen.LoadGenStarter;

public class DemoStartAndLoad {

    @Test
    public void startDemoAndLoad() throws InterruptedException {
        new DemoStarter().startDemo();

        System.out.println("");
        System.out.println("Starting load ...");
        LoadGenStarter.start();
        System.out.println("");
        System.out.println("System is under load now");
    }
}
