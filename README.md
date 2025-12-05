# Arcadia 🎮

**Arcadia** is a modern, feature-rich Android application designed for gamers to discover, track, and manage their video game collections. Built with the latest Android technologies, Arcadia leverages the **RAWG API** for extensive game data and **Gemini AI** for personalized insights and recommendations.

## ✨ Features

*   **Game Discovery**: Explore a vast library of games powered by the [RAWG Video Games Database API](https://rawg.io/apidocs).
*   **Smart Tracking**: Manage your gaming journey with dedicated lists for **My Games**, **Backlog**, and **Favorites**.
*   **AI-Powered Insights**: Utilize **Gemini** and **Groq** AI models to get personalized game recommendations, summaries, and insights.
*   **Rich Media Experience**: High-quality game artwork and video trailers (powered by **ExoPlayer** and **Coil**).
*   **User Management**: Secure authentication and cloud synchronization using **Firebase Auth** and **Firestore**.
*   **Modern UI/UX**: A sleek, responsive interface built with **Jetpack Compose** and **Material 3**, featuring smooth animations and drag-and-drop interactions.
*   **Offline Support**: Robust local caching with **Room Database** ensures your data is available even without an internet connection.

## 🛠️ Tech Stack

Arcadia is built using modern Android development best practices and libraries:

*   **Language**: [Kotlin](https://kotlinlang.org/)
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetbrains/compose) (Material 3)
*   **Architecture**: Clean Architecture (Data, Domain, Presentation) + MVVM
*   **Dependency Injection**: [Koin](https://insert-koin.io/)
*   **Networking**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
*   **Local Database**: [Room](https://developer.android.com/training/data-storage/room)
*   **Asynchronous Programming**: [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
*   **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
*   **Video Playback**: [ExoPlayer (Media3)](https://developer.android.com/media/media3/exoplayer)
*   **Backend & Auth**: [Firebase](https://firebase.google.com/) (Auth, Firestore, Storage)
*   **AI Integration**: [Google AI Client SDK](https://ai.google.dev/) (Gemini)

## 🚀 Getting Started

### Prerequisites

*   Android Studio Ladybug or newer.
*   JDK 21.
*   API Keys for:
    *   **RAWG API**: Get one at [rawg.io](https://rawg.io/apidocs).
    *   **Gemini API**: Get one at [aistudio.google.com](https://aistudio.google.com/).
    *   **Groq API**: Get one at [console.groq.com](https://console.groq.com/).

### Installation

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/Arcadia.git
    cd Arcadia
    ```

2.  **Configure API Keys**:
    Create a `local.properties` file in the root directory (if it doesn't exist) and add your API keys:
    ```properties
    sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk
    RAWG_API_KEY=your_rawg_api_key_here
    GEMINI_API_KEY=your_gemini_api_key_here
    GROQ_API_KEY=your_groq_api_key_here
    ```

3.  **Sync Project**:
    Open the project in Android Studio and sync Gradle files.

4.  **Run the App**:
    Connect an Android device or start an emulator and run the `app` configuration.

## 📂 Project Structure

The project follows the **Clean Architecture** principles:

*   **`domain`**: Contains the business logic, use cases, and repository interfaces. Pure Kotlin, no Android dependencies.
*   **`data`**: Implements the repository interfaces, handles API calls (Retrofit), and local storage (Room).
*   **`presentation`**: Contains the UI code (Compose screens, ViewModels, Components).
*   **`di`**: Dependency injection modules (Koin).

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
