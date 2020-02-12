/**
 * Reusing code from:
 *  - Node.java from our Miniproject3
 *  - https://www.geeksforgeeks.org/introducing-threads-socket-programming-java/?fbclid=IwAR2yaPygh_ZwzEtnZKRXWHOEMhfGCoV8E8eOlmYr5RVnIT9JGGcvGwy7YiM
 */

import java.io.*;
import java.net.*;

public class Bidder {
    private static int serverPort;
    private static int backupPort;
    private static Socket server;
    private static DataInputStream dis;
    private static DataOutputStream dos;

    public static void main(String[] args) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            serverPort = Integer.parseInt(args[0]);
            initConnection(serverPort);

            backupPort = Integer.parseInt(dis.readUTF());
            while (true) {
                String msg;
                System.out.println(dis.readUTF());
                msg = br.readLine();
                try {
                    dos.writeUTF(msg);
                } catch (SocketException se) {
                    // re-initialize the connection using the backup server
                    System.out.println("\n### SERVER ERROR! REQUEST NOT REGISTERED! ###\n");
                    initConnection(backupPort);
                    continue;
                }

                // if we want to exit the auction
                if(msg.equals("exit")) {
                    server.close();
                    break;
                }

                String res = dis.readUTF();
                System.out.println(res);

                // exit once winner is announced
                if(res.toLowerCase().contains("winner")) server.close();
            }
        } catch(Exception e) {
            System.exit(0);
        }
    }

    private static void initConnection(int port) throws IOException {
        server = new Socket(InetAddress.getByName("localhost"), port);
        dis = new DataInputStream(server.getInputStream());
        dos = new DataOutputStream(server.getOutputStream());
    }
}
