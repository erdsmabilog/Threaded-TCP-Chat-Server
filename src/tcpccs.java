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

        Scanner scanner = new Scanner(System.in);
        try {
            final int PORT = 12345;
            Socket socket = new Socket(server_hostname, PORT);

            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(bos));
            BufferedReader br = new BufferedReader(new InputStreamReader(bis));
            bw.write("[" + username +"]");
            bw.newLine();
            bw.flush();
            while (true) {
                String response = br.readLine();
                if (response.equals("[CONNECTED]")) {
                    break;
                } else {
                    System.out.println("Invalid username -- please try again");
                    System.out.print("Username: ");
                    username = scanner.nextLine();
                    bw.write("[" + username + "]");
                    bw.newLine();
                    bw.flush();
                }
            }

            Thread MessageListenerThread = new Thread(new MessageListenerThread(socket, br));
            Thread UserInputThread = new Thread(new UserInputThread(socket, username, bw, scanner));

            MessageListenerThread.start();
            UserInputThread.start();

            System.out.println("Connected to the server. You can start sending messages.");
        } catch (Exception e) {
            System.out.println("Error: Server is not reachable");
        }
    }
}

class UserInputThread implements Runnable {
    private final Socket socket;
    private final BufferedWriter bw;
    private final String username;
    private final Scanner scanner;
    private String header;
    private String message;

    UserInputThread(Socket socket, String username, BufferedWriter bw, Scanner scanner) {
        this.socket = socket;
        this.username = username;
        this.bw = bw;
        this.scanner = scanner;
        this.header = "";
        this.message = "";
    }

    @Override
    public void run() {
        try {
            while (!message.equals("/quit")) {
                header = "[MSG]";
                message = scanner.nextLine();
                if (message.startsWith("/")) {
                    header = "[CMD]";
                }
                bw.write(header);
                bw.newLine();
                bw.write(message);
                bw.newLine();
                bw.flush();
            }
            socket.close();
        } catch (Exception e) {}
    }
}

class MessageListenerThread implements Runnable {
    private final Socket socket;
    private final BufferedReader br;

    MessageListenerThread(Socket socket, BufferedReader br) {
        this.socket = socket;
        this.br = br;
    }

    @Override
    public void run() {
        try {
            String header = "";
            while (!header.equals("[EXIT]")) {
                header = br.readLine();
                if (header == null) {
                    header = "[EXIT]";
                }

                String data = br.readLine();
                if (data == null) {
                    data = "Exiting...";
                }

                switch (header) {
                    case "[FILE_CONFIRMATION]":

                        break;
                    case "[FILE_TRANSFER_START]":
                        break;
                    case "[FILE]":
                        break;
                    default:
                        System.out.println(data);
                }
            }
        } catch (IOException e) {
            System.out.println("Connection lost.");
        }
    }
}


class FileReceptionThread implements Runnable {
    private final Socket socket;
    private final File file;
    private DataInputStream dis;
    private DataOutputStream dos;
    private boolean isValid = true;

    FileReceptionThread(Socket socket, File file) {
        this.socket = socket;
        this.file = file;
        try {
            this.dis = new DataInputStream(new FileInputStream(file.getAbsolutePath()));
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            isValid = false;
        }
    }

    @Override
    public void run() {
        /*
        try {
            while (!socket.isClosed()) {
                if (br.ready()) {
                    header = br.readLine();
                    data = br.readLine();
                    if (header.equals("[FILE]")) {

                    } else if (header.equals("[MSG]")) {
                        System.out.println(data);
                    }
                }
            }
        } catch (Exception e) {}
        */
    }
}