package org.example.Client;

import org.example.factory.ShapeFactory;
import org.example.figure.Shape;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class ClientGUI extends JFrame {
    private ClientLogic logic;

    private JComboBox<String> objectTypeBox;
    private JPanel paramsPanel;
    private CardLayout cardLayout;

    private JPanel circlePanel;
    private JTextField circleXField, circleYField, circleRadiusField;

    private JPanel rectanglePanel;
    private JTextField rectXField, rectYField, rectWidthField, rectHeightField;

    private JPanel linePanel;
    private JTextField lineX1Field, lineY1Field, lineX2Field, lineY2Field;

    private JButton sendButton, getObjectsButton;
    private JTextArea responseArea;
    private DrawingPanel drawingPanel;

    private JTextField shapeIdField;
    private JButton deleteButton, getShapeByIdButton;

    private Shape currentShape;

    public ClientGUI(){
        super("Client TCP");
        setSize(800,500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logic = new ClientLogic("localhost",12345);
        boolean connected = logic.connect();
        if(!connected){
            JOptionPane.showMessageDialog(this,"Не удалось подключиться к серверу");
            System.exit(0);
        }

        drawingPanel = new DrawingPanel();
        drawingPanel.setPreferredSize(new Dimension(400,400));
        add(drawingPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new BorderLayout());
        add(rightPanel, BorderLayout.CENTER);

        JPanel topPanel = new JPanel();
        objectTypeBox = new JComboBox<>(new String[]{"Circle","Rectangle","Line"});
        topPanel.add(new JLabel("Класс объекта:"));
        topPanel.add(objectTypeBox);
        rightPanel.add(topPanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        paramsPanel = new JPanel(cardLayout);

        circlePanel = new JPanel(new GridLayout(3,2,5,5));
        circleXField = new JTextField("100");
        circleYField = new JTextField("200");
        circleRadiusField = new JTextField("50");
        circlePanel.add(new JLabel("X:")); circlePanel.add(circleXField);
        circlePanel.add(new JLabel("Y:")); circlePanel.add(circleYField);
        circlePanel.add(new JLabel("Радиус:")); circlePanel.add(circleRadiusField);

        rectanglePanel = new JPanel(new GridLayout(4,2,5,5));
        rectXField = new JTextField("10");
        rectYField = new JTextField("10");
        rectWidthField = new JTextField("100");
        rectHeightField = new JTextField("50");
        rectanglePanel.add(new JLabel("X:")); rectanglePanel.add(rectXField);
        rectanglePanel.add(new JLabel("Y:")); rectanglePanel.add(rectYField);
        rectanglePanel.add(new JLabel("Ширина:")); rectanglePanel.add(rectWidthField);
        rectanglePanel.add(new JLabel("Высота:")); rectanglePanel.add(rectHeightField);

        linePanel = new JPanel(new GridLayout(4,2,5,5));
        lineX1Field = new JTextField("0");
        lineY1Field = new JTextField("0");
        lineX2Field = new JTextField("200");
        lineY2Field = new JTextField("200");
        linePanel.add(new JLabel("X1:")); linePanel.add(lineX1Field);
        linePanel.add(new JLabel("Y1:")); linePanel.add(lineY1Field);
        linePanel.add(new JLabel("X2:")); linePanel.add(lineX2Field);
        linePanel.add(new JLabel("Y2:")); linePanel.add(lineY2Field);

        paramsPanel.add(circlePanel, "Circle");
        paramsPanel.add(rectanglePanel, "Rectangle");
        paramsPanel.add(linePanel, "Line");

        JPanel centerParamsPanel = new JPanel();
        centerParamsPanel.add(paramsPanel);
        rightPanel.add(centerParamsPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        sendButton = new JButton("Отправить");
        responseArea = new JTextArea(5,20);
        responseArea.setEditable(false);
        bottomPanel.add(new JScrollPane(responseArea), BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(sendButton, BorderLayout.NORTH);

        getObjectsButton = new JButton("Получить все фигуры с сервера");
        buttonPanel.add(getObjectsButton, BorderLayout.SOUTH);

        JPanel commandPanel = new JPanel();
        shapeIdField = new JTextField(5);
        deleteButton = new JButton("Удалить по id");
        getShapeByIdButton = new JButton("Получить по id");
        commandPanel.add(new JLabel("ID фигуры:"));
        commandPanel.add(shapeIdField);
        commandPanel.add(deleteButton);
        commandPanel.add(getShapeByIdButton);
        buttonPanel.add(commandPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);

        objectTypeBox.addActionListener(e -> {
            String sel = (String) objectTypeBox.getSelectedItem();
            cardLayout.show(paramsPanel, sel);
            updateCurrentShape();
            drawingPanel.repaint();
        });

        DocumentListener docListener = new DocumentListener(){
            @Override
            public void insertUpdate(DocumentEvent e){ updateAndRepaint(); }
            @Override
            public void removeUpdate(DocumentEvent e){ updateAndRepaint(); }
            @Override
            public void changedUpdate(DocumentEvent e){ updateAndRepaint(); }
            private void updateAndRepaint(){
                updateCurrentShape();
                drawingPanel.repaint();
            }
        };

        circleXField.getDocument().addDocumentListener(docListener);
        circleYField.getDocument().addDocumentListener(docListener);
        circleRadiusField.getDocument().addDocumentListener(docListener);
        rectXField.getDocument().addDocumentListener(docListener);
        rectYField.getDocument().addDocumentListener(docListener);
        rectWidthField.getDocument().addDocumentListener(docListener);
        rectHeightField.getDocument().addDocumentListener(docListener);
        lineX1Field.getDocument().addDocumentListener(docListener);
        lineY1Field.getDocument().addDocumentListener(docListener);
        lineX2Field.getDocument().addDocumentListener(docListener);
        lineY2Field.getDocument().addDocumentListener(docListener);

        sendButton.addActionListener(e -> sendObjectToServer());

        getObjectsButton.addActionListener(e -> {
            List<Object> objs = logic.getAllObjects();
            showObjectsDialog(objs);
        });

        deleteButton.addActionListener(e -> {
            int id = parseIntOrZero(shapeIdField.getText());
            String response = logic.sendCommand("DELETE " + id);
            responseArea.append("Server: " + response + "\n");
        });

        getShapeByIdButton.addActionListener(e -> {
            int id = parseIntOrZero(shapeIdField.getText());
            String response = logic.sendCommand("GET " + id);
            responseArea.append("Server: " + response + "\n");
        });
    }

    private void sendObjectToServer(){
        String selected = (String) objectTypeBox.getSelectedItem();
        Map<String, Integer> params = gatherParams(selected);
        try {
            Shape shape = ShapeFactory.createShape(selected, params);
            currentShape = shape;
            drawingPanel.repaint();
            String response = logic.sendShape(shape);
            responseArea.append("Server: " + response + "\n");
        } catch (IllegalArgumentException e) {
            responseArea.append("Ошибка: " + e.getMessage() + "\n");
        }
    }

    private Map<String, Integer> gatherParams(String type) {
        Map<String, Integer> params = new HashMap<>();
        switch (type) {
            case "Circle":
                params.put("x", parseIntOrZero(circleXField.getText()));
                params.put("y", parseIntOrZero(circleYField.getText()));
                params.put("radius", parseIntOrZero(circleRadiusField.getText()));
                break;
            case "Rectangle":
                params.put("x", parseIntOrZero(rectXField.getText()));
                params.put("y", parseIntOrZero(rectYField.getText()));
                params.put("width", parseIntOrZero(rectWidthField.getText()));
                params.put("height", parseIntOrZero(rectHeightField.getText()));
                break;
            case "Line":
                params.put("x1", parseIntOrZero(lineX1Field.getText()));
                params.put("y1", parseIntOrZero(lineY1Field.getText()));
                params.put("x2", parseIntOrZero(lineX2Field.getText()));
                params.put("y2", parseIntOrZero(lineY2Field.getText()));
                break;
        }
        return params;
    }

    private int parseIntOrZero(String s) {
        try {
            return Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    private void showObjectsDialog(List<Object> objs) {
        JDialog dialog = new JDialog(this, "Список фигур на сервере", true);
        dialog.setSize(300,200);
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Object o : objs) {
            listModel.addElement(o.toString());
        }
        JList<String> list = new JList<>(listModel);
        dialog.add(new JScrollPane(list));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void updateCurrentShape() {
        String selected = (String) objectTypeBox.getSelectedItem();
        Map<String, Integer> params = gatherParams(selected);
        try {
            currentShape = ShapeFactory.createShape(selected, params);
        } catch (IllegalArgumentException e) {
            currentShape = null;
        }
    }

    class DrawingPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int leftMargin = 10, topMargin = 10;
            int drawWidth = getWidth() - 2 * leftMargin;
            int drawHeight = getHeight() - 2 * topMargin;
            g2.translate(leftMargin, topMargin);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, drawWidth, drawHeight);
            g2.setColor(Color.BLACK);
            g2.drawRect(0, 0, drawWidth, drawHeight);

            if (currentShape != null) {
                g2.setColor(Color.RED);
                currentShape.draw(g2);
            }
        }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}
