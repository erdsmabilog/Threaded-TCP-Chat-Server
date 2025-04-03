import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

class   tcpcss {
    public static void main(String[] args) {
        final int PORT = 12345; // Port to listen on
        ArrayList<Thread> clients = new ArrayList<Thread>();
        ArrayList<Socket> clientSockets = new ArrayList<Socket>();
        ArrayList<String> users = new ArrayList<String>();
        // take in an array of users (limited to a pair by implementation)
        // takes in {fileSender, fileReceiver}
        ArrayList<String[]> usersFileTransfer = new ArrayList<String[]>();

        ClientHandlerThread clientHandlerThread;
        Thread client;
        ServerSocket serverSocket = null;
        Socket clientSocket;
        String username;

        // if user enters a valid
        try {
            int port = Integer.parseInt(args[0]);
            if (port >= 0 && port < 65536) {
                serverSocket = new ServerSocket(port);
            }
            else {
                throw new IllegalArgumentException("Invalid port number");
            }
        } catch (Exception e1) {
            try {
                serverSocket = new ServerSocket(PORT);
            } catch (Exception e2) {}
        }

        System.out.println("Listening on port " + serverSocket.getLocalPort());
        System.out.println("Waiting for connections...");

        BufferedInputStream bis;
        BufferedOutputStream bos;
        BufferedReader br;
        BufferedWriter bw;

        try {
            serverSocket.setSoTimeout(5000);
        } catch (Exception e) {}

        while (clients.isEmpty()) {
            try {
                clientSocket = serverSocket.accept();
                bis = new BufferedInputStream(clientSocket.getInputStream());
                bos = new BufferedOutputStream(clientSocket.getOutputStream());
                br = new BufferedReader(new InputStreamReader(bis));
                bw = new BufferedWriter(new OutputStreamWriter(bos));
                username = br.readLine();
                clientSockets.add(clientSocket);
                users.add(username);
                bw.write("[CONNECTED]");
                bw.newLine();
                bw.flush();
                clientHandlerThread = new ClientHandlerThread(clientSockets, users, usersFileTransfer, clientSocket, username, br, bw);
                client = new Thread(clientHandlerThread);
                clients.add(client);
                System.out.println("New connection, thread name is " + client.getName()
                        + ", ip is: " + clientSocket.getInetAddress().toString().substring(1)
                        + ", port: " + clientSocket.getPort());
                client.start();
            } catch (Exception e) {}
        }

        while (!clients.isEmpty()) {
            try {
                clientSocket = serverSocket.accept();
                if (!clientSockets.contains(clientSocket)) {
                    bis = new BufferedInputStream(clientSocket.getInputStream());
                    bos = new BufferedOutputStream(clientSocket.getOutputStream());
                    br = new BufferedReader(new InputStreamReader(bis));
                    bw = new BufferedWriter(new OutputStreamWriter(bos));
                    username = br.readLine();
                    while (users.contains(username)) {
                        bw.newLine();
                        bw.flush();
                        username = br.readLine();
                    }
                    bw.write("[CONNECTED]");
                    bw.newLine();
                    bw.flush();
                    clientSockets.add(clientSocket);
                    users.add(username);
                    clientHandlerThread = new ClientHandlerThread(clientSockets, users, usersFileTransfer, clientSocket, username, br, bw);
                    client = new Thread(clientHandlerThread);
                    clients.add(client);
                    System.out.println("New connection, thread name is " + client.getName()
                            + ", ip is: " + clientSocket.getInetAddress().toString().substring(1)
                            + ", port: " + clientSocket.getPort());
                    client.start();
                }
            } catch (Exception e) {}
        }
    }
}

class ClientHandlerThread implements Runnable {
    private ArrayList<Socket> clientSockets;
    private ArrayList<String> users;
    private ArrayList<String[]> usersFileTransfer;
    private Socket clientSocket;
    private String username;
    private String header;
    private String message;
    private BufferedOutputStream bos;
    private BufferedReader br;
    private BufferedWriter bw;

    ClientHandlerThread(ArrayList<Socket> clientSockets, ArrayList<String> users, ArrayList<String[]> usersFileTransfer, Socket clientSocket, String username, BufferedReader br, BufferedWriter bw) {
        this.clientSockets = clientSockets;
        this.users = users;
        this.usersFileTransfer = usersFileTransfer;
        this.clientSocket = clientSocket;
        this.username = username;
        this.header = "";
        this.message = "";
        this.br = br;
        this.bw = bw;
    }

    @Override
    public void run() {
        try {
            while (!clientSocket.isClosed()) {
                if (br.ready()) {
                    header = br.readLine();
                    message = br.readLine();
                    System.out.println(username + " " + message);
                    switch (header) {
                        case "[CMD]":
                            execute(message);
                            break;
                        default:
                            broadcast(message);
                    }
                }
            }
        } catch (Exception e) {}

        try {
            clientSockets.remove(clientSocket);
            users.remove(username);
            clientSocket.close();
        } catch (Exception e) {}
    }

    public void broadcast(String message) {
        for (int i = 0; i < clientSockets.size(); i++) {
            BufferedOutputStream tempbos;
            BufferedWriter tempbw;
            if (!clientSocket.equals(clientSockets.get(i))) {
                try {
                    tempbos = new BufferedOutputStream(clientSockets.get(i).getOutputStream());
                    tempbw = new BufferedWriter(new OutputStreamWriter(tempbos));
                    tempbw.write("[MSG]");
                    tempbw.newLine();
                    tempbw.write(username + " " + message);
                    tempbw.newLine();
                    tempbw.flush();
                } catch (Exception e) {}
            }
        }
    }

    public void execute(String message) {
        try {
            String[] data = message.split(" ");
            switch (data[0]) {
                case "/sendfile":
                    if (data.length == 3) {
                        if (users.contains("[" + data[1] + "]")) {
                            sendFile(data[1], data[2]);
                        } else {
                            bw.write("[MSG]");
                            bw.newLine();
                            bw.write("> Invalid user");
                            bw.newLine();
                            bw.flush();
                        }
                    } else {
                        invalidCommand();
                    }
                    break;
                case "/acceptfile":
                    if (data.length == 2) {
                        acceptFile(username, data[1]);
                    } else {
                        invalidCommand();
                    }
                    break;
                case "/rejectfile":
                    if (data.length == 2) {
                        rejectFile(data[1]);
                    } else {
                        invalidCommand();
                    }
                    break;
                case "/who":
                    if (data.length == 1) {
                        int i;

                        System.out.println(username + " requested online users list.");
                        System.out.printf("[Online users: ");
                        for (i = 0; i < users.size(); i++) {
                            System.out.printf(users.get(i).substring(1, users.get(i).length()-1));
                            if (i != users.size()-1) {
                                System.out.printf(", ");
                            }
                        }
                        System.out.println("]");

                        bw.write("[MSG]");
                        bw.newLine();
                        bw.write("[Online users: ");
                        for (i = 0; i < users.size(); i++) {
                            bw.write(users.get(i).substring(1, users.get(i).length()-1));
                            if (i != users.size()-1) {
                                bw.write(", ");
                            }
                        }
                        bw.write("]");
                        bw.newLine();
                        bw.flush();
                    } else {
                        invalidCommand();
                    }
                    break;
                case "/quit":
                    if (data.length == 1) {
                        cleanExit();
                    } else {
                        invalidCommand();
                    }
                    break;
                default:
                    invalidCommand();
            }
        } catch (Exception e) {}
    }

    public void sendFile(String user, String fileName) {

    }

    public void acceptFile(String fileSender, String fileReceiver, File file) {
        if (usersFileTransfer.contains(new String[]{fileSender, fileReceiver})) {
        }
        try {
            bw.write("[FILE_TRANSFER_START]");
            bw.newLine();
            bw.write(fileName);
            bw.newLine();
            bw.write(fileSize);
            bw.newLine();
            bw.flush();
        } catch (Exception e) {}
    }

    public void rejectFile(String user) {

    }

    public void cleanExit() {
        try {
            bw.write("[EXIT]");
            bw.newLine();
            bw.write("Exiting...");
            bw.newLine();
            bw.flush();
            clientSocket.close();
        } catch (Exception e) {}
    }

    public void invalidCommand() {
        try {
            bw.write("[MSG]");
            bw.newLine();
            bw.write("> Invalid command");
            bw.newLine();
            bw.flush();
        } catch (Exception e) {}
    }
}