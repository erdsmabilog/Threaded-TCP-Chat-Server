import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPEchoServer {
    public static void main(String[] args) {
        final int PORT = 12345;

        ServerSocket serverSocket = null;
        Socket socket = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        BufferedReader br = null;
        BufferedWriter bw = null;
        String data = "";

        try {
            serverSocket = new ServerSocket(Integer.valueOf(args[0]));
        } catch (Exception e1) {
            try {
                serverSocket = new ServerSocket(PORT);
            } catch (Exception e2) {
                System.err.println("Error: " + e2.getMessage());
            }
        }


        System.out.println("Server is now running...\n");

        while (!data.equals("/quit")) {
            try {
                socket = serverSocket.accept();
                bis = new BufferedInputStream(socket.getInputStream());
                bos = new BufferedOutputStream(socket.getOutputStream());
                br = new BufferedReader(new InputStreamReader(bis));
                bw = new BufferedWriter(new OutputStreamWriter(bos));

                while (!data.equals("/quit")) {
                    data = br.readLine();
                    System.out.println("Received: " + data);
                    System.out.println("Proceeding to echo...\n");
                    bw.write(data);
                    bw.newLine();
                    bw.flush();
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        try {
            socket.close();
            serverSocket.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}

