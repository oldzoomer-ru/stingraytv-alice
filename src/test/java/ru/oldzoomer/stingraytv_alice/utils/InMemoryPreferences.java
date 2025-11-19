package ru.oldzoomer.stingraytv_alice.utils;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

public class InMemoryPreferences extends Preferences {
    private final Map<String, Object> map = new HashMap<>();

    @Override
    public void put(String key, String value) {
        map.put(key, value);
    }

    @Override
    public String get(String key, String def) {
        return (String) map.getOrDefault(key, def);
    }

    @Override
    public void remove(String key) {
        map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public void putInt(String key, int value) {
        map.put(key, value);
    }

    @Override
    public int getInt(String key, int def) {
        return (int) map.getOrDefault(key, def);
    }

    @Override
    public void putLong(String key, long value) {
        map.put(key, value);
    }

    @Override
    public long getLong(String key, long def) {
        return (long) map.get(key);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        map.put(key, value);
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        return (boolean) map.getOrDefault(key, def);
    }

    @Override
    public void putFloat(String key, float value) {
        map.put(key, value);
    }

    @Override
    public float getFloat(String key, float def) {
        return (float) map.getOrDefault(key, def);
    }

    @Override
    public void putDouble(String key, double value) {
        map.put(key, value);
    }

    @Override
    public double getDouble(String key, double def) {
        return (double) map.getOrDefault(key, def);
    }

    @Override
    public void putByteArray(String key, byte[] value) {
        map.put(key, value);
    }

    @Override
    public byte[] getByteArray(String key, byte[] def) {
        return (byte[]) map.getOrDefault(key, def);
    }

    @Override
    public String[] keys() {
        return map.keySet().toArray(new String[0]);
    }

    @Override
    public String[] childrenNames() {
        return new String[0];
    }

    @Override
    public Preferences parent() {
        return null;
    }

    @Override
    public Preferences node(String pathName) {
        return null;
    }

    @Override
    public boolean nodeExists(String pathName) {
        return true;
    }

    @Override
    public void removeNode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public String absolutePath() {
        return "";
    }

    @Override
    public boolean isUserNode() {
        return true;
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sync() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addNodeChangeListener(NodeChangeListener ncl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeNodeChangeListener(NodeChangeListener ncl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportNode(OutputStream os) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportSubtree(OutputStream os) {
        throw new UnsupportedOperationException();
    }
}
