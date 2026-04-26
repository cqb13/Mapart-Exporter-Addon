package dev.cqb13.MapartExporter.modules;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.joml.Vector3d;

import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class MapartSelector extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Boolean> renderSelected = sgGeneral.add(new BoolSetting.Builder()
            .name("render-selected")
            .description("Highlight selected maps in the world.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> showCoords = sgGeneral.add(new BoolSetting.Builder()
            .name("show-coords")
            .description("Display relative grid coordinates above selected maps.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> showName = sgGeneral.add(new BoolSetting.Builder()
            .name("show-name")
            .description("Display map name above selected maps.")
            .defaultValue(true)
            .build());

    private final Setting<Double> nametagScale = sgGeneral.add(new DoubleSetting.Builder()
            .name("nametag-scale")
            .description("Scale of the nametag text.")
            .defaultValue(1.25)
            .min(0.5)
            .sliderMax(4)
            .visible(() -> showCoords.get() || showName.get())
            .build());

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How selected map highlights are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());

    private final Setting<SettingColor> selectedSideColor = sgColors.add(new ColorSetting.Builder()
            .name("selected-side-color")
            .description("Fill color for selected maps.")
            .defaultValue(new SettingColor(73, 107, 190, 50))
            .build());

    private final Setting<SettingColor> selectedLineColor = sgColors.add(new ColorSetting.Builder()
            .name("selected-line-color")
            .description("Outline color for selected maps.")
            .defaultValue(new SettingColor(73, 107, 190, 255))
            .build());

    private final Setting<SettingColor> nametagColor = sgColors.add(new ColorSetting.Builder()
            .name("nametag-color")
            .description("Color of the nametag text.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> showCoords.get() || showName.get())
            .build());

    private final Map<Integer, SelectedMapEntry> selectedMaps = new LinkedHashMap<>();
    private Map<Integer, int[]> gridCoordsCache = null;

    public MapartSelector() {
        super(Categories.Misc, "mapart-selector",
                "Middle-click maps in item frames to select them, then use the .export-selected-map/s commands");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();

        WHorizontalList row = list.add(theme.horizontalList()).expandX().widget();

        WButton clearBtn = row.add(theme.button("Clear Selection")).expandX().widget();
        clearBtn.action = () -> {
            selectedMaps.clear();
            gridCoordsCache = null;
        };

        return list;
    }

    @Override
    public void onDeactivate() {
        selectedMaps.clear();
        gridCoordsCache = null;
    }

    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        if (event.action != KeyAction.Press || event.button() != GLFW_MOUSE_BUTTON_MIDDLE || mc.currentScreen != null) {
            return;
        }

        HitResult hitResult = mc.crosshairTarget;
        if (!(hitResult instanceof EntityHitResult ehr)) {
            return;
        }

        Entity entity = ehr.getEntity();
        if (!(entity instanceof ItemFrameEntity frame)) {
            return;
        }

        ItemStack stack = frame.getHeldItemStack();
        if (stack.isEmpty() || stack.getItem() != Items.FILLED_MAP) {
            return;
        }

        int mapId = getMapId(stack);
        if (mapId < 0) {
            return;
        }

        if (selectedMaps.containsKey(mapId)) {
            selectedMaps.remove(mapId);
            gridCoordsCache = null;

            if (this.chatFeedback) {
                info("Deselected map " + stack.getName().getString() + " (" + selectedMaps.size() + " selected)");
            }
        } else {
            selectedMaps.put(mapId, new SelectedMapEntry(mapId, stack.getName().getString(), frame));
            gridCoordsCache = null;

            if (this.chatFeedback) {
                info("Selected map " + stack.getName().getString() + " (" + selectedMaps.size() + " selected)");
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderSelected.get() || mc.world == null || selectedMaps.isEmpty()) {
            return;
        }

        Color fill = new Color(selectedSideColor.get());
        Color outline = new Color(selectedLineColor.get());

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemFrameEntity frame)) {
                continue;
            }

            ItemStack stack = frame.getHeldItemStack();
            if (stack.isEmpty() || stack.getItem() != Items.FILLED_MAP) {
                continue;
            }

            int mapId = getMapId(stack);
            if (mapId < 0 || !selectedMaps.containsKey(mapId)) {
                continue;
            }

            Box box = frame.getBoundingBox();
            event.renderer.box(box, fill, outline, shapeMode.get(), 0);
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if ((!showCoords.get() && !showName.get()) || mc.world == null || selectedMaps.isEmpty()) {
            return;
        }

        Map<Integer, int[]> gridCoords = getGridCoords();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemFrameEntity frame)) {
                continue;
            }

            ItemStack stack = frame.getHeldItemStack();
            if (stack.isEmpty() || stack.getItem() != Items.FILLED_MAP) {
                continue;
            }

            int mapId = getMapId(stack);
            if (mapId < 0 || !selectedMaps.containsKey(mapId)) {
                continue;
            }

            SelectedMapEntry entry = selectedMaps.get(mapId);
            int[] coords = gridCoords.get(mapId);

            StringBuilder label = new StringBuilder();
            if (showName.get()) {
                label.append(entry.name);
            }

            if (showCoords.get() && coords != null) {
                if (!label.isEmpty()) {
                    label.append(" ");
                }
                label.append("[").append(coords[0]).append(",").append(coords[1]).append("]");
            }

            if (label.isEmpty()) {
                continue;
            }

            String text = label.toString();
            Vector3d vec3 = new Vector3d(entity.getX(), entity.getY(), entity.getZ());

            if (NametagUtils.to2D(vec3, nametagScale.get())) {
                NametagUtils.begin(vec3);
                TextRenderer.get().begin(1, false, true);
                double w = TextRenderer.get().getWidth(text) / 2;
                TextRenderer.get().render(text, -w, 0, nametagColor.get(), true);
                TextRenderer.get().end();
                NametagUtils.end();
            }
        }
    }

    public Map<Integer, SelectedMapEntry> getSelectedMaps() {
        return selectedMaps;
    }

    public Map<Integer, int[]> getGridCoords() {
        if (gridCoordsCache == null) {
            gridCoordsCache = computeGridCoordinates();
        }
        return gridCoordsCache;
    }

    public void clearSelection() {
        selectedMaps.clear();
        gridCoordsCache = null;
    }

    @Override
    public String getInfoString() {
        return selectedMaps.isEmpty() ? null : String.valueOf(selectedMaps.size());
    }

    private Map<Integer, int[]> computeGridCoordinates() {
        if (selectedMaps.isEmpty())
            return new HashMap<>();

        Map<Integer, int[]> result = new LinkedHashMap<>();

        for (SelectedMapEntry entry : selectedMaps.values()) {
            BlockPos pos = entry.framePos;
            int col, row;

            switch (entry.frame.getFacing()) {
                case EAST -> {
                    col = -pos.getZ();
                    row = -pos.getY();
                }
                case NORTH -> {
                    col = -pos.getX();
                    row = -pos.getY();
                }
                case WEST -> {
                    col = pos.getZ();
                    row = -pos.getY();
                }
                case SOUTH -> {
                    col = pos.getX();
                    row = -pos.getY();
                }
                case UP -> {
                    col = pos.getX();
                    row = pos.getZ();
                }
                case DOWN -> {
                    col = pos.getX();
                    row = -pos.getZ();
                }
                default -> {
                    col = pos.getX();
                    row = -pos.getY();
                }
            }

            result.put(entry.mapId, new int[] { row, col });
        }

        int minRow = result.values().stream().mapToInt(v -> v[0]).min().orElse(0);
        int minCol = result.values().stream().mapToInt(v -> v[1]).min().orElse(0);
        result.replaceAll((k, v) -> new int[] { v[0] - minRow, v[1] - minCol });

        return result;
    }

    private int getMapId(ItemStack stack) {
        if (mc.world == null || stack == null || stack.getItem() != Items.FILLED_MAP)
            return -1;
        try {
            MapIdComponent mapId = stack.get(DataComponentTypes.MAP_ID);
            return mapId != null ? mapId.id() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static class SelectedMapEntry {
        public final int mapId;
        public final String name;
        public final ItemFrameEntity frame;
        public final BlockPos framePos;

        SelectedMapEntry(int mapId, String name, ItemFrameEntity frame) {
            this.mapId = mapId;
            this.name = name;
            this.frame = frame;
            this.framePos = frame.getBlockPos();
        }
    }
}
