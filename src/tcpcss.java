import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

class tcpcss {
    public static void main(String[] args) {
        final int PORT = 12345; // Port to listen on
        ArrayList<Thread> clients = new ArrayList<Thread>();
        ArrayList<Socket> clientSockets = new ArrayList<Socket>();
        ArrayList<String> users = new ArrayList<String>();

        ClientHandlerThread clientHandlerThread;
        Thread client;
        ServerSocket serverSocket = null;
        Socket clientSocket;
        String username;

        try {
            serverSocket = new ServerSocket(Integer.valueOf(args[0]));
        } catch (Exception e1) {
            try {
                serverSocket = new ServerSocket(PORT);
            } catch (Exception e2) {}
        }

        System.out.println("Listening on port " + serverSocket.getLocalPort());
        System.out.println("Waiting for connections...");
        while (clients.isEmpty()) {
            try {
                clientSocket = serverSocket.accept();
                if (!clientSockets.contains(clientSocket)) {
                    BufferedInputStream bis = new BufferedInputStream(clientSocket.getInputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(bis));

                    clientSockets.add(clientSocket);
                    username = br.readLine();
                    users.add(username);
                    clientHandlerThread = new ClientHandlerThread(clientSockets, users, clientSocket, username, br);
                    client = new Thread(clientHandlerThread);
                    clients.add(client);
                    System.out.println("New connection, thread name is " + client.getName()
                            + ", ip is: " + clientSocket.getInetAddress().toString().substring(1)
                            + ", port: " + clientSocket.getPort());
                    client.start();
                }
            } catch (Exception e) {}
        }

        while (!clients.isEmpty()) {
            try {
                clientSocket = serverSocket.accept();
                if (!clientSockets.contains(clientSocket)) {
                    BufferedInputStream bis = new BufferedInputStream(clientSocket.getInputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(bis));

                    clientSockets.add(clientSocket);
                    username = br.readLine();
                    users.add(username);
                    clientHandlerThread = new ClientHandlerThread(clientSockets, users, clientSocket, username, br);
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
    ArrayList<Socket> clientSockets;
    ArrayList<String> users;
    Socket clientSocket;
    String username;
    String message;
    BufferedOutputStream bos;
    BufferedReader br;
    BufferedWriter bw;

    ClientHandlerThread(ArrayList<Socket> clientSockets, ArrayList<String> users, Socket clientSocket, String username, BufferedReader br) {
        this.clientSockets = clientSockets;
        this.users = users;
        this.clientSocket = clientSocket;
        this.username = username;
        this.message = "";
        this.br = br;
    }

    @Override
    public void run() {
        try {
            while (!clientSocket.isClosed()) {
                this.bw = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(clientSocket.getOutputStream())));
                if (br.ready()) {
                    message = br.readLine();
                    System.out.println(username + " " + message);
                    if (message.startsWith("/")) {
                        execute(message);
                    }
                    else {
                        broadcast(message);
                    }
                }
            }
        } catch (Exception e) {}

        try {
            clientSocket.close();
        } catch (Exception e) {}
    }

    public void broadcast(String message) {
        for (int i = 0; i < clientSockets.size(); i++) {
            if (!clientSocket.equals(clientSockets.get(i))) {
                try {
                    bos = new BufferedOutputStream(clientSockets.get(i).getOutputStream());
                    bw = new BufferedWriter(new OutputStreamWriter(bos));
                    bw.write(username + " " + message);
                    bw.newLine();
                    bw.flush();
                } catch (Exception e) {}
            }
        }
    }

    public void execute(String message) {
        String[] data = message.split(" ");
        switch (data[0]) {
            case "/quit":
                if (data.length == 1) {
                    cleanExit();
                }
                else {
                    invalidCommand();
                }
                break;
            case "/sendfile":
                if (data.length == 3) {
                    sendFile(data[1], data[2]);
                }
                else {
                    invalidCommand();
                }
                break;
            case "/acceptfile":
                if (data.length == 2) {
                    acceptFile(data[1]);
                }
                else {
                    invalidCommand();
                }
                break;
            case "/rejectfile":
                if (data.length == 2) {
                    rejectFile(data[1]);
                }
                else {
                    invalidCommand();
                }
                break;
            default:
                invalidCommand();
        }
    }

    public void cleanExit() {
        try {
            bw.write("Exiting...");
            bw.newLine();
            bw.flush();
            clientSocket.close();
        } catch (Exception e) {}
    }

    public void sendFile(String user, String fileName) {

    }

    public void acceptFile(String user) {

    }

    public void rejectFile(String user) {

    }

    public void invalidCommand() {
        try {
            bw.write("> Invalid command");
            bw.newLine();
            bw.flush();
        } catch (Exception e) {}
    }
}