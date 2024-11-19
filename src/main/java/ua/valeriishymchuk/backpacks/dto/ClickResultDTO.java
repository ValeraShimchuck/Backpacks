package ua.valeriishymchuk.backpacks.dto;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ClickResultDTO {

    @Getter
    boolean shouldCancel;
    @Nullable
    ItemStack[] updatedContent;
    //@Nullable
    //BackpackUpdateDTO updatedBackpack;

    public Option<ItemStack[]> getUpdatedContent() {
        return Option.of(updatedContent);
    }

    //public Option<BackpackUpdateDTO> getUpdatedBackpack() {
    //    return Option.of(updatedBackpack);
    //}

}
