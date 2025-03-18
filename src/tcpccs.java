import java.io.*;
import java.net.Socket;
import java.util.Scanner;

class tcpccs {
    public static void main(String[] args) throws InterruptedException {
        String server_hostname;
        String username;
        if (args.length == 2) {
            server_hostname = args[0];
            username = args[1];
        }
        else {
            System.out.println("Usage: java tcpccs <server_hostname> <username>");
            return;
        }

        try {
            final int PORT = 12345;
            Socket socket = new Socket(server_hostname, PORT);

            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(bos));
            BufferedReader br = new BufferedReader(new InputStreamReader(bis));

            Thread MessageListenerThread = new Thread(new MessageListenerThread(socket, br));
            Thread UserInputThread = new Thread(new UserInputThread(socket, username, bw));

            MessageListenerThread.start();
            UserInputThread.start();
            System.out.println("Connected to the server. You can start sending messages.");

        } catch (Exception e) {
            System.out.println("Error: Server is not reachable");
        }
    }
}

class UserInputThread implements Runnable {
    private Socket socket;
    private String username;
    private Scanner scanner;
    private String message;
    private BufferedWriter bw;

    UserInputThread(Socket socket, String username, BufferedWriter bw) {
        this.socket = socket;
        this.username = username;
        this.bw = bw;
        this.scanner = new Scanner(System.in);
        this.message = "";
    }

    @Override
    public void run() {
        try {
            bw.write("[" + username +"]");
            bw.newLine();
            bw.flush();
            while (!message.equals("/quit")) {
                message = scanner.nextLine();
                bw.write(message);
                bw.newLine();
                bw.flush();
            }
            socket.close();
        } catch (Exception e) {}
    }
}

class MessageListenerThread implements Runnable {
    private Socket socket;
    private BufferedReader br;
    private String data;

    MessageListenerThread(Socket socket, BufferedReader br) {
        this.socket = socket;
        this.br = br;
        this.data = "";
    }

    @Override
    public void run() {
        try {
            while (!socket.isClosed()) {
                if (br.ready()) {
                    data = br.readLine();
                    System.out.println(data);
                }
            }
        } catch (Exception e) {}
    }
}