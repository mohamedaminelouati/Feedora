# Privacy Policy for Feedora

**Last updated:** August 29, 2026

**Feedora** is an open-source Android RSS feed reader developed and maintained by **Mohamed Amine Louati** ([mohamedaminlouati@gmail.com](mailto:mohamedaminlouati@gmail.com)).

Your privacy is a foundational principle of Feedora. This Privacy Policy outlines how your information is handled when using the Feedora application.

---

## 1. Zero Personal Data Collection

- **No Data Collection:** Feedora does not collect, harvest, store, or transmit any personal identifiable information (PII).
- **No Analytics or Trackers:** The application contains zero advertising libraries, zero tracking SDKs, and zero telemetry or analytics frameworks.
- **No User Registration:** Feedora does not require an account or registration with us to use the app.

---

## 2. Direct Network Communication

Feedora communicates over the network solely to fulfill your direct requests:

- **RSS / Atom Feeds:** The application connects directly to the RSS/Atom feed URLs you subscribe to in order to download and parse article content and favicons.
- **Third-Party RSS Services:** If you configure an account (Miniflux, Tiny Tiny RSS, Feedbin, Inoreader, Feedly, Fever, Google Reader API / FreshRSS), the application communicates directly with the endpoint server specified using the credentials or access tokens you provide. These credentials are encrypted and stored solely on your local device.
- **Optional AI Summarization:** If you explicitly invoke the AI summary feature on an article, the article URL is sent directly to your chosen summarizer provider to format the summary.
- **Application Updates:** When checking for application updates, the application queries the public GitHub Releases API (`https://api.github.com/repos/mohamedaminelouati/Feedora/releases/latest`) to determine if a newer version is available.

---

## 3. Local Storage & Data Security

- All your subscribed feeds, cached articles, reading history, and app preferences are stored exclusively on your local device storage in a private SQLite database.
- Exported OPML and backup files are saved only to the location you choose on your device.
- You can erase all application data at any time through the app settings or via the Android System Application Settings.

---

## 4. Permissions

Feedora requests only the minimal Android permissions necessary for its functionality:

- `android.permission.INTERNET`: Required to fetch RSS feeds and synchronize with chosen cloud providers.
- `android.permission.POST_NOTIFICATIONS`: Optional permission to alert you when new articles are fetched during background synchronization.
- `android.permission.WRITE_EXTERNAL_STORAGE` (on older Android versions): Only used when you explicitly export OPML or backup files.

---

## 5. Compliance & Open Source

Feedora is free software distributed under the terms of the **GNU General Public License v3.0 (GPL-3.0)**. The complete source code is publicly auditable at [https://github.com/mohamedaminelouati/Feedora](https://github.com/mohamedaminelouati/Feedora).

---

## 6. Contact & Inquiries

If you have any questions or feedback regarding this Privacy Policy, please contact:

- **Maintainer:** Mohamed Amine Louati
- **Email:** [mohamedaminlouati@gmail.com](mailto:mohamedaminlouati@gmail.com)
- **LinkedIn:** [https://www.linkedin.com/in/mohamed-amine-louati-a383a367](https://www.linkedin.com/in/mohamed-amine-louati-a383a367)
- **GitHub Issues:** [https://github.com/mohamedaminelouati/Feedora/issues](https://github.com/mohamedaminelouati/Feedora/issues)
