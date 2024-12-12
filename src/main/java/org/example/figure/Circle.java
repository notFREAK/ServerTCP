package org.example.figure;

public class Circle {
    private int x, y, radius;

    public Circle(int x,int y,int radius){
        this.x=x; this.y=y; this.radius=radius;
    }

    public int getX(){return x;}
    public int getY(){return y;}
    public int getRadius(){return radius;}

    @Override
    public String toString(){
        return "Circle(x="+x+", y="+y+", r="+radius+")";
    }
}
