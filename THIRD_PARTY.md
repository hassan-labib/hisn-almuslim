# Third-party content

## Quran Foundation (Quran.com) content: NOT in this repository
Quran data provided by Quran Foundation (https://quran.foundation). Their Developer Terms
(https://api-docs.quran.foundation/legal/developer-terms/) do not allow including their content or raw API data in a public repository.
For that reason these files are removed from the source tree and are not licensed by this repo:
- app/src/main/assets/mushaf.json (Madinah Mushaf page layout and glyph codes, from the Quran.com API)
- app/src/main/assets/ayat.json (verse text used for tafsir lookup and copy)
- app/src/main/assets/rub.json (hizb quarter to page map)
- app/src/main/assets/fonts/p1.ttf to p3.ttf (QCF page fonts of the King Fahd Complex, supplied through Quran Foundation)
At run time the app downloads the other page fonts, and the tafsir (Muyassar, Sa'di, Ibn Kathir), from Quran Foundation hosts (verses.quran.foundation, api.quran.com).
Credit shown in the app: "Quran data provided by Quran Foundation."

## Other content
- Hisn al-Muslim text: asellam/HisnElMuslim (MIT), from the book by Sa'id bin Ali bin Wahf al-Qahtani.
- English adhkar translations: fitrahive/dua-dhikr (MIT).
- Hadith texts: fawazahmed0/hadith-api (Unlicense), downloaded at run time.
- Prayer mat recognition model: MobileNetV2 from TensorFlow Lite (Apache 2.0).
- Adhan recordings (Wikimedia Commons, all CC BY 3.0, https://creativecommons.org/licenses/by/3.0/), trimmed and volume adjusted:
  - adhan_makkah.ogg: "Adhan, Great Mosque of Mecca - Jan 21, 2013.webm", by Seyfula Islam
  - adhan_makkah2.ogg: "Maghrib Adhan at the Masjid al Haram, Mecca - 25 Feb, 2012.webm", by 3omar Faruq
  - adhan_madinah.ogg: "33937 ejaz215 call-to-prayer-from-the-prophet-s-mo.ogg", by ejaz215 (original on Freesound)
  - adhan_fajr.ogg: "Eid al-Fitr Fajr azan at Malmö Mosque - 19 August 2012.webm", by Islamic Center Malmö
  Licenses were re-checked on the Commons API on 2026-11-24.
