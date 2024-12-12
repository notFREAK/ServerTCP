package org.example.figure;

public class Rectangle {
    private int x,y,width,height;

    public Rectangle(int x, int y, int width, int height){
        this.x=x;this.y=y;this.width=width;this.height=height;
    }

    public int getX(){return x;}
    public int getY(){return y;}
    public int getWidth(){return width;}
    public int getHeight(){return height;}

    @Override
    public String toString(){
        return "Rectangle(x="+x+", y="+y+", w="+width+", h="+height+")";
    }
}

