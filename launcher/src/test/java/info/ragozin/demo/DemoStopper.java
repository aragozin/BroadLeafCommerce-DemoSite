package info.ragozin.demo;

import org.junit.Test;

import info.ragozin.demostarter.DemoInitializer;

public class DemoStopper {

    @Test
    public void stop() {
        System.out.println("Stopping HSQL ...");
        DemoInitializer.kill("hsqldb");
        System.out.println("Stopping Solr ...");
        DemoInitializer.kill("solr");
        System.out.println("Stopping Storefront ...");
        DemoInitializer.kill("storefront");
        System.out.println("Stopping LoadGen ...");
        DemoInitializer.kill("loadgen");
        System.out.println("Demo stopped");
    }
}
