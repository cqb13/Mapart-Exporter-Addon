package dev.cqb13.MapartExporter;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import org.slf4j.Logger;

public class MapartExporter extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

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
