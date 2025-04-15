package render;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to divide an image area into smaller tiles.
 */
public class TileCalculator {

    /**
     * Divides the specified image dimensions into tiles of a given size.
     * The last row/column of tiles might be smaller if dimensions are not divisible.
     *
     * @param imageWidth  Total width of the image.
     * @param imageHeight Total height of the image.
     * @param tileSize    Desired size (width and height) of each tile.
     * @return A list of Tile objects covering the entire image area.
     */
    public static List<Tile> calculateTiles(int imageWidth, int imageHeight, int tileSize) {
        if (imageWidth <= 0 || imageHeight <= 0 || tileSize <= 0) {
            return List.of(); // Возвращаем пустой список для невалидных размеров
        }

        List<Tile> tiles = new ArrayList<>();
        for (int y = 0; y < imageHeight; y += tileSize) {
            for (int x = 0; x < imageWidth; x += tileSize) {
                int currentTileWidth = Math.min(tileSize, imageWidth - x);
                int currentTileHeight = Math.min(tileSize, imageHeight - y);
                tiles.add(new Tile(x, y, currentTileWidth, currentTileHeight));
            }
        }
        return tiles;
    }
}