package info.ragozin.demo;

import org.junit.Test;

import info.ragozin.demostarter.DemoInitializer;
import info.ragozin.hsql.HsqlStarter;
import info.ragozin.site.SiteStarter;
import info.ragozin.solr.SolrStarter;

public class DemoStarter {

    @Test
    public void startDemo() throws InterruptedException {

        DemoInitializer.initConfiguration();

        if (!HsqlStarter.check()) {
            System.out.println("Starting HSQL");
            HsqlStarter.start();
        }
        else {
            System.out.println("Already started HSQL");
        }

        if (!DemoInitializer.check("solr")) {
            System.out.println("Starting Solr");
            SolrStarter.provisionAndStartSolr();
        }
        else {
            System.out.println("Already started Solr");
        }

        if (!SiteStarter.check()) {
            System.out.println("Starting Spring Boot app");
            System.out.println("Please wait ...");
            SiteStarter.start();
        }
        else {
            System.out.println("Already started Spring Boot app");
        }
        System.out.println("");
        System.out.println("Tomcat instance started at http://localhost:8080");
        System.out.println("");
        System.out.println("Remove \"pids\" directory to stop demo enviroment");
        System.out.println("");
        System.out.println("");
    }
}
