<div align="center">
<img src="app/src/main/ic_rounded_publisher.png" width=120 height=120 alt="TubeAIO NextGen Logo">

# TubeAIO NextGen

### 🎉 The Next Generation of All-in-One Video — Rewritten from the Ground Up
[![TubeAIO NextGen](https://img.shields.io/badge/TubeAIO-NextGen-brightgreen?style=for-the-badge)](https://github.com/shibaFoss/AIO-Video-Downloader)
----
[![GitHub repo](https://img.shields.io/github/stars/shibafoss/AIO-Video-Downloader?color=brightgreen&label=Stars&style=for-the-badge)](https://github.com/shibaFoss/AIO-Video-Downloader)
[![Follow on Instagram](https://img.shields.io/badge/Follow-%40shibafoss-ff69b4?logo=instagram&style=for-the-badge)](https://instagram.com/shibafoss)
<p align="center">
  <a href="#-about-the-project">About</a> •
  <a href="#-vision--goals">Vision</a> •
  <a href="#-key-features">Features</a> •
  <a href="#-screenshots">Screenshots</a> •
  <a href="#-tech-stack--architecture">Architecture</a> •
  <a href="#-getting-started">Getting Started</a> •
  <a href="#-join-the-team">Contributing</a>
</p>

<details>
  <summary>🌐 <b>Select Language (Read in your native language)</b></summary>
  <p align="center">
    <a href="README.md">English</a> | 
    <a href="docs/README_ZH.md">简体中文</a> | 
    <a href="docs/README_HI.md">हिन्दी</a> | 
    <a href="docs/README_ES.md">Español</a> | 
    <a href="docs/README_FR.md">Français</a> | 
    <a href="docs/README_ID.md">Bahasa Indonesia</a> | 
    <a href="docs/README_RU.md">Русский</a> | 
    <a href="docs/README_VI.md">Tiếng Việt</a>
  </p>
</details>

<br>

![TubeAIO NextGen](others/graphics/feature_cover.png)
</div>

---

## 📌 About the Project

**TubeAIO NextGen** is a ground-up rewrite of the beloved **[AIO Video Downloader](https://github.com/shibaFoss/AIO-Video-Downloader)** — one of the most popular open-source video downloaders on GitHub.

The original AIO was an incredible project. It served millions of users, was built on solid foundations like **yt-dlp**, and delivered a smooth, privacy-respecting experience that people genuinely loved. But as the feature set grew, so did the architectural constraints. Some ideas — like a recommendation engine, torrent support, a full-featured browser, and advanced background playback — were simply too complex to bolt onto the existing structure without compromising performance and maintainability.

**So we started fresh.**

TubeAIO NextGen is a complete reimagining of what an all-in-one video platform can be on Android. Every line of code, every module, and every design decision has been reconsidered from the ground up to support the features we know our community wants and deserves. The soul of AIO remains — open, private, ad-free, and community-driven — but everything else has been rebuilt to be faster, more scalable, and ready for the future.

> 🔗 **Heads up!** This project is a fresh branch of the original AIO Video Downloader. For the current stable release of the original AIO, check out the [main AIO repo here](https://github.com/shibaFoss/AIO-Video-Downloader).

---

## 🎯 Vision & Goals

TubeAIO NextGen isn't just an update — it's a statement of intent. Here's what we're building toward:

- 🤖 **Smart Recommendation Engine** — Get personalized video recommendations based on your watch history, searches, and interactions. Think of it as your own private curator.
- 🎵 **Robust Background Audio Playback** — Keep the music and podcasts playing even when you switch apps or lock your screen. No interruptions, just pure audio.
- 🌐 **Built-in Ad-Free Web Browser** — Browse the web without ads, and with a built-in video grabber that's always watching. Find a video anywhere, grab it instantly.
- 🎬 **Movie Download Section** — A dedicated section to search movie repositories, stream or download films — all from within the app.
- ⚡ **Torrent Support** — Download files and media directly via torrent, without needing a separate app.
- 🔒 **Private Vault** — Secure, app-locked storage for sensitive media, keeping your files hidden from the gallery.
- 🎬 **Powerful Video Player** — Hardware acceleration, subtitle support, and smooth playback up to 4K.
- 🌍 **Universal Platform Support** — Works with 1000+ websites via yt-dlp, plus a built-in browser for everything else.
- 🛡️ **100% Ad-Free & Open Source** — No ads, no trackers, no nonsense. Transparent, private, and community-owned.

And of course — everything the original AIO had and more. We're not leaving anything behind.

---

## ✨ Key Features

- 🎯 **Simple & Intuitive** — One-tap downloads and smart content detection.
- ⚡ **Supercharged Speed** — Multi-connection downloads and background processing.
- 🎬 **Powerful Player** — HW acceleration, subtitle support, and background playback.
- 🔒 **Private Vault** — Secure, app-locked storage for sensitive media.
- 🌐 **Universal Support** — Works with 1000+ sites via built-in secure browser.
- 🛡️ **Ad-Free & Open Source** — Transparent, safe, and privacy-respecting.

---

## 📱 Screenshots

> 📸 **Screenshots below are from the original AIO Video Downloader (legacy). The TubeAIO NextGen UI is currently being built from scratch. Fresh screenshots coming soon!**

<div align="center">
  <img src="others/graphics/screenshots/1.0.jpg" width="30%" />
  <img src="others/graphics/screenshots/2.0.jpg" width="30%" />
  <img src="others/graphics/screenshots/3.0.jpg" width="30%" />
  <br>
  <img src="others/graphics/screenshots/4.0.jpg" width="30%" />
  <img src="others/graphics/screenshots/5.0.jpg" width="30%" />
  <img src="others/graphics/screenshots/6.0.jpg" width="30%" />
</div>

---

## 💻 Tech Stack & Architecture

- **Language:** 100% Kotlin
- **Architecture:** Modular MVVM with strict separation of concerns, designed for scalability
- **Independent Modules:** Core logic is decoupled from UI for maintainability and testability
- **Data Layer:** Robust metadata handling, recommendation logic, and file state management
- **Custom UI:** Performance-optimized custom themes (Non-Material strict)
- **Primary Engines:**
  - [yt-dlp](https://github.com/yt-dlp/yt-dlp) / [youtubedl-android](https://github.com/yausername/youtubedl-android)
  - [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor)

---

## 🚀 Getting Started

1. **Browse or Search** — Use the built-in ad-free browser or search for movies and videos directly in the app.
2. **Auto-Detect** — The app automatically finds high-quality streams and torrents.
3. **Choose & Download** — Select your resolution (up to 4K) or pick a torrent to start downloading.
4. **Watch Anywhere** — Play back downloaded content with the powerful built-in player, or cast to your TV.
5. **Stay Private** — Move sensitive downloads to the **Private Folder** to hide them from the gallery.

---

## 🤝 Join the Team (Maintainers Wanted)

We are looking for **Project Maintainers and Contributors** to help drive the technical future of TubeAIO NextGen. This is a large-scale rewrite with ambitious goals — if you're a developer looking for a clean, modular Kotlin project to contribute to, we'd love to have you.

### 🛠 How You Can Help:

- **Core Development** — Build and optimize the download engine, torrent module, and background playback system.
- **Recommendation Engine** — Design and implement the smart recommendation system (algorithms, data pipelines, UI).
- **Browser Integration** — Help build the ad-free web browser with seamless video detection.
- **UI/UX Development** — Evolve the custom-themed, performance-first interface for the NextGen experience.
- **Movie Section** — Implement the movie search and repository integration.
- **Code Quality** — Review PRs, write tests, and help maintain a stable main branch.

### 📋 Tech Stack:
- **Language:** 100% Kotlin
- **Architecture:** Modular MVVM
- **Engines:** yt-dlp, NewPipe Extractor

> **Interested?** Please open a [New Issue](https://github.com/shibaFoss/AIO-Video-Downloader/issues) with the tag `[NextGen Contributor]` to introduce yourself and discuss how you'd like to help.

---

## 🔧 Technical Specifications

- **Platform:** Android 8.0+ (API 26)
- **Engine:** yt-dlp / youtubedl-android
- **Language:** Kotlin
- **License:** Custom Open Source License

---

<div align="center">
  <b>Made with ❤️ in India 🇮🇳</b>
  <br>
  <i>Respecting Privacy • Promoting Transparency</i>
</div>