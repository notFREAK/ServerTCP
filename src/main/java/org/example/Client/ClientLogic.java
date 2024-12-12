package org.example.Client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.figure.Circle;
import org.example.figure.Line;
import org.example.figure.Rectangle;

import java.io.*;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClientLogic {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final String host;
    private final int port;
    private final Gson gson = new Gson();

    public ClientLogic(String host, int port) {
        this.host=host;this.port=port;
    }

    public boolean connect(){
        try{
            socket=new Socket(host,port);
            out=new PrintWriter(socket.getOutputStream(),true);
            in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return true;
        }catch(IOException ex){
            ex.printStackTrace();
            return false;
        }
    }

    public String sendCircle(Circle c){
        return sendObject("Circle", c);
    }

    public String sendRectangle(Rectangle r){
        return sendObject("Rectangle",r);
    }

    public String sendLine(Line l){
        return sendObject("Line", l);
    }

    private String sendObject(String type, Object obj){
        if(out==null) return "Not connected";
        String json=gson.toJson(obj);
        out.println(type+json);
        return readResponse();
    }

    private String readResponse(){
        if(in==null) return "No input stream";
        try{
            String resp=in.readLine();
            if(resp==null)return "No response";
            return resp;
        }catch(IOException e){
            e.printStackTrace();
            return "Error reading";
        }
    }

    public List<Object> getAllObjects() {
        if (out == null) {
            return Collections.emptyList();
        }
        out.println("GET_ALL");
        try {
            String line = in.readLine();
            if (line == null) {
                return Collections.emptyList();
            }

            Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> list = gson.fromJson(line, listType);
            return mapListToObjects(list);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private List<Object> mapListToObjects(List<Map<String, Object>> list) {
        List<Object> result = new ArrayList<>();
        for (Map<String, Object> m : list) {
            Object obj = mapToObject(m);
            if (obj != null) {
                result.add(obj);
            }
        }
        return result;
    }

    private Object mapToObject(Map<String, Object> m) {
        String type = (String) m.get("type");
        if (type == null) return null;

        Object data = m.get("data");
        if (!(data instanceof Map)) return null;

        String jsonData = gson.toJson(data);

        switch (type) {
            case "Circle":
                return gson.fromJson(jsonData, Circle.class);
            case "Rectangle":
                return gson.fromJson(jsonData, Rectangle.class);
            case "Line":
                return gson.fromJson(jsonData, Line.class);
            default:
                return null;
        }
    }
}
