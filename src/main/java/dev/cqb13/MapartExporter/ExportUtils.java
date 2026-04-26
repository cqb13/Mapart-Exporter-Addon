package dev.cqb13.MapartExporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
