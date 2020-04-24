/*
 * Author Gints Kristiāns Kuļikovskis
 */

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Map;

public class Listener extends Thread {
    private final Connection conn;
    private final PGConnection postgresConn;
    private final MyPasswordAuthentication myPasswordAuthentication;

    Listener(Connection conn) throws SQLException {
        this.conn = conn;
        this.postgresConn = (PGConnection) conn;
        Statement statement = conn.createStatement();

        //Listens to a "pass" notification which is set up by the
        // create_notifier_that_will_be_called_by_trigger
        statement.execute("LISTEN pass");
        statement.close();
        myPasswordAuthentication = new MyPasswordAuthentication();
    }

    /**
     * Waits for a user table update notification, parses the returned row json, calls
     * hashAndUpdate that will check if hashing is needed and update the row if so.
     */
    @SneakyThrows
    public void run() {
        while (true) {
            // Issues a dummy query to contact the backend
            // and receive any pending notifications.
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");
            rs.close();
            stmt.close();
            PGNotification[] notifications = postgresConn.getNotifications();
            if (notifications != null) {
                for (PGNotification notification : notifications) {
                    //For debugging, in production Notification Payload removed!
                    System.console().printf("Got notification %s with payload: %s at %s\n", notification.getName(), notification.getParameter(), LocalDateTime.now().toString());
                    Map<String, String> row = new ObjectMapper()
                            .readValue(notification.getParameter(),
                                    new TypeReference<Map<String, String>>() {
                                    });
                    hashAndUpdate(conn, row.get("password"), Long.parseLong(row.get("id")));
                }
            }
            // Wait 5 seconds before checking again for new notifications
            Thread.sleep(60000);
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
        if (password.length() < 50 || !password.startsWith("$31$16$")) {

            String SQL = "UPDATE tudelft_oopp.public.user SET password = ? WHERE id = ?";

            PreparedStatement updateStatement = conn.prepareStatement(SQL);

            updateStatement.setString(1, myPasswordAuthentication.hash(password.toCharArray()));
            updateStatement.setLong(2, id);
            updateStatement.executeUpdate();
        }
    }
}
