/*
 * Author Gints Kristiāns Kuļikovskis
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        String url = "[enter your JDBC URL here]";

        //Start notification listener
        Connection lConn = DriverManager.getConnection(url, "[enter DB username]", "[enter DB password]");
        Listener listener = new Listener(lConn);
        listener.start();
    }
}
