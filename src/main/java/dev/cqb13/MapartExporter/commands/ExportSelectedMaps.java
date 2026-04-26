package dev.cqb13.MapartExporter.commands;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

public class ExportSelectedMaps extends Command {
    public ExportSelectedMaps() {
        super("export-selected-maps", "Exports each map selected via the Mapart Selector module individually");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            String baseName = "Map";

            MapartSelector module = Modules.get().get(MapartSelector.class);
            if (module == null || !module.isActive()) {
                ChatUtils.sendMsg(Formatting.RED, "Mapart Selector module is not enabled.");
                return 0;
            }

            Map<Integer, SelectedMapEntry> selected = module.getSelectedMaps();
            if (selected.isEmpty()) {
                ChatUtils.sendMsg(Formatting.RED,
                        "No maps selected. Middle-click maps in item frames to select them.");
                return 0;
            }

            String sanitizedBase = ExportUtils.sanitizeMapName(baseName);

            Set<String> usedNames = new HashSet<>();
            int exported = 0;

            for (SelectedMapEntry entry : selected.values()) {
                MapState mapState = FilledMapItem.getMapState(entry.frame.getHeldItemStack(), mc.world);
                if (mapState == null) {
                    ChatUtils.sendMsg(Formatting.YELLOW, "Skipping " + entry.name + " (no map data available)");
                    continue;
                }

                String itemName = entry.frame.getHeldItemStack().getName().getString();
                String filenameBase = (itemName == null || itemName.isEmpty()) ? sanitizedBase
                        : ExportUtils.sanitizeMapName(itemName);

                String filename = filenameBase;
                int attempts = 0;
                while (usedNames.contains(filename) && attempts < 100) {
                    filename = filenameBase + "-" + ExportUtils.randomDigits(4);
                    attempts++;
                }
                usedNames.add(filename);

                try {
                    ExportUtils.saveImageFromMapColors(mapState.colors.clone(), filename, true);
                    exported++;
                } catch (Exception e) {
                    ChatUtils.sendMsg(Formatting.RED, "Failed to save " + filename + ": " + e.getMessage());
                }
            }

            module.clearSelection();
            ChatUtils.sendMsg(Formatting.GREEN, "Exported " + exported + " map(s). Selection cleared.");
            return 1;
        });
    }
}
