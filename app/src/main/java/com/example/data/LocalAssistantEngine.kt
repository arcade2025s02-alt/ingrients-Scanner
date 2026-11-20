package com.example.data

import com.example.model.HealthProfile
import com.example.model.IngredientRiskLevel
import com.example.model.ScanRecord
import java.util.Locale

object LocalAssistantEngine {

    fun generateReply(
        question: String,
        activeScan: ScanRecord?,
        profile: HealthProfile
    ): String {
        val q = question.lowercase(Locale.ROOT).trim()

        // 1. Check if the user asked about a specific ingredient/chemical in our knowledge base
        val directMatches = LocalRiskAnalyzerEngine.lookupIngredientInfo(q)
        if (directMatches.isNotEmpty()) {
            return buildString {
                appendLine("Here is the WHO/FSSAI/EPA breakdown for what you asked about:")
                directMatches.take(3).forEach { item ->
                    appendLine()
                    appendLine("• ${item.commonName} (${item.chemicalName}) — ${item.riskLevel.label}")
                    appendLine("  Pros: ${item.pros}")
                    appendLine("  Cons: ${item.cons}")
                    appendLine("  Short-Term: ${item.shortTermEffects}")
                    appendLine("  Long-Term: ${item.longTermEffects}")
                }
            }.trim()
        }

        // 2. Active scan specific Q&A
        if (activeScan != null) {
            if (q.contains("safe") || q.contains("profile") || q.contains("allerg") || q.contains("eat") || q.contains("use")) {
                return buildString {
                    appendLine("Safety Check for ${activeScan.productName} (${activeScan.score}/100 • ${activeScan.ratingLabel}):")
                    if (activeScan.personalAlerts.isNotEmpty()) {
                        appendLine()
                        appendLine("⚠️ Personal Profile Conflicts Found:")
                        activeScan.personalAlerts.forEach { appendLine("• $it") }
                        appendLine()
                        appendLine("Recommendation: Avoid or replace this item with a cleaner alternative.")
                    } else if (profile.hasAnyRestrictions()) {
                        appendLine()
                        appendLine("✅ No direct conflicts were detected against your saved profile (${profile.summaryForDisplay()}).")
                        if (activeScan.score < 60L) {
                            appendLine("However, its overall score is ${activeScan.score}/100, so limit frequency and consider the recommended swap.")
                        }
                    } else {
                        appendLine()
                        appendLine("Your Personal Health Profile currently has no allergies or conditions listed. Update your Profile tab for personalized allergen & medical alerts.")
                    }
                }.trim()
            }

            if (q.contains("highest") || q.contains("worst") || q.contains("additive") || q.contains("risk") || q.contains("explain")) {
                val risky = activeScan.ingredients.filter {
                    it.parsedRiskLevel() == IngredientRiskLevel.HIGH_RISK ||
                        it.parsedRiskLevel() == IngredientRiskLevel.MODERATE_RISK
                }
                return if (risky.isEmpty()) {
                    "${activeScan.productName} has no high-risk or moderate-risk additives detected! All listed ingredients fall into Low-Risk or Risk-Free tiers."
                } else {
                    buildString {
                        appendLine("Key Risk Ingredients in ${activeScan.productName}:")
                        risky.take(4).forEach { ing ->
                            appendLine()
                            appendLine("• ${ing.commonName} (${ing.chemicalName}) — ${ing.parsedRiskLevel().label}")
                            appendLine("  Concern: ${ing.cons}")
                            appendLine("  Long-Term Impact: ${ing.longTermEffects}")
                        }
                    }.trim()
                }
            }

            if (q.contains("swap") || q.contains("alternative") || q.contains("better") || q.contains("replace") || q.contains("natural")) {
                val alt = activeScan.healthierAlternative
                return if (alt != null) {
                    buildString {
                        appendLine("Recommended Cleaner Swap for ${activeScan.productName}:")
                        appendLine()
                        appendLine("✅ ${alt.productName} (${alt.brand}) — ${alt.score}/100 Excellent")
                        appendLine("Why it's better: ${alt.reason}")
                        if (alt.keyBenefits.isNotEmpty()) {
                            appendLine("Benefits: ${alt.keyBenefits.joinToString(" • ")}")
                        }
                    }.trim()
                } else {
                    "Look for minimally processed, single-ingredient whole alternatives without synthetic preservatives, excess sodium, or plasticizers."
                }
            }

            if (q.contains("who") || q.contains("fssai") || q.contains("epa") || q.contains("standard") || q.contains("limit")) {
                return buildString {
                    appendLine("Regulatory Context (${activeScan.standardsCited.joinToString(", ")}) for ${activeScan.productName}:")
                    appendLine()
                    appendLine("• WHO recommends <2,000 mg sodium/day (<5g salt), <10% energy from free sugars, and zero industrial trans fats.")
                    appendLine("• FSSAI clean-label initiatives (Label Padhega India) emphasize checking INS/E-numbers, palm oil, and added sugars on packaged foods.")
                    appendLine("• EPA guidelines flag synthetic nitrate runoff, non-biodegradable EDTA chelates, BPA, phthalates, and PFAS in agriculture & packaging.")
                    appendLine()
                    appendLine("Product Summary: ${activeScan.summary}")
                }.trim()
            }

            return buildString {
                appendLine("Summary for ${activeScan.productName} (${activeScan.score}/100 • ${activeScan.ratingLabel}):")
                appendLine(activeScan.summary)
                if (activeScan.negatives.isNotEmpty()) {
                    appendLine()
                    appendLine("Main Negatives: ${activeScan.negatives.joinToString("; ") { "${it.label} (${it.value})" }}")
                }
                appendLine()
                appendLine("Ask me: 'Is this safe for my health profile?', 'Explain the highest-risk additive', or name any ingredient/INS code!")
            }.trim()
        }

        // 3. General guidance when no scan is attached
        return buildString {
            appendLine("Smart Risk Analyzer Offline Knowledge Assistant (100% Free, Zero API Cost):")
            appendLine()
            appendLine("I can help you decode:")
            appendLine("• Food Additives & INS/E-codes (try asking about E621/MSG, E211 Sodium Benzoate, E150d Caramel, Palm Oil, Maida, or Aspartame)")
            appendLine("• Agricultural Fertilizers (Ammonium Nitrate, Urea, DAP, EDTA, Vermicompost, Neem Cake)")
            appendLine("• Packaging Resin Codes (#1 PET, #3 PVC, #6 Styrofoam, #7 BPA Polycarbonate, PFAS, Sugarcane Bagasse)")
            appendLine()
            appendLine("Scan any product first to ask personalized questions linked to your saved Health Profile!")
        }.trim()
    }
}
