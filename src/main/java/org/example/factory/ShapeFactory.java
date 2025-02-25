package org.example.factory;

import org.example.figure.Circle;
import org.example.figure.Line;
import org.example.figure.Rectangle;
import org.example.figure.Shape;

import java.util.Map;

public class ShapeFactory {
    public static Shape createShape(String type, Map<String, Integer> params) {
        return switch (type) {
            case "Circle" -> new Circle(params.get("x"), params.get("y"), params.get("radius"));
            case "Rectangle" -> new Rectangle(
                    params.get("x"),
                    params.get("y"),
                    params.get("width"),
                    params.get("height")
            );
            case "Line" -> new Line(
                    params.get("x1"),
                    params.get("y1"),
                    params.get("x2"),
                    params.get("y2")
            );
            default -> throw new IllegalArgumentException("Неизвестный тип фигуры: " + type);
        };
    }
}
