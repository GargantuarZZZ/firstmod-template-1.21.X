package net.garz.firstmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CropGrowthHandler {

    private static class GrowthGroup {
        String name;
        Set<RegistryKey<Biome>> biomes = new HashSet<>();
        double multiplier = 1.0;
    }

    private static class ConfigData {
        List<GroupEntry> groups = new ArrayList<>();

        static class GroupEntry {
            String name;
            List<String> biomes;
            double growth_multiplier;
        }
    }

    private static final List<GrowthGroup> GROUPS = new ArrayList<>();

    public static void loadConfig() {
        try {
            String resourcePath = "assets/firstmod/crop_growth_by_biome.json";
            InputStream in = CropGrowthHandler.class.getClassLoader().getResourceAsStream(resourcePath);
            if (in == null) {
                System.err.println("Resource not found: " + resourcePath);
                return;
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new GsonBuilder().create();
            ConfigData cfg = gson.fromJson(text, ConfigData.class);
            if (cfg == null) {
                System.err.println("Parsed config is null");
                return;
            }
            GROUPS.clear();
            for (ConfigData.GroupEntry ge : cfg.groups) {
                GrowthGroup gg = new GrowthGroup();
                gg.name = ge.name;
                gg.multiplier = ge.growth_multiplier;
                for (String biomeId : ge.biomes) {
                    gg.biomes.add(RegistryKey.of(RegistryKeys.BIOME, Identifier.of(biomeId)));
                }
                GROUPS.add(gg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取给定 world 中 pos 位置所处生物群系的生长倍率。
     * 若未匹配任何组，则返回 1.0（不做修改）。
     */
    public static double getMultiplier(World world, int x, int y, int z) {
        RegistryKey<Biome> biomeKey = world.getBiome(new net.minecraft.util.math.BlockPos(x, y, z))
                .getKey().orElse(null);
        if (biomeKey == null) return 1.0;
        for (GrowthGroup group : GROUPS) {
            if (group.biomes.contains(biomeKey)) {
                return group.multiplier;
            }
        }
        return 1.0;
    }
}
