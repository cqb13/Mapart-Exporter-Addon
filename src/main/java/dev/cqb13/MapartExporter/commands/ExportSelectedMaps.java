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
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class ExportSelectedMaps extends Command {
    public ExportSelectedMaps() {
        super("export-selected-maps", "Exports each map selected via the Mapart Selector module individually");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            String baseName = "Map";

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

            String sanitizedBase = ExportUtils.sanitizeMapName(baseName);

            Set<String> usedNames = new HashSet<>();
            int exported = 0;

            for (SelectedMapEntry entry : selected.values()) {
                MapItemSavedData mapState = MapItem.getSavedData(entry.frame.getItem(), mc.level);
                if (mapState == null) {
                    ChatUtils.sendMsg(ChatFormatting.YELLOW, "Skipping " + entry.name + " (no map data available)");
                    continue;
                }

                String itemName = entry.frame.getItem().getHoverName().getString();
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
                    ChatUtils.sendMsg(ChatFormatting.RED, "Failed to save " + filename + ": " + e.getMessage());
                }
            }

            module.clearSelection();
            ChatUtils.sendMsg(ChatFormatting.GREEN, "Exported " + exported + " map(s). Selection cleared.");
            return 1;
        });
    }
}
