package com.example.waktusolatmalaysia.fragments;

import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.Reading;
import com.example.waktusolatmalaysia.preferences.UserStore;
import com.example.waktusolatmalaysia.repo.ContentRepository;
import com.example.waktusolatmalaysia.ui.*;
import com.google.android.material.button.MaterialButton;
import java.util.*;

public final class CollectionFragment extends LibraryFragment {
  private static final String[] KINDS = {"doa", "wirid", "niat", "hadith"};
  private String kind = "doa", query = "", selected;
  private boolean saved, loading, failed, restoring;
  private List<Reading> data = new ArrayList<>();
  private final List<Integer> visible = new ArrayList<>();
  private RecyclerView.Adapter<Holder> adapter;
  private ChoiceBar tabs;
  private EditText search;
  private MaterialButton only;
  private Bundle categories = new Bundle();
  private Parcelable listPosition;

  @Override
  protected boolean inlineReader() {
    return true;
  }

  @Override
  protected void build(Bundle state) {
    UserStore store = new UserStore(requireContext());
    if (state != null && state.getBundle("categories") != null)
      categories = state.getBundle("categories");
    kind = state == null ? store.text("collection_tab", "doa") : state.getString("kind", "doa");
    if (!Arrays.asList(KINDS).contains(kind)) kind = "doa";
    tabs =
        new ChoiceBar(
            requireContext(),
            new String[] {"Doa", "Wirid", "Niat", "Hadis"},
            null,
            Arrays.asList(KINDS).indexOf(kind),
            index -> {
              saveCategory();
              clearDetail();
              load(KINDS[index]);
            });
    tabs.setId(R.id.collection_tabs);
    Ui.add(top, tabs, 0);
    search = new EditText(requireContext());
    search.setSingleLine();
    search.setId(R.id.collection_search);
    search.setHint(I18n.t(requireContext(), "Cari dalam koleksi"));
    search.setTextSize(14);
    search.setMinHeight(Ui.dp(requireContext(), 48));
    com.google.android.material.textfield.TextInputLayout field =
        new com.google.android.material.textfield.TextInputLayout(requireContext());
    field.setBoxBackgroundMode(
        com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
    field.setBoxCornerRadii(
        Ui.dp(requireContext(), 12),
        Ui.dp(requireContext(), 12),
        Ui.dp(requireContext(), 12),
        Ui.dp(requireContext(), 12));
    field.setHint(I18n.t(requireContext(), "Cari dalam koleksi"));
    field.addView(search, new LinearLayout.LayoutParams(-1, -2));
    field.setHintEnabled(false);
    field.setBoxBackgroundColor(Ui.color(requireContext(), R.color.surface));
    search.setHint(I18n.t(requireContext(), "Cari dalam koleksi"));
    search.setHintTextColor(Ui.color(requireContext(), R.color.text_secondary));
    search.setBackground(null);
    search.setPadding(
        Ui.dp(requireContext(), 14),
        Ui.dp(requireContext(), 14),
        Ui.dp(requireContext(), 14),
        Ui.dp(requireContext(), 14));
    search.setTextColor(Ui.color(requireContext(), R.color.text_primary));
    Ui.add(head, field, 12);
    only = Ui.quiet(requireContext(), "", R.drawable.ic_bookmark);
    only.setId(R.id.collection_saved_filter);
    only.setOnClickListener(
        v -> {
          saved = !saved;
          filter();
        });
    Ui.add(head, only, 8);
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int a, int c, int f) {}

          public void onTextChanged(CharSequence s, int a, int b, int c) {
            if (!restoring) {
              query = s.toString();
              filter();
            }
          }

          public void afterTextChanged(Editable e) {}
        });
    list.setId(R.id.collection_list);
    list.setItemAnimator(null);
    adapter =
        new RecyclerView.Adapter<Holder>() {
          @Override
          public Holder onCreateViewHolder(ViewGroup p, int type) {
            return new Holder(new FrameLayout(p.getContext()));
          }

          @Override
          public int getItemCount() {
            return visible.size();
          }

          @Override
          public long getItemId(int position) {
            return data.get(visible.get(position)).id.hashCode();
          }

          @Override
          public void onBindViewHolder(Holder h, int position) {
            int index = visible.get(position);
            Reading reading = data.get(index);
            h.frame.removeAllViews();
            h.frame.setPadding(0, 0, 0, Ui.dp(requireContext(), 8));
            View row =
                Ui.line(
                    requireContext(),
                    reading.title,
                    reading.reference,
                    kind.equals("hadith") ? R.drawable.ic_hadith : R.drawable.ic_doa,
                    () -> select(index));
            row.setSelected(reading.id.equals(selected));
            if (row.isSelected())
              row.setBackground(Ui.background(requireContext(), R.color.soft_gold, 12));
            androidx.core.view.ViewCompat.setStateDescription(
                row,
                row.isSelected()
                    ? I18n.choose(requireContext(), "Bacaan dipilih", "Selected reading")
                    : null);
            h.frame.addView(
                row,
                new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
          }
        };
    adapter.setHasStableIds(true);
    list.setAdapter(adapter);
    status.setOnClickListener(
        v -> {
          if (failed) load(kind);
        });
    load(kind);
  }

  private void saveCategory() {
    Bundle s = new Bundle();
    s.putString("query", query);
    s.putString("selected", selected);
    s.putBoolean("saved", saved);
    if (list != null && list.getLayoutManager() != null)
      s.putParcelable("scroll", list.getLayoutManager().onSaveInstanceState());
    categories.putBundle(kind, s);
  }

  private void load(String k) {
    kind = k;
    new UserStore(requireContext()).set("collection_tab", k);
    Bundle s = categories.getBundle(k);
    if (s == null) s = new Bundle();
    query = s.getString("query", "");
    selected = s.getString("selected");
    saved = s.getBoolean("saved");
    listPosition = s.getParcelable("scroll");
    restoring = true;
    search.setText(query);
    restoring = false;
    loading = true;
    failed = false;
    data = new ArrayList<>();
    filter();
    task(
        () -> new ContentRepository(requireContext()).readings(k),
        rows -> {
          if (!kind.equals(k)) return;
          data = rows;
          loading = false;
          filter();
          if (listPosition != null) {
            list.getLayoutManager().onRestoreInstanceState(listPosition);
            listPosition = null;
          }
        },
        error -> {
          if (kind.equals(k)) {
            loading = false;
            failed = true;
            filter();
          }
        });
  }

  void refreshSaved() {
    filter();
  }

  private void filter() {
    if (adapter == null) return;
    only.setText(
        I18n.choose(
            requireContext(),
            saved ? "Tersimpan sahaja · aktif" : "Semua bacaan",
            saved ? "Saved only · active" : "All readings"));
    only.setSelected(saved);
    androidx.core.view.ViewCompat.setStateDescription(
        only, saved ? I18n.choose(requireContext(), "Penapis aktif", "Filter active") : null);
    visible.clear();
    UserStore store = new UserStore(requireContext());
    String q = query.toLowerCase(Locale.ROOT).trim();
    for (int i = 0; i < data.size(); i++) {
      Reading r = data.get(i);
      String roman =
          String.join(" ", r.sections.stream().map(s -> s.transliteration).toArray(String[]::new));
      if ((!saved || store.saved(r.id))
          && (r.title + " " + r.meaning + " " + r.arabic + " " + roman)
              .toLowerCase(Locale.ROOT)
              .contains(q)) visible.add(i);
    }
    status.setText(
        loading
            ? I18n.choose(requireContext(), "Memuat bacaan…", "Loading readings…")
            : failed
                ? I18n.choose(
                    requireContext(),
                    "Bacaan tidak tersedia. Ketik untuk cuba semula.",
                    "Readings unavailable. Tap to retry.")
                : visible.isEmpty()
                    ? I18n.choose(
                        requireContext(),
                        saved ? "Tiada bacaan disimpan yang sepadan." : "Tiada bacaan sepadan.",
                        saved ? "No matching saved readings." : "No matching readings.")
                    : I18n.choose(
                        requireContext(),
                        visible.size() + " bacaan",
                        visible.size() + " readings"));
    status.setMinHeight(Ui.dp(requireContext(), failed ? 48 : 0));
    status.setClickable(failed);
    adapter.notifyDataSetChanged();
    if (embedded() && !loading && !failed) {
      Integer match =
          visible.stream().filter(i -> data.get(i).id.equals(selected)).findFirst().orElse(null);
      if (match == null && !visible.isEmpty()) match = visible.get(0);
      if (match != null) show(match);
      else clearDetail();
    }
  }

  private void select(int index) {
    show(index);
    adapter.notifyDataSetChanged();
  }

  private void show(int index) {
    selected = data.get(index).id;
    Bundle args = new Bundle();
    args.putString("kind", kind);
    args.putString("reading_id", selected);
    args.putInt("index", index);
    open(R.id.readingFragment, args, new ReadingFragment());
  }

  @Override
  protected void onPaneChanged(boolean wide) {
    if (wide) filter();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (adapter != null) filter();
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    saveCategory();
    super.onSaveInstanceState(out);
    out.putString("kind", kind);
    out.putBundle("categories", categories);
  }
}
