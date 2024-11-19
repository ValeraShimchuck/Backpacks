package ua.valeriishymchuk.backpacks;

import cloud.commandframework.CommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.paper.PaperCommandManager;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.yaml.NodeStyle;
import ua.valeriishymchuk.backpacks.common.configuration.ConfigLoader;
import ua.valeriishymchuk.backpacks.common.configuration.builder.ConfigLoaderConfigurationBuilder;
import ua.valeriishymchuk.backpacks.common.namespacedkey.NamespacedKeyProvider;
import ua.valeriishymchuk.backpacks.common.scheduler.BukkitTaskScheduler;
import ua.valeriishymchuk.backpacks.controllers.CommandsController;
import ua.valeriishymchuk.backpacks.controllers.EventsController;
import ua.valeriishymchuk.backpacks.controllers.RecipeController;
import ua.valeriishymchuk.backpacks.repository.ConfigRepository;
import ua.valeriishymchuk.backpacks.repository.IConfigRepository;
import ua.valeriishymchuk.backpacks.services.BackpackService;
import ua.valeriishymchuk.backpacks.services.IBackpackService;

import java.util.function.Function;

public class BackpacksPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        NamespacedKeyProvider namespacedKeyProvider = new NamespacedKeyProvider(this);
        BukkitTaskScheduler bukkitTaskScheduler = new BukkitTaskScheduler(this);
        IConfigRepository configRepository = new ConfigRepository(yamlLoader());
        configRepository.reload();
        IBackpackService backpackService = new BackpackService(
                namespacedKeyProvider,
                configRepository
        );
        Bukkit.getPluginManager().registerEvents(new EventsController(backpackService, bukkitTaskScheduler), this);
        new CommandsController(setupCommandManager(), backpackService).initCommands();
        new RecipeController(backpackService, namespacedKeyProvider).registerRecipes();
    }

    private ConfigLoader yamlLoader() {
        return new ConfigLoader(
                getDataFolder(),
                ".yml",
                ConfigLoaderConfigurationBuilder.yaml()
                        .peekBuilder(b -> b.indent(2).nodeStyle(NodeStyle.BLOCK))
                        .build()
        );
    }

    @SneakyThrows
    private CommandManager<CommandSender> setupCommandManager() {
        CommandManager<CommandSender> manager = new PaperCommandManager<CommandSender>(
                this,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(),
                Function.identity()
        );
        new MinecraftExceptionHandler<CommandSender>()
                .withDefaultHandlers()
                .withHandler(
                        MinecraftExceptionHandler.ExceptionType.NO_PERMISSION,
                        (sender, exception) -> Component.text("You don't have permission to use this command! "  + exception.getMessage())
                )
                .withHandler(
                        MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX,
                        (sender, exception) -> Component.text("Invalid syntax! " + exception.getMessage())
                )
        .apply(manager, s -> s);
        return manager;
    }

}