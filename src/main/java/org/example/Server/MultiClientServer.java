package org.example.Server;

import org.example.figure.Circle;
import org.example.figure.Line;
import org.example.figure.Rectangle;

import java.io.*;
import java.net.*;
import java.awt.*;
import javax.swing.*;

public class MultiClientServer extends JFrame implements ServerLogic.ServerListener {
    private ServerSocket serverSocket; //Серверный сокет
    private boolean running = false;   //Запущен ли сервер
    private final ServerLogic logic;   //Хранение и сериализация объектов
    //Графика
    private final DefaultListModel<String> objectListModel = new DefaultListModel<>();
    private final JList<String> objectList = new JList<>(objectListModel);
    private JButton startButton, stopButton;

    public MultiClientServer(){
        super("TCP Server port:12345");
        setSize(400,300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        logic = new ServerLogic();
        logic.addListener(this);

        startButton = new JButton("Запустить сервер");
        stopButton = new JButton("Остановить сервер");
        stopButton.setEnabled(false);

        JPanel topPanel = new JPanel();
        topPanel.add(startButton);
        topPanel.add(stopButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(objectList), BorderLayout.CENTER);

        startButton.addActionListener(e->startServer(12345));
        stopButton.addActionListener(e->stopServer());
    }

    private void startServer(int port) {
        if(running) return;
        logic.loadObjectsFromFile();
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            Thread acceptThread = new Thread(()->{
                while(running){
                    try{
                        Socket client = serverSocket.accept();
                        new ClientHandler(client).start();
                    }catch(IOException ex){
                        if(!running) break;
                    }
                }
            });
            acceptThread.start();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void stopServer(){
        running = false;
        try{
            if(serverSocket!=null && !serverSocket.isClosed()){
                serverSocket.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    @Override
    public void onNewObjectReceived(Object obj) {
        SwingUtilities.invokeLater(()->objectListModel.addElement(obj.toString()));
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        ClientHandler(Socket s){ this.socket=s; }

        @Override
        public void run(){
            try{
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(),true);

                String line;
                while((line=in.readLine())!=null && running){
                    if(line.equals("GET_ALL")){
                        String json = logic.getReceivedObjectsJson();
                        out.println(json);
                        continue;
                    }

                    int idx=line.indexOf('{');
                    if(idx>0){
                        String className=line.substring(0,idx);
                        String json=line.substring(idx);
                        Object obj=null;
                        switch(className){
                            case "Circle":
                                obj = new com.google.gson.Gson().fromJson(json, Circle.class);
                                break;
                            case "Rectangle":
                                obj = new com.google.gson.Gson().fromJson(json, Rectangle.class);
                                break;
                            case "Line":
                                obj = new com.google.gson.Gson().fromJson(json, Line.class);
                                break;
                            default:
                                out.println("ERROR: Unknown class");
                                continue;
                        }
                        if(obj!=null){
                            logic.addObject(obj);
                            out.println("OK: Received "+className);
                        }
                    } else {
                        out.println("ERROR: Invalid format");
                    }
                }
            }catch(IOException ex){
                //Клиент отключился
            }finally{
                closeConnection();
            }
        }

        private void closeConnection(){
            try{
                if(in!=null) in.close();
                if(out!=null) out.close();
                if(socket!=null && !socket.isClosed()) socket.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(()-> new MultiClientServer().setVisible(true));
    }
}
