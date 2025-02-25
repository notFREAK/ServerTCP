package org.example.Server;

import com.google.gson.Gson;
import org.example.figure.Circle;
import org.example.figure.Line;
import org.example.figure.Rectangle;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

public class MultiClientServer extends JFrame implements ServerLogic.ServerListener {
    private ServerSocket serverSocket;
    private boolean running = false;
    private final ServerLogic logic;
    private final DefaultListModel<String> objectListModel = new DefaultListModel<>();
    private final JList<String> objectList = new JList<>(objectListModel);
    private JButton startButton, stopButton;

    public MultiClientServer(){
        super("TCP Server port:12345");
        setSize(400,300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        logic = new ServerLogic();
        logic.addListener(new ServerLogic.ServerListener() {
            @Override
            public void onNewObjectReceived(int id, Object obj) {
                SwingUtilities.invokeLater(() ->
                        objectListModel.addElement("ID=" + id + ": " + obj.toString())
                );
            }
            @Override
            public void onObjectDeleted(int id) {
                SwingUtilities.invokeLater(() -> {
                    for (int i = objectListModel.getSize()-1; i >= 0; i--) {
                        String item = objectListModel.getElementAt(i);
                        if (item.startsWith("ID=" + id + ":")) {
                            objectListModel.remove(i);
                        }
                    }
                });
            }
        });

        startButton = new JButton("Запустить сервер");
        stopButton = new JButton("Остановить сервер");
        stopButton.setEnabled(false);

        JPanel topPanel = new JPanel();
        topPanel.add(startButton);
        topPanel.add(stopButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(objectList), BorderLayout.CENTER);

        startButton.addActionListener(e -> startServer(12345));
        stopButton.addActionListener(e -> stopServer());
    }

    private void startServer(int port) {
        if (running) return;
        logic.loadObjectsFromFile();
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            Thread acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        new ClientHandler(client).start();
                    } catch(IOException ex) {
                        if (!running) break;
                    }
                }
            });
            acceptThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopServer(){
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()){
                serverSocket.close();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    @Override
    public void onNewObjectReceived(int id, Object obj) {
    }

    @Override
    public void onObjectDeleted(int id) {
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        ClientHandler(Socket s) { this.socket = s; }

        @Override
        public void run(){
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                String line;
                while((line = in.readLine()) != null && running){
                    if(line.equals("GET_ALL")){
                        String json = logic.getAllObjectsJson();
                        out.println(json);
                        continue;
                    }
                    else if(line.startsWith("DELETE ")){
                        try {
                            int id = Integer.parseInt(line.substring("DELETE ".length()).trim());
                            boolean result = logic.deleteObject(id);
                            if(result)
                                out.println("OK: Deleted id " + id);
                            else
                                out.println("ERROR: Not found id " + id);
                        } catch(NumberFormatException e) {
                            out.println("ERROR: Invalid id format");
                        }
                        continue;
                    }
                    else if(line.startsWith("GET ")){
                        try {
                            int id = Integer.parseInt(line.substring("GET ".length()).trim());
                            Object obj = logic.getObject(id);
                            if(obj != null) {
                                Map<String, Object> map = new LinkedHashMap<>();
                                map.put("id", id);
                                String type;
                                if(obj instanceof Circle) {
                                    type = "Circle";
                                } else if(obj instanceof Rectangle) {
                                    type = "Rectangle";
                                } else if(obj instanceof Line) {
                                    type = "Line";
                                } else {
                                    type = "Unknown";
                                }
                                map.put("type", type);
                                Map<String, Object> dataMap = new Gson().fromJson(new Gson().toJson(obj), Map.class);
                                map.put("data", dataMap);
                                out.println(new Gson().toJson(map));
                            } else {
                                out.println("ERROR: Not found id " + id);
                            }
                        } catch(NumberFormatException e) {
                            out.println("ERROR: Invalid id format");
                        }
                        continue;
                    }
                    else if(line.startsWith("ADD ")){
                        String content = line.substring("ADD ".length());
                        int idx = content.indexOf('{');
                        if(idx > 0){
                            String className = content.substring(0, idx);
                            String json = content.substring(idx);
                            Object obj = null;
                            switch(className){
                                case "Circle":
                                    obj = new Gson().fromJson(json, Circle.class);
                                    break;
                                case "Rectangle":
                                    obj = new Gson().fromJson(json, Rectangle.class);
                                    break;
                                case "Line":
                                    obj = new Gson().fromJson(json, Line.class);
                                    break;
                                default:
                                    out.println("ERROR: Unknown class");
                                    continue;
                            }
                            if(obj != null){
                                int id = logic.addObject(obj);
                                out.println("OK: Received " + className + " with id " + id);
                            }
                        } else {
                            out.println("ERROR: Invalid format for ADD");
                        }
                        continue;
                    }
                    else {
                        out.println("ERROR: Unknown command");
                    }
                }
            } catch(IOException ex) {
            } finally {
                closeConnection();
            }
        }

        private void closeConnection(){
            try {
                if(in != null) in.close();
                if(out != null) out.close();
                if(socket != null && !socket.isClosed()) socket.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> new MultiClientServer().setVisible(true));
    }
}
