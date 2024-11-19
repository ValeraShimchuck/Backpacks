package ua.valeriishymchuk.backpacks.common.namespacedkey;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class NamespacedKeyProvider {

    Plugin plugin;

    public NamespacedKey create(String key) {
        return new NamespacedKey(plugin, key);
    }
}
