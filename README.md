# Friday — Android AI assistant (by GM TUSHARX)

1. Android Studio (Ladybug 2024.2+ / newer), JDK 17 (bundled).
2. File > Open > select this `Friday` folder. Let Gradle sync (needs internet the first time).
   If Studio offers to upgrade AGP/Gradle/Kotlin, accept.
3. Use a real device (or an emulator image with Google Play + a working mic). Enable "Speech Recognition & Synthesis by Google".
4. Run ▶. Grant microphone + notification permissions when asked.
5. Try: "YouTube par Arijit Singh ke gaane chalao", "Play lofi on YouTube", "Search weather", "WhatsApp kholo",
   "Check status", "Tumhe kisne banaya?", "Who made you?".
6. Proactive check-in: gear icon > set minutes (1–30). While Friday is open it speaks after that much silence;
   after you leave the app, WorkManager posts a notification and speaks.
