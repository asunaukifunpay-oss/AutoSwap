package dev.autoswap;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class AutoSwapConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("autoswap.properties");
    private static final Properties PROPERTIES = new Properties();
    private static boolean enabled = true;
    private static int keyCode = 72; // H

    static void load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) { PROPERTIES.load(reader); }
            catch (IOException ignored) { }
        }
        enabled = Boolean.parseBoolean(PROPERTIES.getProperty("enabled", "true"));
        try { keyCode = Integer.parseInt(PROPERTIES.getProperty("swapKey", "72")); }
        catch (NumberFormatException ignored) { keyCode = 72; }
    }
    static void save() {
        PROPERTIES.setProperty("enabled", Boolean.toString(enabled));
        PROPERTIES.setProperty("swapKey", Integer.toString(keyCode));
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) { PROPERTIES.store(writer, "AutoSwap settings"); }
        } catch (IOException ignored) { }
    }
    static boolean isEnabled() { return enabled; }
    static int getKeyCode() { return keyCode; }
    static void setEnabled(boolean value) { enabled = value; }
    static void setKeyCode(int value) { keyCode = value; }
    private AutoSwapConfig() { }
}
