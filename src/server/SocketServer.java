package server;

import room.Room;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class SocketServer {
    private ServerSocket serverSocket;
    private ArrayList<SocketServerThread> socketThreads;
    private ArrayList<Room> rooms;
    private int maxThreads;
    private boolean alive;

    public SocketServer(int maxThreads) {
        alive = true;
        socketThreads = new ArrayList<>();
        rooms = new ArrayList<>();
        this.maxThreads = maxThreads;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public ArrayList<SocketServerThread> getSocketThreads() {
        return socketThreads;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public boolean isAlive() {
        return alive;
    }

    private synchronized void listenToClients() throws IOException {
        SocketServerThread newThread;
        Socket socket;
        // Always listen to new client socket connections
        while (alive) {
            System.out.println("Waiting for a client to connect");
            socket = serverSocket.accept();
            if (!alive) {
                break;
            }
            if (socketThreads.size() < maxThreads) {
                newThread = new SocketServerThread(socket, socketThreads, rooms);
                socketThreads.add(newThread);
                new Thread(newThread).start();
                System.out.println("Client connected to server successfully from: " + socket.getInetAddress().getHostAddress());
            } else {
                System.out.println("Maximum number of threads achieved, could not add another socket connection");
                socket.close();
            }
        }
        // Kill running socket threads before killing the server
        for (SocketServerThread currentThread : socketThreads) {
            currentThread.killThread();
        }
    }

    public void listen(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server is listening on port " + port + "\n");
        listenToClients();
    }

    public void killServer() throws IOException {
        alive = false;
        // Simulate a new client socket connection to exit the blocking code of serverSocket.accept()
        // and break out of the infinite loop
        new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort()).close();
        serverSocket.close();
    }

    public static void main(String[] args) throws IOException {
        SocketServer server = null;
        try {
            server = new SocketServer(20);
            server.listen(6000);
        } catch (IOException e) {
            e.printStackTrace();
            server.killServer();
//            System.exit(1);
        }
    }
}
