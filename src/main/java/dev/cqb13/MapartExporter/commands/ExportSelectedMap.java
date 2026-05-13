package dev.cqb13.MapartExporter.commands;

import java.io.IOException;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.cqb13.MapartExporter.ExportUtils;
import dev.cqb13.MapartExporter.modules.MapartSelector;
import dev.cqb13.MapartExporter.modules.MapartSelector.SelectedMapEntry;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class ExportSelectedMap extends Command {
    public ExportSelectedMap() {
        super("export-selected-map", "Exports and links maps selected via the Mapart Selector module");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("name", StringArgumentType.string())
                .executes(context -> {
                    String baseName = context.getArgument("name", String.class);

                    MapartSelector module = Modules.get().get(MapartSelector.class);
                    if (module == null || !module.isActive()) {
                        ChatUtils.sendMsg(ChatFormatting.RED, "Mapart Selector module is not enabled.");
                        return 0;
                    }

                    Map<Integer, SelectedMapEntry> selected = module.getSelectedMaps();
                    if (selected.isEmpty()) {
                        ChatUtils.sendMsg(ChatFormatting.RED,
                                "No maps selected. Middle-click maps in item frames to select them.");
                        return 0;
                    }

                    Map<Integer, int[]> gridCoords = module.getGridCoords();
                    String sanitized = ExportUtils.sanitizeMapName(baseName);

                    Map<String, byte[]> maps = new java.util.HashMap<>();

                    for (SelectedMapEntry entry : selected.values()) {
                        int[] coords = gridCoords.get(entry.mapId);
                        if (coords == null)
                            continue;

                        MapItemSavedData mapState = MapItem.getSavedData(entry.frame.getItem(), mc.level);
                        if (mapState == null) {
                            ChatUtils.sendMsg(ChatFormatting.YELLOW,
                                    "Skipping " + entry.name + " (no map data available)");
                            continue;
                        }

                        byte[] mapColors = mapState.colors.clone();
                        maps.put(coords[0] + "," + coords[1], mapColors);
                    }

                    if (maps.isEmpty()) {
                        ChatUtils.sendMsg(ChatFormatting.RED, "No valid selected maps to export.");
                        return 0;
                    }

                    try {
                        if (maps.size() == 1) {
                            java.util.Map.Entry<String, byte[]> e = maps.entrySet().iterator().next();
                            String[] parts = e.getKey().split(",");
                            String filename = sanitized + "-" + parts[0] + "-" + parts[1];
                            ExportUtils.saveImageFromMapColors(e.getValue(), filename, true);
                        } else {
                            ExportUtils.saveCompositeImage(sanitized, maps, true);
                        }
                    } catch (IOException e) {
                        ChatUtils.sendMsg(ChatFormatting.RED, "Failed to save export: " + e.getMessage());
                        return 0;
                    }

                    module.clearSelection();
                    ChatUtils.sendMsg(ChatFormatting.GREEN, "Export complete. Selection cleared.");
                    return 1;
                }));
    }
}
