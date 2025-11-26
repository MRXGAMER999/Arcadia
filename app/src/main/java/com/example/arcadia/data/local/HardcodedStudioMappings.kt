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
    
    // ==================== Game Series to Studio Mappings ====================
    
    /**
     * Maps popular game series/franchises to their primary developer studios.
     * This provides instant, accurate results for game-based searches.
     */
    private val GAME_SERIES_TO_STUDIOS: Map<String, List<StudioInfo>> = mapOf(
        // Sega / Ryu Ga Gotoku
        "yakuza" to listOf(StudioInfo("Ryu Ga Gotoku Studio", "ryu-ga-gotoku-studio")),
        "like a dragon" to listOf(StudioInfo("Ryu Ga Gotoku Studio", "ryu-ga-gotoku-studio")),
        "judgment" to listOf(StudioInfo("Ryu Ga Gotoku Studio", "ryu-ga-gotoku-studio")),
        "lost judgment" to listOf(StudioInfo("Ryu Ga Gotoku Studio", "ryu-ga-gotoku-studio")),
        
        // Call of Duty (multiple studios)
        "call of duty" to listOf(
            StudioInfo("Infinity Ward", "infinity-ward"),
            StudioInfo("Treyarch", "treyarch"),
            StudioInfo("Sledgehammer Games", "sledgehammer-games")
        ),
        "cod" to listOf(
            StudioInfo("Infinity Ward", "infinity-ward"),
            StudioInfo("Treyarch", "treyarch"),
            StudioInfo("Sledgehammer Games", "sledgehammer-games")
        ),
        "modern warfare" to listOf(StudioInfo("Infinity Ward", "infinity-ward")),
        "black ops" to listOf(StudioInfo("Treyarch", "treyarch")),
        "warzone" to listOf(
            StudioInfo("Infinity Ward", "infinity-ward"),
            StudioInfo("Raven Software", "raven-software")
        ),
        
        // PlayStation Studios
        "god of war" to listOf(StudioInfo("Santa Monica Studio", "santa-monica-studio")),
        "the last of us" to listOf(StudioInfo("Naughty Dog", "naughty-dog")),
        "uncharted" to listOf(StudioInfo("Naughty Dog", "naughty-dog")),
        "spider-man" to listOf(StudioInfo("Insomniac Games", "insomniac-games")),
        "ratchet" to listOf(StudioInfo("Insomniac Games", "insomniac-games")),
        "horizon" to listOf(StudioInfo("Guerrilla Games", "guerrilla-games")),
        "ghost of tsushima" to listOf(StudioInfo("Sucker Punch Productions", "sucker-punch-productions")),
        "infamous" to listOf(StudioInfo("Sucker Punch Productions", "sucker-punch-productions")),
        "gran turismo" to listOf(StudioInfo("Polyphony Digital", "polyphony-digital")),
        "returnal" to listOf(StudioInfo("Housemarque", "housemarque")),
        "demon's souls" to listOf(StudioInfo("Bluepoint Games", "bluepoint-games")),
        "days gone" to listOf(StudioInfo("Bend Studio", "bend-studio")),
        
        // Xbox Game Studios
        "halo" to listOf(StudioInfo("343 Industries", "343-industries")),
        "gears of war" to listOf(StudioInfo("The Coalition", "the-coalition")),
        "gears" to listOf(StudioInfo("The Coalition", "the-coalition")),
        "forza horizon" to listOf(StudioInfo("Playground Games", "playground-games")),
        "forza motorsport" to listOf(StudioInfo("Turn 10 Studios", "turn-10-studios")),
        "sea of thieves" to listOf(StudioInfo("Rare", "rare")),
        "hellblade" to listOf(StudioInfo("Ninja Theory", "ninja-theory")),
        "outer worlds" to listOf(StudioInfo("Obsidian Entertainment", "obsidian-entertainment")),
        "avowed" to listOf(StudioInfo("Obsidian Entertainment", "obsidian-entertainment")),
        "pillars of eternity" to listOf(StudioInfo("Obsidian Entertainment", "obsidian-entertainment")),
        "wasteland" to listOf(StudioInfo("inXile Entertainment", "inxile-entertainment")),
        "psychonauts" to listOf(StudioInfo("Double Fine Productions", "double-fine-productions")),
        "state of decay" to listOf(StudioInfo("Undead Labs", "undead-labs")),
        
        // Bethesda
        "elder scrolls" to listOf(StudioInfo("Bethesda Game Studios", "bethesda-game-studios")),
        "skyrim" to listOf(StudioInfo("Bethesda Game Studios", "bethesda-game-studios")),
        "fallout" to listOf(StudioInfo("Bethesda Game Studios", "bethesda-game-studios")),
        "starfield" to listOf(StudioInfo("Bethesda Game Studios", "bethesda-game-studios")),
        "doom" to listOf(StudioInfo("id Software", "id-software")),
        "quake" to listOf(StudioInfo("id Software", "id-software")),
        "dishonored" to listOf(StudioInfo("Arkane Studios", "arkane-studios")),
        "deathloop" to listOf(StudioInfo("Arkane Studios", "arkane-studios")),
        "prey" to listOf(StudioInfo("Arkane Studios", "arkane-studios")),
        "evil within" to listOf(StudioInfo("Tango Gameworks", "tango-gameworks")),
        "hi-fi rush" to listOf(StudioInfo("Tango Gameworks", "tango-gameworks")),
        "wolfenstein" to listOf(StudioInfo("MachineGames", "machinegames")),
        "indiana jones" to listOf(StudioInfo("MachineGames", "machinegames")),
        
        // Rockstar
        "gta" to listOf(StudioInfo("Rockstar North", "rockstar-north")),
        "grand theft auto" to listOf(StudioInfo("Rockstar North", "rockstar-north")),
        "red dead" to listOf(StudioInfo("Rockstar Games", "rockstar-games")),
        
        // Ubisoft
        "assassin's creed" to listOf(
            StudioInfo("Ubisoft Montreal", "ubisoft-montreal"),
            StudioInfo("Ubisoft Quebec", "ubisoft-quebec")
        ),
        "assassins creed" to listOf(
            StudioInfo("Ubisoft Montreal", "ubisoft-montreal"),
            StudioInfo("Ubisoft Quebec", "ubisoft-quebec")
        ),
        "far cry" to listOf(StudioInfo("Ubisoft Montreal", "ubisoft-montreal")),
        "watch dogs" to listOf(StudioInfo("Ubisoft Montreal", "ubisoft-montreal")),
        "rainbow six" to listOf(StudioInfo("Ubisoft Montreal", "ubisoft-montreal")),
        "the division" to listOf(StudioInfo("Massive Entertainment", "massive-entertainment")),
        "avatar" to listOf(StudioInfo("Massive Entertainment", "massive-entertainment")),
        
        // EA
        "battlefield" to listOf(StudioInfo("EA DICE", "ea-dice")),
        "apex legends" to listOf(StudioInfo("Respawn Entertainment", "respawn-entertainment")),
        "titanfall" to listOf(StudioInfo("Respawn Entertainment", "respawn-entertainment")),
        "jedi" to listOf(StudioInfo("Respawn Entertainment", "respawn-entertainment")),
        "star wars jedi" to listOf(StudioInfo("Respawn Entertainment", "respawn-entertainment")),
        "mass effect" to listOf(StudioInfo("BioWare", "bioware")),
        "dragon age" to listOf(StudioInfo("BioWare", "bioware")),
        "dead space" to listOf(StudioInfo("Motive Studios", "motive-studios")),
        "need for speed" to listOf(StudioInfo("Criterion Games", "criterion-games")),
        "burnout" to listOf(StudioInfo("Criterion Games", "criterion-games")),
        "f1" to listOf(StudioInfo("Codemasters", "codemasters")),
        "dirt" to listOf(StudioInfo("Codemasters", "codemasters")),
        
        // Nintendo
        "zelda" to listOf(StudioInfo("Nintendo EPD", "nintendo-epd")),
        "mario" to listOf(StudioInfo("Nintendo EPD", "nintendo-epd")),
        "metroid prime" to listOf(StudioInfo("Retro Studios", "retro-studios")),
        "donkey kong" to listOf(StudioInfo("Retro Studios", "retro-studios")),
        "xenoblade" to listOf(StudioInfo("Monolith Soft", "monolith-soft")),
        "kirby" to listOf(StudioInfo("HAL Laboratory", "hal-laboratory")),
        "smash bros" to listOf(StudioInfo("HAL Laboratory", "hal-laboratory")),
        "fire emblem" to listOf(StudioInfo("Intelligent Systems", "intelligent-systems")),
        "pokemon" to listOf(StudioInfo("Game Freak", "game-freak")),
        
        // FromSoftware
        "dark souls" to listOf(StudioInfo("FromSoftware", "fromsoftware")),
        "elden ring" to listOf(StudioInfo("FromSoftware", "fromsoftware")),
        "sekiro" to listOf(StudioInfo("FromSoftware", "fromsoftware")),
        "bloodborne" to listOf(StudioInfo("FromSoftware", "fromsoftware")),
        "armored core" to listOf(StudioInfo("FromSoftware", "fromsoftware")),
        
        // Square Enix
        "final fantasy" to listOf(StudioInfo("Square Enix", "square-enix")),
        "kingdom hearts" to listOf(StudioInfo("Square Enix", "square-enix")),
        "tomb raider" to listOf(StudioInfo("Crystal Dynamics", "crystal-dynamics")),
        "deus ex" to listOf(StudioInfo("Eidos Montreal", "eidos-montreal")),
        "guardians of the galaxy" to listOf(StudioInfo("Eidos Montreal", "eidos-montreal")),
        
        // CD Projekt
        "witcher" to listOf(StudioInfo("CD Projekt Red", "cd-projekt-red")),
        "cyberpunk" to listOf(StudioInfo("CD Projekt Red", "cd-projekt-red")),
        
        // Capcom
        "resident evil" to listOf(StudioInfo("Capcom", "capcom")),
        "monster hunter" to listOf(StudioInfo("Capcom", "capcom")),
        "devil may cry" to listOf(StudioInfo("Capcom", "capcom")),
        "street fighter" to listOf(StudioInfo("Capcom", "capcom")),
        
        // Warner Bros
        "batman arkham" to listOf(StudioInfo("Rocksteady Studios", "rocksteady-studios")),
        "suicide squad" to listOf(StudioInfo("Rocksteady Studios", "rocksteady-studios")),
        "mortal kombat" to listOf(StudioInfo("NetherRealm Studios", "netherrealm-studios")),
        "injustice" to listOf(StudioInfo("NetherRealm Studios", "netherrealm-studios")),
        "shadow of mordor" to listOf(StudioInfo("Monolith Productions", "monolith-productions")),
        "shadow of war" to listOf(StudioInfo("Monolith Productions", "monolith-productions")),
        "lego" to listOf(StudioInfo("TT Games", "tt-games")),
        "hogwarts legacy" to listOf(StudioInfo("Avalanche Software", "avalanche-software")),
        
        // Atlus
        "persona" to listOf(StudioInfo("Atlus", "atlus")),
        "shin megami tensei" to listOf(StudioInfo("Atlus", "atlus")),
        "smt" to listOf(StudioInfo("Atlus", "atlus")),
        
        // Other notable series
        "civilization" to listOf(StudioInfo("Firaxis Games", "firaxis-games")),
        "xcom" to listOf(StudioInfo("Firaxis Games", "firaxis-games")),
        "borderlands" to listOf(StudioInfo("Gearbox Software", "gearbox-software")),
        "mafia" to listOf(StudioInfo("Hangar 13", "hangar-13")),
        "metro" to listOf(StudioInfo("4A Games", "4a-games")),
        "saints row" to listOf(StudioInfo("Volition", "volition")),
        "serious sam" to listOf(StudioInfo("Croteam", "croteam")),
        "cities skylines" to listOf(StudioInfo("Colossal Order", "colossal-order")),
        "crusader kings" to listOf(StudioInfo("Paradox Development Studio", "paradox-development-studio")),
        "europa universalis" to listOf(StudioInfo("Paradox Development Studio", "paradox-development-studio")),
        "stellaris" to listOf(StudioInfo("Paradox Development Studio", "paradox-development-studio"))
    )
    
    /**
     * Get studios that develop a specific game series.
     * Returns null if the query doesn't match a known game series.
     */
    fun getStudiosForGameSeries(query: String): List<StudioInfo>? {
        val normalized = query.lowercase().trim()
        
        // Direct lookup
        GAME_SERIES_TO_STUDIOS[normalized]?.let { return it }
        
        // Partial match - find series that contain the query
        GAME_SERIES_TO_STUDIOS.entries.find { (series, _) ->
            series.contains(normalized) || normalized.contains(series)
        }?.let { return it.value }
        
        return null
    }
    
    /**
     * Check if query matches a known game series.
     */
    fun isGameSeries(query: String): Boolean {
        val normalized = query.lowercase().trim()
        return GAME_SERIES_TO_STUDIOS.containsKey(normalized) ||
               GAME_SERIES_TO_STUDIOS.keys.any { it.contains(normalized) || normalized.contains(it) }
    }
}
