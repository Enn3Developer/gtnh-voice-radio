package com.enn3developer.gtnhvoiceradio.compat;

import java.util.Collection;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.enn3developer.gtnhvoiceradio.CommonProxy;
import com.enn3developer.gtnhvoiceradio.GtnhVoiceRadio;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

/**
 * The radio's only recipe: an LV assembler job - the LV Sensor is the receiver, the steel rod the antenna. Only
 * ever loaded when GregTech is present, so nothing here links against GT on other setups.
 */
public final class GregTechRecipes {

    private GregTechRecipes() {}

    public static void register() {
        Collection<GTRecipe> added = GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Casing_LV.get(1),
                ItemList.Sensor_LV.get(1),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 2),
                new ItemStack(Blocks.noteblock),
                GTOreDictUnificator.get(OrePrefixes.stick, Materials.Steel, 1),
                GTOreDictUnificator.get(OrePrefixes.wireFine, Materials.Copper, 4))
            .itemOutputs(new ItemStack(CommonProxy.radio))
            .duration(10 * 20)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);
        // GT drops a recipe with a missing input instead of throwing - make that visible.
        if (added.isEmpty()) GtnhVoiceRadio.LOG.error("[Radio] The radio assembler recipe was rejected by GregTech");
        else GtnhVoiceRadio.LOG.info("[Radio] Registered the radio assembler recipe");
    }
}
