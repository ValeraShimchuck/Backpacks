package ua.valeriishymchuk.backpacks.controllers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import ua.valeriishymchuk.backpacks.common.namespacedkey.NamespacedKeyProvider;
import ua.valeriishymchuk.backpacks.services.IBackpackService;

import java.util.Arrays;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RecipeController {

    IBackpackService backpackService;
    NamespacedKeyProvider namespacedKeyProvider;

    public void registerRecipes() {
        backpackService.getRecipes().forEach(recipe -> {
            ShapedRecipe bukkitRecipe = new ShapedRecipe(
                namespacedKeyProvider.create(recipe.getName()), recipe.getResult()
            );
            bukkitRecipe.shape(recipe.getRecipe().toArray(String[]::new));
            recipe.getItemMap().forEach(bukkitRecipe::setIngredient);
            Bukkit.addRecipe(bukkitRecipe);
        });
    }

}
