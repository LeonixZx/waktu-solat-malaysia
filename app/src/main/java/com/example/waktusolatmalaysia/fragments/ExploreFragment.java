package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.ui.*;
import java.util.*;

public final class ExploreFragment extends FeatureFragment {
  private LinearLayout list;
  private List<CatalogEntry> data = new ArrayList<>();
  private int filter;

  @Override
  protected void build(@Nullable Bundle s) {
    heading(
        "Terokai",
        I18n.choose(
            requireContext(),
            "Belajar daripada sumber yang jelas.",
            "Learn from clearly attributed sources."));
    Ui.add(
        page,
        Ui.banner(
            requireContext(),
            R.drawable.explore_v3,
            I18n.t(requireContext(), "Video"),
            "TV JAKIM · Yaqeen Institute",
            null),
        16);
    filter = s == null ? 0 : s.getInt("explore_filter", 0);
    ChoiceBar tabs =
        new ChoiceBar(
            requireContext(),
            new String[] {
              I18n.t(requireContext(), "Semua"),
              I18n.t(requireContext(), "Video"),
              I18n.t(requireContext(), "Artikel")
            },
            new int[] {R.drawable.ic_explore, R.drawable.ic_video, R.drawable.ic_article},
            filter,
            index -> {
              filter = index;
              if (list != null) render();
            });
    tabs.setId(R.id.explore_filters);
    Ui.add(page, tabs, 16);
    list = Ui.column(requireContext());
    list.setId(R.id.explore_list);
    Ui.add(page, list, 8);
    task(
        () -> CatalogRepository.load(requireContext()),
        entries -> {
          data = entries;
          render();
        },
        e ->
            Ui.add(
                list,
                Ui.text(
                    requireContext(),
                    I18n.choose(
                        requireContext(),
                        "Katalog belum dapat dibaca.",
                        "The catalog could not be read."),
                    14,
                    false,
                    R.color.text_secondary),
                12));
  }

  private void render() {
    list.removeAllViews();
    for (CatalogEntry e : data) {
      if (filter == 1 && e.type.equals("article") || filter == 2 && !e.type.equals("article"))
        continue;
      LinearLayout body = Ui.column(requireContext());
      if (!e.thumbnail.isEmpty()) {
        ImageView thumbnail = new ImageView(requireContext());
        thumbnail.setScaleType(ImageView.ScaleType.CENTER);
        thumbnail.setImageResource(
            e.type.equals("article") ? R.drawable.ic_article : R.drawable.ic_video);
        thumbnail.setBackgroundColor(Ui.color(requireContext(), R.color.control_surface));
        body.addView(thumbnail, new LinearLayout.LayoutParams(-1, Ui.dp(requireContext(), 150)));
        task(
            () -> {
              javax.net.ssl.HttpsURLConnection connection =
                  (javax.net.ssl.HttpsURLConnection) new java.net.URL(e.thumbnail).openConnection();
              connection.setConnectTimeout(10000);
              connection.setReadTimeout(10000);
              try (java.io.InputStream in = connection.getInputStream()) {
                android.graphics.BitmapFactory.Options options =
                    new android.graphics.BitmapFactory.Options();
                options.inSampleSize = 2;
                return android.graphics.BitmapFactory.decodeStream(in, null, options);
              } finally {
                connection.disconnect();
              }
            },
            bitmap -> {
              if (thumbnail.isAttachedToWindow() && bitmap != null) {
                thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                thumbnail.setImageBitmap(bitmap);
              }
            },
            error -> {});
      }
      Ui.add(
          body,
          Ui.text(
              requireContext(),
              e.provider + " · " + (e.language.equals("en") ? "English" : "Bahasa Melayu"),
              11,
              true,
              R.color.prayer_text),
          0);
      Ui.add(body, Ui.text(requireContext(), e.title, 17, true, R.color.text_primary), 8);
      Ui.add(body, Ui.text(requireContext(), e.summary, 13, false, R.color.text_secondary), 8);
      LinearLayout actions = Ui.row(requireContext());
      android.view.View open =
          Ui.quiet(
              requireContext(),
              e.type.equals("article") ? "Buka sumber" : "Tonton",
              e.type.equals("article") ? R.drawable.ic_article : R.drawable.ic_play);
      open.setOnClickListener(
          v -> {
            if (e.type.equals("article") || e.youtubeId.isEmpty())
              CatalogRepository.external(requireContext(), e.url);
            else {
              Bundle args = new Bundle();
              args.putString("id", e.id);
              open(R.id.videoFragment, args);
            }
          });
      LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(0, -2, 1);
      actionParams.setMarginEnd(Ui.dp(requireContext(), 8));
      actions.addView(open, actionParams);
      UserStore store = new UserStore(requireContext());
      actions.addView(
          Ui.iconButton(
              requireContext(),
              store.saved("catalog:" + e.id)
                  ? R.drawable.ic_bookmark_filled
                  : R.drawable.ic_bookmark,
              "Simpan",
              () -> {
                store.toggle("catalog:" + e.id);
                render();
              }));
      Ui.add(body, actions, 12);
      Ui.add(list, Ui.card(requireContext(), body, 16), 12);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    out.putInt("explore_filter", filter);
  }

  @Override
  public void onDestroyView() {
    list = null;
    super.onDestroyView();
  }
}
