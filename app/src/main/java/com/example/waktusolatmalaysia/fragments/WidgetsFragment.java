package com.example.waktusolatmalaysia.fragments;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.services.WidgetScheduleRefreshWorker;
import com.example.waktusolatmalaysia.ui.*;
import com.example.waktusolatmalaysia.widgets.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import java.util.*;

public final class WidgetsFragment extends FeatureFragment {
  private LinearLayout content;
  private TabLayout tabs;

  @Override
  protected void build(@Nullable Bundle state) {
    heading(
        I18n.choose(requireContext(), "Widget waktu solat", "Prayer widgets"),
        I18n.choose(
            requireContext(),
            "Ringkas, jelas dan tersedia di skrin utama.",
            "Compact prayer times for your home screen."));
    tabs = new TabLayout(requireContext());
    tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
    tabs.addTab(
        tabs.newTab().setText(I18n.choose(requireContext(), "Tambah widget", "Add widgets")));
    tabs.addTab(tabs.newTab().setText(I18n.choose(requireContext(), "Widget saya", "My widgets")));
    Ui.add(page, tabs, 16);
    content = Ui.column(requireContext());
    Ui.add(page, content, 8);
    tabs.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          public void onTabSelected(TabLayout.Tab t) {
            show(t.getPosition());
          }

          public void onTabUnselected(TabLayout.Tab t) {}

          public void onTabReselected(TabLayout.Tab t) {
            show(t.getPosition());
          }
        });
    if (state != null) tabs.selectTab(tabs.getTabAt(state.getInt("widgetTab", 0)));
    show(tabs.getSelectedTabPosition());
  }

  @Override
  public void onResume() {
    super.onResume();
    if (tabs != null && content != null) show(tabs.getSelectedTabPosition());
  }

  @Override
  public void onSaveInstanceState(Bundle state) {
    if (tabs != null) state.putInt("widgetTab", tabs.getSelectedTabPosition());
    super.onSaveInstanceState(state);
  }

  private void show(int tab) {
    if (content == null) return;
    content.removeAllViews();
    if (tab == 0) {
      for (WidgetProviderDescriptor d : WidgetProviderDescriptor.ALL) {
        Ui.add(
            content,
            Ui.text(
                requireContext(),
                I18n.choose(requireContext(), d.ms, d.en) + " · " + d.size,
                18,
                true,
                R.color.text_primary),
            24);
        preview(d, 0);
        View add =
            Ui.quiet(
                requireContext(),
                I18n.choose(requireContext(), "Tambah ke skrin utama", "Add to home screen"),
                R.drawable.ic_widget);
        add.setContentDescription(
            I18n.choose(requireContext(), "Tambah ", "Add ")
                + I18n.choose(requireContext(), d.ms, d.en));
        add.setOnClickListener(v -> pin(d));
        Ui.add(content, add, 8);
      }
    } else {
      Map<Integer, WidgetProviderDescriptor> all =
          WidgetProviderDescriptor.instances(requireContext());
      if (all.isEmpty())
        Ui.add(
            content,
            Ui.text(
                requireContext(),
                I18n.choose(
                    requireContext(),
                    "Belum ada widget. Tambah daripada tab sebelah atau menu widget pelancar.",
                    "No widgets yet. Add one from the other tab or your launcher's widget menu."),
                14,
                false,
                R.color.text_secondary),
            16);
      for (Map.Entry<Integer, WidgetProviderDescriptor> e : all.entrySet()) {
        int id = e.getKey();
        WidgetProviderDescriptor d = e.getValue();
        WidgetConfigV2 cfg = WidgetConfigV2.read(requireContext(), id, d);
        com.example.waktusolatmalaysia.model.PrayerZone z = cfg.resolvedZone(requireContext());
        Ui.add(
            content,
            Ui.text(
                requireContext(),
                I18n.choose(requireContext(), d.ms, d.en) + " · #" + id,
                18,
                true,
                R.color.text_primary),
            24);
        Ui.add(
            content,
            Ui.text(
                requireContext(),
                (z == null ? "—" : WidgetLocations.name(z, false))
                    + " · "
                    + (cfg.theme.equals("system")
                        ? I18n.choose(requireContext(), "Automatik", "Automatic")
                        : cfg.theme.equals("light") ? "Ivory" : "Noir")
                    + (cfg.trackingEnabled
                        ? I18n.choose(requireContext(), " · Rekod aktif", " · Tracking on")
                        : "")
                    + " · "
                    + (cfg.layoutStyle.equals("timeline")
                        ? I18n.choose(requireContext(), "Garis masa", "Timeline")
                        : I18n.choose(requireContext(), "Klasik", "Classic")),
                14,
                false,
                R.color.text_secondary),
            4);
        preview(d, id);
        View edit =
            Ui.quiet(
                requireContext(),
                I18n.choose(requireContext(), "Tetapan widget", "Widget settings"),
                R.drawable.ic_settings);
        edit.setOnClickListener(
            v ->
                startActivity(
                    new Intent(requireContext(), WidgetConfigActivity.class)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)));
        Ui.add(content, edit, 8);
        View refresh =
            Ui.quiet(
                requireContext(),
                I18n.choose(requireContext(), "Muat semula jadual", "Refresh schedules"),
                R.drawable.ic_refresh);
        refresh.setOnClickListener(
            v -> {
              if (z != null) WidgetScheduleRefreshWorker.enqueue(requireContext(), z.code);
              PrayerWidgets.refreshAsync(requireContext());
              Toast.makeText(
                      requireContext(),
                      I18n.choose(
                          requireContext(),
                          "Kemas kini dijadualkan apabila bersambung.",
                          "Refresh queued for a network connection."),
                      Toast.LENGTH_SHORT)
                  .show();
            });
        Ui.add(content, refresh, 4);
      }
    }
    View guide =
        Ui.quiet(
            requireContext(),
            I18n.choose(requireContext(), "Panduan menambah widget", "Widget setup guide"),
            R.drawable.ic_info);
    guide.setOnClickListener(v -> guide());
    Ui.add(content, guide, 24);
  }

  private void preview(WidgetProviderDescriptor d, int id) {
    WidgetPreviewView box = new WidgetPreviewView(requireContext());
    box.setTag("widget-preview-" + id);
    if (d == WidgetProviderDescriptor.NEXT) box.setId(R.id.widget_preview);
    if (id <= 0) {
      Ui.add(
          content,
          Ui.text(
              requireContext(),
              I18n.choose(
                  requireContext(),
                  "Contoh susun atur. Saiz sebenar ditentukan oleh pelancar skrin utama.",
                  "Layout example. Your home-screen launcher determines the actual size."),
              13,
              false,
              R.color.text_secondary),
          4);
      ChoiceBar sizes =
          new ChoiceBar(
              requireContext(),
              new String[] {
                I18n.choose(requireContext(), "Padat", "Compact"),
                I18n.choose(requireContext(), "Luas", "Spacious")
              },
              null,
              0,
              choice -> showPreview(box, d, id, exampleSize(d, choice == 1)));
      Ui.add(content, sizes, 8);
    } else {
      Ui.add(
          content,
          Ui.text(
              requireContext(),
              I18n.choose(
                  requireContext(),
                  "Mengikut saiz widget dipasang",
                  "Uses the placed widget's size"),
              13,
              false,
              R.color.text_secondary),
          4);
    }
    content.addView(box, new LinearLayout.LayoutParams(-1, -2));
    showPreview(
        box,
        d,
        id,
        id > 0 ? PrayerWidgets.presentationSize(requireContext(), id, d) : exampleSize(d, false));
  }

  private android.util.SizeF exampleSize(WidgetProviderDescriptor d, boolean spacious) {
    if (spacious)
      return d == WidgetProviderDescriptor.NEXT
          ? new android.util.SizeF(280, 300)
          : new android.util.SizeF(d.width, d.height);
    if (d == WidgetProviderDescriptor.NEXT) return new android.util.SizeF(180, 180);
    if (d.preferStrip) return new android.util.SizeF(250, 82);
    if (d == WidgetProviderDescriptor.DAILY) return new android.util.SizeF(280, 150);
    return new android.util.SizeF(280, 300);
  }

  private void showPreview(
      WidgetPreviewView box, WidgetProviderDescriptor d, int id, android.util.SizeF size) {
    box.show(
        PrayerWidgets.render(
            requireContext(),
            id,
            d,
            WidgetConfigV2.read(requireContext(), id, d),
            size.getWidth(),
            size.getHeight(),
            true),
        size);
  }

  private void pin(WidgetProviderDescriptor d) {
    AppWidgetManager manager = AppWidgetManager.getInstance(requireContext());
    if (Build.VERSION.SDK_INT >= 26 && manager.isRequestPinAppWidgetSupported()) {
      int[] before = manager.getAppWidgetIds(new ComponentName(requireContext(), d.component));
      Intent callback =
          new Intent(requireContext(), WidgetPinReceiver.class)
              .putExtra("component", d.component.getName())
              .putExtra("before", before)
              .setData(Uri.parse("wsm://widget-pin/" + SystemClock.elapsedRealtime()));
      PendingIntent result =
          PendingIntent.getBroadcast(
              requireContext(),
              0,
              callback,
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      if (manager.requestPinAppWidget(
          new ComponentName(requireContext(), d.component), null, result)) {
        Toast.makeText(
                requireContext(),
                I18n.choose(
                    requireContext(),
                    "Sahkan permintaan pelancar. Jika tiada permintaan, gunakan panduan di bawah.",
                    "Confirm the launcher request. If none appears, use the setup guide below."),
                Toast.LENGTH_LONG)
            .show();
        return;
      }
    }
    guide();
  }

  private void guide() {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(I18n.choose(requireContext(), "Panduan widget", "Widget setup guide"))
        .setMessage(
            I18n.choose(
                requireContext(),
                "Tekan lama ruang kosong skrin utama → Widget → Waktu Solat Malaysia. Pilih widget,"
                    + " kemudian seret ke skrin utama. Tekan lama widget untuk mengubah saiz. Pada"
                    + " MIUI/Xiaomi, gunakan menu ini jika butang tambah tidak menunjukkan"
                    + " permintaan. Tetapan setiap widget tersedia dalam Widget saya.",
                "Long-press an empty home-screen area → Widgets → Waktu Solat Malaysia. Choose a"
                    + " widget and drag it onto the home screen. Long-press it to resize. On"
                    + " MIUI/Xiaomi, use this menu if Add shows no request. Each instance can be"
                    + " configured in My widgets."))
        .setPositiveButton(I18n.choose(requireContext(), "Tutup", "Close"), null)
        .show();
  }
}
