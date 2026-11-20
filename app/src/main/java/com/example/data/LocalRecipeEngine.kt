package com.example.data

import com.example.model.HealthProfile
import com.example.model.ScanRecord
import com.example.model.SmartRecipe
import java.util.Locale

object LocalRecipeEngine {

    fun generateThreeBalancingRecipes(
        scan: ScanRecord,
        profile: HealthProfile
    ): List<SmartRecipe> {
        val product = scan.productName
        val negativesText = (scan.negatives.joinToString(" ") { "${it.label} ${it.value}" } + " " + scan.summary)
            .lowercase(Locale.ROOT)

        val highSodium = negativesText.contains("sodium") || negativesText.contains("salt")
        val highSugar = negativesText.contains("sugar") || negativesText.contains("syrup")
        val isVegan = profile.dietaryPreferences.any { it.equals("Vegan", true) } ||
            profile.allergies.any { it.equals("Dairy", true) }
        val isNutFree = profile.allergies.any { it.contains("peanut", true) || it.contains("nut", true) }

        val proteinSource = if (isVegan) "1/2 cup boiled chickpeas or cubed tofu" else "1/2 cup plain unsweetened curd or grilled paneer"
        val healthyFat = if (isNutFree) "1 tbsp roasted pumpkin & sunflower seeds" else "1 tbsp crushed roasted walnuts or flaxseeds"

        val sodiumNote = if (highSodium) {
            "Uses potassium-rich spinach, lemon juice, and fresh herbs with ZERO extra table salt to counterbalance sodium."
        } else {
            "Adds high-fiber greens and lean protein to slow digestion and lower overall glycemic load."
        }

        val sugarNote = if (highSugar) {
            "Pairs a small portion with soluble chia fiber, cinnamon, and protein to blunt blood sugar spikes."
        } else {
            "Balances refined calories with antioxidant-rich vegetables and omega-3 seeds."
        }

        return listOf(
            SmartRecipe(
                title = "Potassium-Rich Green Detox Bowl with $product",
                prepTime = "15 mins",
                balancingNote = sodiumNote,
                ingredients = listOf(
                    "1/2 serving of $product (skip half of any seasoning/sauce packet)",
                    "2 cups fresh baby spinach & steamed broccoli florets",
                    proteinSource,
                    "1 tbsp cold-pressed olive or mustard oil",
                    "Fresh lemon juice, grated ginger, cracked black pepper & coriander"
                ),
                steps = listOf(
                    "Prepare half the standard portion of $product using plain water and no extra added salt.",
                    "Lightly sauté spinach, broccoli, and grated ginger for 3 minutes until bright green and tender-crisp.",
                    "Fold in $product and $proteinSource so vegetables make up at least 60% of the bowl.",
                    "Finish with fresh lemon juice, black pepper, and chopped coriander for natural flavor."
                )
            ),
            SmartRecipe(
                title = "High-Fiber Gut-Balancing Skillet Toss",
                prepTime = "18 mins",
                balancingNote = sugarNote,
                ingredients = listOf(
                    "1/2 serving of $product",
                    "1/2 cup sprouted moong beans or boiled lentils",
                    "1 diced bell pepper + 1/2 cup grated carrot",
                    healthyFat,
                    "1/2 tsp turmeric, cumin seeds & roasted garlic"
                ),
                steps = listOf(
                    "Dry-roast cumin seeds and garlic in a pan, then toss in bell peppers, carrots, and sprouted moong.",
                    "Cook covered for 5 minutes with a splash of water to preserve water-soluble vitamins.",
                    "Stir in a measured portion of $product so the extra fiber slows carbohydrate absorption.",
                    "Top with $healthyFat right before serving for crunch and healthy unsaturated fats."
                )
            ),
            SmartRecipe(
                title = "Antioxidant Herb & Whole-Grain Power Plate",
                prepTime = "12 mins",
                balancingNote = "Offsets processed additives with vitamin-C rich produce, polyphenols, and clean whole-food micronutrients.",
                ingredients = listOf(
                    "Small side portion of $product",
                    "1 cup cucumber, cherry tomato & pomegranate salad",
                    "1/2 cup cooked quinoa, oats, or millet base",
                    "Fresh mint leaves, roasted flaxseed powder & squeeze of lime"
                ),
                steps = listOf(
                    "Build the base of your plate with cooked millet/quinoa and fresh cucumber-tomato-pomegranate salad.",
                    "Serve a controlled side portion of $product alongside the fresh bowl instead of eating it alone.",
                    "Sprinkle roasted flaxseed powder and chopped mint with lime juice to maximize antioxidant uptake."
                )
            )
        )
    }
}
