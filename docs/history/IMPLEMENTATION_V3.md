# v3 delivery record

- [x] Original SVG mark, Android launcher/themed icons, PWA maskable icons, shared tokens and optimized generated artwork.
- [x] Five adaptive hubs with a single primary menu, inset-aware branding, separate Settings entry, clipped growing cards, grouped prayer rows, virtualized/two-pane readers and bilingual controls.
- [x] Migrated per-prayer modes, prayer-relative task CRUD, cache-only alarm restoration, background prefetch and three configurable native widgets.
- [x] Quran text queue/storage/delete, published English selection, devotional readers, curated articles/video, Saved, calendar export and portable backup previews.
- [x] React/TypeScript/Vite PWA, IndexedDB, Workbox and a bounded Cloudflare API adapter.
- [x] Native/unit/browser builds and automated checks, responsive captures, emulator widget host, Doze delivery, download queue and tablet upgrade checks recorded in VERIFICATION_V3.md.

The navigation cleanup removes the redundant drawer, shrinks the sidebar, flattens rail shadows and puts Downloads in Quran. The 3.0.2 follow-up keeps Settings selection separate from Saved, including after recreation and Back. See NAVIGATION_UX.md for its focused regression record. Production configuration is a release step: publisher signing, Cloudflare account/domain, content permissions and manual assistive-technology/OEM endurance review. The v3 baseline also passed widget host, offline, Doze and reboot checks. C:\Tools\waktu-re\ remains read-only.
