package info.ragozin.demostarter;

import org.junit.Test;

public class DemoInitializerCheck {

    @Test
    public void check() {
        System.out.println(DemoInitializer.getDemoHome());
    }

    @Test
    public void checkConfig() {
        DemoInitializer.initConfiguration();
    }

}
