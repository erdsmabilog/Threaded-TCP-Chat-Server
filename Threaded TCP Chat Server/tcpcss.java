import java.io.*;
import java.net.*;
import java.util.*;

class tcpcss {
    public static void main(String[] args) {
        final int PORT = 12345;
        List<Socket> clientSockets = Collections.synchronizedList(new ArrayList<>());
        List<String> users = Collections.synchronizedList(new ArrayList<>());
        // sender, receiver
        List<String[]> userFileTransferPair = Collections.synchronizedList(new ArrayList<>());
        // fileName, fileSize
        List<String[]> userFileTransferInfo = Collections.synchronizedList(new ArrayList<>());

        ServerSocket serverSocket = null;
        try {
            int port = PORT;
            if (args.length == 1) {
                int parsedPort = Integer.parseInt(args[0]);
                if (parsedPort >= 0 && parsedPort < 65536) port = parsedPort;
            }
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            System.out.println("Failed to bind to port");
            return;
        }

        System.out.println("Listening on port " + serverSocket.getLocalPort());
        System.out.println("Waiting for connections...");

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(clientSocket.getInputStream())));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(clientSocket.getOutputStream())));
                String username = br.readLine();
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
                ClientHandlerThread handler = new ClientHandlerThread(clientSockets, users, userFileTransferPair, userFileTransferInfo, clientSocket, username, br, bw);
                Thread client = new Thread(handler);
                System.out.println("New connection, thread name is " + client.getName() + ", ip is: " + clientSocket.getInetAddress().toString().substring(1) + ", port: " + clientSocket.getPort());
                client.start();
            } catch (Exception e) {}
        }
    }
}

class ClientHandlerThread implements Runnable {
    private final List<Socket> clientSockets;
    private final List<String> users;
    private final List<String[]> userFileTransferPair;
    private final List<String[]> userFileTransferInfo;
    private final Socket clientSocket;
    private final String username;
    private final BufferedReader br;
    private final BufferedWriter bw;
    private String header = "";
    private String data = "";

    ClientHandlerThread(List<Socket> clientSockets, List<String> users, List<String[]> userFileTransferPair, List<String[]> userFileTransferInfo, Socket clientSocket, String username, BufferedReader br, BufferedWriter bw) {
        this.clientSockets = clientSockets;
        this.users = users;
        this.userFileTransferPair = userFileTransferPair;
        this.userFileTransferInfo = userFileTransferInfo;
        this.clientSocket = clientSocket;
        this.username = username;
        this.br = br;
        this.bw = bw;
    }

    public void run() {
        try {
            broadcast("[" + username + "] has joined the chat.");
            while (!clientSocket.isClosed()) {
                if (br.ready()) {
                    header = br.readLine();
                    switch (header) {
                        case "[COMMAND]":
                            data = br.readLine();
                            execute(data);
                            break;
                        case "[FILE_TRANSFER_INFO]":
                            String fileName = br.readLine();
                            String fileSender = br.readLine();
                            String fileReceiver = br.readLine();
                            endAcceptFile(fileSender, fileReceiver, fileName);
                            break;
                        default:
                            data = br.readLine();
                            broadcast("[" + username + "] " + data);
                    }
                }
            }
        } catch (Exception e) {}
    }

    public void broadcast(String message) {
        synchronized (clientSockets) {
            for (int i = 0; i < clientSockets.size(); i++) {
                if (!clientSocket.equals(clientSockets.get(i))) {
                    try {
                        BufferedWriter tempbw = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(clientSockets.get(i).getOutputStream())));
                        tempbw.write("[MESSAGE]");
                        tempbw.newLine();
                        tempbw.write(message);
                        tempbw.newLine();
                        tempbw.flush();
                    } catch (Exception e) {
                    }
                }
            }
        }
        System.out.println(message);
    }

    public void execute(String message) {
        try {
            String[] data = message.split(" ", 3);
            switch (data[0]) {
                case "/sendfile":
                    if (data.length == 3 && users.contains(data[1]))
                        sendFile(data[1], data[2]);
                    else
                        invalidCommand();
                    break;
                case "/acceptfile":
                    if (data.length == 2)
                        startAcceptFile(data[1], username);
                    else
                        invalidCommand();
                    break;
                case "/rejectfile":
                    if (data.length == 2)
                        rejectFile(data[1]);
                    else
                        invalidCommand();
                    break;
                case "/who":
                    if (data.length == 1) {
                        bw.write("[MESSAGE]");
                        bw.newLine();
                        bw.write("[Online users: " + String.join(", ", users) + "]");
                        bw.newLine();
                        bw.flush();
                        System.out.println("[" + username + "] requested online users list.");
                        System.out.println("[Online users: " + String.join(", ", users) + "]");
                    } else
                        invalidCommand();
                    break;
                case "/quit":
                    if (data.length == 1)
                        cleanExit();
                    else
                        invalidCommand();
                    break;
                default:
                    invalidCommand();
            }
        } catch (Exception e) {}
    }

    public void sendFile(String fileReceiver, String fileName) throws Exception {
        String[] userPair = {username, fileReceiver};
        boolean pairFound = false;

        synchronized (userFileTransferPair) {
            for (String[] pair : userFileTransferPair) {
                if (Arrays.equals(pair, userPair)) {
                    pairFound = true;
                    break;
                }
            }
        }

        if (pairFound) {
            bw.write("[MESSAGE]");
            bw.newLine();
            bw.write("You have already attempted to send a file to " + fileReceiver + ".");
            bw.newLine();
            bw.write("[MESSAGE]");
            bw.newLine();
            bw.write("Please wait for a response.");
            bw.newLine();
            bw.flush();
        } else {
            bw.write("[FILE_VERIFICATION]");
            bw.newLine();
            bw.write(fileName);
            bw.newLine();
            bw.flush();
            String response = br.readLine();
            if (response.equals("File found.")) {
                response = br.readLine();
                long fileSize = Long.parseLong(response);
                userFileTransferPair.add(userPair);
                userFileTransferInfo.add(new String[]{fileName, response});
                String message = "[File transfer initiated from " + username + " to " + fileReceiver + " " + fileName + " (" + (fileSize/1024) + " KB)]";
                bw.write("[MESSAGE]");
                bw.newLine();
                bw.write(message);
                bw.newLine();
                bw.flush();
                broadcast(message);
            } else {
                bw.write("[MESSAGE]");
                bw.newLine();
                bw.write("File not found.");
                bw.newLine();
                bw.flush();
            }
        }
    }

    public void startAcceptFile(String fileSender, String fileReceiver) throws Exception {
        String[] userPair = {fileSender, fileReceiver};
        int pairIdx;
        boolean pairFound = false;
        String[] fileInfo;
        String fileName = "";

        synchronized (userFileTransferPair) {
            for (pairIdx = 0; pairIdx < userFileTransferPair.size(); pairIdx++) {
                if (Arrays.equals(userPair, userFileTransferPair.get(pairIdx))) {
                    pairFound = true;
                    fileInfo = userFileTransferInfo.get(pairIdx);
                    fileName = fileInfo[0];
                    userFileTransferPair.remove(pairIdx);
                    userFileTransferInfo.remove(pairIdx);
                    break;
                }
            }
        }

        if (pairFound) {
            int senderIdx = users.indexOf(fileSender);
            BufferedWriter senderbw = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(clientSockets.get(senderIdx).getOutputStream())));

            senderbw.write("[FILE_TRANSFER_START]");
            senderbw.newLine();
            senderbw.write("[SENDER]");
            senderbw.newLine();
            senderbw.write(fileName);
            senderbw.newLine();
            senderbw.write(fileSender);
            senderbw.newLine();
            senderbw.write(fileReceiver);
            senderbw.newLine();
            senderbw.flush();
        } else {
            bw.write("[MESSAGE]");
            bw.newLine();
            bw.write(fileSender + " has not sent a file.");
            bw.newLine();
            bw.flush();
        }
    }

    public void endAcceptFile(String fileSender, String fileReceiver, String fileName) throws Exception {
        String message1 = "[File transfer accepted from " + fileReceiver + " to " + fileSender + "]";
        String message2 = "[Starting file transfer between " + fileSender + " and " + fileReceiver + "]";

        int receiverIdx = users.indexOf(fileReceiver);
        BufferedWriter receiverbw = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(clientSockets.get(receiverIdx).getOutputStream())));

        String address = br.readLine();
        String port = br.readLine();

        receiverbw.write("[FILE_TRANSFER_START]");
        receiverbw.newLine();
        receiverbw.write("[RECEIVER]");
        receiverbw.newLine();
        receiverbw.write(fileName);
        receiverbw.newLine();
        receiverbw.write(fileSender);
        receiverbw.newLine();
        receiverbw.write(fileReceiver);
        receiverbw.newLine();
        receiverbw.write(address);
        receiverbw.newLine();
        receiverbw.write(port);
        receiverbw.newLine();
        receiverbw.flush();

        bw.write("[MESSAGE]");
        bw.newLine();
        bw.write(message1);
        bw.newLine();
        bw.write("[MESSAGE]");
        bw.newLine();
        bw.write(message2);
        bw.newLine();
        bw.flush();
        broadcast(message1);
        broadcast(message2);
    }

    public void rejectFile(String fileSender) throws Exception {
        String[] userPair = {fileSender, username};
        int pairIdx;
        boolean pairFound = false;

        synchronized (userFileTransferPair) {
            for (pairIdx = 0; pairIdx < userFileTransferPair.size(); pairIdx++) {
                if (Arrays.equals(userPair, userFileTransferPair.get(pairIdx))) {
                    pairFound = true;
                    userFileTransferPair.remove(pairIdx);
                    userFileTransferInfo.remove(pairIdx);
                    break;
                }
            }
        }

        if (pairFound) {
            BufferedWriter senderbw = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(clientSockets.get(users.indexOf(fileSender)).getOutputStream())));
            senderbw.write("[MESSAGE]");
            senderbw.newLine();
            senderbw.write(username + " rejected your file transfer request.");
            senderbw.newLine();
            senderbw.flush();
        } else {
            bw.write("[MESSAGE]");
            bw.newLine();
            bw.write(fileSender + " has not sent a file.");
            bw.newLine();
            bw.flush();
        }
    }

    public void cleanExit() throws Exception {
        bw.write("[EXIT]");
        bw.newLine();
        bw.write("Exiting...");
        bw.newLine();
        bw.flush();
        broadcast("[" + username + "] has left the chat.");
        bw.close();
        br.close();
        clientSockets.remove(clientSocket);
        users.remove(username);
        clientSocket.close();
    }

    public void invalidCommand() throws Exception {
        bw.write("[MESSAGE]");
        bw.newLine();
        bw.write("> Invalid command");
        bw.newLine();
        bw.flush();
    }
}
