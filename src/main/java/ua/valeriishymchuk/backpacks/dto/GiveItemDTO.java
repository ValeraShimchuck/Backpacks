package ua.valeriishymchuk.backpacks.dto;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GiveItemDTO {

    @Nullable
    ItemStack item;
    @Getter
    Component message;

    public Option<ItemStack> getItem() {
        return Option.of(item);
    }

}
