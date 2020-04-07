/*
 * Author Gints Kristiāns Kuļikovskis
 */

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import java.sql.*;
import java.util.Map;

public class Listener extends Thread {
    private Connection conn;
    private PGConnection postgresConn;

    Listener(Connection conn) throws SQLException {
        this.conn = conn;
        this.postgresConn = (PGConnection) conn;
        Statement statement = conn.createStatement();

        //Listens to a "pass" notification which is set up by the
        // create_notifier_that_will_be_called_by_trigger
        statement.execute("LISTEN pass");
        statement.close();
    }

    /**
     * Waits for a user table update notification, parses the returned row json, calls
     * hashAndUpdate that will check if hashing is needed and update the row if so.
     */
    public void run() {
        while (true) {
            try {
                // Issues a dummy query to contact the backend
                // and receive any pending notifications.
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1");
                rs.close();
                stmt.close();
                PGNotification[] notifications = postgresConn.getNotifications();
                if (notifications != null) {
                    for (PGNotification notification : notifications) {

                        System.out.println("Got notification: " +
                                notification.getName() + " " + notification.getParameter());

                        Map<String, String> row = new ObjectMapper()
                                .readValue(notification.getParameter(),
                                        new TypeReference<Map<String, String>>() {
                                        });
                        hashAndUpdate(conn, row.get("password"), Long.parseLong(row.get("id")));
                    }
                }

                // Wait a while before checking again for new notifications
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * If password is not hashed, hash it and update the appropriate row.
     *
     * @param conn     the POSTGRES connection
     * @param password the user password
     * @param id       user id
     * @throws SQLException in case something goes wrong indeed
     */
    private void hashAndUpdate(Connection conn, String password, long id) throws SQLException {
        if (password.length() < 7 || !password.substring(0, 7).equals("$31$16$")) {

            String SQL = "UPDATE [schema].user SET password = ? WHERE id = ?";

            PreparedStatement updateStatement = conn.prepareStatement(SQL);

            updateStatement.setString(1, new PasswordAuthentication().hash(password.toCharArray()));
            updateStatement.setLong(2, id);
            updateStatement.executeUpdate();
        }
    }
}
