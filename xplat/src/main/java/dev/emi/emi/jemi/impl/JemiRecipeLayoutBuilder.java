package dev.emi.emi.jemi.impl;

import java.util.List;

import com.google.common.collect.Lists;

import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.ISlottedWidgetFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;

public class JemiRecipeLayoutBuilder implements IRecipeLayoutBuilder {
	public final List<JemiIngredientAcceptor> ingredients = Lists.newArrayList();
	public final List<JemiRecipeSlotBuilder> slots = Lists.newArrayList();
	public boolean shapeless = false;

	@Override
	public IRecipeSlotBuilder addSlot(RecipeIngredientRole role) {
		return addSlot(role, 0, 0);
	}

	@Override
	public IRecipeSlotBuilder addSlot(RecipeIngredientRole recipeIngredientRole, int x, int y) {
		JemiRecipeSlotBuilder builder = new JemiRecipeSlotBuilder(recipeIngredientRole, x, y);
		ingredients.add(builder.acceptor);
		slots.add(builder);
		return builder;
	}

	@Override
	public IIngredientAcceptor<?> addInvisibleIngredients(RecipeIngredientRole recipeIngredientRole) {
		JemiIngredientAcceptor acceptor = new JemiIngredientAcceptor(recipeIngredientRole);
		ingredients.add(acceptor);
		return acceptor;
	}

	@Override
	public void moveRecipeTransferButton(int posX, int posY) {
	}

	@Override
	public void setShapeless() {
		shapeless = true;
	}

	@Override
	public void setShapeless(int posX, int posY) {
		shapeless = true;
	}

	@Override
	public void createFocusLink(IIngredientAcceptor<?>... slots) {
	}

	@Override
	public IRecipeSlotBuilder addSlotToWidget(RecipeIngredientRole role, ISlottedWidgetFactory<?> widgetFactory) {
		// TODO actually implement this factory method of adding slots
		return addSlot(role, 0, 0);
	}
}
