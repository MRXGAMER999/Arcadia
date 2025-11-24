package com.example.arcadia.data.local

/**
 * L3 Cache: Compile-time hardcoded studio mappings for zero-latency lookups.
 * Contains the top 20 major publishers and their subsidiaries with RAWG API slugs.
 */
object HardcodedStudioMappings {

    /**
     * Data class to hold both display name and RAWG API slug
     */
    data class StudioInfo(
        val displayName: String,
        val slug: String
    )

    /**
     * Maps parent studio key to list of subsidiary studios with their slugs.
     * Slugs are used for RAWG API filtering.
     */
    private val STUDIO_HIERARCHY: Map<String, List<StudioInfo>> = mapOf(
        "bethesda" to listOf(
            StudioInfo("Bethesda Softworks", "bethesda-softworks"),
            StudioInfo("Bethesda Game Studios", "bethesda-game-studios"),
            StudioInfo("id Software", "id-software"),
            StudioInfo("Arkane Studios", "arkane-studios"),
            StudioInfo("Tango Gameworks", "tango-gameworks"),
            StudioInfo("MachineGames", "machinegames"),
            StudioInfo("ZeniMax Online Studios", "zenimax-online-studios")
        ),
        "xbox game studios" to listOf(
            StudioInfo("Xbox Game Studios", "xbox-game-studios"),
            StudioInfo("343 Industries", "343-industries"),
            StudioInfo("The Coalition", "the-coalition"),
            StudioInfo("Playground Games", "playground-games"),
            StudioInfo("Turn 10 Studios", "turn-10-studios"),
            StudioInfo("Rare", "rare"),
            StudioInfo("Ninja Theory", "ninja-theory"),
            StudioInfo("Obsidian Entertainment", "obsidian-entertainment"),
            StudioInfo("inXile Entertainment", "inxile-entertainment"),
            StudioInfo("Double Fine Productions", "double-fine-productions"),
            StudioInfo("Compulsion Games", "compulsion-games"),
            StudioInfo("Undead Labs", "undead-labs")
        ),
        // Microsoft Gaming includes Xbox + Bethesda (use this for all Microsoft studios)
        "microsoft" to listOf(
            StudioInfo("Xbox Game Studios", "xbox-game-studios"),
            StudioInfo("343 Industries", "343-industries"),
            StudioInfo("The Coalition", "the-coalition"),
            StudioInfo("Playground Games", "playground-games"),
            StudioInfo("Turn 10 Studios", "turn-10-studios"),
            StudioInfo("Rare", "rare"),
            StudioInfo("Ninja Theory", "ninja-theory"),
            StudioInfo("Obsidian Entertainment", "obsidian-entertainment"),
            StudioInfo("inXile Entertainment", "inxile-entertainment"),
            StudioInfo("Double Fine Productions", "double-fine-productions"),
            StudioInfo("Compulsion Games", "compulsion-games"),
            StudioInfo("Undead Labs", "undead-labs"),
            StudioInfo("Bethesda Softworks", "bethesda-softworks"),
            StudioInfo("Bethesda Game Studios", "bethesda-game-studios"),
            StudioInfo("id Software", "id-software"),
            StudioInfo("Arkane Studios", "arkane-studios"),
            StudioInfo("Tango Gameworks", "tango-gameworks"),
            StudioInfo("MachineGames", "machinegames"),
            StudioInfo("ZeniMax Online Studios", "zenimax-online-studios")
        ),
        "playstation studios" to listOf(
            StudioInfo("Naughty Dog", "naughty-dog"),
            StudioInfo("Santa Monica Studio", "santa-monica-studio"),
            StudioInfo("Guerrilla Games", "guerrilla-games"),
            StudioInfo("Insomniac Games", "insomniac-games"),
            StudioInfo("Sucker Punch Productions", "sucker-punch-productions"),
            StudioInfo("Polyphony Digital", "polyphony-digital"),
            StudioInfo("Media Molecule", "media-molecule"),
            StudioInfo("Bend Studio", "bend-studio"),
            StudioInfo("Housemarque", "housemarque"),
            StudioInfo("Bluepoint Games", "bluepoint-games")
        ),
        "electronic arts" to listOf(
            StudioInfo("Electronic Arts", "electronic-arts"),
            StudioInfo("EA DICE", "ea-dice"),
            StudioInfo("Respawn Entertainment", "respawn-entertainment"),
            StudioInfo("BioWare", "bioware"),
            StudioInfo("Motive Studios", "motive-studios"),
            StudioInfo("Criterion Games", "criterion-games"),
            StudioInfo("Codemasters", "codemasters")
        ),
        "ubisoft" to listOf(
            StudioInfo("Ubisoft", "ubisoft"),
            StudioInfo("Ubisoft Montreal", "ubisoft-montreal"),
            StudioInfo("Ubisoft Toronto", "ubisoft-toronto"),
            StudioInfo("Ubisoft Paris", "ubisoft-paris"),
            StudioInfo("Ubisoft Quebec", "ubisoft-quebec"),
            StudioInfo("Massive Entertainment", "massive-entertainment")
        ),
        "activision blizzard" to listOf(
            StudioInfo("Activision", "activision"),
            StudioInfo("Blizzard Entertainment", "blizzard-entertainment"),
            StudioInfo("Treyarch", "treyarch"),
            StudioInfo("Infinity Ward", "infinity-ward"),
            StudioInfo("Sledgehammer Games", "sledgehammer-games"),
            StudioInfo("Raven Software", "raven-software"),
            StudioInfo("High Moon Studios", "high-moon-studios")
        ),
        "take-two interactive" to listOf(
            StudioInfo("Rockstar Games", "rockstar-games"),
            StudioInfo("Rockstar North", "rockstar-north"),
            StudioInfo("2K Games", "2k-games"),
            StudioInfo("Firaxis Games", "firaxis-games"),
            StudioInfo("Gearbox Software", "gearbox-software"),
            StudioInfo("Hangar 13", "hangar-13"),
            StudioInfo("Visual Concepts", "visual-concepts")
        ),
        "nintendo" to listOf(
            StudioInfo("Nintendo", "nintendo"),
            StudioInfo("Nintendo EPD", "nintendo-epd"),
            StudioInfo("Retro Studios", "retro-studios"),
            StudioInfo("Monolith Soft", "monolith-soft"),
            StudioInfo("HAL Laboratory", "hal-laboratory"),
            StudioInfo("Intelligent Systems", "intelligent-systems"),
            StudioInfo("Game Freak", "game-freak")
        ),
        "sega" to listOf(
            StudioInfo("Sega", "sega"),
            StudioInfo("Ryu Ga Gotoku Studio", "ryu-ga-gotoku-studio"),
            StudioInfo("Creative Assembly", "creative-assembly"),
            StudioInfo("Sports Interactive", "sports-interactive"),
            StudioInfo("Atlus", "atlus")
        ),
        "square enix" to listOf(
            StudioInfo("Square Enix", "square-enix"),
            StudioInfo("Crystal Dynamics", "crystal-dynamics"),
            StudioInfo("Eidos Montreal", "eidos-montreal")
        ),
        "capcom" to listOf(
            StudioInfo("Capcom", "capcom")
        ),
        "bandai namco" to listOf(
            StudioInfo("Bandai Namco Entertainment", "bandai-namco-entertainment"),
            StudioInfo("FromSoftware", "fromsoftware")
        ),
        "warner bros" to listOf(
            StudioInfo("Warner Bros. Interactive Entertainment", "warner-bros-interactive-entertainment"),
            StudioInfo("Rocksteady Studios", "rocksteady-studios"),
            StudioInfo("NetherRealm Studios", "netherrealm-studios"),
            StudioInfo("Monolith Productions", "monolith-productions"),
            StudioInfo("TT Games", "tt-games"),
            StudioInfo("Avalanche Software", "avalanche-software")
        ),
        "cd projekt" to listOf(
            StudioInfo("CD Projekt Red", "cd-projekt-red")
        ),
        "thq nordic" to listOf(
            StudioInfo("THQ Nordic", "thq-nordic"),
            StudioInfo("Deep Silver", "deep-silver"),
            StudioInfo("Volition", "volition"),
            StudioInfo("4A Games", "4a-games"),
            StudioInfo("Coffee Stain Studios", "coffee-stain-studios")
        ),
        "devolver digital" to listOf(
            StudioInfo("Devolver Digital", "devolver-digital"),
            StudioInfo("Croteam", "croteam")
        ),
        "paradox interactive" to listOf(
            StudioInfo("Paradox Interactive", "paradox-interactive"),
            StudioInfo("Paradox Development Studio", "paradox-development-studio"),
            StudioInfo("Colossal Order", "colossal-order")
        )
    )

    private val NORMALIZED_MAP: Map<String, List<StudioInfo>> by lazy {
        STUDIO_HIERARCHY.mapKeys { it.key.lowercase().trim() }
    }

    // Reverse lookup maps for O(1) access by slug or display name
    private val SLUG_TO_PARENT: Map<String, String> by lazy {
        NORMALIZED_MAP.flatMap { (parent, studios) ->
            studios.map { it.slug to parent }
        }.toMap()
    }

    private val NAME_TO_PARENT: Map<String, String> by lazy {
        NORMALIZED_MAP.flatMap { (parent, studios) ->
            studios.map { it.displayName.lowercase() to parent }
        }.toMap()
    }

    /**
     * Get subsidiary studios for a parent studio.
     * Returns list of StudioInfo with display names and slugs.
     * Uses O(1) reverse lookup maps for efficient searching.
     */
    fun getSubsidiaries(studioName: String): List<StudioInfo>? {
        val normalized = studioName.lowercase().trim()
        
        // Direct lookup by parent name (O(1))
        NORMALIZED_MAP[normalized]?.let { return it }
        
        // Reverse lookup by slug (O(1))
        SLUG_TO_PARENT[normalized]?.let { parent ->
            return NORMALIZED_MAP[parent]
        }
        
        // Reverse lookup by display name (O(1))
        NAME_TO_PARENT[normalized]?.let { parent ->
            return NORMALIZED_MAP[parent]
        }
        
        return null
    }

    /**
     * Get comma-separated slugs for API filtering.
     */
    fun getSubsidiarySlugs(studioName: String): String? {
        return getSubsidiaries(studioName)?.joinToString(",") { it.slug }
    }

    /**
     * Get display names for UI.
     */
    fun getSubsidiaryNames(studioName: String): Set<String>? {
        return getSubsidiaries(studioName)?.map { it.displayName }?.toSet()
    }

    /**
     * Check if a studio name is known (either as parent or subsidiary).
     * Uses O(1) lookups via reverse maps.
     */
    fun isKnownStudio(studioName: String): Boolean {
        val normalized = studioName.lowercase().trim()
        return NORMALIZED_MAP.containsKey(normalized) ||
                SLUG_TO_PARENT.containsKey(normalized) ||
                NAME_TO_PARENT.containsKey(normalized)
    }

    fun getAllMappings(): Map<String, List<StudioInfo>> = NORMALIZED_MAP
}
