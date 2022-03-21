package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Connection {
    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = (ObjectOutputStream) socket.getOutputStream();
        this.in = (ObjectInputStream) socket.getInputStream();
    }
}
