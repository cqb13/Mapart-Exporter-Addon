package dev.cqb13.MapartExporter.commands;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.cqb13.MapartExporter.ExportUtils;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.MapColor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.command.CommandSource;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Formatting;

public class LinkedInventoryMapExport extends Command {
    public LinkedInventoryMapExport() {
        super("export-linked-inventory-maps", "Exports connected maps in your inventory");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("name", StringArgumentType.string())
                .executes(context -> {
                    String baseName = ExportUtils.sanitizeMapName(context.getArgument("name", String.class));
                    if (mc.player == null) {
                        return 0;
                    }

                    final int cols = 9;
                    final int rows = 4;

                    Map<String, byte[]> maps = new HashMap<>();

                    for (int slot = 0; slot < 36; slot++) {
                        ItemStack stack = mc.player.getInventory().getStack(slot);

                        if (!(stack.getItem() instanceof FilledMapItem))
                            continue;

                        MapState mapState = FilledMapItem.getMapState(stack, mc.player.getEntityWorld());
                        if (mapState == null) {
                            continue;
                        }

                        int col = slot % 9;
                        int row;

                        // needed so hotbar row is at bottom
                        if (slot < 9) {
                            row = 3;
                        } else if (slot < 18) {
                            row = 0;
                        } else if (slot < 27) {
                            row = 1;
                        } else {
                            row = 2;
                        }

                        maps.put(row + "," + col, mapState.colors.clone());
                    }

                    NativeImage finalImage = new NativeImage(cols * 128, rows * 128, true);

                    for (int r = 0; r < rows; r++) {
                        for (int c = 0; c < cols; c++) {
                            byte[] colors = maps.get(r + "," + c);

                            for (int i = 0; i < 128 * 128; i++) {
                                int x = i % 128;
                                int y = i / 128;

                                int globalX = c * 128 + x;
                                int globalY = r * 128 + y;

                                if (colors != null) {
                                    int color = MapColor.getRenderColor(colors[i]);
                                    finalImage.setColorArgb(globalX, globalY, color);
                                } else {
                                    finalImage.setColorArgb(globalX, globalY, 0x00000000);
                                }
                            }
                        }
                    }

                    try {
                        ExportUtils.saveImage(baseName, finalImage, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return 0;
                    }

                    ChatUtils.sendMsg(Formatting.GREEN, "Export complete.");
                    return 1;
                }));
    }
}
