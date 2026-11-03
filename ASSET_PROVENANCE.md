# Identity and asset provenance

## Public repository presentation

The README mosque masthead is original **built-in imagegen** artwork, generated without an image
input. Its exact prompt/mode and export records are in `design/assets/readme-art-v1.json`.
The original is `design/assets/originals/readme-mosque-v1.png`; the 1600px WebP derivative is
`docs/assets/readme-mosque.webp` (about 64KB). This is decorative art, not an app screenshot.

`docs/assets/android-{home,worship,dark}-v2.webp` are resized/encoded fresh Android 14 emulator
captures of the preserved v3.3.4 APK, checked for foreground focus and Android error dialogs at
capture time. They replace an initial selection that mistakenly included stale System UI ANR
frames. See `docs/SCREENSHOT_VERIFICATION.md` for the correction and testing boundaries.
They use the public Kuala Lumpur fixture and contain no personal phone data. No UI was generated,
painted, or removed from these captures. Each is under 35KB. Current native widget fixtures remain
in `design/widgets-v333/`.

Superseded widget preview masters, the logo's first draft and old v2 diagnostic source were archived
locally during public-repository cleanup. Their historical paths below describe earlier evidence,
not files required by the current build. All final runtime assets and original art masters remain.
See `docs/CONTENT_RIGHTS.md` for unresolved inherited-content/recording permissions.

Updated 2 November 2026 for the ivory/gold 3.0 redesign.

| Component | Source |
|---|---|
| Display name | Waktu Solat Malaysia, chosen by the owner; no trademark clearance claimed |
| Package | `com.example.waktusolatmalaysia`, development identity separate from the reference publisher |
| Native UI, logic and navigation | Authored for this project in Java and XML |
| Visual references | User-supplied UI-kit screenshots, used for mood/hierarchy; no screenshot art imported |
| Mosque hero | Original built-in imagegen output → `app/src/main/res/drawable-nodpi/hero_mosque.webp` |
| Quran banner | Original built-in imagegen output → `app/src/main/res/drawable-nodpi/banner_quran.webp` |
| Wisdom banner | Original built-in imagegen output → `app/src/main/res/drawable-nodpi/banner_hikmah.webp` |
| Prompts / mode | `design/assets/image-prompts.json`; built-in imagegen, new generation, opaque background |
| Icons and launcher | Original native vector paths/drawing code, a consistent vector feature set plus the new crescent/arch/clock launcher |
| Latin typography | Plus Jakarta Sans from Google Fonts, SIL OFL. Static derivatives named **WSM Sans** at 400/500/600/700; generated with `tools/prepare_fonts.py` to avoid redistributing a derivative under the reserved family name |
| Arabic typography | Amiri Regular from Google Fonts, SIL OFL |
| Font licenses | `app/src/main/assets/licenses/jakarta-ofl.txt`, `amiri-ofl.txt`; original copyright retained in font metadata |
| Zone catalog | 60 records obtained directly from JAKIM; retrieval metadata in `prayer_zones.json` |
| Prayer times | Direct public JAKIM HTTPS e-Solat monthly endpoint; app-private validated cache |
| Quran metadata/text/translation/audio URLs | AlQuran Cloud / Islamic Network: quran-uthmani, ms.basmeih, en.sahih, ar.alafasy |
| Offline hadith | fawazahmed0/hadith-api: Sahih al-Bukhari Arabic, English and Indonesian IDs 1, 8, 10, 13, 15, 16, 52, 69, 71, 5027, 6018, 6412; repository Unlicense saved as `licenses/hadith-api.txt` |
| Prayer guides | Owner-authorized copy of reference `assets/doa.json` → `prayer_guides.json` |
| Wirid | Reference `assets/wiridZikir.json` → `wirid.json` |
| Prayer intentions | Reference `assets/niatSolat.json` → `prayer_intentions.json` |
| Important dates | Reference `assets/importantIslamicDates.json` → `islamic_dates.json`, 2023–2032 |
| Azan | Reference `res/raw/azanbiasa.mp3` and `azansubuh.mp3` → `adhan_standard.mp3` / `adhan_fajr.mp3` |

All reference reads/copies were performed without modifying `C:\Tools\waktu-re\`. The user explicitly authorized resources from the reference for this development build. This authorization does **not** establish third-party public redistribution rights. Keep these developer provenance records and obtain the applicable content, translation and recording permissions before public distribution. No decoded smali, original app fonts, layout files, branding, credentials, ad SDKs or certificate material were imported.

The app's source-credit screen identifies Quran and hadith providers and labels the Indonesian hadith translations. The Arabic religious texts, translations and supplied calendar references still warrant content review. Public API access is not a blanket license. A publisher-owned final application ID, name clearance, provider terms and distribution rights remain release tasks; this document makes no legal guarantee.

The root `.apks` input remains excluded from the Android build and version control.

Primary source records:

- https://www.e-solat.gov.my/
- https://alquran.cloud/api
- https://github.com/fawazahmed0/hadith-api
- https://github.com/google/fonts/tree/main/ofl/plusjakartasans
- https://github.com/google/fonts/tree/main/ofl/amiri

## v3 original assets and curated media

- `design/brand/mark.svg` is the original geometric crescent/arch/clock master. `icon.svg` adds the ivory field. Android vectors include an unpadded drawer brand mark, padded launcher foreground and monochrome mark. `web/tools/brand.mjs` exports the legacy/favicon/PWA PNG derivatives; platform safe bounds are built into the exports.
- `design/assets/image-prompts-v3.json` records the new onboarding, Explore library and Ramadan artwork. Mode: **built-in imagegen, new generation, opaque background**. No reference-publisher artwork was used as an image input. All generated PNG originals are retained in `design/assets/originals/`.
- Runtime files are `app/src/main/res/drawable-nodpi/{hero_mosque,banner_quran,banner_hikmah,onboarding_v3,explore_v3,ramadan_v3}.webp` and matching `web/public/assets/` derivatives. Each is 1200×800, 58–89 KB; all six together are approximately 440 KB. Original PNGs are source assets, excluded from the APK/PWA runtime.
- Plus Jakarta/WSM Sans and Amiri license records remain bundled on both platforms.
- Hadith English fields use the corresponding published `eng-bukhari` edition from Hadith API; translations are not generated. Guide/wirid/niat English fields come from the supplied source data.
- `shared/catalog.json` records eight reviewed TV JAKIM, Mufti Wilayah and Yaqeen entries with stable IDs, publisher, language, original URL and permitted provider video IDs where available. YouTube thumbnails are genuine publisher thumbnails loaded from `i.ytimg.com`; they are not generated. Full articles remain at the original publisher.


## Android widget assets — 3.2.0

The widget check/unchecked glyphs, resize glyph, backgrounds and ripple resources in `res/drawable/widget_*` are original native XML vectors/shapes created for this release. All prayer labels/times remain native scalable text. No Imagegen output, reference-APK artwork or new external image/font dependency was added. Jakarta/system font fallback and the existing original launcher mark reuse the recorded app assets. Report previews are crops of direct device AppWidgetHost screenshots; full unmodified captures are retained separately.


## Widget refinement — 3.2.1

`res/drawable/widget_prayer_*.xml` contains original 24-unit line-art vectors for prayer/daylight cues. `widget_bg_light.xml` and `widget_bg_dark.xml` use original native gradients and strokes. `res/drawable-nodpi/widget_preview_*.png` is generated from our own native renderer with labelled sample data. Both palette masters are in `design/widgets-v321/`; the export test is `WidgetAppearanceV321Test`. These static picker images are separate from live widget text, which remains native. The installed reference app’s public previews were inspected in a local QA directory only; no publisher asset is included in resources or delivery archives.

## Generated mosque and Android identity — 3.2.2

Built-in **imagegen** created `design/brand-v322/mosque-original.png` and `logo-first-draft.png`; a built-in imagegen cleanup edit produced `logo-original.png` from that draft. All retain genuine source alpha. Exact prompts and the input role are in `design/brand-v322/prompts.json`. No reference-publisher image was supplied to generation or editing.

Runtime derivatives are `res/drawable-nodpi/widget_mosque_v322.png` (600×400), `brand_mark_v322.png` (512×512), padded `brand_launcher_foreground_v322.png` (512×512) and density-specific legacy launcher PNGs. Exporting resizes/re-encodes and applies platform padding/background; source imagery is retained. Widget opacity is applied by the native view, not baked into labels or schedules. The foreground silhouette also supplies the monochrome mask. Native-rendered picker masters are in `design/widgets-v322/`; shipping derivatives remain under 250 KB each. See `DESIGN_V322.md` for locations and implementation details.

## Phone UI — 3.3.0

`res/drawable/nav_{home,quran,worship,explore,saved}.xml` contains original monochrome navigation vectors. The floating navigation surface, night artwork scrims, selection colours, settings animation XML and compact widget templates are native project assets. The 3.2.2 generated logo/mosque sources and their provenance remain unchanged. No publisher resource, generated religious text or new external asset was added.

## Timeline widgets — 3.3.1

The reference's compact/wide/square/rectangle layouts and picker previews were inspected read-only to understand density and hierarchy. The new `widget_timeline_*` XML, geometric curve, rings, dividers and plate shapes were authored in this project. No reference XML, smali, logo, font or raster asset was copied. The existing generated mosque, gold mark and Jakarta fonts retain their previous source and licence records; no new image generation was needed.

Native picker captures and their labelled fixture metadata are retained in `design/widgets-v331/`. The four shipping `widget_preview_*.png` derivatives are under 250KB each and come from `WidgetTimelineV331Test` at the providers' preferred dimensions, with tracking disabled. Runtime times remain native validated text; static preview fixtures are never written into production prayer caches.

## Compact widget previews — 3.3.2

The compact curved rectangle and shallow micro-band use project-authored native XML and existing vectors, Jakarta fonts, logo and generated mosque. No new image generation or reference asset was added. My widgets/configuration render the actual instance dimensions before scaling the finished non-interactive preview; static launcher images remain preferred-size examples. The delivery's launcher image is a crop of our widget from a direct phone screenshot, with unrelated home-screen content excluded. Its app-page capture and native render are direct exports, not redrawn mockups.

## Paired calendar dates and place names — 3.3.3

The compact header spacing, named Hijri labels and readable location captions are project-authored native UI. `WidgetDates` formats JAKIM's supplied date without conversion or generated religious text. The eight original renderer captures in `design/widgets-v333/` use labelled 3 November 2026 fixtures; four optimized light picker derivatives replace the old illustrations. They reuse the existing generated mosque/logo and Jakarta fonts, add no third-party artwork, and each remain below 250KB. The export tool and `previews.json` record paths, sizes and hashes. Fixtures are never written to prayer caches.
