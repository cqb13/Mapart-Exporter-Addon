package dev.cqb13.MapartExporter;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

import org.slf4j.Logger;

public class MapartExporter extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public final static Path EXPORT_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("saved_maps");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Mapart Exporter");

        LOG.info("Initialized Mapart Exporter");
    }

    @Override
    public String getPackage() {
        return "dev.cqb13.MapartExporter";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("cqb13", "mapart-exporter");
    }
}
