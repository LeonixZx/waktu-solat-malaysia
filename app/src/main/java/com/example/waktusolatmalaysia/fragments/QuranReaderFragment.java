package com.example.waktusolatmalaysia.fragments;

import android.media.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.ui.*;
import java.util.*;
import java.util.concurrent.*;

public final class QuranReaderFragment extends Fragment {
  private LinearLayout root;
  private RecyclerView list;
  private TextView title, status, playStatus;
  private UserStore store;
  private MediaPlayer player;
  private ExecutorService worker;
  private List<QuranVerse> verses = new ArrayList<>();
  private String name = "Al-Quran";
  private int number, generation;
  private ImageButton save;
  private RecyclerView.Adapter<AyahHolder> adapter;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle saved) {
    worker = Executors.newSingleThreadExecutor();
    generation++;
    store = new UserStore(requireContext());
    number = requireArguments().getInt("surah", 1);
    root = Ui.column(requireContext());
    root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
    root.setPadding(
        Ui.dp(requireContext(), 20), Ui.dp(requireContext(), 12), Ui.dp(requireContext(), 20), 0);
    LinearLayout header = Ui.row(requireContext());
    title = Ui.text(requireContext(), "Al-Quran", 24, true, R.color.text_primary);
    header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    save =
        Ui.iconButton(
            requireContext(),
            R.drawable.ic_bookmark,
            "Simpan surah",
            () -> {
              store.toggle("surah:" + number);
              updateSaved();
            });
    save.setId(R.id.reader_bookmark);
    header.addView(save);
    root.addView(header);
    status =
        Ui.text(
            requireContext(),
            I18n.choose(requireContext(), "Memuatkan teks…", "Loading text…"),
            12,
            false,
            R.color.text_secondary);
    Ui.add(root, status, 8);
    list = new RecyclerView(requireContext());
    list.setId(R.id.reader_verses);
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout bar = Ui.row(requireContext());
    bar.setPadding(0, Ui.dp(requireContext(), 8), 0, Ui.dp(requireContext(), 8));
    playStatus =
        Ui.text(
            requireContext(),
            I18n.choose(requireContext(), "Alafasy · Audio distrim", "Alafasy · Streamed audio"),
            11,
            false,
            R.color.text_secondary);
    bar.addView(playStatus, new LinearLayout.LayoutParams(0, -2, 1));
    bar.addView(Ui.iconButton(requireContext(), R.drawable.ic_pause, "Henti", this::stop));
    root.addView(bar);
    load();
    return root;
  }

  private void load() {
    int token = generation;
    ContentRepository repo = new ContentRepository(requireContext());
    String language = store.text("quran_language", I18n.english(requireContext()) ? "en" : "ms");
    worker.execute(
        () -> {
          try {
            Surah meta = repo.surahs().get(number - 1);
            List<QuranVerse> data = repo.verses(number, language);
            new Handler(Looper.getMainLooper())
                .post(
                    () -> {
                      if (root == null || token != generation) return;
                      name = meta.name;
                      title.setText(name);
                      verses = data;
                      status.setText(
                          data.size()
                              + I18n.choose(requireContext(), " ayat · ", " ayahs · ")
                              + (language.equals("en")
                                  ? "Sahih International"
                                  : "Abdullah Muhammad Basmeih"));
                      updateSaved();
                      bind();
                    });
          } catch (Exception e) {
            new Handler(Looper.getMainLooper())
                .post(
                    () -> {
                      if (root == null || token != generation) return;
                      status.setText(
                          I18n.choose(
                              requireContext(),
                              "Bacaan belum tersimpan. Semak internet dan ketik untuk cuba lagi.",
                              "Text is not cached. Check the connection and tap to retry."));
                      status.setOnClickListener(v -> load());
                    });
          }
        });
  }

  private void bind() {
    adapter =
        new RecyclerView.Adapter<AyahHolder>() {
          @Override
          public int getItemCount() {
            return verses.size();
          }

          @Override
          public long getItemId(int position) {
            return verses.get(position).globalNumber;
          }

          @Override
          public AyahHolder onCreateViewHolder(ViewGroup parent, int type) {
            return new AyahHolder(new FrameLayout(parent.getContext()));
          }

          @Override
          public void onBindViewHolder(AyahHolder holder, int position) {
            holder.bind(verses.get(position));
          }
        };
    adapter.setHasStableIds(true);
    list.setItemAnimator(null);
    list.setItemViewCacheSize(8);
    list.setAdapter(adapter);
    int start = Math.max(0, Math.min(verses.size() - 1, requireArguments().getInt("verse", 1) - 1));
    list.scrollToPosition(start);
    store.bookmark(number, start + 1, name);
    list.addOnScrollListener(
        new RecyclerView.OnScrollListener() {
          @Override
          public void onScrollStateChanged(@NonNull RecyclerView view, int state) {
            if (state == RecyclerView.SCROLL_STATE_IDLE) {
              int position =
                  ((LinearLayoutManager) view.getLayoutManager()).findFirstVisibleItemPosition();
              if (position >= 0) store.bookmark(number, position + 1, name);
            }
          }
        });
  }

  private final class AyahHolder extends RecyclerView.ViewHolder {
    private final TextView label, arabic, meaning;
    private final ImageButton bookmark;
    private QuranVerse verse;

    AyahHolder(FrameLayout frame) {
      super(frame);
      android.content.Context c = frame.getContext();
      frame.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
      frame.setPadding(0, Ui.dp(c, 12), 0, 0);
      LinearLayout body = Ui.column(c), actions = Ui.row(c);
      label = Ui.text(c, "", 11, true, R.color.prayer_text);
      actions.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
      bookmark =
          Ui.iconButton(
              c,
              R.drawable.ic_bookmark,
              "Simpan",
              () -> {
                if (verse == null) return;
                store.toggle("ayah:" + number + ":" + verse.number);
                store.bookmark(number, verse.number, name);
                savedState();
              });
      actions.addView(bookmark);
      actions.addView(
          Ui.iconButton(
              c,
              R.drawable.ic_play,
              "Dengar",
              () -> {
                if (verse != null) play(verse);
              }));
      actions.addView(
          Ui.iconButton(
              c,
              R.drawable.ic_share,
              "Kongsi",
              () -> {
                if (verse != null)
                  Ui.share(
                      c, name + " " + verse.number + "\n" + verse.arabic + "\n\n" + verse.meaning);
              }));
      body.addView(actions);
      arabic = Ui.arabic(c, "", store.number("arabic_size", 30));
      Ui.add(body, arabic, 16);
      meaning = Ui.text(c, "", 15, false, R.color.text_secondary);
      meaning.setTextIsSelectable(true);
      Ui.add(body, meaning, 16);
      meaning.setVisibility(store.flag("show_translation", true) ? View.VISIBLE : View.GONE);
      body.setOnLongClickListener(
          v -> {
            if (verse == null) return false;
            store.bookmark(number, verse.number, name);
            Toast.makeText(
                    c,
                    I18n.choose(c, "Bacaan terakhir disimpan", "Reading position saved"),
                    Toast.LENGTH_SHORT)
                .show();
            return true;
          });
      frame.addView(Ui.card(c, body, 16));
    }

    void bind(QuranVerse value) {
      verse = value;
      label.setText(I18n.choose(requireContext(), "AYAT ", "AYAH ") + verse.number);
      arabic.setText(verse.arabic);
      meaning.setText(verse.meaning);
      savedState();
    }

    private void savedState() {
      boolean saved = store.saved("ayah:" + number + ":" + verse.number);
      bookmark.setImageResource(saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
      bookmark.setContentDescription(I18n.t(requireContext(), saved ? "Disimpan" : "Simpan"));
      bookmark.setSelected(saved);
    }
  }

  private void updateSaved() {
    save.setImageResource(
        store.saved("surah:" + number) ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
    save.setContentDescription(
        I18n.t(
            requireContext(), store.saved("surah:" + number) ? "Surah disimpan" : "Simpan surah"));
  }

  private void play(QuranVerse verse) {
    stop();
    playStatus.setText(
        I18n.choose(requireContext(), "Memuatkan audio · ", "Loading audio · ") + verse.number);
    try {
      player = new MediaPlayer();
      player.setAudioAttributes(
          new AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build());
      player.setDataSource(verse.audio);
      player.setOnPreparedListener(
          mp -> {
            if (root == null) {
              stop();
              return;
            }
            playStatus.setText(name + " · " + verse.number);
            mp.start();
          });
      player.setOnCompletionListener(mp -> stop());
      player.setOnErrorListener(
          (mp, a, b) -> {
            stop();
            if (root != null)
              playStatus.setText(
                  I18n.choose(
                      requireContext(),
                      "Audio tidak tersedia. Cuba lagi.",
                      "Audio unavailable. Please retry."));
            return true;
          });
      player.prepareAsync();
    } catch (Exception e) {
      stop();
      if (playStatus != null)
        playStatus.setText(
            I18n.choose(
                requireContext(),
                "Audio tidak tersedia. Cuba lagi.",
                "Audio unavailable. Please retry."));
    }
  }

  private void stop() {
    if (player != null) {
      player.release();
      player = null;
    }
    if (playStatus != null)
      playStatus.setText(
          I18n.choose(requireContext(), "Alafasy · Audio distrim", "Alafasy · Streamed audio"));
  }

  @Override
  public void onStop() {
    stop();
    super.onStop();
  }

  @Override
  public void onDestroyView() {
    generation++;
    stop();
    worker.shutdownNow();
    root = null;
    list = null;
    playStatus = null;
    super.onDestroyView();
  }
}
