package org.example.figure;

import java.awt.Graphics2D;

public class Circle implements Shape {
    private int x, y, radius;

    public Circle(int x, int y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getRadius() { return radius; }

    @Override
    public void draw(Graphics2D g) {
        g.drawOval(x, y, radius, radius);
    }

    @Override
    public String getType() {
        return "Circle";
    }

    @Override
    public String toString() {
        return "Circle(x=" + x + ", y=" + y + ", r=" + radius + ")";
    }
}
