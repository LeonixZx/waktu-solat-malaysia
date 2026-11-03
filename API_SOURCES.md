# API/source records · 2 November 2026

## Used by this build

- JAKIM e-Solat: `https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=month&zone=WLY01&year=2026&month=11`. HTTPS monthly response validated for requested zone and date. All date/time calculations use Asia/Kuala_Lumpur.
- Quran metadata: `https://api.alquran.cloud/v1/surah` (114 records bundled).
- Quran content: `https://api.alquran.cloud/v1/surah/1/editions/quran-uthmani,ms.basmeih,ar.alafasy,en.sahih`. Verified four editions with seven aligned Al-Fatihah ayahs. Surah responses are validated for edition, surah, ayah count/order and global ayah IDs before atomic caching.
- Recitation: HTTPS audio URLs returned by AlQuran Cloud; Islamic Network CDN. Audio streams on demand and stops when the reader leaves the foreground.
- Hadith: `https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ind-bukhari/1.min.json` and corresponding `ara-bukhari`. Twelve referenced records are bundled. Indonesian translations are labelled explicitly.

## Reference endpoints inspected, not integrated

| Reference service | Read-only endpoint observation |
|---|---|
| Hadith CMS | `https://cms-app.waktusolat.digital/api/v1/hadiths/mobile` returned HTTP 401 |
| Quran | `https://raudhahku.com.my/api/v1/surahs` returned HTTP 401 |
| Doa | `https://www.waktusolat.digital/api/doa.json` returned HTTP 403 |
| Islamic calendar | `https://www.waktusolat.digital/api/dates.json` returned HTTP 403 |
| Articles | `https://www.waktusolat.digital/wp-json/wp/v2/posts?per_page=4&_embed=1` returned HTTP 403 |

These statuses describe this session, not a permanent availability claim. No embedded API keys, authentication tokens or publisher credentials are copied or replayed. The offline guides/date/audio resources explicitly requested by the owner are catalogued in ASSET_PROVENANCE.md. The publisher feed is excluded from the launch catalog; v3 uses reviewed institutional source links instead.

## v3 additions

v3.1 adds a user-opened reference to [eFalak Selangor](https://efalak.muftiselangor.gov.my), as described in [Jabatan Mufti Selangor's compass-accuracy article](https://www.muftiselangor.gov.my/2025/11/11/17-taudhih-al-falak-ketepatan-aplikasi-telefon-bimbit-dalam-menentukan-arah-kiblat/). This is an external reference link, not a new upstream API or embedded map.

- Published English Quran translation: `en.sahih` in the four-edition AlQuran Cloud response.
- Published English hadith: `https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-bukhari/1.min.json` and corresponding selected IDs.
- TV JAKIM: https://www.islam.gov.my/en/pengumuman/12-orang-awam/2271-tv-jakim ; official video/live-channel links in shared/catalog.json.
- Mufti Wilayah articles: original `muftiwp.gov.my/ms/artikel/al-kafi-li-al-fatawi/` pages in the reviewed catalog.
- Yaqeen: https://yaqeeninstitute.org/watch and original article/video pages in the catalog. English entries carry their language label.
- Official YouTube IFrame API: https://developers.google.com/youtube/iframe_api_reference . Provider restrictions/errors produce an original-source fallback; no private playback service is used.

The local Worker adapter was exercised against live JAKIM (30 days in November 2026) and Al-Fatihah (four editions). It exposes fixed provider routes only. See ARCHITECTURE_V3.md for endpoint contracts and limits.
