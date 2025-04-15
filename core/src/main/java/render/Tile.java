package render;

/**
 * Represents a rectangular tile of the image to be rendered.
 */
public class Tile {
    final int startX;
    final int startY;
    final int width;
    final int height;

    /**
     * Constructs a Tile.
     * @param startX Top-left X coordinate (pixel).
     * @param startY Top-left Y coordinate (pixel).
     * @param width Width of the tile (pixels).
     * @param height Height of the tile (pixels).
     */
    public Tile(int startX, int startY, int width, int height) {
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
    }

    // Геттеры при необходимости
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