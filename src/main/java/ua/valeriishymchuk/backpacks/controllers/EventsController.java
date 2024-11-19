package ua.valeriishymchuk.backpacks.controllers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ua.valeriishymchuk.backpacks.common.scheduler.BukkitTaskScheduler;
import ua.valeriishymchuk.backpacks.dto.BackpackUpdateDTO;
import ua.valeriishymchuk.backpacks.dto.ClickDTO;
import ua.valeriishymchuk.backpacks.dto.ClickResultDTO;
import ua.valeriishymchuk.backpacks.dto.OpenBackpackDTO;
import ua.valeriishymchuk.backpacks.services.IBackpackService;

import java.util.HashMap;
import java.util.Map;

import static ua.valeriishymchuk.backpacks.services.IBackpackService.PLAYER_INVENTORY_SiZE;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class EventsController implements Listener {


    IBackpackService backpackService;
    BukkitTaskScheduler scheduler;

    @EventHandler
    private void onRightClick(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        if (!backpackService.isBackpack(event.getItem())) return;
        event.setCancelled(true);
        int slot = event.getHand() == EquipmentSlot.HAND?
                event.getPlayer().getInventory().getHeldItemSlot() :
                IBackpackService.OFFSET_HAND_SLOT;
        OpenBackpackDTO dto = backpackService.open(event.getItem(), event.getPlayer(), slot);
        Inventory inventory = Bukkit.createInventory(null, dto.getSize(), dto.getTitle());
        inventory.setContents(dto.getContent());
        event.getPlayer().openInventory(inventory);
    }

    @EventHandler(ignoreCancelled = true)
    private void onDrag(InventoryDragEvent event) {
        if (!backpackService.hasMenu((Player) event.getWhoClicked())) return;
        Player player = (Player) event.getWhoClicked();
        boolean shouldCancel = backpackService.drag(
                player,
                event.getNewItems()
        );
        event.setCancelled(shouldCancel);
        if (shouldCancel) return;
        scheduler.runTask(() -> {
            BackpackUpdateDTO updateDTO = backpackService.updateBackpack(
                    player, event.getView().getTopInventory().getContents()
            );
            player.getInventory().setItem(updateDTO.getSlot(), updateDTO.getItemStack());
        });
    }

    @EventHandler(ignoreCancelled = true)
    private void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!backpackService.hasMenu(player)) return;
        Map<Integer, ItemStack> affectedSlots = new HashMap<>();
        affectedSlots.put(event.getRawSlot(), event.getCurrentItem());
        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            int slot;
            ItemStack item;
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                slot = -1;
                item = player.getInventory().getItemInOffHand();
            } else {
                slot = event.getHotbarButton() + event.getView().getTopInventory().getSize() +
                        PLAYER_INVENTORY_SiZE - 9;
                item  = event.getView().getItem(slot);
            }
            affectedSlots.put(slot, item);
        }
        ClickDTO clickDTO = new ClickDTO(affectedSlots, event.getCursor(), player);
        ClickResultDTO clickResultDTO = backpackService.click(clickDTO);
        event.setCancelled(clickResultDTO.isShouldCancel());
        clickResultDTO.getUpdatedContent().peek(content -> event.getView().getTopInventory().setContents(content));
        if (event.isCancelled()) return;
        scheduler.runTask(() -> {
            BackpackUpdateDTO updateDTO = backpackService.updateBackpack(
                    player, event.getView().getTopInventory().getContents()
            );
            player.getInventory().setItem(updateDTO.getSlot(), updateDTO.getItemStack());
        });
    }

    @EventHandler
    private void onClose(InventoryCloseEvent event) {
        backpackService.close((Player) event.getPlayer());
    }



}
