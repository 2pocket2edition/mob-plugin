package de.kniffo80.mobplugin;

public class AutoSpawnTask implements Runnable {
    @Override
    public void run() {
        MobPlugin.spawnMobs();
    }
}
