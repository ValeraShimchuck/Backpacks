package ua.valeriishymchuk.backpacks.dto;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class ClickDTO {
    Map<Integer, ItemStack> affectedSlots;
    @Getter(AccessLevel.NONE)
    @Nullable
    ItemStack cursor;
    Player player;
    //InventoryAction action;

    public Option<ItemStack> getCursor() {
        return Option.of(cursor);
    }
}
