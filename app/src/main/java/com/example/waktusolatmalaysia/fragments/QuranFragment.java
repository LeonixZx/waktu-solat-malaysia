package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.ui.*;
import com.google.android.material.tabs.TabLayout;
import java.util.*;

public final class QuranFragment extends LibraryFragment {
  private List<Surah> data = new ArrayList<>();
  private final List<int[]> visible = new ArrayList<>();
  private boolean saved;
  private String query = "";
  private UserStore store;
  private RecyclerView.Adapter<SurahHolder> adapter;

  @Override
  protected void build(Bundle state) {
    store = new UserStore(requireContext());
    heading(
        "Al-Quran",
        I18n.choose(
            requireContext(),
            "Baca dan sambung dari ayat terakhir.",
            "Read and continue from your last ayah."));
    android.view.View resume = Ui.quiet(requireContext(), "Sambung bacaan", R.drawable.ic_quran);
    resume.setOnClickListener(
        v -> read(store.number("last_surah", 1), store.number("last_verse", 1)));
    View downloads = Ui.quiet(requireContext(), "Muat turun", R.drawable.ic_download);
    downloads.setOnClickListener(v -> open(R.id.downloadsFragment, null, null));
    LinearLayout actions = Ui.row(requireContext());
    LinearLayout.LayoutParams resumeSize = new LinearLayout.LayoutParams(0, -2, 1);
    resumeSize.setMarginEnd(Ui.dp(requireContext(), 8));
    actions.addView(resume, resumeSize);
    actions.addView(downloads, new LinearLayout.LayoutParams(0, -2, 1));
    Ui.add(head, actions, 12);
    TabLayout tabs = new TabLayout(requireContext());
    for (String title : new String[] {"Semua surah", "Tersimpan"})
      tabs.addTab(tabs.newTab().setText(I18n.t(requireContext(), title)));
    tabs.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          public void onTabSelected(TabLayout.Tab t) {
            saved = t.getPosition() == 1;
            filter();
          }

          public void onTabUnselected(TabLayout.Tab t) {}

          public void onTabReselected(TabLayout.Tab t) {}
        });
    Ui.add(head, tabs, 8);
    EditText search = new EditText(requireContext());
    search.setId(R.id.quran_search);
    search.setHint(I18n.t(requireContext(), "Cari nama atau nombor surah"));
    search.setSingleLine();
    search.setTextSize(14);
    search.setMinHeight(Ui.dp(requireContext(), 48));
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int a, int c, int f) {}

          public void onTextChanged(CharSequence s, int a, int before, int count) {
            query = s.toString();
            filter();
          }

          public void afterTextChanged(Editable e) {}
        });
    Ui.add(head, search, 8);
    list.setId(R.id.quran_list);
    adapter =
        new RecyclerView.Adapter<SurahHolder>() {
          @Override
          public SurahHolder onCreateViewHolder(ViewGroup parent, int type) {
            return new SurahHolder(new FrameLayout(parent.getContext()));
          }

          @Override
          public int getItemCount() {
            return visible.size();
          }

          @Override
          public long getItemId(int position) {
            int[] item = visible.get(position);
            return item[0] * 1000L + item[1];
          }

          @Override
          public void onBindViewHolder(SurahHolder holder, int position) {
            int[] item = visible.get(position);
            Surah surah = data.get(item[0] - 1);
            holder.number.setText(String.format(Locale.ROOT, "%02d", surah.number));
            holder.title.setText(surah.name);
            holder.subtitle.setText(
                item[1] > 0
                    ? I18n.choose(requireContext(), "Ayat ", "Ayah ") + item[1]
                    : surah.verses
                        + I18n.choose(requireContext(), " ayat · ", " ayahs · ")
                        + (surah.revelation.equals("Meccan") ? "Makkiyah" : "Madaniyah"));
            if (holder.arabic != null) holder.arabic.setText(surah.arabicName);
          }
        };
    adapter.setHasStableIds(true);
    list.setItemAnimator(null);
    list.setItemViewCacheSize(12);
    list.setAdapter(adapter);
    task(
        () -> new ContentRepository(requireContext()).surahs(),
        result -> {
          data = result;
          filter();
        },
        error ->
            heading(
                I18n.choose(requireContext(), "Senarai tidak tersedia", "List unavailable"), ""));
  }

  private final class SurahHolder extends RecyclerView.ViewHolder {
    final TextView number, title, subtitle, arabic;

    SurahHolder(FrameLayout frame) {
      super(frame);
      android.content.Context c = frame.getContext();
      frame.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
      frame.setPadding(0, 0, 0, Ui.dp(c, 8));
      LinearLayout row = Ui.row(c);
      row.setPadding(Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14));
      row.setBackground(Ui.background(c, R.color.surface, 20));
      number = Ui.text(c, "", 12, true, R.color.prayer_text);
      number.setGravity(Gravity.CENTER);
      row.addView(
          number,
          new LinearLayout.LayoutParams(
              Ui.dp(c, 36 * Math.max(1, c.getResources().getConfiguration().fontScale)), -2));
      LinearLayout words = Ui.column(c);
      LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, -2, 1);
      wp.leftMargin = Ui.dp(c, 8);
      row.addView(words, wp);
      title = Ui.text(c, "", 15, true, R.color.text_primary);
      subtitle = Ui.text(c, "", 11, false, R.color.text_secondary);
      words.addView(title);
      Ui.add(words, subtitle, 4);
      if (c.getResources().getConfiguration().fontScale <= 1.3f) {
        arabic = Ui.arabic(c, "", 18);
        arabic.setTextIsSelectable(false);
        row.addView(arabic);
      } else arabic = null;
      row.setOnClickListener(
          v -> {
            int position = getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            int[] item = visible.get(position);
            read(item[0], Math.max(1, item[1]));
          });
      frame.addView(row);
    }
  }

  private void filter() {
    if (adapter == null || data.isEmpty()) return;
    visible.clear();
    String q = query.trim().toLowerCase(Locale.ROOT);
    for (Surah s : data)
      if ((!saved || store.saved("surah:" + s.number))
          && (s.name + " " + s.arabicName + " " + s.number).toLowerCase(Locale.ROOT).contains(q))
        visible.add(new int[] {s.number, 0});
    if (saved)
      for (String id : new TreeSet<>(store.savedIds()))
        if (id.startsWith("ayah:"))
          try {
            String[] p = id.split(":");
            int n = Integer.parseInt(p[1]), v = Integer.parseInt(p[2]);
            if (n >= 1
                && n <= data.size()
                && (data.get(n - 1).name + " " + n + " " + v).toLowerCase(Locale.ROOT).contains(q))
              visible.add(new int[] {n, v});
          } catch (Exception ignored) {
          }
    adapter.notifyDataSetChanged();
  }

  private void read(int number, int verse) {
    Bundle args = new Bundle();
    args.putInt("surah", number);
    args.putInt("verse", verse);
    open(R.id.quranReaderFragment, args, new QuranReaderFragment());
  }

  @Override
  public void onResume() {
    super.onResume();
    filter();
  }
}
