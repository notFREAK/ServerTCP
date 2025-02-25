package org.example.figure;

import java.awt.Graphics2D;

public class Line implements Shape {
    private int x1, y1, x2, y2;

    public Line(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }

    @Override
    public void draw(Graphics2D g) {
        g.drawLine(x1, y1, x2, y2);
    }

    @Override
    public String getType() {
        return "Line";
    }

    @Override
    public String toString() {
        return "Line(x1=" + x1 + ", y1=" + y1 + ", x2=" + x2 + ", y2=" + y2 + ")";
    }
}
