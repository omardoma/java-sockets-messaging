package server;

import message.ChatMessage;
import message.JoinRoomRequest;
import message.JoinRoomResponse;
import room.Room;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class SocketServerThread implements Runnable {
    private Socket socket;
    private ObjectInputStream serverInput;
    private ObjectOutputStream serverOutput;
    private ArrayList<SocketServerThread> socketThreads;
    private ArrayList<Room> rooms;
    private boolean alive;

    public SocketServerThread(Socket socket, ArrayList<SocketServerThread> socketThreads, ArrayList<Room> rooms) throws IOException {
        this.socket = socket;
        this.socketThreads = socketThreads;
        this.rooms = rooms;
        alive = true;
        serverOutput = new ObjectOutputStream(this.socket.getOutputStream());
        serverInput = new ObjectInputStream(this.socket.getInputStream());
    }

    public Socket getSocket() {
        return socket;
    }

    public ObjectInputStream getServerInput() {
        return serverInput;
    }

    public ObjectOutputStream getServerOutput() {
        return serverOutput;
    }

    public ArrayList<SocketServerThread> getSocketThreads() {
        return socketThreads;
    }

    public ArrayList<Room> getRooms() {
        return rooms;
    }

    public boolean isAlive() {
        return alive;
    }

    private void sendToClient(Object message) throws IOException {
        serverOutput.writeObject(message);
        serverOutput.flush();
    }

    private synchronized void broadcast(ChatMessage message) throws IOException {
        for (SocketServerThread sT : socketThreads) {
            if (sT != this) {
                sT.sendToClient(message);
            }
        }
    }

    private synchronized void sendToRoomMembers(ChatMessage message) throws IOException {
        for (Room room : rooms) {
            if (room.getName().equalsIgnoreCase(message.getRoomName())) {
                for (SocketServerThread sT : room.getMembers()) {
                    if (sT != this) {
                        sT.sendToClient(message);
                    }
                }
            }
            break;
        }
    }

    private void onChatMessage(ChatMessage message) throws IOException {
        switch (message.getType()) {
            case BROADCAST:
                broadcast(message);
                break;
            case ROOM:
                sendToRoomMembers(message);
                break;
        }
    }

    private synchronized void onJoinRoomRequest(JoinRoomRequest request) throws IOException {
        for (Room room : rooms) {
            if (room.getName().equalsIgnoreCase(request.getRoomName())) {
                boolean joined = room.addMember(this);
                if (joined) {
                    sendToClient(new JoinRoomResponse("Joined room: " + room.getName()));
                } else {
                    sendToClient(new JoinRoomResponse("Room: " + room.getName() + " is full", true));
                }
                return;
            }
        }
        // If no room with this name
        Room newRoom;
        if (request.isGroup()) {
            newRoom = new Room(request.getRoomName());
        } else {
            newRoom = new Room(request.getRoomName(), 2);
        }
        newRoom.addMember(this);
        rooms.add(newRoom);
        sendToClient(new JoinRoomResponse("Joined room: " + newRoom.getName()));
    }

    private void cleanUp() throws IOException {
        serverOutput.close();
        serverInput.close();
        socket.close();

        // Make sure no other thread is accessing the shared ArrayList before accessing it
        synchronized (socketThreads) {
            socketThreads.remove(this);
        }

        // Make sure no other thread is accessing the shared ArrayList before accessing it
        synchronized (rooms) {
            for (Room room : rooms) {
                if (room.getMembers().contains(this)) {
                    room.removeMember(this);
                }
            }
        }
    }

    @Override
    public void run() {
        while (alive) {
            try {
                Object incomingMessage = serverInput.readObject();
                if (!alive) {
                    break;
                }
                if (incomingMessage instanceof ChatMessage) {
                    onChatMessage((ChatMessage) incomingMessage);
                } else if (incomingMessage instanceof JoinRoomRequest) {
                    onJoinRoomRequest((JoinRoomRequest) incomingMessage);
                }
            } catch (IOException | ClassNotFoundException e) {
                killThread();
            }
        }
        try {
            cleanUp();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void killThread() {
        alive = false;
    }
}
