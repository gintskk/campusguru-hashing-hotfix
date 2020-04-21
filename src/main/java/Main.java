/*
 * Author Gints Kristiāns Kuļikovskis
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;

public class Main {
    //First argument: server address (with port)
    public static void main(String[] args) throws InterruptedException {
        if (args == null || args.length < 1) {
            System.console().printf("Missing command line argument: Server URL (host:port OR domainName)\n");
            return;
        }
        if (args.length < 2) {
            String[] tmpArgs = new String[2];
            tmpArgs[0] = args[0];
            tmpArgs[1] = "0";
            args = tmpArgs;
        }
        try {
            Class.forName("org.postgresql.Driver");
            String url = String.format("jdbc:postgresql://%s/tudelft_oopp", args[0]);
            System.console().printf("Connecting to server %s\n", url);
            //Start notification listener
            Connection lConn = DriverManager.getConnection(url, "[enter DB username]", "[enter DB password]");
            System.console().printf("***Starting Postgres notification listener at %s\n", LocalDateTime.now().toString());
            Listener listener = new Listener(lConn);
            listener.start();
        } catch (Exception e) {
            //Prints exception stack once every 30 seconds to not overflow logs
            int exceptionThrownTimes = Integer.parseInt(args[1]);
            if (exceptionThrownTimes % 6 == 0) {
                System.console().printf("Exception %s thrown at %s\n", e.getClass(), LocalDateTime.now().toString());
                e.printStackTrace();
                //Set times to 1 to prevent integer overflow (in tens of years of potential running time but to be sure)
                args[1] = Integer.toString(1);
            } else {
                //Exception thrown times++
                args[1] = Integer.toString(exceptionThrownTimes + 1);
            }
            Thread.sleep(5000);
            Main.main(args);
        }
    }
}
