import java.io.*;
import java.net.*;
import java.util.*;

class tcpccs {
    public static void main(String[] args) {
        // work with command line arguments
        String server_hostname;
        String username;
        if (args.length == 2) {
            server_hostname = args[0];
            username = args[1];
        } else {
            System.out.println("Usage: java tcpccs <server_hostname> <username>");
            return;
        }

        // default scanner
        Scanner scanner = new Scanner(System.in);

        try {
            // port to connect to
            final int PORT = 12345;
            // attempt to connect to port on server
            Socket socket = new Socket(server_hostname, PORT);

            // create buffered writer/reader to write to socket (formatted)
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(socket.getOutputStream())));
            BufferedReader br = new BufferedReader(new InputStreamReader( new BufferedInputStream(socket.getInputStream())));

            // inform server of username
            bw.write(username);
            bw.newLine();
            bw.flush();

            // validate username (check for duplicate in server)
            while (true) {
                String response = br.readLine();
                if (response.equals("[CONNECTED]")) {
                    break;
                } else {
                    System.out.println("Username already taken. Try another: ");
                    username = scanner.nextLine();
                    bw.write(username);
                    bw.newLine();
                    bw.flush();
                }
            }

            // add shutdown hook to check if program is terminated without /quit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (!socket.isClosed()) {
                        BufferedWriter tempBW = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        tempBW.write("[COMMAND]");
                        tempBW.newLine();
                        tempBW.write("/quit");
                        tempBW.newLine();
                        tempBW.flush();
                        bw.close();
                        br.close();
                        scanner.close();
                        socket.close();
                    }
                } catch (Exception e) {}
            }));

            // create/start message listener and user input threads
            Thread listener = new Thread(new MessageListenerThread(bw, br));
            Thread input = new Thread(new UserInputThread(socket, bw, scanner));

            listener.start();
            input.start();

            // inform user of their ability to start sending messages
            System.out.println("Connected to the server. You can start sending messages.");

            // make it so that listener and input have to close before main thread
            listener.join();
            input.join();

        } catch (Exception e) {
            System.out.println("Error: Server is not reachable");
        }
    }
}

class UserInputThread implements Runnable {
    private final Socket socket;
    private final BufferedWriter bw;
    private final Scanner scanner;
    private String header;
    private String data;

    UserInputThread(Socket socket, BufferedWriter bw, Scanner scanner) {
        this.socket = socket;
        this.bw = bw;
        this.scanner = scanner;
        this.header = "";
        this.data = "";
    }

    @Override
    public void run() {
        try {

            while (!data.equals("/quit")) {
                header = "[MESSAGE]";
                data = scanner.nextLine();
                if (data.startsWith("/")) header = "[COMMAND]";
                bw.write(header);
                bw.newLine();
                bw.write(data);
                bw.newLine();
                bw.flush();
            }
        } catch (Exception e) {}
    }
}

class MessageListenerThread implements Runnable {
    private final BufferedWriter bw;
    private final BufferedReader br;

    MessageListenerThread(BufferedWriter bw, BufferedReader br) {
        this.bw = bw;
        this.br = br;
    }

    @Override
    public void run() {
        try {
            String header = "";
            while (!header.equals("[EXIT]")) {
                header = br.readLine();
                if (header == null) header = "[EXIT]";
                String data = br.readLine();
                if (data == null) data = "Exiting...";

                switch (header) {
                    case "[FILE_VERIFICATION]":
                        findFile(data);
                        break;
                    case "[FILE_TRANSFER_START]":
                        String fileName = br.readLine();
                        String fileSender = br.readLine();
                        String fileReceiver = br.readLine();
                        if (data.equals("[SENDER]")) {
                            bw.write("[FILE_TRANSFER_INFO]");
                            bw.newLine();
                            bw.write(fileName);
                            bw.newLine();
                            bw.write(fileSender);
                            bw.newLine();
                            bw.write(fileReceiver);
                            bw.newLine();
                            bw.flush();

                            Thread fileSend = new Thread(new FileTransferThread(fileName, bw));
                            fileSend.start();
                        } else if (data.equals("[RECEIVER]")) {
                            String address = br.readLine();
                            String port = br.readLine();
                            Thread recv = new Thread(new FileReceptionThread(fileName, fileSender, fileReceiver, address, port));
                            recv.start();
                        }
                        break;
                    case "[MESSAGE]":
                        System.out.println(data);
                    default:
                }
            }
        } catch (Exception e) {
            System.out.println("Connection lost.");
        }
    }

    public void findFile(String fileName) {
        try {
            File file = new File(fileName);
            if (file.exists() && file.isFile()) {
                bw.write("File found.");
                bw.newLine();
                bw.write(Long.toString(file.length()));
                bw.newLine();
                bw.flush();
            } else {
                bw.write("File not found.");
                bw.newLine();
                bw.flush();
            }
        } catch (Exception e) {}
    }
}

class FileTransferThread implements Runnable {
    private final ServerSocket serverSocket;
    private Socket socket;
    private final File file;
    private final long fileSize;
    private DataOutputStream dos;
    private FileInputStream fis;

    FileTransferThread(String fileName, BufferedWriter bw) throws IOException {
        this.serverSocket = new ServerSocket(0);
        this.serverSocket.setSoTimeout(10000);
        bw.write(InetAddress.getLocalHost().getHostAddress());
        bw.newLine();
        bw.write(String.valueOf(serverSocket.getLocalPort()));
        bw.newLine();
        bw.flush();
        this.file = new File(fileName);
        this.fileSize = file.length();
        this.fis = new FileInputStream(file);
    }

    public void run() {
        try {
            socket = serverSocket.accept();
            dos = new DataOutputStream(socket.getOutputStream());
            dos.writeLong(file.length());
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalSent = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
                totalSent += bytesRead;
            }

            fis.close();
            dos.flush();
            dos.close();
            serverSocket.close();
        } catch (IOException e) {}
    }
}

class FileReceptionThread implements Runnable {
    private final Socket socket;
    private String fileSender;
    private String fileReceiver;
    private final File file;
    private long fileSize;
    private DataInputStream dis;

    FileReceptionThread(String fileName, String fileSender, String fileReceiver, String address, String port) throws IOException {
        System.out.println("e");
        this.socket = new Socket(address, Integer.parseInt(port));
        this.fileSender = fileSender;
        this.fileReceiver = fileReceiver;
        this.file = getUniqueFile(fileName);
        this.dis = new DataInputStream(socket.getInputStream());
    }

    public void run() {
        boolean valid = false;
        do {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[4096];
                this.fileSize = dis.readLong();
                int bytesRead;
                long totalRead = 0;
                while (totalRead < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }

                if (totalRead == fileSize) {
                    System.out.println("[File transfer complete from" + fileSender + " to " + fileReceiver + " " + file.getName() + " ( + " + file.length()/1024 + " KB)]");
                    valid = true;
                } else {
                    System.out.println("[File transfer incomplete or corrupted]");
                    System.out.println("[Reattempting File Transfer...]");
                    file.delete();
                    continue;
                }

                fos.flush();
                fos.close();
                dis.close();
                socket.close();

            } catch (IOException e) {
                System.out.println("[Lost connection while receiving: " + e.getMessage() + "]");
                file.delete();
                break;
            }
        } while (!valid);
    }

    private File getUniqueFile(String baseName) {
        File file = new File(baseName);
        if (!file.exists()) return file;
        String base;
        String ext = "";
        int dotIndex = baseName.lastIndexOf(".");
        if (dotIndex != -1) {
            base = baseName.substring(0, dotIndex);
            ext = baseName.substring(dotIndex);
        } else {
            base = baseName;
        }
        int count = 1;
        File newFile;
        do {
            String newName = base + " (" + count + ")" + ext;
            newFile = new File(newName);
            count++;
        } while (newFile.exists());
        return newFile;
    }
}