package net.garz.firstmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

public class BiomeEffectHandler {

    private static class EffectGroup {
        String name;
        Set<RegistryKey<Biome>> biomes = new HashSet<>();
        String effectType;
        int amplifier;
        int durationTicks;
    }

    private static class ConfigData {
        int check_interval_ticks = 20;
        List<GroupEntry> groups = new ArrayList<>();

        static class GroupEntry {
            String name;
            List<String> biomes;
            EffectEntry effect;
        }

        static class EffectEntry {
            String type;
            int amplifier;
            int duration_ticks;
        }
    }

    private static final List<EffectGroup> GROUPS = new ArrayList<>();
    private static int checkIntervalTicks = 20;
    private static int tickCounter = 0;

    public static void register() {
        loadConfig();
        ServerTickEvents.START_SERVER_TICK.register((MinecraftServer server) -> {
            tickCounter++;
            if (tickCounter < checkIntervalTicks) {
                return;
            }
            tickCounter = 0;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                handlePlayer(player);
            }
        });
    }

    private static void loadConfig() {
        try {
            // 从 resource 内读取 JSON 文件
            String resourcePath = "assets/firstmod/firstmod_biome_effects.json";
            InputStream in = BiomeEffectHandler.class.getClassLoader().getResourceAsStream(resourcePath);
            if (in == null) {
                // 未找到资源，打印错误并返回
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
            checkIntervalTicks = cfg.check_interval_ticks;
            GROUPS.clear();
            for (ConfigData.GroupEntry ge : cfg.groups) {
                EffectGroup eg = new EffectGroup();
                eg.name = ge.name;
                eg.effectType = ge.effect.type;
                eg.amplifier = ge.effect.amplifier;
                eg.durationTicks = ge.effect.duration_ticks;
                for (String biomeId : ge.biomes) {
                    eg.biomes.add(RegistryKey.of(RegistryKeys.BIOME, Identifier.of(biomeId)));
                }
                GROUPS.add(eg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void handlePlayer(ServerPlayerEntity player) {
        World world = player.getWorld();
        if (world.isClient) return;
        BlockPos pos = player.getBlockPos();
        RegistryKey<Biome> biomeKey = world.getBiome(pos).getKey().orElse(null);
        if (biomeKey == null) return;
        for (EffectGroup group : GROUPS) {
            if (group.biomes.contains(biomeKey)) {
                StatusEffectInstance effect = null;
                if ("slowness".equalsIgnoreCase(group.effectType)) {
                    effect = new StatusEffectInstance(StatusEffects.SLOWNESS,
                            group.durationTicks,
                            group.amplifier,
                            false, false, true);
                } else if ("speed".equalsIgnoreCase(group.effectType)) {
                    effect = new StatusEffectInstance(StatusEffects.SPEED,
                            group.durationTicks,
                            group.amplifier,
                            false, false, true);
                }
                if (effect != null) {
                    player.addStatusEffect(effect);
                }
                return;
            }
        }
    }
}
