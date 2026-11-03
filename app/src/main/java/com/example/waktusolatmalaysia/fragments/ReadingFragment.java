package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.Reading;
import com.example.waktusolatmalaysia.preferences.UserStore;
import com.example.waktusolatmalaysia.repo.ContentRepository;
import com.example.waktusolatmalaysia.ui.*;

public final class ReadingFragment extends FeatureFragment {
  @Override protected int maxContentWidth() { return 680; }
  @Override protected void build(Bundle saved) {
    String kind = requireArguments().getString("kind", "doa");
    String id = requireArguments().getString("reading_id");
    int index = requireArguments().getInt("index", 0);
    task(() -> new ContentRepository(requireContext()).readings(kind), rows -> {
      Reading reading = id == null ? (index >= 0 && index < rows.size() ? rows.get(index) : null)
          : rows.stream().filter(r -> r.id.equals(id)).findFirst().orElse(null);
      if (reading == null) { heading(I18n.choose(requireContext(), "Bacaan tidak ditemui", "Reading not found"), ""); return; }
      UserStore store = new UserStore(requireContext());
      LinearLayout header = Ui.row(requireContext()); header.setGravity(Gravity.TOP);
      TextView title = Ui.text(requireContext(), reading.title, 22, true, R.color.text_primary);
      androidx.core.view.ViewCompat.setAccessibilityHeading(title,true);
      header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
      View bookmark = Ui.iconButton(requireContext(), R.drawable.ic_bookmark, "", () -> {});
      bookmark.setId(R.id.reading_bookmark);
      Runnable bookmarkState = () -> {
        boolean on = store.saved(reading.id);
        bookmark.setSelected(on);
        bookmark.setBackground(Ui.background(requireContext(), on ? R.color.soft_gold : R.color.surface, 12));
        bookmark.setContentDescription(I18n.choose(requireContext(), on ? "Bacaan disimpan" : "Simpan bacaan", on ? "Reading saved" : "Save reading"));
        androidx.core.view.ViewCompat.setStateDescription(bookmark, on ? I18n.choose(requireContext(), "Disimpan", "Saved") : null);
      };
      bookmark.setOnClickListener(v -> { store.toggle(reading.id); bookmarkState.run();
        if (getParentFragment() instanceof CollectionFragment) ((CollectionFragment)getParentFragment()).refreshSaved(); });
      bookmarkState.run(); header.addView(bookmark);
      StringBuilder share = new StringBuilder(reading.title);
      for (Reading.Section section : reading.sections) {
        share.append("\n\n").append(section.title).append("\n").append(section.arabic);
        if (!section.transliteration.isEmpty()) share.append("\nRumi\n").append(section.transliteration);
        share.append("\n").append(language(section.language(reading.english))).append("\n").append(section.translation(reading.english));
      }
      share.append("\n\n").append(reading.reference);
      View shareButton = Ui.iconButton(requireContext(), R.drawable.ic_share, I18n.choose(requireContext(), "Kongsi bacaan", "Share reading"), () -> Ui.share(requireContext(), share.toString()));
      LinearLayout.LayoutParams shareSize = (LinearLayout.LayoutParams) shareButton.getLayoutParams();
      shareSize.setMarginStart(Ui.dp(requireContext(),8)); header.addView(shareButton,shareSize);
      Ui.add(page, header, 0);
      for (Reading.Section section : reading.sections) {
        LinearLayout body = Ui.column(requireContext());
        if (!section.title.isEmpty()) Ui.add(body, Ui.text(requireContext(), section.title, 16, true, R.color.text_primary), 0);
        Ui.add(body, Ui.text(requireContext(), I18n.choose(requireContext(), "Arab", "Arabic"), 11, true, R.color.prayer_text), 12);
        Ui.add(body, Ui.arabic(requireContext(), section.arabic, store.number("arabic_size", 30)), 12);
        if (!section.transliteration.isEmpty()) {
          Ui.add(body, Ui.text(requireContext(), "Rumi", 12, true, R.color.prayer_text), 20);
          TextView roman = Ui.text(requireContext(), section.transliteration, 14, false, R.color.text_secondary);
          roman.setTextIsSelectable(true); Ui.add(body, roman, 6);
          roman.setTextLocale(java.util.Locale.forLanguageTag("ms-Latn"));
        }
        if (reading.english && section.englishTranslation.isEmpty())
          Ui.add(body, Ui.text(requireContext(), "English translation unavailable. The supplied translation is labelled below.", 12, false, R.color.text_secondary), 20);
        Ui.add(body, Ui.text(requireContext(), I18n.choose(requireContext(), "Terjemahan · ", "Translation · ") + language(section.language(reading.english)), 12, true, R.color.prayer_text), 20);
        TextView meaning = Ui.text(requireContext(), section.translation(reading.english).isEmpty()
            ? I18n.choose(requireContext(), "Terjemahan belum tersedia.", "Translation unavailable.") : section.translation(reading.english), 15, false, R.color.text_secondary);
        meaning.setTextIsSelectable(true); Ui.add(body, meaning, 6);
        meaning.setTextLocale(java.util.Locale.forLanguageTag(section.language(reading.english)));
        Ui.add(page, Ui.card(requireContext(), body, 20), 16);
      }
      Ui.add(page, Ui.text(requireContext(), I18n.choose(requireContext(), "Rujukan · ", "Reference · ") + reading.reference, 12, false, R.color.text_secondary), 20);
      int position = saved == null ? 0 : saved.getInt("reading_scroll");
      scroll.post(() -> { if (scroll != null) scroll.scrollTo(0, position); });
    }, error -> heading(I18n.choose(requireContext(), "Bacaan belum tersedia", "Reading unavailable"), I18n.choose(requireContext(), "Sila buka koleksi semula.", "Please reopen the collection.")));
  }
  private String language(String code) { return code.equals("en") ? "English" : code.equals("id") ? "Bahasa Indonesia" : "Bahasa Melayu"; }
  @Override public void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); if (scroll != null) out.putInt("reading_scroll", scroll.getScrollY()); }
}
