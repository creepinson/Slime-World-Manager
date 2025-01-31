package com.grinderwolf.swm.plugin.loaders;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.log.Logging;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class FileLoader implements SlimeLoader {

    private static final File WORLD_DIR = new File("slime_worlds");

    {
        if (WORLD_DIR.exists() && !WORLD_DIR.isDirectory()) {
            Logging.warning("A file named '" + WORLD_DIR.getName() + "' has been deleted, as this is the name used for the worlds directory.");
            WORLD_DIR.delete();
        }

        WORLD_DIR.mkdirs();
    }

    @Override
    public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException, IOException, WorldInUseException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        if (!readOnly) {
            File lockFile = new File(WORLD_DIR, worldName + ".slime_lock");

            if (lockFile.exists()) {
                throw new WorldInUseException(worldName);
            }

            lockFile.createNewFile();

            try (FileOutputStream fileStream = new FileOutputStream(lockFile);
                 DataOutputStream dataStream = new DataOutputStream(fileStream)) {
                dataStream.writeLong(System.currentTimeMillis());
            }
        }

        File file = new File(WORLD_DIR, worldName + ".slime");

        return Files.readAllBytes(file.toPath());
    }

    @Override
    public boolean worldExists(String worldName) {
        return new File(WORLD_DIR, worldName + ".slime").exists();
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld, boolean lock) throws IOException {
        File lastBackup = new File(WORLD_DIR, worldName + ".slime_old");

        if (lastBackup.exists()) {
            lastBackup.delete();
        }

        File file = new File(WORLD_DIR, worldName + ".slime");

        if (file.exists()) {
            file.renameTo(lastBackup);
        }

        try (FileOutputStream fileStream = new FileOutputStream(file, false)) {
            fileStream.write(serializedWorld);
            fileStream.flush();
            lastBackup.delete();
        }

        if (lock) {
            // Make sure the lock file is there
            File lockFile = new File(WORLD_DIR, worldName + ".slime_lock");

            if (!lockFile.exists()) {
                lockFile.createNewFile();

                try (FileOutputStream fileStream = new FileOutputStream(lockFile);
                     DataOutputStream dataStream = new DataOutputStream(fileStream)) {
                    dataStream.writeLong(System.currentTimeMillis());
                }
            }
        }
    }

    @Override
    public void unlockWorld(String worldName) throws UnknownWorldException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        File lockFile = new File(WORLD_DIR, worldName + ".slime_lock");
        lockFile.delete();
    }

    @Override
    public boolean isWorldLocked(String worldName) {
        return new File(WORLD_DIR, worldName + ".slime_lock").exists();
    }

    @Override
    public void deleteWorld(String worldName) throws UnknownWorldException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        new File(WORLD_DIR, worldName + ".slime_lock").delete();
        new File(WORLD_DIR, worldName + ".slime").delete();
    }
}
