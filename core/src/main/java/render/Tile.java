package render;


public class Tile {
    final int startX;
    final int startY;
    final int width;
    final int height;

    public Tile(int startX, int startY, int width, int height) {
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
    }

    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    @Override
    public String toString() {
        return "Tile{" +
               "startX=" + startX +
               ", startY=" + startY +
               ", width=" + width +
               ", height=" + height +
               '}';
    }
}