package ua.valeriishymchuk.backpacks.services;

import io.papermc.paper.persistence.PersistentDataContainerView;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
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
                backpackInfo.type,
                backpackInfo.size > 56? 0 : null
        );
        ConfigEntity.Backpack backpack = getConfig().getBackpacks().get(backpackInfo.type);
        backpacks.put(player, backpackMenuInfo);
        return new OpenBackpackDTO(
            backpack.getTitle().bake(),
            backpackMenuInfo.getInventorySize(),
            renderContent(backpackMenuInfo)
        );
    }

    private ItemStack[] renderContent(BackpackMenuInfo backpackMenuInfo) {
        BackpackInfo backpackInfo = readBackpackInfo(backpackMenuInfo.backpack);
        ItemStack[] content = new ItemStack[backpackMenuInfo.getInventorySize()];
        int srcPos = backpackInfo.size > 56 ? backpackMenuInfo.currentPage * backpackMenuInfo.getPerPageSize() : 0;
        System.arraycopy(backpackInfo.content, srcPos, content, 0, backpackMenuInfo.getCurrentAvailableSlots());
        for (int i = 0; i < content.length; i++) {
            if (backpackMenuInfo.isBorderSlot(i)) {
                content[i] = getConfig().getBorderItem().toItemStack();
                if (backpackMenuInfo.isNextPage(i)) {
                    content[i] = getConfig().getNextPageButton()
                            .toItemStack(c -> c.replaceText("%page%", backpackMenuInfo.currentPage + 1));
                }
                if (backpackMenuInfo.isPreviousPage(i)) {
                    content[i] = getConfig().getPrevPageButton()
                            .toItemStack(c -> c.replaceText("%page%", backpackMenuInfo.currentPage + 1));
                }
            } else if (!backpackMenuInfo.isSlotAvailable(i)) {
                content[i] = getConfig().getUnavailableSlot().toItemStack();
            }
        }
        return content;
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
        List<Integer> affectedTopInventorySlots = clickDTO.getAffectedSlots().keySet().stream()
                .filter(slot -> slot < backpackMenuInfo.getInventorySize()).toList();
        ClickResultDTO allow = new ClickResultDTO(
                false,
                null
        );
        if (affectedTopInventorySlots.isEmpty()) return allow;
        int slot = affectedTopInventorySlots.getFirst();
        if (backpackMenuInfo.isSlotAvailable(slot)) return allow;
        if (!backpackMenuInfo.hasPagination()) return cancel;
        boolean isNextPage = backpackMenuInfo.isNextPage(slot);
        boolean isPrevPage = backpackMenuInfo.isPreviousPage(slot);
        if (isPrevPage || isNextPage) {
            if (isNextPage) backpackMenuInfo.currentPage++;
            else backpackMenuInfo.currentPage--;
            return new ClickResultDTO(
                true,
                    renderContent(backpackMenuInfo)
            );
        }
        return cancel;
    }

    @Override
    public boolean drag(Player player, Map<Integer, ItemStack> affectedSlots) {
        BackpackMenuInfo backpackMenuInfo = backpacks.get(player);
        if (affectedSlots.containsKey(backpackMenuInfo.backpackSlot)) return true;
        if (affectedSlots.values().stream().anyMatch(this::isBackpack)) return true;
        List<Integer> topInventorySlots = affectedSlots.keySet().stream()
                .filter(slot -> slot < backpackMenuInfo.getInventorySize()).toList();
        if (topInventorySlots.isEmpty()) return false;
        return topInventorySlots.stream()
                .anyMatch(slot -> !backpackMenuInfo.isSlotAvailable(slot) ||
                        (backpackMenuInfo.getMaxPage() != 0 && backpackMenuInfo.isBorderSlot(slot)));
    }


    @Override
    public BackpackUpdateDTO updateBackpack(Player player, ItemStack[] content) {
        BackpackMenuInfo backpackMenuInfo = backpacks.get(player);
        ItemStack[] backpackContent = readBackpackInfo(backpackMenuInfo.backpack).content;
        int start = !backpackMenuInfo.hasPagination()? 0 : backpackMenuInfo.currentPage * backpackMenuInfo.getPerPageSize();
        System.arraycopy(content,0, backpackContent, start, backpackMenuInfo.getCurrentAvailableSlots());
        ItemStack newItem = backpackMenuInfo.backpack.clone();
        writeBackpackInfo(newItem, new BackpackInfo(
                backpackMenuInfo.size,
                backpackContent,
                backpackMenuInfo.type
        ));
        backpackMenuInfo.backpack = newItem;
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
    @AllArgsConstructor
    private static class BackpackMenuInfo {
        private static final int ELEMENTS_PER_PAGE = 45;
        private static final int MAXIMUM_INVENTORY_SLOTS = 54;

        int backpackSlot;
        @NonFinal
        ItemStack backpack;
        int size;
        String type;
        @NonFinal
        @Nullable Integer currentPage;

        private int getInventorySize() {
            if (size == 0) return 0;
            if (size > MAXIMUM_INVENTORY_SLOTS) return MAXIMUM_INVENTORY_SLOTS;
            return Math.ceilDiv(size, 9) * 9;
        }

        private int getPerPageSize() {
            if (size <= MAXIMUM_INVENTORY_SLOTS) return size;
            return ELEMENTS_PER_PAGE;
        }

        private int getCurrentAvailableSlots() {
            if (size <= MAXIMUM_INVENTORY_SLOTS) return size;
            Objects.requireNonNull(currentPage);
            return Math.min(size - currentPage * ELEMENTS_PER_PAGE, ELEMENTS_PER_PAGE);
        }

        private int getMaxPage() {
            if (size <= MAXIMUM_INVENTORY_SLOTS) return 0;
            return Math.ceilDiv(size, ELEMENTS_PER_PAGE);
        }

        private boolean hasPagination() {
            return currentPage != null;
        }

        private int getContentIndex(int elementInPage) {
            Objects.requireNonNull(currentPage);
            if (elementInPage >= ELEMENTS_PER_PAGE) throw new IllegalArgumentException("Out of bounds: " + elementInPage);
            return currentPage * ELEMENTS_PER_PAGE + elementInPage;
        }

        private boolean isSlotAvailable(int slot) {
            if (size <= MAXIMUM_INVENTORY_SLOTS) {
                return slot < size;
            }
            Objects.requireNonNull(currentPage);
            return ELEMENTS_PER_PAGE * currentPage + slot < size && slot < ELEMENTS_PER_PAGE;
        }

        private boolean isBorderSlot(int slot) {
            if (size <= MAXIMUM_INVENTORY_SLOTS) {
                return false;
            }
            return slot >= 45;
        }

        private boolean isPreviousPage(int slot) {
            if (size <= MAXIMUM_INVENTORY_SLOTS) return  false;
            Objects.requireNonNull(currentPage);
            return currentPage > 0 && slot == 45;
        }

        private boolean isNextPage(int slot) {
            if (size <= MAXIMUM_INVENTORY_SLOTS) return false;
            Objects.requireNonNull(currentPage);
            return currentPage < getMaxPage() - 1 && slot == MAXIMUM_INVENTORY_SLOTS - 1;
        }

    }


    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    private static class BackpackInfo {
        int size;
        @Nullable ItemStack @NotNull [] content;
        String type;
    }



}
