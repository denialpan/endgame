package com.ddd.endgame.compat;

import com.ddd.endgame.dddsendgame;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public class GalaxyIngotJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "jei");
    private static final RecipeType<GalaxyIngotRecipe> GALAXY_INGOT_RECIPE_TYPE = RecipeType.create(
            dddsendgame.MODID,
            "galaxy_compressor",
            GalaxyIngotRecipe.class
    );

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new GalaxyIngotRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(GALAXY_INGOT_RECIPE_TYPE, List.of(new GalaxyIngotRecipe()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(dddsendgame.GALAXY_COMPRESSOR_ITEM.get(), GALAXY_INGOT_RECIPE_TYPE);
    }

    public static final class GalaxyIngotRecipe {
    }

    private static final class GalaxyIngotRecipeCategory implements IRecipeCategory<GalaxyIngotRecipe> {
        private final IDrawable background;
        private final IDrawable icon;
        private final IDrawable arrow;

        private GalaxyIngotRecipeCategory(IGuiHelper guiHelper) {
            this.background = guiHelper.createBlankDrawable(126, 54);
            this.icon = guiHelper.createDrawableItemStack(new ItemStack(dddsendgame.GALAXY_COMPRESSOR_ITEM.get()));
            this.arrow = guiHelper.createAnimatedRecipeArrow(200);
        }

        @Override
        public RecipeType<GalaxyIngotRecipe> getRecipeType() {
            return GALAXY_INGOT_RECIPE_TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.dddsendgame.galaxy_compressor");
        }

        @Override
        public IDrawable getBackground() {
            return this.background;
        }

        @Override
        public IDrawable getIcon() {
            return this.icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, GalaxyIngotRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(20, 17)
                    .setStandardSlotBackground()
                    .addItemStack(new ItemStack(dddsendgame.GALAXY_COMPRESSOR_ITEM.get()));
            builder.addOutputSlot(88, 17)
                    .setOutputSlotBackground()
                    .addItemStack(new ItemStack(dddsendgame.GALAXY_INGOT.get()));
        }

        @Override
        public void draw(GalaxyIngotRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
            this.arrow.draw(guiGraphics, 51, 18);
            Component requirements = Component.translatable("jei.dddsendgame.galaxy_compressor.requirements");
            int textX = (this.background.getWidth() - Minecraft.getInstance().font.width(requirements)) / 2;
            guiGraphics.drawString(Minecraft.getInstance().font, requirements, Math.max(0, textX), 42, 0xFF555555, false);
        }
    }
}
