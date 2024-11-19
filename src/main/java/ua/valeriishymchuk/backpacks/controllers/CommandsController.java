package ua.valeriishymchuk.backpacks.controllers;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.context.CommandContext;
import io.vavr.Tuple;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.valeriishymchuk.backpacks.dto.GiveItemDTO;
import ua.valeriishymchuk.backpacks.services.IBackpackService;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CommandsController {

    CommandManager<CommandSender> commandManager;
    IBackpackService backpackService;
    @NonFinal
    private Command.Builder<CommandSender> builder;

    public void initCommands() {
        builder = commandManager.commandBuilder("backpacks");
        registerCommands(
                give(),
                reload()
        );
    }

    private Command.Builder<CommandSender> reload() {
        return builder.literal("reload")
                .permission(backpackService.getReloadPermission())
                .handler(ctx -> ctx.getSender().sendMessage(backpackService.reload()));
    }

    private Command.Builder<CommandSender> give() {
        return builder.literal("give")
                .permission(backpackService.getBackpackPermission())
                .argument(
                        StringArgument.<CommandSender>builder("backpack")
                                .withSuggestionsProvider(this::backpacksSuggestion)
                )
                .argument(PlayerArgument.optional("player"))
                .handler(ctx -> {
                    String backpack = ctx.get("backpack");
                    Option<Player> playerOption = Option.when(ctx.contains("player"), () -> ctx.<Player>get("player"))
                            .orElse(tryCast(ctx.getSender(), Player.class));
                    GiveItemDTO giveItemDTO = backpackService.giveItem(backpack, playerOption.getOrNull());
                    ctx.getSender().sendMessage(giveItemDTO.getMessage());
                    playerOption
                            .flatMap(p -> giveItemDTO.getItem().map(item -> Tuple.of(p, item)))
                            .peek(tuple -> {
                                Player player = tuple._1();
                                ItemStack item = tuple._2();
                                player.getInventory().addItem(item);
                            });
                });
    }

    private <T> Option<T> tryCast(Object obj, Class<T> clazz) {
        if (clazz.isInstance(obj)) {
            return Option.of(clazz.cast(obj));
        }
        return Option.none();
    }

    private List<String> backpacksSuggestion(CommandContext<CommandSender> ctx, String input) {
        return backpackService.getAllBackpacksKeys().stream().filter(s -> s.startsWith(input)).toList();
    }

    private void registerCommand(Command.Builder<CommandSender> command) {
        commandManager.command(command.build());
    }

    @SafeVarargs
    private void registerCommands(Command.Builder<CommandSender>... commands) {
        for (Command.Builder<CommandSender> command : commands) {
            registerCommand(command);
        }
    }

}
