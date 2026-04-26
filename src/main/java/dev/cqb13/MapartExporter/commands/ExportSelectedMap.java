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
import net.minecraft.command.CommandSource;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Formatting;

public class ExportSelectedMap extends Command {
    public ExportSelectedMap() {
        super("export-selected-map", "Exports maps selected via the Map Exporter module");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("name", StringArgumentType.string())
                .executes(context -> {
                    String baseName = context.getArgument("name", String.class);

                    MapartSelector module = Modules.get().get(MapartSelector.class);
                    if (module == null || !module.isActive()) {
                        ChatUtils.sendMsg(Formatting.RED, "Map Exporter module is not enabled.");
                        return 0;
                    }

                    Map<Integer, SelectedMapEntry> selected = module.getSelectedMaps();
                    if (selected.isEmpty()) {
                        ChatUtils.sendMsg(Formatting.RED,
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

                        MapState mapState = FilledMapItem.getMapState(entry.frame.getHeldItemStack(), mc.world);
                        if (mapState == null) {
                            ChatUtils.sendMsg(Formatting.YELLOW, "Skipping " + entry.name + " (no map data available)");
                            continue;
                        }

                        byte[] mapColors = mapState.colors.clone();
                        maps.put(coords[0] + "," + coords[1], mapColors);
                    }

                    if (maps.isEmpty()) {
                        ChatUtils.sendMsg(Formatting.RED, "No valid selected maps to export.");
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
                        ChatUtils.sendMsg(Formatting.RED, "Failed to save export: " + e.getMessage());
                        return 0;
                    }

                    module.clearSelection();
                    ChatUtils.sendMsg(Formatting.GREEN, "Export complete. Selection cleared.");
                    return 1;
                }));
    }
}
