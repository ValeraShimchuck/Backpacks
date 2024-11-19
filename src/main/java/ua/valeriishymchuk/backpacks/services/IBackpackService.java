package ua.valeriishymchuk.backpacks.services;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.valeriishymchuk.backpacks.dto.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public interface IBackpackService {

    int OFFSET_HAND_SLOT = -1;
    int PLAYER_INVENTORY_SiZE = 36;

    boolean isBackpack(ItemStack item);
    OpenBackpackDTO open(ItemStack item, Player player, int itemSlot);
    ClickResultDTO click(ClickDTO clickDTO);
    boolean drag(Player player, Map<Integer, ItemStack> affectedSlots);
    BackpackUpdateDTO updateBackpack(Player player, ItemStack[] content);
    boolean hasMenu(Player player);
    void close(Player player);

    List<RecipeDTO> getRecipes();
    List<String> getAllBackpacksKeys();
    GiveItemDTO giveItem(String item, @Nullable Player player);

    String getBackpackPermission();
    String getReloadPermission();
    Component reload();

}
