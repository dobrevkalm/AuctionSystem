/**
 * Reusing code from:
 *  - Main.java that was given to us with Miniproject2
 *  - Node.java from our Miniproject3
 *  - https://www.geeksforgeeks.org/introducing-threads-socket-programming-java/?fbclid=IwAR2yaPygh_ZwzEtnZKRXWHOEMhfGCoV8E8eOlmYr5RVnIT9JGGcvGwy7YiM
 */

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 7007;
    // a server should be working at this port
    private static final int BACKUP_PORT = 7008;
    // use Java's concurrent package to ensure thread safety
    private static ConcurrentMap<Integer, Integer> bids = new ConcurrentHashMap<>();
    // the current winner of the auction
    private volatile static int winner;
    // the highest bid
    private volatile static int result;
    // the output stream for the backup server
    private static DataOutputStream backupDOS;
    // used for synchronization locking
    private volatile static Object lock = new Object();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        // auction will run for 2 minute (120K ms)
        long time = System.currentTimeMillis() + 30000;
        // connect with the backup server
        initBackupServer(time);
        System.out.println("\n*****AUCTION STARTED*****\n\n");

        // listen for bidders
        while (System.currentTimeMillis() < time) {
            Socket client;
            try {
                client = serverSocket.accept();
                System.out.println("\nClient connected -> " + client.getPort());
                DataInputStream dis = new DataInputStream(client.getInputStream());
                DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                new ClientHandler(time, client, dis, dos).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("*** AUCTION HAS ENDED ***");
        backupDOS.writeUTF("END");
        System.exit(0);
    }

    private static void initBackupServer(long time) throws IOException {
        try {
            Socket backupServer = new Socket(InetAddress.getByName("localhost"), BACKUP_PORT);
            backupDOS = new DataOutputStream(backupServer.getOutputStream());
            // signal the time the auction should run for
            backupDOS.writeUTF(Long.toString(time));
        } catch (ConnectException ce) {
            System.out.println("### No backup server running. Make sure backup server is up! ###");
            System.exit(0);
        }
    }

    private static class ClientHandler extends Thread {
        private final DataInputStream dis;
        private final DataOutputStream dos;
        private final Socket client;
        private final long time;

        public ClientHandler(long time, Socket client, DataInputStream dis, DataOutputStream dos) throws IOException {
            this.time = time;
            this.client = client;
            this.dis = dis;
            this.dos = dos;
            // each client receives the backup server port when connected
            dos.writeUTF(Integer.toString(BACKUP_PORT));
        }

        @Override
        public void run() {
            String msg;
            while (System.currentTimeMillis() < time) {
                try {
                    dos.writeUTF("\nEnter request...");
                    msg = dis.readUTF().toLowerCase();

                    if (msg.equals("exit")) {
                        System.out.println("\nClient disconnected -> " + this.client.getPort());
                        this.client.close();
                        break;
                    } else {
                        if(msg.startsWith("bid")) {
                            handleBid(msg);
                        } else if (msg.startsWith("result")) {
                            handleRes(this.client.getPort());
                        } else {
                            dos.writeUTF("\n### Wrong call! Available commands: 'bid', 'result', 'exit'");
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
            try {
                dos.writeUTF("\n*** AUCTION ENDED! ***\n*** WINNER IS " + winner + " ***");
                this.client.close();
            } catch (Exception e) {
                System.out.println("\nClient disconnected -> " + this.client.getPort());
            }
        }

        private void handleBid(String msg) throws IOException {
            synchronized(lock) {
                String[] req = msg.split(" ");
                int bid = Integer.parseInt(req[1]);
                if (bid > result) {
                    int bidder = this.client.getPort();
                    // all the data is sent to the backup server as well
                    updateBackupServer(bidder, bid);
                    bids.put(bidder, bid);
                    winner = bidder;
                    result = bid;
                    System.out.println("\n*** New highest bid! Bidder: " + bidder + ", bid: " + bid);
                    dos.writeUTF("\n*** Bid registered successfully ***");
                } else {
                    dos.writeUTF("\n### Bid must be higher than the current highest ###");
                }
            }
        }

        private void updateBackupServer(int bidder, int bid) {
            try {
                backupDOS.writeUTF("UPDATE " + bidder + " " + bid);
            } catch (Exception e) {
                System.out.println("### Backup Server has failed! ###");
            }
        }

        private void handleRes(int port) throws IOException {
            if(port == winner) dos.writeUTF("\n$$$ You are the current highest bidder");
            else dos.writeUTF("\n$$$ Current highest bid is: " + result);
            dos.writeUTF("!!! Time remaining: " + (time-System.currentTimeMillis())/1000 + " !!!");
        }

        /**
         * This method is not fully implemented since there is no premium accounts.
         * Should be called within the run() method
         */
        private void handleTop(int port) {
            synchronized (lock) {
                // check if user is premium
                // result++;
                // update the data in both servers
                // send confirmation to the bidder
            }
        }
    }
}
