package com.javachat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    private static class Handler extends Thread {
        private final Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            Message message;

            connection.send(new Message(MessageType.NAME_REQUEST, "Введите свое имя."));

            message = connection.receive();
            String userName = message.getData();

            if (message.getType() == MessageType.USER_NAME
                    && !userName.isEmpty()
                    && !connectionMap.containsKey(userName)) {
                connectionMap.put(userName, connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return userName;
            } else {
                return serverHandshake(connection);
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {

            for (Map.Entry<String, Connection> pair : connectionMap.entrySet()) {
                String name = pair.getKey();
                if (!name.equals(userName)) {
                    Message message = new Message(MessageType.USER_ADDED, name);
                    connection.send(message);
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message messageReceived = connection.receive();

                if (messageReceived.getType() == MessageType.TEXT) {
                    Message messageToSend = new Message(MessageType.TEXT, userName + ": " + messageReceived.getData());
                    sendBroadcastMessage(messageToSend);
                } else {
                    ConsoleHelper.writeMessage("Ошибка! Тип сообщения должен быть TEXT!");
                }
            }
        }

        public void run() {
            ConsoleHelper.writeMessage(socket.getRemoteSocketAddress().toString());
            try (Connection connection = new Connection(socket);) {

                String name = serverHandshake(connection);
                if (!name.isEmpty()) {
                    sendBroadcastMessage(new Message(MessageType.USER_ADDED, name));
                    notifyUsers(connection, name);
                    serverMainLoop(connection, name);
                    connectionMap.remove(name);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, name));
                }
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, name));
                notifyUsers(connection, name);
                serverMainLoop(connection, name);
                ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто.");
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом.");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ConsoleHelper.writeMessage("Введите порт сервера.");
        ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt());
        ConsoleHelper.writeMessage("Сервер запущен!");

        try {
            while (true) {
                new Handler(serverSocket.accept()).start();
            }
        } catch (Exception e) {
            serverSocket.close();
            ConsoleHelper.writeMessage(e.getMessage());
        }
    }

    public static void sendBroadcastMessage(Message message) {
        for (Map.Entry<String, Connection> pair : connectionMap.entrySet()) {
            try {
                pair.getValue().send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Произошла ощибка при отправке сообщения!");
            }
        }
    }
}
