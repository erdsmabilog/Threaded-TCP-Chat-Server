import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class TCPEchoClient {
    public static void main(String[] args) {
        String host = null;
        Integer port = null;
        try {
            host = args[0];
            port = Integer.valueOf(args[1]);
        } catch (Exception e) {
            System.out.println("Usage: java TCPEchoClient <host> <port>");
            return;
        }

        try {
            Socket socket = new Socket(host, port);
            Scanner scanner = new Scanner(System.in);
            String message = "";
            String echo = "";

            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(bis));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(bos));

            while (!message.equals("/quit")) {
                System.out.println("Reading");
                System.out.print("> ");
                message = scanner.nextLine();
                bw.write(message);
                bw.newLine();
                bw.flush();

                echo = br.readLine();
                System.out.println("Received: " + echo + "\n");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
