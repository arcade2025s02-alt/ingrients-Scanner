package com.example.data

import com.example.model.FactorItem
import com.example.model.HealthProfile
import com.example.model.HealthierAlternative
import com.example.model.IngredientDetail
import com.example.model.IngredientRiskLevel
import com.example.model.ProductCategory
import com.example.model.ScanRecord
import com.example.model.scoreToRatingLabel
import java.util.Locale

private data class KnowledgeEntry(
    val keywords: List<String>,
    val categoryHint: ProductCategory?,
    val commonName: String,
    val chemicalName: String,
    val riskLevel: IngredientRiskLevel,
    val scoreImpact: Int,
    val pros: String,
    val cons: String,
    val shortTermEffects: String,
    val longTermEffects: String,
    val allergenTags: List<String> = emptyList(),
    val conditionTags: List<String> = emptyList(),
    val chemicalTags: List<String> = emptyList(),
    val nonVeg: Boolean = false,
    val nonVegan: Boolean = false
)

object LocalRiskAnalyzerEngine {

    private val knowledgeBase: List<KnowledgeEntry> = listOf(
        // === PACKAGED FOOD HIGH & MODERATE RISK ===
        KnowledgeEntry(
            keywords = listOf("e621", "ins 621", "monosodium glutamate", "msg"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Monosodium Glutamate (MSG)",
            chemicalName = "INS 621 / C5H8NO4Na",
            riskLevel = IngredientRiskLevel.MODERATE_RISK,
            scoreImpact = -10,
            pros = "Enhances savory umami taste, allowing food makers to boost flavor intensity.",
            cons = "Contributes hidden sodium and frequently masks ultra-processed low-nutrient formulations.",
            shortTermEffects = "May trigger mild headache, flushing, or thirst in MSG-sensitive individuals.",
            longTermEffects = "Habitual consumption of ultra-processed MSG foods correlates with high sodium load.",
            conditionTags = listOf("hypertension", "heart disease", "asthma"),
            chemicalTags = listOf("msg", "e621", "flavor enhancer")
        ),
        KnowledgeEntry(
            keywords = listOf("e635", "ins 635", "disodium ribonucleotides", "disodium inosinate", "disodium guanylate"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Disodium 5'-Ribonucleotides",
            chemicalName = "INS 635 (Inosinate & Guanylate)",
            riskLevel = IngredientRiskLevel.MODERATE_RISK,
            scoreImpact = -9,
            pros = "Synergistically amplifies savory flavor in instant snacks and noodles.",
            cons = "Purine-rich synthetic flavor potentiator often paired with MSG.",
            shortTermEffects = "Occasional skin irritation or itchiness in sensitive consumers.",
            longTermEffects = "Not recommended in excess for individuals managing uric acid or gout.",
            chemicalTags = listOf("msg", "flavor enhancer", "artificial preservatives")
        ),
        KnowledgeEntry(
            keywords = listOf("e211", "ins 211", "sodium benzoate"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Sodium Benzoate",
            chemicalName = "INS 211 / C7H5NaO2",
            riskLevel = IngredientRiskLevel.HIGH_RISK,
            scoreImpact = -14,
            pros = "Prevents mold, yeast, and bacterial spoilage in acidic foods and sauces.",
            cons = "Can react with ascorbic acid (Vitamin C) under heat/light to form trace benzene.",
            shortTermEffects = "May irritate stomach lining or aggravate asthma and hives in sensitive people.",
            longTermEffects = "Linked in WHO/EFSA reviews to hyperactivity concerns when paired with synthetic dyes.",
            conditionTags = listOf("asthma", "hypertension"),
            chemicalTags = listOf("sodium benzoate", "artificial preservatives", "preservative")
        ),
        KnowledgeEntry(
            keywords = listOf("e150d", "ins 150d", "caramel color", "caramel colour", "sulfite ammonia caramel"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Sulfite Ammonia Caramel (Class IV)",
            chemicalName = "INS 150d (4-MEI byproduct risk)",
            riskLevel = IngredientRiskLevel.HIGH_RISK,
            scoreImpact = -12,
            pros = "Provides consistent dark brown visual coloring in sodas, sauces, and noodles.",
            cons = "Purely cosmetic additive; manufacturing can produce 4-methylimidazole (4-MEI).",
            shortTermEffects = "Generally minimal acute effects, though sulfite-sensitive users may react.",
            longTermEffects = "WHO/IARC lists 4-MEI as possibly carcinogenic at high cumulative exposures.",
            allergenTags = listOf("sulfites"),
            chemicalTags = listOf("artificial dyes", "caramel")
        ),
        KnowledgeEntry(
            keywords = listOf("e102", "ins 102", "tartrazine", "yellow 5", "e110", "sunset yellow", "e129", "allura red"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Synthetic Azo Food Dye",
            chemicalName = "INS 102 / 110 / 129 (Petroleum-derived colorant)",
            riskLevel = IngredientRiskLevel.HIGH_RISK,
            scoreImpact = -15,
            pros = "Adds bright shelf-stable coloring to candies, drinks, and snacks.",
            cons = "Zero nutritional benefit; flagged by EFSA and FSSAI safety reviews for child attention sensitivity.",
            shortTermEffects = "Can trigger allergic rashes, asthma flares, or restlessness in children.",
            longTermEffects = "Chronic intake of synthetic azo dyes is discouraged by pediatric nutrition bodies.",
            allergenTags = listOf("artificial dyes"),
            conditionTags = listOf("asthma"),
            chemicalTags = listOf("artificial dyes")
        ),
        KnowledgeEntry(
            keywords = listOf("e951", "ins 951", "aspartame", "e950", "acesulfame"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Aspartame / Artificial Sweetener",
            chemicalName = "INS 951 (L-aspartyl-L-phenylalanine methyl ester)",
            riskLevel = IngredientRiskLevel.HIGH_RISK,
            scoreImpact = -13,
            pros = "Delivers intense sweetness with virtually zero calories.",
            cons = "WHO 2023 guidance advises against non-sugar sweeteners for long-term weight control.",
            shortTermEffects = "Headaches or digestive discomfort in sensitive individuals; unsafe for PKU.",
            longTermEffects = "IARC classified aspartame as Group 2B (possibly carcinogenic) with potential gut microbiome shifts.",
            chemicalTags = listOf("aspartame", "artificial sweetener")
        ),
        KnowledgeEntry(
            keywords = listOf("e250", "ins 250", "sodium nitrite", "sodium nitrate"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Sodium Nitrite",
            chemicalName = "INS 250 / NaNO2",
            riskLevel = IngredientRiskLevel.HIGH_RISK,
            scoreImpact = -16,
            pros = "Inhibits Clostridium botulinum and preserves pink color in cured meats.",
            cons = "Can form carcinogenic nitrosamines when cooked at high temperatures.",
            shortTermEffects = "May cause flushing or headaches in nitrate-sensitive individuals.",
            longTermEffects = "WHO/IARC links high nitrite-cured meat intake to elevated colorectal cancer risk.",
            conditionTags = listOf("heart disease", "hypertension"),
            chemicalTags = listOf("artificial preservatives", "synthetic nitrates")
        ),
        KnowledgeEntry(
            keywords = listOf("palm oil", "palmolein", "refined palm"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Refined Palm Oil",
            chemicalName = "Elaeis guineensis oil (~50% saturated palmitic acid)",
            riskLevel = IngredientRiskLevel.MODERATE_RISK,
            scoreImpact = -11,
            pros = "Heat-stable frying oil that extends crispiness and shelf life at low cost.",
            cons = "High in saturated palmitic acid; refining at high heat can generate 3-MCPD esters.",
            shortTermEffects = "Heavy caloric density with low satiety when combined with refined carbs.",
            longTermEffects = "Excess saturated fat elevates LDL cholesterol and cardiovascular risk per WHO guidelines.",
            conditionTags = listOf("heart disease", "diabetes"),
            chemicalTags = listOf("palm oil")
        ),
        KnowledgeEntry(
            keywords = listOf("hydrogenated", "trans fat", "vanaspati", "partially hydrogenated"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Hydrogenated Vegetable Fat (Trans Fat)",
            chemicalName = "Industrial Trans Fatty Acids (iTFA)",
            riskLevel = IngredientRiskLevel.HIGH_RISK,
            scoreImpact = -20,
            pros = "Creates flaky bakery texture and long room-temperature stability.",
            cons = "WHO & FSSAI strongly target global elimination of industrial trans fats.",
            shortTermEffects = "Promotes systemic vascular inflammation even in small recurring servings.",
            longTermEffects = "Raises LDL while lowering protective HDL cholesterol, sharply increasing heart disease risk.",
            conditionTags = listOf("heart disease", "diabetes", "hypertension")
        ),
        KnowledgeEntry(
            keywords = listOf("maida", "refined wheat flour", "bleached flour", "enriched wheat flour"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Refined Wheat Flour (Maida)",
            chemicalName = "Endosperm Triticum aestivum starch (bran & germ stripped)",
            riskLevel = IngredientRiskLevel.MODERATE_RISK,
            scoreImpact = -10,
            pros = "Provides soft texture and quick energy in baked goods and noodles.",
            cons = "Stripped of natural fiber, B-vitamins, and minerals; high glycemic index (~71).",
            shortTermEffects = "Causes rapid blood glucose spike followed by energy crash.",
            longTermEffects = "Frequent replacement of whole grains increases insulin resistance and weight gain risk.",
            allergenTags = listOf("gluten"),
            conditionTags = listOf("diabetes", "celiac disease", "pcos"),
            chemicalTags = listOf("gluten")
        ),
        KnowledgeEntry(
            keywords = listOf("high fructose corn syrup", "hfcs", "corn syrup", "liquid glucose", "invert sugar", "added sugar", "sugar", "sucrose"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Added Refined Sugar / Syrup",
            chemicalName = "Sucrose / Fructose-Glucose Syrup (C12H22O11)",
            riskLevel = IngredientRiskLevel.MODERATE_RISK,
            scoreImpact = -10,
            pros = "Adds palatability, browning, and natural preservation in jams or desserts.",
            cons = "WHO recommends limiting free sugars to <10% (ideally <5%) of daily energy intake.",
            shortTermEffects = "Sharp blood glucose and insulin spike; dental enamel acid exposure.",
            longTermEffects = "Chronic excess increases risk of type 2 diabetes, fatty liver, and dental caries.",
            conditionTags = listOf("diabetes", "pcos", "heart disease")
        ),
        KnowledgeEntry(
            keywords = listOf("iodized salt", "salt", "sodium chloride", "sodium"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Salt / Sodium",
            chemicalName = "Sodium Chloride (NaCl)",
            riskLevel = IngredientRiskLevel.MODERATE_RISK,
            scoreImpact = -7,
            pros = "Essential electrolyte supporting nerve function; iodized salt prevents iodine deficiency.",
            cons = "WHO recommends <2,000 mg sodium/day (<5g salt); packaged snacks often exceed 40% in one pack.",
            shortTermEffects = "Fluid retention, increased thirst, and temporary blood pressure elevation.",
            longTermEffects = "Chronic high sodium intake is a leading driver of hypertension, stroke, and kidney strain.",
            conditionTags = listOf("hypertension", "kidney disease", "heart disease")
        ),
        KnowledgeEntry(
            keywords = listOf("e451", "ins 451", "triphosphate", "e500", "ins 500", "acidity regulator"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Phosphate / Carbonate Acidity Regulators",
            chemicalName = "INS 451 / INS 500 (Sodium Triphosphate & Carbonates)",
            riskLevel = IngredientRiskLevel.LOW_RISK,
            scoreImpact = -4,
            pros = "Maintains pH stability, moisture retention, and noodle springiness.",
            cons = "Inorganic phosphate additives are absorbed at >90% efficiency compared to natural phosphorus.",
            shortTermEffects = "Generally well tolerated in healthy adults at FSSAI limits.",
            longTermEffects = "Excess inorganic phosphates can burden chronic kidney disease patients.",
            conditionTags = listOf("kidney disease")
        ),
        KnowledgeEntry(
            keywords = listOf("groundnut", "peanut"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Peanuts / Groundnut Protein",
            chemicalName = "Arachis hypogaea legume protein",
            riskLevel = IngredientRiskLevel.RISK_FREE,
            scoreImpact = +6,
            pros = "Rich in plant protein, heart-healthy monounsaturated fats, and vitamin E.",
            cons = "Major top-8 global allergen capable of causing severe reactions in allergic individuals.",
            shortTermEffects = "Sustained satiety for non-allergic users; severe allergic risk if peanut-allergic.",
            longTermEffects = "Regular moderate intake supports cardiovascular health in non-allergic people.",
            allergenTags = listOf("peanuts")
        ),
        KnowledgeEntry(
            keywords = listOf("almond", "cashew", "walnut", "hazelnut", "pistachio", "tree nut"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Tree Nuts",
            chemicalName = "Natural unsaturated lipid & plant protein matrix",
            riskLevel = IngredientRiskLevel.RISK_FREE,
            scoreImpact = +8,
            pros = "Dense in omega fatty acids, magnesium, vitamin E, and dietary fiber.",
            cons = "Calorie-dense and a major allergen for tree-nut-sensitive individuals.",
            shortTermEffects = "Steady energy release without glycemic spikes.",
            longTermEffects = "Associated with lower LDL cholesterol and improved metabolic markers.",
            allergenTags = listOf("tree nuts")
        ),
        KnowledgeEntry(
            keywords = listOf("milk", "whey", "casein", "curd", "yogurt", "butter", "cheese", "dairy", "lactose"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Dairy / Milk Solids",
            chemicalName = "Casein, Whey Proteins & Lactose",
            riskLevel = IngredientRiskLevel.RISK_FREE,
            scoreImpact = +5,
            pros = "Complete high-biological-value protein and bioavailable calcium.",
            cons = "Unsuitable for milk allergy, lactose intolerance, or vegan diets.",
            shortTermEffects = "Bloating or digestive upset only in lactose-intolerant or milk-allergic users.",
            longTermEffects = "Supports bone mineral density and muscle recovery.",
            allergenTags = listOf("dairy"),
            nonVegan = true
        ),
        KnowledgeEntry(
            keywords = listOf("soy", "soya", "lecithin", "tofu"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Soy / Soy Lecithin",
            chemicalName = "Glycine max protein / Phosphatidylcholine",
            riskLevel = IngredientRiskLevel.RISK_FREE,
            scoreImpact = +4,
            pros = "Complete plant protein and natural phospholipid emulsifier.",
            cons = "Common food allergen; some individuals with thyroid issues monitor heavy unfermented soy.",
            shortTermEffects = "Well tolerated unless soy-allergic.",
            longTermEffects = "Heart-healthy plant protein alternative.",
            allergenTags = listOf("soy")
        ),
        KnowledgeEntry(
            keywords = listOf("oats", "rolled oats", "ragi", "millet", "foxtail", "jowar", "bajra", "quinoa", "whole wheat", "brown rice"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Whole Grains / Millets",
            chemicalName = "Intact Cereal Grain (Bran, Germ & Beta-Glucan Fiber)",
            riskLevel = IngredientRiskLevel.RISK_FREE,
            scoreImpact = +12,
            pros = "Rich in soluble & insoluble fiber, complex carbohydrates, iron, and B-vitamins.",
            cons = "Whole wheat contains gluten; naturally gluten-free millets/oats avoid this issue.",
            shortTermEffects = "Slow, steady glucose release and lasting fullness.",
            longTermEffects = "WHO strongly recommends whole grains to lower diabetes and cardiovascular disease risk."
        ),
        KnowledgeEntry(
            keywords = listOf("chia", "flax", "spinach", "lentil", "chickpea", "moong", "turmeric", "ginger"),
            categoryHint = ProductCategory.PACKAGED_FOOD,
            commonName = "Whole Plant Superfood / Pulse",
            chemicalName = "Natural Phytonutrient & Dietary Fiber Source",
            riskLevel = IngredientRiskLevel.RISK_FREE,
            scoreImpact = +10,
            pros = "High antioxidants, plant protein, potassium, and prebiotic fiber.",
            cons = "Minimal risk; thoroughly clean and minimally processed.",
            shortTermEffects = "Supports digestion and stable energy.",
            longTermEffects = "Supports gut diversity and metabolic longevity."
        ),

        // === AGRICULTURAL FERTILIZERS ===
        KnowledgeEntry(
            keywords = listOf("ammonium nitrate", "npk", "urea", "nitrate"),
            categoryHint = ProductCategory.AGRICULTURAL_FERTILIZER,
            commonName = "Synthetic Nitrogen (Ammonium Nitrate / Urea)",
            chemicalName = "NH4NO3 / CO(NH2)2",
            riskLevel = IngredientRiskLevel.HIGH_RISK,
            scoreImpact = -18,
            pros = "Delivers rapid plant-available nitrogen for fast vegetative leaf growth.",
            cons = "High groundwater nitrate leaching and eutrophication risk per EPA water quality standards.",
            shortTermEffects = "Can burn tender crop roots if over-applied; skin/eye irritation during handling.",
            longTermEffects = "Soil acidification, loss of beneficial soil microbes, and drinking water nitrate contamination.",
            chemicalTags = listOf("synthetic nitrates", "synthetic fertilizers")
        ),
        KnowledgeEntry(
            keywords = listOf("monoammonium phosphate", "diammonium phosphate", "dap", "superphosphate", "phosphate"),
            categoryHint = ProductCategory.AGRICULTURAL_FERTILIZER,
            commonName = "Synthetic Phosphate Salt (MAP / DAP)",
            chemicalName = "NH4H2PO4 / (NH4)2HPO4",
            riskLevel = IngredientRiskLevel.MODERATE_RISK,
            scoreImpact = -12,
            pros = "Promotes early root development and flowering.",
            cons = "Excess runoff causes algal blooms; rock-derived phosphates may carry trace heavy metals (cadmium).",
            shortTermEffects = "Dust inhalation or eye irritation if applied without protective gloves/mask.",
            longTermEffects = "Cumulative cadmium buildup in agricultural soils and aquatic oxygen depletion.",
            chemicalTags = listOf("synthetic fertilizers")
        ),
        KnowledgeEntry(
            keywords = listOf("potassium chloride", "muriate of potash", "mop"),
            categoryHint = ProductCategory.AGRICULTURAL_FERTILIZER,
            commonName = "Potassium Chloride (MOP)",
            chemicalName = "KCl (High-Salt Index Potash)",
            riskLevel = IngredientRiskLevel.MODERATE_RISK,
            scoreImpact = -10,
            pros = "Economical source of potassium for crop water regulation and fruit fill.",
            cons = "High chloride salt index harms chloride-sensitive crops and earthworm populations.",
            shortTermEffects = "Soil salinity spike immediately after heavy application.",
            longTermEffects = "Long-term soil salinization and reduced microbial biodiversity in arid soils.",
            chemicalTags = listOf("synthetic fertilizers")
        ),
        KnowledgeEntry(
            keywords = listOf("edta", "synthetic chelate", "copper sulfate", "sodium borate", "chemical dye"),
            categoryHint = ProductCategory.AGRICULTURAL_FERTILIZER,
            commonName = "Synthetic Chelates & Trace Metal Salts",
            chemicalName = "EDTA / CuSO4 / Na2B4O7",
            riskLevel = IngredientRiskLevel.HIGH_RISK,
            scoreImpact = -15,
            pros = "Keeps micronutrients soluble across wide soil pH ranges.",
            cons = "EDTA resists biodegradation and can mobilize toxic heavy metals in waterways per EPA assessments.",
            shortTermEffects = "Copper sulfate is acutely toxic to aquatic fish and beneficial amphibians.",
            longTermEffects = "Persistent soil and water accumulation of non-biodegradable synthetic ligands.",
            chemicalTags = listOf("synthetic fertilizers", "artificial dyes")
        ),
        KnowledgeEntry(
            keywords = listOf("vermicompost", "neem cake", "seaweed", "compost", "humic", "bio-fertilizer", "organic manure"),
            categoryHint = ProductCategory.AGRICULTURAL_FERTILIZER,
            commonName = "Organic Bio-Fertilizer / Vermicompost",
            chemicalName = "Humic-Rich Microbial Organic Matter",
            riskLevel = IngredientRiskLevel.RISK_FREE,
            scoreImpact = +16,
            pros = "Slow-release balanced nutrition that builds soil carbon, water retention, and beneficial microbes.",
            cons = "Slower initial nutrient release compared to synthetic salts.",
            shortTermEffects = "Non-burning and safe for farmers, pollinators, and earthworms.",
            longTermEffects = "Regenerates long-term soil fertility with zero toxic waterway runoff."
        ),

        // === PACKAGING MATERIALS ===
        KnowledgeEntry(
            keywords = listOf("bpa", "bisphenol", "polycarbonate", "#7", "resin code #7", "resin 7"),
            categoryHint = ProductCategory.PACKAGING_MATERIAL,
            commonName = "Polycarbonate Plastic / Bisphenol-A (BPA)",
            chemicalName = "Resin Code #7 (PC) / C15H16O2",
            riskLevel = IngredientRiskLevel.HIGH_RISK,
            scoreImpact = -24,
            pros = "Rigid, shatter-resistant transparent plastic for reusable bottles and containers.",
            cons = "Recognized endocrine disruptor; leaches significantly faster into hot, acidic, or fatty foods.",
            shortTermEffects = "Elevated urinary BPA biomarkers after consuming heated food from PC containers.",
            longTermEffects = "WHO/EFSA link chronic BPA exposure to hormonal, metabolic, and reproductive risks.",
            conditionTags = listOf("pcos", "thyroid", "diabetes"),
            chemicalTags = listOf("bpa", "phthalates")
        ),
        KnowledgeEntry(
            keywords = listOf("phthalate", "dehp", "pvc", "#3", "polyvinyl chloride", "plasticizer"),
            categoryHint = ProductCategory.PACKAGING_MATERIAL,
            commonName = "Phthalate Plasticizers / PVC",
            chemicalName = "DEHP / Resin Code #3 (Polyvinyl Chloride)",
            riskLevel = IngredientRiskLevel.HIGH_RISK,
            scoreImpact = -22,
            pros = "Makes plastic films and wraps flexible and clingy.",
            cons = "Not chemically bound to plastic—migrates readily into oily foods; toxic dioxins if incinerated.",
            shortTermEffects = "Rapid dietary uptake when contacting warm or fatty takeout meals.",
            longTermEffects = "Associated with endocrine disruption, liver stress, and developmental toxicity per EPA.",
            chemicalTags = listOf("bpa", "phthalates")
        ),
        KnowledgeEntry(
            keywords = listOf("pfas", "grease-resistant", "fluorochemical", "ptfe", "non-stick liner"),
            categoryHint = ProductCategory.PACKAGING_MATERIAL,
            commonName = "PFAS ('Forever Chemicals') Liner",
            chemicalName = "Per- and Polyfluoroalkyl Substances",
            riskLevel = IngredientRiskLevel.HIGH_RISK,
            scoreImpact = -22,
            pros = "Repels oil, grease, and moisture on paper takeout boxes and microwave bags.",
            cons = "Ultra-persistent environmental pollutants that bioaccumulate in human blood and soil.",
            shortTermEffects = "Transfers into hot greasy foods within minutes.",
            longTermEffects = "EPA links PFAS exposure to immune suppression, elevated cholesterol, and thyroid disruption.",
            conditionTags = listOf("thyroid", "kidney disease"),
            chemicalTags = listOf("pfas")
        ),
        KnowledgeEntry(
            keywords = listOf("polystyrene", "styrofoam", "#6", "expanded polystyrene"),
            categoryHint = ProductCategory.PACKAGING_MATERIAL,
            commonName = "Polystyrene Foam (Styrofoam)",
            chemicalName = "Resin Code #6 (PS / Styrene monomer)",
            riskLevel = IngredientRiskLevel.HIGH_RISK,
            scoreImpact = -18,
            pros = "Lightweight thermal insulation for hot cups and takeout trays.",
            cons = "Leaches styrene into hot drinks; breaks into persistent microplastics; rarely recycled.",
            shortTermEffects = "Chemical taste and styrene leaching when holding boiling liquids.",
            longTermEffects = "IARC lists styrene as probably carcinogenic; severe marine microplastic persistence."
        ),
        KnowledgeEntry(
            keywords = listOf("pet", "pete", "#1", "polyethylene terephthalate"),
            categoryHint = ProductCategory.PACKAGING_MATERIAL,
            commonName = "PET Plastic (#1)",
            chemicalName = "Resin Code #1 (Polyethylene Terephthalate)",
            riskLevel = IngredientRiskLevel.MODERATE_RISK,
            scoreImpact = -8,
            pros = "Lightweight, widely recyclable, and BPA-free for single-use cold beverages.",
            cons = "Intended for single use only; heat or sun exposure increases antimony and microplastic shedding.",
            shortTermEffects = "Minimal for cold single use; avoid refilling or leaving in hot cars.",
            longTermEffects = "Repeated reuse degrades polymer chains, shedding microplastics."
        ),
        KnowledgeEntry(
            keywords = listOf("hdpe", "#2", "polypropylene", "#5", "pp plastic"),
            categoryHint = ProductCategory.PACKAGING_MATERIAL,
            commonName = "Food-Grade HDPE (#2) / Polypropylene (#5)",
            chemicalName = "Resin Code #2 / #5 (BPA-Free Polyolefin)",
            riskLevel = IngredientRiskLevel.LOW_RISK,
            scoreImpact = +4,
            pros = "Safer BPA-free plastic resins with higher heat tolerance and good recyclability.",
            cons = "Still petroleum-derived; avoid high-heat microwaving to minimize microplastic release.",
            shortTermEffects = "Low leaching at ambient temperatures.",
            longTermEffects = "Significantly lower chemical migration than #3 PVC, #6 PS, or #7 PC."
        ),
        KnowledgeEntry(
            keywords = listOf("bagasse", "sugarcane", "borosilicate", "glass", "stainless steel", "kraft paper", "compostable"),
            categoryHint = ProductCategory.PACKAGING_MATERIAL,
            commonName = "Inert / Plant-Based Eco Packaging",
            chemicalName = "PFAS-Free Cellulose Fiber / Borosilicate Glass / 304 Steel",
            riskLevel = IngredientRiskLevel.RISK_FREE,
            scoreImpact = +18,
            pros = "Zero BPA, zero phthalates, zero microplastic shedding; compostable or infinitely recyclable.",
            cons = "Slightly heavier (glass/steel) or higher upfront cost than cheap disposable plastics.",
            shortTermEffects = "Completely inert food contact even with hot or acidic dishes.",
            longTermEffects = "Eliminates dietary plasticizer exposure and landfill polymer persistence."
        )
    )

    fun analyze(
        productNameHint: String,
        ingredientText: String,
        hasAttachedPhoto: Boolean,
        profile: HealthProfile
    ): ScanRecord {
        val combinedRaw = "$productNameHint\n$ingredientText".trim()
        val combinedLower = combinedRaw.lowercase(Locale.ROOT)

        // 1. Auto-detect Category
        val category = detectCategory(combinedLower)

        // 2. Match known entries from our WHO/FSSAI/EPA knowledge base
        val matchedEntries = knowledgeBase.filter { entry ->
            entry.keywords.any { kw -> combinedLower.contains(kw) }
        }

        // 3. Extract raw tokens so user-entered unlisted ingredients also appear cleanly
        val extraIngredients = extractUnmatchedIngredients(ingredientText, matchedEntries)

        val allIngredients = (matchedEntries.map { entry ->
            IngredientDetail(
                commonName = entry.commonName,
                chemicalName = entry.chemicalName,
                riskLevel = entry.riskLevel.name,
                pros = entry.pros,
                cons = entry.cons,
                shortTermEffects = entry.shortTermEffects,
                longTermEffects = entry.longTermEffects
            )
        } + extraIngredients).distinctBy { it.commonName.lowercase(Locale.ROOT) }

        // 4. Parse numeric nutrition hints if present
        val sodiumMg = extractNumberBeforeUnit(combinedLower, listOf("sodium", "na"), listOf("mg"))
        val sugarG = extractNumberBeforeUnit(combinedLower, listOf("sugar", "sugars"), listOf("g"))
        val caloriesKcal = extractNumberBeforeUnit(combinedLower, listOf("calories", "energy"), listOf("kcal", "cal"))
        val proteinG = extractNumberBeforeUnit(combinedLower, listOf("protein"), listOf("g"))
        val fiberG = extractNumberBeforeUnit(combinedLower, listOf("fiber", "fibre"), listOf("g"))

        // 5. Calculate realistic 0-100 Score
        var score = when (category) {
            ProductCategory.PACKAGED_FOOD -> 74
            ProductCategory.AGRICULTURAL_FERTILIZER -> 72
            ProductCategory.PACKAGING_MATERIAL -> 76
        }

        matchedEntries.forEach { score += it.scoreImpact }

        val negatives = mutableListOf<FactorItem>()
        val positives = mutableListOf<FactorItem>()

        // Build Negatives & Positives breakdown tailored to Category
        val highRiskCount = matchedEntries.count { it.riskLevel == IngredientRiskLevel.HIGH_RISK }
        val modRiskCount = matchedEntries.count { it.riskLevel == IngredientRiskLevel.MODERATE_RISK }

        if (category == ProductCategory.PACKAGED_FOOD) {
            if (highRiskCount > 0 || modRiskCount > 0) {
                negatives.add(
                    FactorItem(
                        label = "Additives & Processing",
                        value = "$highRiskCount high-risk, $modRiskCount moderate",
                        severity = if (highRiskCount > 0) "HIGH" else "MODERATE",
                        explanation = "Detected synthetic additives, refined oils, or ultra-processed enhancers flagged in WHO/FSSAI clean-label reviews.",
                        iconType = "ADDITIVES"
                    )
                )
            }
            if (sodiumMg != null && sodiumMg >= 400.0) {
                val isHigh = sodiumMg >= 600.0
                score -= if (isHigh) 12 else 6
                negatives.add(
                    FactorItem(
                        label = "Sodium",
                        value = "${sodiumMg.toInt()} mg",
                        severity = if (isHigh) "HIGH" else "MODERATE",
                        explanation = "WHO advises keeping total daily sodium below 2,000 mg. High sodium burdens blood pressure and kidneys.",
                        iconType = "SODIUM"
                    )
                )
            } else if (combinedLower.contains("salt") || combinedLower.contains("sodium")) {
                negatives.add(
                    FactorItem(
                        label = "Sodium",
                        value = "Present (check serving size)",
                        severity = "MODERATE",
                        explanation = "Added salt/sodium compounds detected; monitor daily WHO limit (<2,000 mg/day).",
                        iconType = "SODIUM"
                    )
                )
            }
            if (sugarG != null && sugarG >= 10.0) {
                val isHigh = sugarG >= 18.0
                score -= if (isHigh) 12 else 6
                negatives.add(
                    FactorItem(
                        label = "Sugar",
                        value = "${sugarG.toInt()} g",
                        severity = if (isHigh) "HIGH" else "MODERATE",
                        explanation = "Exceeds clean-label sugar guidance; WHO advises limiting free sugars to <25g/day for optimal metabolic health.",
                        iconType = "SUGAR"
                    )
                )
            } else if (combinedLower.contains("sugar") || combinedLower.contains("syrup")) {
                negatives.add(
                    FactorItem(
                        label = "Sugar",
                        value = "Added sugars detected",
                        severity = "MODERATE",
                        explanation = "Contains added refined sugar or syrup contributing fast-absorbing empty calories.",
                        iconType = "SUGAR"
                    )
                )
            }
            if (caloriesKcal != null && caloriesKcal >= 380.0) {
                score -= 6
                negatives.add(
                    FactorItem(
                        label = "Calories",
                        value = "${caloriesKcal.toInt()} kcal",
                        severity = if (caloriesKcal >= 450.0) "HIGH" else "MODERATE",
                        explanation = "Energy-dense formulation per 100g/serving; pair with high-fiber fresh produce for balanced satiety.",
                        iconType = "CALORIES"
                    )
                )
            }
            if (proteinG != null && proteinG >= 6.0) {
                score += if (proteinG >= 12.0) 8 else 4
                positives.add(
                    FactorItem(
                        label = "Protein",
                        value = "${proteinG.toInt()} g",
                        severity = if (proteinG >= 12.0) "EXCELLENT" else "GOOD",
                        explanation = "Provides helpful protein supporting muscle maintenance and steady fullness.",
                        iconType = "PROTEIN"
                    )
                )
            }
            if (fiberG != null && fiberG >= 3.0) {
                score += if (fiberG >= 6.0) 9 else 4
                positives.add(
                    FactorItem(
                        label = "Dietary Fiber",
                        value = "${fiberG.toInt()} g",
                        severity = if (fiberG >= 6.0) "EXCELLENT" else "GOOD",
                        explanation = "Supports gut microbiome health and slows blood sugar absorption.",
                        iconType = "FIBER"
                    )
                )
            }
            if (matchedEntries.any { it.riskLevel == IngredientRiskLevel.RISK_FREE }) {
                positives.add(
                    FactorItem(
                        label = "Whole Food Ingredients",
                        value = "${matchedEntries.count { it.riskLevel == IngredientRiskLevel.RISK_FREE }} clean items",
                        severity = "EXCELLENT",
                        explanation = "Includes minimally processed natural grains, nuts, pulses, or clean food ingredients.",
                        iconType = "ORGANIC"
                    )
                )
            }
        } else if (category == ProductCategory.AGRICULTURAL_FERTILIZER) {
            if (highRiskCount > 0 || modRiskCount > 0) {
                negatives.add(
                    FactorItem(
                        label = "Chemical Runoff & Soil Acidification",
                        value = "$highRiskCount high-risk synthetic compounds",
                        severity = if (highRiskCount > 0) "HIGH" else "MODERATE",
                        explanation = "Synthetic nitrates, phosphates, or EDTA chelates can leach into groundwater and stress soil microbes per EPA standards.",
                        iconType = "CHEMICAL"
                    )
                )
            }
            if (matchedEntries.any { it.riskLevel == IngredientRiskLevel.RISK_FREE }) {
                positives.add(
                    FactorItem(
                        label = "Organic Soil Regeneration",
                        value = "Eco-safe bio inputs",
                        severity = "EXCELLENT",
                        explanation = "Supports earthworms, carbon retention, and sustainable slow-release crop nutrition.",
                        iconType = "ORGANIC"
                    )
                )
            }
        } else {
            if (highRiskCount > 0 || modRiskCount > 0) {
                negatives.add(
                    FactorItem(
                        label = "Chemical Leaching & Microplastics",
                        value = "$highRiskCount high-risk polymer/additive flags",
                        severity = if (highRiskCount > 0) "HIGH" else "MODERATE",
                        explanation = "Contains BPA, phthalates, PFAS, or non-biodegradable resins that may migrate into hot or fatty foods.",
                        iconType = "PLASTIC"
                    )
                )
            }
            if (matchedEntries.any { it.riskLevel == IngredientRiskLevel.RISK_FREE || it.riskLevel == IngredientRiskLevel.LOW_RISK }) {
                positives.add(
                    FactorItem(
                        label = "Food-Contact Safety & Recyclability",
                        value = "Low-leaching / Eco material",
                        severity = "EXCELLENT",
                        explanation = "Reduces chemical migration into food and supports safer circular recycling or composting.",
                        iconType = "ECO"
                    )
                )
            }
        }

        if (positives.isEmpty() && score >= 65) {
            positives.add(
                FactorItem(
                    label = "Clean Label Profile",
                    value = "No major red-flag additives",
                    severity = "GOOD",
                    explanation = "No high-toxicity synthetic additives, endocrine disruptors, or banned preservatives detected.",
                    iconType = "ORGANIC"
                )
            )
        }

        val finalScore = score.coerceIn(5, 98).toLong()
        val ratingLabel = scoreToRatingLabel(finalScore)

        // 6. Cross-check against Personal Health Profile for Immediate Red Alerts
        val personalAlerts = buildPersonalAlerts(combinedLower, matchedEntries, sodiumMg, sugarG, profile)

        // 7. Standards Cited
        val standards = when (category) {
            ProductCategory.PACKAGED_FOOD -> listOf("WHO", "FSSAI", "EFSA")
            ProductCategory.AGRICULTURAL_FERTILIZER -> listOf("EPA", "FAO", "WHO")
            ProductCategory.PACKAGING_MATERIAL -> listOf("EPA", "FSSAI", "EFSA")
        }

        // 8. Summary
        val productName = productNameHint.trim().ifBlank { inferProductName(category, allIngredients) }
        val brand = if (productNameHint.contains("-")) {
            productNameHint.substringBefore("-").trim()
        } else if (hasAttachedPhoto) {
            "Scanned Label"
        } else {
            "Label Analysis"
        }

        val summary = buildString {
            append("$productName scored $finalScore/100 ($ratingLabel) under ${standards.joinToString("/")} safety criteria. ")
            if (highRiskCount > 0) {
                append("Detected $highRiskCount high-risk ingredient(s) requiring caution.")
            } else if (modRiskCount > 0) {
                append("Contains $modRiskCount moderate-risk component(s); enjoy mindfully or consider a cleaner swap.")
            } else {
                append("Clean ingredient profile with no major synthetic risk flags detected.")
            }
        }

        // 9. Healthier Alternative
        val alternative = buildHealthierAlternative(category, productName, finalScore)

        return ScanRecord(
            productName = productName,
            brand = brand,
            category = category.name,
            score = finalScore,
            ratingLabel = ratingLabel,
            summary = summary,
            standardsCited = standards,
            personalAlerts = personalAlerts,
            negatives = negatives,
            positives = positives,
            ingredients = allIngredients.ifEmpty {
                listOf(
                    IngredientDetail(
                        commonName = "General Formulation",
                        chemicalName = "User-provided label",
                        riskLevel = IngredientRiskLevel.LOW_RISK.name,
                        pros = "No recognized high-risk synthetic additives listed.",
                        cons = "Provide full ingredient list for deeper additive-level analysis.",
                        shortTermEffects = "Generally well tolerated.",
                        longTermEffects = "Check full packaging nutrition panel for daily totals."
                    )
                )
            },
            healthierAlternative = alternative,
            rawInputText = ingredientText.takeIf { it.isNotBlank() }
        )
    }

    private fun detectCategory(text: String): ProductCategory {
        val fertilizerHits = listOf(
            "npk", "urea", "ammonium nitrate", "potash", "potassium chloride", "phosphate",
            "fertilizer", "fertiliser", "vermicompost", "neem cake", "copper sulfate",
            "edta", "borate", "glyphosate", "soil", "micronutrient", "manure"
        ).count { text.contains(it) }

        val packagingHits = listOf(
            "polycarbonate", "bpa", "bisphenol", "phthalate", "dehp", "pfas", "resin",
            "plastic", "styrofoam", "polystyrene", "pvc", "hdpe", "ldpe", "pet bottle",
            "bagasse", "borosilicate", "packaging", "container", "wrapper", "#7", "#6", "#3", "#1"
        ).count { text.contains(it) }

        return when {
            fertilizerHits >= 2 && fertilizerHits >= packagingHits -> ProductCategory.AGRICULTURAL_FERTILIZER
            packagingHits >= 2 -> ProductCategory.PACKAGING_MATERIAL
            fertilizerHits == 1 && text.contains("fertiliz") -> ProductCategory.AGRICULTURAL_FERTILIZER
            packagingHits == 1 && (text.contains("packaging") || text.contains("container")) -> ProductCategory.PACKAGING_MATERIAL
            else -> ProductCategory.PACKAGED_FOOD
        }
    }

    private fun buildPersonalAlerts(
        combinedLower: String,
        matched: List<KnowledgeEntry>,
        sodiumMg: Double?,
        sugarG: Double?,
        profile: HealthProfile
    ): List<String> {
        val alerts = mutableListOf<String>()

        // Allergies
        profile.allergies.forEach { allergy ->
            val key = allergy.lowercase(Locale.ROOT)
            val hitByEntry = matched.firstOrNull { e -> e.allergenTags.any { it.contains(key) || key.contains(it) } }
            val hitDirect = combinedLower.contains(key) || (key == "gluten" && combinedLower.contains("wheat")) ||
                (key == "peanuts" && combinedLower.contains("groundnut")) ||
                (key == "dairy" && (combinedLower.contains("milk") || combinedLower.contains("whey")))
            if (hitByEntry != null || hitDirect) {
                alerts.add("ALLERGY ALERT ($allergy): Detected ${hitByEntry?.commonName ?: allergy} — avoid this product.")
            }
        }

        // Medical Conditions
        profile.medicalConditions.forEach { cond ->
            val key = cond.lowercase(Locale.ROOT)
            if (key.contains("hypertension") && ((sodiumMg != null && sodiumMg >= 400.0) || combinedLower.contains("salt") || combinedLower.contains("e621"))) {
                alerts.add("MEDICAL ALERT ($cond): High sodium or salt-based flavor enhancers conflict with blood pressure management.")
            }
            if (key.contains("diabetes") && ((sugarG != null && sugarG >= 10.0) || combinedLower.contains("sugar") || combinedLower.contains("maida") || combinedLower.contains("syrup"))) {
                alerts.add("MEDICAL ALERT ($cond): Refined carbohydrates or added sugars may trigger sharp blood glucose spikes.")
            }
            if (key.contains("celiac") && (combinedLower.contains("wheat") || combinedLower.contains("maida") || combinedLower.contains("gluten"))) {
                alerts.add("MEDICAL ALERT ($cond): Contains wheat/gluten — unsafe for Celiac Disease.")
            }
            if (key.contains("kidney") && (combinedLower.contains("phosphate") || combinedLower.contains("e451") || (sodiumMg != null && sodiumMg >= 500.0))) {
                alerts.add("MEDICAL ALERT ($cond): High sodium or inorganic phosphate additives strain kidney function.")
            }
            if (key.contains("heart") && (combinedLower.contains("palm oil") || combinedLower.contains("trans fat") || combinedLower.contains("hydrogenated"))) {
                alerts.add("MEDICAL ALERT ($cond): Saturated palm oil or trans fats conflict with heart-health goals.")
            }
            if (key.contains("asthma") && (combinedLower.contains("e211") || combinedLower.contains("sulfite") || combinedLower.contains("e102"))) {
                alerts.add("MEDICAL ALERT ($cond): Contains preservatives/dyes known to aggravate asthma sensitivity.")
            }
            if ((key.contains("thyroid") || key.contains("pcos")) && (combinedLower.contains("bpa") || combinedLower.contains("pfas") || combinedLower.contains("phthalate"))) {
                alerts.add("MEDICAL ALERT ($cond): Endocrine-disrupting packaging chemicals detected.")
            }
        }

        // Dietary Preferences
        profile.dietaryPreferences.forEach { diet ->
            val key = diet.lowercase(Locale.ROOT)
            if (key.contains("gluten-free") && (combinedLower.contains("wheat") || combinedLower.contains("maida") || combinedLower.contains("gluten"))) {
                alerts.add("DIET CONFLICT ($diet): Product contains gluten-bearing wheat flour.")
            }
            if (key.contains("low sodium") && ((sodiumMg != null && sodiumMg >= 400.0) || combinedLower.contains("salt"))) {
                alerts.add("DIET CONFLICT ($diet): Significant sodium detected for a Low-Sodium preference.")
            }
            if (key.contains("low sugar") && ((sugarG != null && sugarG >= 8.0) || combinedLower.contains("sugar") || combinedLower.contains("syrup"))) {
                alerts.add("DIET CONFLICT ($diet): Added sugars conflict with your Low-Sugar goal.")
            }
            if (key.contains("vegan") && matched.any { it.nonVegan }) {
                alerts.add("DIET CONFLICT ($diet): Contains dairy or animal-derived ingredients.")
            }
        }

        // Chemical Sensitivities
        profile.chemicalSensitivities.forEach { chem ->
            val key = chem.lowercase(Locale.ROOT).substringBefore("(").trim()
            val hitEntry = matched.firstOrNull { e ->
                e.chemicalTags.any { it.contains(key) || key.contains(it) } ||
                    e.commonName.lowercase(Locale.ROOT).contains(key)
            }
            if (hitEntry != null || (key.length >= 3 && combinedLower.contains(key))) {
                alerts.add("SENSITIVITY ALERT ($chem): Matched ${hitEntry?.commonName ?: chem} in label.")
            }
        }

        return alerts.distinct()
    }

    private fun extractUnmatchedIngredients(
        rawText: String,
        matched: List<KnowledgeEntry>
    ): List<IngredientDetail> {
        if (rawText.isBlank()) return emptyList()
        val rawTokens = rawText
            .replace("\n", ",")
            .split(",", ";")
            .map { it.trim().replace(Regex("\\(.*?\\)"), "").trim() }
            .filter { token ->
                token.length in 3..45 &&
                    !token.lowercase(Locale.ROOT).startsWith("calories") &&
                    !token.lowercase(Locale.ROOT).startsWith("protein") &&
                    !token.lowercase(Locale.ROOT).startsWith("fiber") &&
                    !token.lowercase(Locale.ROOT).startsWith("sugar:") &&
                    !token.lowercase(Locale.ROOT).startsWith("sodium:")
            }

        val matchedKeywords = matched.flatMap { it.keywords }
        return rawTokens
            .filter { token ->
                val lower = token.lowercase(Locale.ROOT)
                matchedKeywords.none { kw -> lower.contains(kw) || kw.contains(lower) }
            }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(10)
            .map { token ->
                IngredientDetail(
                    commonName = token.replaceFirstChar { it.uppercase() },
                    chemicalName = "Standard label ingredient",
                    riskLevel = IngredientRiskLevel.LOW_RISK.name,
                    pros = "Common culinary or functional ingredient with no major WHO/FSSAI red flag.",
                    cons = "Consume in balanced amounts as part of a varied diet or standard usage.",
                    shortTermEffects = "Generally well tolerated in normal amounts.",
                    longTermEffects = "No known severe long-term hazards at standard exposure."
                )
            }
    }

    private fun extractNumberBeforeUnit(text: String, labels: List<String>, units: List<String>): Double? {
        for (label in labels) {
            for (unit in units) {
                val pattern = Regex("$label[^0-9]{0,12}(\\d+(?:\\.\\d+)?)\\s*$unit")
                val match = pattern.find(text)
                if (match != null) {
                    return match.groupValues[1].toDoubleOrNull()
                }
            }
        }
        return null
    }

    private fun inferProductName(category: ProductCategory, ingredients: List<IngredientDetail>): String {
        val top = ingredients.firstOrNull()?.commonName
        return when (category) {
            ProductCategory.PACKAGED_FOOD -> if (top != null) "$top Food Product" else "Packaged Food Item"
            ProductCategory.AGRICULTURAL_FERTILIZER -> if (top != null) "$top Fertilizer Blend" else "Agricultural Fertilizer"
            ProductCategory.PACKAGING_MATERIAL -> if (top != null) "$top Packaging" else "Food Packaging Material"
        }
    }

    private fun buildHealthierAlternative(
        category: ProductCategory,
        scannedName: String,
        score: Long
    ): HealthierAlternative {
        val lower = scannedName.lowercase(Locale.ROOT)
        return when (category) {
            ProductCategory.PACKAGED_FOOD -> when {
                lower.contains("noodle") || lower.contains("pasta") -> HealthierAlternative(
                    productName = "Whole Millet & Ragi Non-Fried Noodles",
                    brand = "Clean Label India",
                    score = 90L,
                    reason = "Air-dried whole grain millet with zero palm oil, zero MSG, and 60% less sodium.",
                    keyBenefits = listOf("No palm oil or INS 621", "High natural dietary fiber", "Whole grain ragi & wheat")
                )
                lower.contains("biscuit") || lower.contains("cookie") || lower.contains("snack") -> HealthierAlternative(
                    productName = "Sprouted Oats & Jaggery Millet Crackers",
                    brand = "PureHarvest Organics",
                    score = 89L,
                    reason = "Baked with cold-pressed oil and whole grains instead of maida and refined sugar.",
                    keyBenefits = listOf("Zero maida & trans fat", "Naturally sweetened", "High plant fiber")
                )
                else -> HealthierAlternative(
                    productName = "100% Organic Whole-Grain & Unsweetened Equivalent",
                    brand = "WHO/FSSAI Clean Choice",
                    score = maxOf(score + 15L, 92L).coerceAtMost(96L),
                    reason = "Minimally processed whole ingredients without synthetic preservatives, excess sodium, or palm oil.",
                    keyBenefits = listOf("Zero artificial additives", "Low sodium & free sugars", "Nutrient-dense whole ingredients")
                )
            }
            ProductCategory.AGRICULTURAL_FERTILIZER -> HealthierAlternative(
                productName = "Enriched Organic Vermicompost + Neem Cake Bio-Blend",
                brand = "EcoSoil Regenerative",
                score = 95L,
                reason = "Replaces synthetic nitrate runoff and EDTA chelates with slow-release organic soil nutrition.",
                keyBenefits = listOf("Zero groundwater nitrate leaching", "Boosts earthworms & soil microbes", "EPA & farmer-safe handling")
            )
            ProductCategory.PACKAGING_MATERIAL -> HealthierAlternative(
                productName = "PFAS-Free Sugarcane Bagasse or Borosilicate Glass",
                brand = "EarthPack Zero",
                score = 96L,
                reason = "Eliminates BPA, phthalates, and forever chemicals with inert, heat-safe or home-compostable material.",
                keyBenefits = listOf("100% BPA & PFAS free", "Safe for hot & oily foods", "Zero microplastic shedding")
            )
        }
    }

    fun lookupIngredientInfo(query: String): List<KnowledgeEntryInfo> {
        val lower = query.lowercase(Locale.ROOT)
        return knowledgeBase
            .filter { entry ->
                entry.keywords.any { lower.contains(it) } ||
                    lower.contains(entry.commonName.lowercase(Locale.ROOT))
            }
            .map {
                KnowledgeEntryInfo(
                    commonName = it.commonName,
                    chemicalName = it.chemicalName,
                    riskLevel = it.riskLevel,
                    pros = it.pros,
                    cons = it.cons,
                    shortTermEffects = it.shortTermEffects,
                    longTermEffects = it.longTermEffects
                )
            }
    }
}

data class KnowledgeEntryInfo(
    val commonName: String,
    val chemicalName: String,
    val riskLevel: IngredientRiskLevel,
    val pros: String,
    val cons: String,
    val shortTermEffects: String,
    val longTermEffects: String
)
