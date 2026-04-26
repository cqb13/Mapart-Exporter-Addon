package dev.cqb13.MapartExporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.MapColor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ExportUtils {
    private final static Random RANDOM = new Random();

    public static String sanitizeMapName(String name) {
        if (name.equals("Map")) {
            return "Map-" + randomDigits(10);
        }

        String sanitized = name.replaceAll("[<>:\"/\\\\|?*]", "_");
        sanitized = sanitized.replace("..", "_");
        sanitized = sanitized.trim().replaceAll("[. ]+$", "");

        return sanitized;
    }

    public static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static void saveImageFromMapColors(byte[] mapColors, String filename, boolean log) {
        try (NativeImage image = new NativeImage(128, 128, false)) {
            for (int i = 0; i < mapColors.length; i++) {
                image.setColorArgb(i % 128, i / 128, MapColor.getRenderColor(mapColors[i]));
            }
            saveImage(filename, image, log);
        } catch (IOException e) {
            MapartExporter.LOG.error("Error saving map:\n{}", e.toString());
        }
    }

    public static NativeImage buildCompositeImage(Map<String, byte[]> maps) {
        if (maps == null || maps.isEmpty()) {
            return null;
        }

        int minRow = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE;

        for (String key : maps.keySet()) {
            String[] parts = key.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);

            if (row < minRow) {
                minRow = row;
            }

            if (row > maxRow) {
                maxRow = row;
            }

            if (col < minCol) {
                minCol = col;
            }

            if (col > maxCol) {
                maxCol = col;
            }
        }

        int firstRow = Integer.MAX_VALUE;
        int firstColInFirstRow = Integer.MAX_VALUE;
        for (String key : maps.keySet()) {
            String[] parts = key.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);

            if (row < firstRow) {
                firstRow = row;
                firstColInFirstRow = col;
            } else if (row == firstRow && col < firstColInFirstRow) {
                firstColInFirstRow = col;
            }
        }

        boolean anyBeforeFirst = false;
        for (String key : maps.keySet()) {
            String[] parts = key.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);
            if (row < firstRow || (row == firstRow && col < firstColInFirstRow)) {
                anyBeforeFirst = true;
                break;
            }
        }

        int originRow = anyBeforeFirst ? minRow : firstRow;
        int originCol = anyBeforeFirst ? minCol : firstColInFirstRow;

        int usedCols = maxCol - originCol + 1;
        int usedRows = maxRow - originRow + 1;

        NativeImage finalImage = new NativeImage(usedCols * 128, usedRows * 128, true);

        for (int row = originRow; row <= maxRow; row++) {
            for (int col = originCol; col <= maxCol; col++) {
                byte[] colors = maps.get(row + "," + col);

                for (int i = 0; i < 128 * 128; i++) {
                    int x = i % 128;
                    int y = i / 128;

                    int globalX = (col - originCol) * 128 + x;
                    int globalY = (row - originRow) * 128 + y;

                    if (colors != null) {
                        int color = MapColor.getRenderColor(colors[i]);
                        finalImage.setColorArgb(globalX, globalY, color);
                    } else {
                        finalImage.setColorArgb(globalX, globalY, 0x00000000);
                    }
                }
            }
        }

        return finalImage;
    }

    public static void saveCompositeImage(String filename, Map<String, byte[]> maps, boolean log) throws IOException {
        NativeImage finalImage = buildCompositeImage(maps);
        if (finalImage == null) {
            throw new IOException("No maps to compose");
        }
        saveImage(filename, finalImage, log);
    }

    public static void saveImage(String filename, NativeImage image, boolean log) throws IOException {
        Path dir = MapartExporter.EXPORT_DIRECTORY;

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        if (!filename.toLowerCase().endsWith(".png")) {
            filename += ".png";
        }

        Path filePath = MapartExporter.EXPORT_DIRECTORY.resolve(filename);
        image.writeTo(filePath);

        if (!log)
            return;

        Text mapartFile = Text.literal(filename)
                .styled(style -> style
                        .withColor(Formatting.GREEN)
                        .withClickEvent(new ClickEvent.OpenFile(filePath.toAbsolutePath().toString()))
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("Open saved image")))
                        .withUnderline(true));

        ChatUtils.sendMsg(mapartFile);
    }
}
