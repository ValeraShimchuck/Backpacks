package ua.valeriishymchuk.backpacks.entities;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import ua.valeriishymchuk.backpacks.common.component.RawComponent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConfigSerializable
@Getter
public class LangEntity {

    RawComponent notPlayer = new RawComponent("You must be a player to use this command");
    RawComponent backIsNotFound = new RawComponent("Backpack with key %key% is not found");
    RawComponent itemWasGiven = new RawComponent("Item %item% was given to %player%");
    RawComponent pluginWasReloaded = new RawComponent("Successfully reloaded plugin");

}
