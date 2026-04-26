package dev.cqb13.CatHack.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import dev.cqb13.CatHack.CatHack;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.MapColor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public class InventoryMapExport extends Command {
    private final static Path SAVE_MAPS_DIR = FabricLoader.getInstance().getGameDir().resolve("saved_maps");
    private final static Random RANDOM = new Random();

    public InventoryMapExport() {
        super("inventory-map-export", "Exports all maps in inventory", "inv-map-export");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            PlayerEntity player = mc.player;

            if (player == null) {
                return 0;
            }

            int count = 0;

            for (ItemStack stack : player.getInventory().getMainStacks()) {
                if (!(stack.getItem() instanceof FilledMapItem)) {
                    continue;
                }

                MapState mapState = FilledMapItem.getMapState(stack, player.getEntityWorld());
                if (mapState == null)
                    continue;

                byte[] mapColors = mapState.colors.clone();

                String rawName = stack.getName().getString();
                String filename = sanitizeName(rawName);

                try {
                    saveImageFromMapColors(player, mapColors, filename);
                    count++;
                } catch (Exception e) {
                    ChatUtils.sendMsg(Formatting.RED, "Failed to save map: " + e.getMessage());
                }
            }

            ChatUtils.sendMsg(Formatting.GREEN, "Exported " + count + " maps.");
            return SINGLE_SUCCESS;
        });
    }

    private static String sanitizeName(String name) {
        if (name.equals("Map")) {
            return "Map-" + randomDigits(10);
        }

        name = name.replace(" - ", "-");

        name = name.replace(" ", "-");

        return name;
    }

    private static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static void saveImageFromMapColors(PlayerEntity player, byte[] mapColors, String filename) {
        try (NativeImage image = new NativeImage(128, 128, false)) {

            for (int i = 0; i < mapColors.length; i++) {
                image.setColorArgb(i % 128, i / 128, MapColor.getRenderColor(mapColors[i]));
            }

            saveMapartFile(player, filename, image);

        } catch (Exception e) {
            CatHack.LOG.error("Error saving map:\n{}", e.toString());
            throw new RuntimeException(e);
        }
    }

    private static void saveMapartFile(PlayerEntity player, String filename, NativeImage image) throws IOException {
        if (!Files.exists(SAVE_MAPS_DIR)) {
            Files.createDirectories(SAVE_MAPS_DIR);
        }

        filename = ensureUniqueFilename(filename);

        Path filePath = SAVE_MAPS_DIR.resolve(filename + ".png");
        image.writeTo(filePath);

        Text mapartFile = Text.literal(filename + ".png")
                .styled(style -> style
                        .withColor(Formatting.GREEN)
                        .withClickEvent(new ClickEvent.OpenFile(filePath.toAbsolutePath().toString()))
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("Open saved image")))
                        .withUnderline(true));

        ChatUtils.sendMsg(mapartFile);
    }

    private static String ensureUniqueFilename(String baseName) {
        Path filePath = SAVE_MAPS_DIR.resolve(baseName + ".png");

        if (!Files.exists(filePath))
            return baseName;

        return baseName + "-" + randomDigits(10);
    }
}
