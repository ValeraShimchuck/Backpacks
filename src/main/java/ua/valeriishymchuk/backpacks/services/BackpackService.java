package ua.valeriishymchuk.backpacks.services;

import io.papermc.paper.persistence.PersistentDataContainerView;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.valeriishymchuk.backpacks.common.namespacedkey.NamespacedKeyProvider;
import ua.valeriishymchuk.backpacks.dto.*;
import ua.valeriishymchuk.backpacks.entities.ConfigEntity;
import ua.valeriishymchuk.backpacks.entities.LangEntity;
import ua.valeriishymchuk.backpacks.repository.IConfigRepository;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class BackpackService implements IBackpackService {

    private static final String BACKPACK_TYPE_KEY = "backpack_type";
    private static final String BACKPACK_SIZE_KEY = "backpack_size";
    private static final String BACKPACK_CONTENT_KEY = "backpack_content";

    NamespacedKeyProvider keyProvider;
    IConfigRepository configRepository;
    Map<Player, BackpackMenuInfo> backpacks = new WeakHashMap<>();

    @Override
    public boolean isBackpack(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return item.getPersistentDataContainer().has(key(BACKPACK_TYPE_KEY), PersistentDataType.STRING) &&
                item.getPersistentDataContainer().has(key(BACKPACK_SIZE_KEY), PersistentDataType.INTEGER) &&
                item.getPersistentDataContainer().has(key(BACKPACK_CONTENT_KEY), PersistentDataType.TAG_CONTAINER);
    }

    private ConfigEntity getConfig() {
        return configRepository.getConfig();
    }

    private LangEntity getLang() {
        return configRepository.getLang();
    }

    private NamespacedKey key(String key) {
        return keyProvider.create(key);
    }

    @Override
    public OpenBackpackDTO open(ItemStack item, Player player, int itemSlot) {
        BackpackInfo backpackInfo = readBackpackInfo(item);
        BackpackMenuInfo backpackMenuInfo = new BackpackMenuInfo(
                itemSlot + backpackInfo.size + PLAYER_INVENTORY_SiZE - 9,
                item,
                backpackInfo.size,
                backpackInfo.type
        );
        ConfigEntity.Backpack backpack = getConfig().getBackpacks().get(backpackInfo.type);
        backpacks.put(player, backpackMenuInfo);
        return new OpenBackpackDTO(
            backpack.getTitle().bake(),
            backpack.getSize(),
            backpackInfo.content
        );
    }

    @Override
    public ClickResultDTO click(ClickDTO clickDTO) {
        BackpackMenuInfo backpackMenuInfo = backpacks.get(clickDTO.getPlayer());
        ClickResultDTO cancel = new ClickResultDTO(
                true,
                null
        );
        if (clickDTO.getAffectedSlots().containsKey(backpackMenuInfo.backpackSlot)) return cancel;
        if (clickDTO.getAffectedSlots().values().stream().anyMatch(this::isBackpack)) return cancel;
        return new ClickResultDTO(
                false,
                null
        );
    }

    @Override
    public boolean drag(Player player, Map<Integer, ItemStack> affectedSlots) {
        BackpackMenuInfo backpackMenuInfo = backpacks.get(player);
        if (affectedSlots.containsKey(backpackMenuInfo.backpackSlot)) return true;
        return affectedSlots.values().stream().anyMatch(this::isBackpack);
    }


    @Override
    public BackpackUpdateDTO updateBackpack(Player player, ItemStack[] content) {
        BackpackMenuInfo backpackMenuInfo = backpacks.get(player);
        ItemStack newItem = backpackMenuInfo.backpack.clone();
        writeBackpackInfo(newItem, new BackpackInfo(
                backpackMenuInfo.size,
                content,
                backpackMenuInfo.type
        ));
        return new BackpackUpdateDTO(
                backpackMenuInfo.backpackSlot - PLAYER_INVENTORY_SiZE + 9 - backpackMenuInfo.size,
                newItem
        );
    }

    @Override
    public boolean hasMenu(Player player) {
        return backpacks.containsKey(player);
    }

    @Override
    public void close(Player player) {
        backpacks.remove(player);
    }

    @Override
    public List<RecipeDTO> getRecipes() {
        return getConfig().getBackpacks().entrySet().stream()
                .map(entry -> {
                    ConfigEntity.Backpack backpack = entry.getValue();
                    ItemStack itemStack = backpack.getItem().toItemStack();
                    writeBackpackInfo(itemStack, new BackpackInfo(backpack.getSize(), new ItemStack[0], entry.getKey()));
                    return new RecipeDTO(
                            entry.getKey(),
                            backpack.getItemMap(),
                            backpack.getRecipe(),
                            itemStack
                    );
                }).toList();
    }


    @Override
    public List<String> getAllBackpacksKeys() {
        return getConfig().getBackpacks().keySet().stream().toList();
    }

    @Override
    public GiveItemDTO giveItem(String item, @Nullable Player player) {
        if (player == null) return new GiveItemDTO(
                null,
                getLang().getNotPlayer().bake()
        );
        ConfigEntity.Backpack backpack = getConfig().getBackpacks().get(item);
        if (backpack == null) return new GiveItemDTO(
                null,
                getLang().getBackIsNotFound().replaceText("%key%", item).bake()
        );
        ItemStack itemStack = backpack.getItem().toItemStack();
        writeBackpackInfo(itemStack, new BackpackInfo(backpack.getSize(), new ItemStack[0], item));
        return new GiveItemDTO(
                itemStack,
                getLang().getItemWasGiven()
                        .replaceText("%player%", player.getName())
                        .replaceText("%item%", item).bake()
        );
    }

    @Override
    public String getBackpackPermission() {
        return getConfig().getBackpackPermission();
    }

    @Override
    public String getReloadPermission() {
        return getConfig().getReloadPermission();
    }

    @Override
    public Component reload() {
        configRepository.reload();
        return getLang().getPluginWasReloaded().bake();
    }

    private BackpackInfo readBackpackInfo(ItemStack item) {
        PersistentDataContainerView persistentDataContainer = item.getPersistentDataContainer();
        String type = persistentDataContainer.get(key(BACKPACK_TYPE_KEY), PersistentDataType.STRING);
        int size = persistentDataContainer.get(key(BACKPACK_SIZE_KEY), PersistentDataType.INTEGER);
        PersistentDataContainer items = persistentDataContainer.get(key(BACKPACK_CONTENT_KEY), PersistentDataType.TAG_CONTAINER);
        ItemStack[] content = new ItemStack[size];
        items.getKeys().stream()
                .map(NamespacedKey::getKey)
                .map(Integer::parseInt)
                .map(id -> {
                    NamespacedKey key = key(String.valueOf(id));
                    ItemStack itemStack = ItemStack.deserializeBytes(items.get(key, PersistentDataType.BYTE_ARRAY));
                    return Map.entry(id, itemStack);
                }).forEach(entry -> content[entry.getKey()] = entry.getValue());
        return new BackpackInfo(size, content, type);
    }

    private void writeBackpackInfo(ItemStack item, BackpackInfo backpackInfo) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        persistentDataContainer.set(key(BACKPACK_TYPE_KEY), PersistentDataType.STRING, backpackInfo.type);
        persistentDataContainer.set(key(BACKPACK_SIZE_KEY), PersistentDataType.INTEGER, backpackInfo.size);
        PersistentDataContainer items = persistentDataContainer.getAdapterContext().newPersistentDataContainer();
        for (int i = 0; i < backpackInfo.content.length; i++) {
            NamespacedKey key = key(String.valueOf(i));
            ItemStack itemStack = backpackInfo.content[i];
            if (itemStack == null) continue;
            items.set(key, PersistentDataType.BYTE_ARRAY, itemStack.serializeAsBytes());
        }
        persistentDataContainer.set(key(BACKPACK_CONTENT_KEY), PersistentDataType.TAG_CONTAINER, items);
        item.setItemMeta(meta);
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    private static class BackpackMenuInfo {
        int backpackSlot;
        ItemStack backpack;
        int size;
        String type;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    private static class BackpackInfo {
        int size;
        @Nullable ItemStack @NotNull [] content;
        String type;
    }



}
