package ua.valeriishymchuk.backpacks.entities;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import ua.valeriishymchuk.backpacks.common.component.RawComponent;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConfigSerializable
@Getter
public class ConfigEntity {


    String backpackPermission = "backpacks.commands.give";
    String reloadPermission = "backpacks.commands.reload";
    ItemConfig unavailableSlot = new ItemConfig(
            Material.RED_STAINED_GLASS_PANE,
            new RawComponent("This slot is unavailable"),
            new RawComponent(),
            0
    );
    ItemConfig nextPageButton = new ItemConfig(
            Material.ARROW,
            new RawComponent("Next page"),
            new RawComponent("Current page: %page%"),
            0
    );
    ItemConfig prevPageButton = new ItemConfig(
            Material.ARROW,
            new RawComponent("Previous page"),
            new RawComponent("Current page: %page%"),
            0
    );
    ItemConfig borderItem = new ItemConfig(
            Material.BLACK_STAINED_GLASS_PANE,
            new RawComponent(" "),
            new RawComponent(),
            0
    );

    Map<String, Backpack> backpacks = Map.of(
            "9_slot", new Backpack(
                    9,
                    new RawComponent("A small backpack"),
                    true,
                    Map.of('A', Material.LEATHER, 'B', Material.IRON_BLOCK),
                    List.of("AAA", "BBB", "AAA"),
                    new ItemConfig(
                            Material.WHITE_SHULKER_BOX,
                            new RawComponent("Backpack"),
                            new RawComponent("This is a backpack", "It can hold up to 9 items"),
                            0
                    )
            ),
            "18_slot", new Backpack(
                    18,
                    new RawComponent("A normal backpack"),
                    true,
                    Map.of('A', Material.LEATHER, 'B', Material.GOLD_BLOCK),
                    List.of("AAA", "BBB", "AAA"),
                    new ItemConfig(
                            Material.YELLOW_SHULKER_BOX,
                            new RawComponent("Backpack"),
                            new RawComponent("This is a backpack", "It can hold up to 18 items"),
                            0
                    )
            ),
            "27_slot", new Backpack(
                    27,
                    new RawComponent("A large backpack"),
                    true,
                    Map.of('A', Material.LEATHER, 'B', Material.DIAMOND_BLOCK),
                    List.of("AAA", "BBB", "AAA"),
                    new ItemConfig(
                            Material.LIGHT_BLUE_SHULKER_BOX,
                            new RawComponent("Backpack"),
                            new RawComponent("This is a backpack", "It can hold up to 27 items"),
                            0
                    )
            ),
            "36_slot", new Backpack(
                    36,
                    new RawComponent("A huge backpack"),
                    true,
                    Map.of('A', Material.LEATHER, 'B', Material.NETHERITE_BLOCK),
                    List.of("AAA", "BBB", "AAA"),
                    new ItemConfig(
                            Material.BLACK_SHULKER_BOX,
                            new RawComponent("Backpack"),
                            new RawComponent("This is a backpack", "It can hold up to 36 items"),
                            0
                    )
            )
    );

    @ConfigSerializable
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    public static class Backpack {
        int size;
        RawComponent title;
        boolean haveRecipe;
        Map<Character, Material> itemMap;
        List<String> recipe;
        ItemConfig item;

        private Backpack() {
            this(0, new RawComponent(), false, Map.of(), List.of(), new ItemConfig());
        }


    }

    @ConfigSerializable
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    public static class ItemConfig {
        Material material;
        RawComponent name;
        RawComponent lore;
        int cmd;

        private ItemConfig() {
            this(Material.AIR, new RawComponent(), new RawComponent(), 0);
        }

        public ItemStack toItemStack() {
            return toItemStack(x -> x);
        }

        public ItemStack toItemStack(UnaryOperator<RawComponent> replacer) {
            ItemStack itemStack = new ItemStack(material);
            ItemMeta meta = itemStack.getItemMeta();
            meta.displayName(replacer.apply(name).bake());
            meta.lore(replacer.apply(lore).bakeAsLore());
            meta.setCustomModelData(cmd);
            itemStack.setItemMeta(meta);
            return itemStack;
        }

    }

}
