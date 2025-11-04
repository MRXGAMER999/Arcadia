package com.example.arcadia.util

/**
 * Countries and their major cities for profile selection
 */
object Countries {
    val countryToCities = mapOf(
        "USA" to listOf("New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose"),
        "India" to listOf("Delhi", "Mumbai", "Bangalore", "Hyderabad", "Chennai", "Kolkata", "Pune", "Ahmedabad", "Jaipur", "Lucknow"),
        "UK" to listOf("London", "Manchester", "Liverpool", "Birmingham", "Leeds", "Glasgow", "Sheffield", "Edinburgh", "Bristol", "Newcastle"),
        "Germany" to listOf("Berlin", "Munich", "Hamburg", "Frankfurt", "Cologne", "Stuttgart", "Dusseldorf", "Dortmund", "Essen", "Leipzig"),
        "Canada" to listOf("Toronto", "Montreal", "Vancouver", "Calgary", "Edmonton", "Ottawa", "Winnipeg", "Quebec City", "Hamilton", "Kitchener"),
        "Australia" to listOf("Sydney", "Melbourne", "Brisbane", "Perth", "Adelaide", "Gold Coast", "Canberra", "Newcastle", "Wollongong", "Geelong"),
        "France" to listOf("Paris", "Marseille", "Lyon", "Toulouse", "Nice", "Nantes", "Strasbourg", "Montpellier", "Bordeaux", "Lille"),
        "Japan" to listOf("Tokyo", "Osaka", "Nagoya", "Sapporo", "Fukuoka", "Kobe", "Kyoto", "Kawasaki", "Saitama", "Hiroshima"),
        "Brazil" to listOf("São Paulo", "Rio de Janeiro", "Brasília", "Salvador", "Fortaleza", "Belo Horizonte", "Manaus", "Curitiba", "Recife", "Porto Alegre"),
        "Mexico" to listOf("Mexico City", "Guadalajara", "Monterrey", "Puebla", "Tijuana", "León", "Juárez", "Zapopan", "Mérida", "San Luis Potosí")
    )
    
    val countries: List<String> = countryToCities.keys.sorted()
    
    fun getCitiesForCountry(country: String): List<String> {
        return countryToCities[country] ?: emptyList()
    }
}

