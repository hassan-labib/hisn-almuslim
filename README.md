# حصن المسلم (Hisn al-Muslim)

Free Islamic Android app with no ads, no accounts and no tracking. Sadaqah jariyah for Hassan and his parents.

Madinah Mushaf pages, tafsir, hadith, adhan and iqama, daily adhkar (one dhikr per screen with a counter), hourly dhikr overlay, bookmarks, prayer lock, Arabic and English interface.

Contact: hassanlabib2155@gmail.com

## Missing Quran data files (read this first)
The Quran page data is Quran Foundation content and may not be redistributed in a public repository, so these files are NOT included:
`app/src/main/assets/mushaf.json`, `ayat.json`, `rub.json`, `fonts/p1.ttf`..`p3.ttf`.
Without them the project compiles, but the Mushaf screens cannot show pages (not yet tested at run time).
The maintainer builds release APKs with the files in place. A script to fetch them from the Quran Foundation API under their terms (free developer account, credit shown in the app) is planned after Quran Foundation answers. See THIRD_PARTY.md.

## Build
JDK 17, Android SDK 34. `gradle assembleRelease` produces an unsigned APK. To sign your own build set `KS_FILE` and `KS_PASS`.

## License
MIT for the code (see LICENSE). Content has its own licenses (THIRD_PARTY.md).
