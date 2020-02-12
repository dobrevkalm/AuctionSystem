/**
 * Reusing code from:
 *  - Main.java that was given to us with Miniproject2
 *  - Node.java from our Miniproject3
 *  - https://www.geeksforgeeks.org/introducing-threads-socket-programming-java/?fbclid=IwAR2yaPygh_ZwzEtnZKRXWHOEMhfGCoV8E8eOlmYr5RVnIT9JGGcvGwy7YiM
 */

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class BackupServer {
    private static final int PORT = 7008;
    private static ConcurrentMap<Integer, Integer> bids = new ConcurrentHashMap<>();
    private volatile static int winner;
    private volatile static int result;
    private volatile static long time;
    private volatile static Object lock = new Object();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("\n*****Server is listening*****\n");
        while (true) {
            Socket client;
            try {
                client = serverSocket.accept();
                System.out.println("\nClient connected -> " + client.getPort());
                DataInputStream dis = new DataInputStream(client.getInputStream());
                DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                new ClientHandler(client, dis, dos).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class ClientHandler extends Thread {
        private final DataInputStream dis;
        private final DataOutputStream dos;
        private final Socket client;


        public ClientHandler(Socket client, DataInputStream dis, DataOutputStream dos) throws IOException {
            this.client = client;
            this.dis = dis;
            this.dos = dos;
            // we save the time when we first init the main server
            time = Long.parseLong(dis.readUTF());
        }

        @Override
        public void run() {
            String msg;
            while (System.currentTimeMillis() < time) {
                try {
                    dos.writeUTF("\nPlease enter request...");
                    // receive the answer from client
                    msg = dis.readUTF().toLowerCase();
                    if (msg.equals("exit")) {
                        System.out.println("\nClient connected -> " + this.client.getPort());
                        this.client.close();
                        break;
                    } else {
                        if (msg.startsWith("bid")) {
                            handleBid(msg);
                        } else if (msg.startsWith("result")) {
                            handleRes(this.client.getPort());
                        } else if (msg.startsWith("update")) {
                            handleUpdate(msg);
                        } else if (msg.contains("end")) {
                            System.out.println("*** AUCTION HAS ENDED ***");
                            System.exit(0);
                        } else {
                            dos.writeUTF("\n### Wrong call! Available commands: 'bid', 'result");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("\nClient disconnected -> " + this.client.getPort());
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

        // updates are received from the main server
        private void handleUpdate(String msg) {
            String[] req = msg.split(" ");
            int bidder = Integer.parseInt(req[1]);
            int bid = Integer.parseInt(req[2]);
            bids.put(bidder, bid);
            result = bid;
            System.out.println("\n*** Backup updated. Result: " + result + " ***\n");
        }

        private void handleBid(String msg) throws IOException {
            synchronized(lock) {
                String[] req = msg.split(" ");
                int bid = Integer.parseInt(req[1]);
                if (bid > result) {
                    int bidder = this.client.getPort();
                    bids.put(bidder, bid);
                    winner = bidder;
                    result = bid;
                    System.out.println("\n*** New highest bid! Bidder: " + bidder + " bid: " + bid);
                    dos.writeUTF("*** Bid registered successfully ***");
                } else {
                    dos.writeUTF("\n### Bid must be higher than the current highest ###");
                }
            }
        }

        private void handleRes(int port) throws IOException {
            if(port == winner) dos.writeUTF("\n$$$ You are the current highest bidder");
            else dos.writeUTF("\n$$$ Current highest bid is: " + result);
        }

        /**
         * This method is not fully implemented since there are no premium accounts.
         * Should be called within the run() method
         */
        private void handleTop(int port) {
            synchronized (lock) {
                // check if user is premium
                // result++;
                // update the data
                // send confirmation to the bidder
            }
        }
    }
}
