import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.h2.server.web.WebServer;
import org.h2.tools.Server;
import org.junit.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;

public class RunH2Console {
	
	@Test
	public void run() throws SQLException, InterruptedException {
		Server.main("-web", "-webPort", "11011", "-browser");
		while(true) {
			Thread.sleep(100);
		}
	}

	@Test
	public void openConsoleToHSQL() throws Exception {
        DataSource dataSource = DataSourceBuilder
                .create()
                .username("SA")
                .password("")
                .url("jdbc:hsqldb:hsql://127.0.0.1:9001/broadleaf")
                .driverClassName("org.hsqldb.jdbcDriver")
                .type(org.apache.tomcat.jdbc.pool.DataSource.class)
                .build();
        
        Connection conn = dataSource.getConnection();
        WebServer server = new WebServer();
        server.init("-webPort", "11011");
        server.start();      
        new Thread() {
        	@Override
        	public void run() {
        		server.listen();
        	}
        }.start();
        
        String url = server.addSession(conn);
        Server.openBrowser(url);
        System.out.println("Open console at " + url);
        while(true) {
        	Thread.sleep(1000);
        	if (conn.isClosed()) {
        		return;
        	}
        }
	}	
}
