package org.example.Server;

import com.google.gson.Gson;
import org.example.figure.Circle;
import org.example.figure.Line;
import org.example.figure.Rectangle;
import org.example.figure.Shape;
import org.example.factory.ShapeFactory;

import java.io.*;
import java.util.*;

public class ServerLogic {
    private final Map<Integer, Object> storedObjects = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Gson gson = new Gson();
    private final String JSON_FILE = "objects_tcp.json";
    private final List<ServerListener> listeners = new ArrayList<>();
    private boolean loaded = false;
    private int currentId = 0;

    public interface ServerListener {
        void onNewObjectReceived(int id, Object obj);
        void onObjectDeleted(int id);
    }

    public void addListener(ServerListener l) {
        listeners.add(l);
    }

    private void notifyNewObject(int id, Object obj) {
        for (ServerListener l : listeners) {
            l.onNewObjectReceived(id, obj);
        }
    }

    private void notifyObjectDeleted(int id) {
        for (ServerListener l : listeners) {
            l.onObjectDeleted(id);
        }
    }

    public synchronized int addObject(Object o) {
        int id = currentId++;
        storedObjects.put(id, o);
        saveObjectsToFile();
        notifyNewObject(id, o);
        return id;
    }

    public synchronized boolean deleteObject(int id) {
        if(storedObjects.containsKey(id)) {
            storedObjects.remove(id);
            saveObjectsToFile();
            notifyObjectDeleted(id);
            return true;
        }
        return false;
    }

    public synchronized Object getObject(int id) {
        return storedObjects.get(id);
    }

    public synchronized String getAllObjectsJson() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Integer, Object> entry : storedObjects.entrySet()) {
            list.add(objectToMapWithType(entry.getKey(), entry.getValue()));
        }
        return gson.toJson(list);
    }

    private Map<String, Object> objectToMapWithType(int id, Object o) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        String type;
        if (o instanceof Circle) {
            type = "Circle";
        } else if (o instanceof Rectangle) {
            type = "Rectangle";
        } else if (o instanceof Line) {
            type = "Line";
        } else {
            type = "Unknown";
        }
        map.put("type", type);
        Map<String, Object> dataMap = gson.fromJson(gson.toJson(o), Map.class);
        map.put("data", dataMap);
        return map;
    }

    private void saveObjectsToFile(){
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Integer, Object> entry : storedObjects.entrySet()) {
            list.add(objectToMapWithType(entry.getKey(), entry.getValue()));
        }
        try (Writer w = new FileWriter(JSON_FILE)) {
            gson.toJson(list, w);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void loadObjectsFromFile(){
        if (loaded) return;
        loaded = true;
        File f = new File(JSON_FILE);
        if(!f.exists()) return;
        try (Reader r = new FileReader(f)) {
            List<Map<String,Object>> list = gson.fromJson(r, ArrayList.class);
            if (list == null) return;
            for (Map<String,Object> m : list) {
                Double idDouble = (Double) m.get("id");
                int id = idDouble.intValue();
                Object obj = mapToObject(m);
                if (obj != null) {
                    storedObjects.put(id, obj);
                    notifyNewObject(id, obj);
                    if(id >= currentId) {
                        currentId = id + 1;
                    }
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private Object mapToObject(Map<String, Object> m) {
        String type = (String) m.get("type");
        if (type == null) return null;
        Object data = m.get("data");
        if (!(data instanceof Map)) return null;
        String jsonData = gson.toJson(data);
        Class<? extends Shape> clazz;
        clazz = ShapeFactory.getShapeClass(type);
        return clazz != null ? gson.fromJson(jsonData, clazz) : null;
    }
}
