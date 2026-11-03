package com.example.waktusolatmalaysia.widgets;

import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.*;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.PrayerZone;
import com.example.waktusolatmalaysia.repo.ZoneRepository;
import com.example.waktusolatmalaysia.services.WidgetScheduleRefreshWorker;
import com.example.waktusolatmalaysia.ui.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.*;
import org.json.*;

/** All changes are local drafts until Save, including after configuration recreation. */
public final class WidgetConfigActivity extends AppCompatActivity {
  private int id;
  private WidgetProviderDescriptor descriptor;
  private JSONObject draft;
  private WidgetPreviewView preview;
  private MaterialButton zoneButton;
  private boolean built;

  @Override
  protected void attachBaseContext(Context c) {
    super.attachBaseContext(I18n.wrap(c));
  }

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setResult(RESULT_CANCELED);
    id = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
    descriptor = WidgetProviderDescriptor.instance(this, id);
    if (descriptor == null) {
      finish();
      return;
    }
    try {
      draft =
          state == null
              ? WidgetConfigV2.read(this, id, descriptor).json()
              : new JSONObject(state.getString("draft"));
    } catch (Exception e) {
      draft = WidgetConfigV2.read(this, id, descriptor).json();
    }
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    ConstraintLayout root = new ConstraintLayout(this);
    root.setBackgroundColor(Ui.color(this, R.color.background));
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setId(View.generateViewId());
    ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0, 0);
    lp.topToTop = 0;
    lp.bottomToBottom = 0;
    lp.startToStart = 0;
    lp.endToEnd = 0;
    root.addView(scroll, lp);
    setContentView(root);
    LinearLayout page = Ui.column(this);
    FrameLayout bounds = new FrameLayout(this);
    scroll.addView(bounds, new ScrollView.LayoutParams(-1, -2));
    bounds.addView(page, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_HORIZONTAL));
    bounds.addOnLayoutChangeListener(
        (v, l, t, r, b, ol, ot, or, ob) -> {
          int w = Math.min(r - l, dp(680));
          if (page.getLayoutParams().width != w) {
            page.getLayoutParams().width = w;
            page.requestLayout();
          }
        });
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, insets) -> {
          androidx.core.graphics.Insets bars =
              insets.getInsets(
                  WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
          root.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return insets;
        });
    page.setPadding(dp(16), dp(16), dp(16), dp(24));
    Ui.add(
        page,
        Ui.text(
            this, I18n.choose(this, descriptor.ms, descriptor.en), 24, true, R.color.text_primary),
        0);
    Ui.add(
        page,
        Ui.text(
            this,
            I18n.choose(
                this,
                "Pratonton menggunakan jadual tersimpan sebenar.",
                "Preview uses real cached schedules."),
            14,
            false,
            R.color.text_secondary),
        8);
    preview = new WidgetPreviewView(this);
    preview.setId(R.id.widget_config_preview);
    preview.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    page.addView(preview, new LinearLayout.LayoutParams(-1, -2));
    MaterialCheckBox follow =
        check(
            I18n.choose(this, "Ikut zon aplikasi", "Follow app zone"),
            draft.optBoolean("followApp", true));
    follow.setId(R.id.widget_follow);
    Ui.add(page, follow, 16);
    zoneButton = Ui.quiet(this, "", R.drawable.ic_location);
    zoneButton.setEnabled(!follow.isChecked());
    zoneLabel();
    zoneButton.setOnClickListener(v -> chooseZone());
    Ui.add(page, zoneButton, 4);
    follow.setOnCheckedChangeListener(
        (v, on) -> {
          put("followApp", on);
          zoneButton.setEnabled(!on);
          zoneLabel();
          preview();
        });
    Ui.add(
        page,
        Ui.text(
            this, I18n.choose(this, "Penampilan", "Appearance"), 16, true, R.color.text_primary),
        16);
    String[] labels = {I18n.choose(this, "Automatik", "Automatic"), "Ivory", "Noir"},
        themes = {"system", "light", "dark"};
    Spinner appearance = new Spinner(this);
    appearance.setMinimumHeight(dp(48));
    appearance.setAdapter(
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
    appearance.setSelection(
        Math.max(0, Arrays.asList(themes).indexOf(draft.optString("theme", "system"))));
    Ui.add(page, appearance, 4);
    appearance.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(android.widget.AdapterView<?> p) {}

          public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long item) {
            put("theme", themes[pos]);
            preview();
          }
        });
    Ui.add(
        page,
        Ui.text(this, I18n.choose(this, "Susun atur", "Layout"), 16, true, R.color.text_primary),
        16);
    ChoiceBar layouts =
        new ChoiceBar(
            this,
            new String[] {
              I18n.choose(this, "Garis masa", "Timeline"), I18n.choose(this, "Klasik", "Classic")
            },
            null,
            draft.optString("layoutStyle", "classic").equals("timeline") ? 0 : 1,
            position -> {
              put("layoutStyle", position == 0 ? "timeline" : "classic");
              preview();
            });
    layouts.setId(R.id.widget_layout_choices);
    Ui.add(page, layouts, 8);
    if (descriptor == WidgetProviderDescriptor.EXPANDED) {
      for (String key : new String[] {"showHijri", "showAuxiliary", "trackingEnabled"}) {
        String label =
            key.equals("showHijri")
                ? I18n.choose(this, "Tarikh Hijri", "Hijri date")
                : key.equals("showAuxiliary")
                    ? I18n.choose(this, "Imsak, Syuruk dan Dhuha", "Imsak, sunrise and Dhuha")
                    : I18n.choose(
                        this,
                        "Rekod solat · ketik untuk rekod atau batal",
                        "Prayer tracking · tap to record or unrecord");
        MaterialCheckBox control =
            check(label, draft.optBoolean(key, !key.equals("trackingEnabled")));
        if (key.equals("trackingEnabled")) control.setId(R.id.widget_tracking);
        Ui.add(page, control, 8);
        control.setOnCheckedChangeListener(
            (v, on) -> {
              put(key, on);
              preview();
            });
      }
    }
    Ui.add(
        page,
        Ui.text(
            this,
            I18n.choose(
                this,
                "Bahasa dan format 12/24 jam mengikut aplikasi. Kawalan rekod memerlukan ruang yang"
                    + " mencukupi; besarkan widget jika diminta.",
                "Language and 12/24-hour format follow the app. Tracking controls require"
                    + " sufficient space; resize the widget when prompted."),
            14,
            false,
            R.color.text_secondary),
        16);
    MaterialButton save = Ui.button(this, I18n.choose(this, "Simpan", "Save"), R.drawable.ic_check);
    save.setId(R.id.widget_save);
    save.setOnClickListener(v -> save());
    Ui.add(page, save, 24);
    MaterialButton cancel = Ui.quiet(this, I18n.choose(this, "Batal", "Cancel"), 0);
    cancel.setId(R.id.widget_cancel);
    cancel.setOnClickListener(v -> finish());
    Ui.add(page, cancel, 4);
    built = true;
    preview();
  }

  private MaterialCheckBox check(String label, boolean on) {
    MaterialCheckBox v = new MaterialCheckBox(this);
    v.setMinHeight(dp(48));
    v.setText(label);
    v.setChecked(on);
    return v;
  }

  private int dp(float n) {
    return Ui.dp(this, n);
  }

  private void put(String key, Object value) {
    try {
      draft.put(key, value);
    } catch (JSONException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private void zoneLabel() {
    WidgetConfigV2 cfg = new WidgetConfigV2(draft, descriptor);
    PrayerZone z = cfg.resolvedZone(this);
    zoneButton.setText(
        z == null
            ? I18n.choose(this, "Pilih negeri / zon", "Choose state / zone")
            : z.code + " · " + z.name);
  }

  private void chooseZone() {
    LinearLayout list = Ui.column(this);
    list.setPadding(dp(16), 0, dp(16), 0);
    EditText search = new EditText(this);
    search.setHint(I18n.choose(this, "Cari negeri, zon atau kod", "Search state, zone or code"));
    search.setMinHeight(dp(48));
    list.addView(search);
    ListView rows = new ListView(this);
    list.addView(rows, new LinearLayout.LayoutParams(-1, dp(300)));
    List<PrayerZone> zones = new ArrayList<>();
    for (List<PrayerZone> group : ZoneRepository.load(this).values()) zones.addAll(group);
    List<PrayerZone> shown = new ArrayList<>(zones);
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    rows.setAdapter(adapter);
    Runnable filter =
        () -> {
          String q = search.getText().toString().toLowerCase(Locale.ROOT);
          shown.clear();
          adapter.clear();
          for (PrayerZone z : zones)
            if ((z.state + " " + z.name + " " + z.code).toLowerCase(Locale.ROOT).contains(q)) {
              shown.add(z);
              adapter.add(z.state + " · " + z.code + "\n" + z.name);
            }
          adapter.notifyDataSetChanged();
        };
    filter.run();
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int count, int after) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            filter.run();
          }

          public void afterTextChanged(Editable e) {}
        });
    androidx.appcompat.app.AlertDialog dialog =
        new MaterialAlertDialogBuilder(this)
            .setTitle(I18n.choose(this, "Zon widget", "Widget zone"))
            .setView(list)
            .setNegativeButton(I18n.choose(this, "Batal", "Cancel"), null)
            .create();
    rows.setOnItemClickListener(
        (a, v, pos, item) -> {
          PrayerZone z = shown.get(pos);
          put("zone", z.code);
          put("name", z.name);
          put("state", z.state);
          zoneLabel();
          preview();
          dialog.dismiss();
        });
    dialog.show();
  }

  private void preview() {
    if (!built) return;
    android.util.SizeF size = PrayerWidgets.presentationSize(this, id, descriptor);
    RemoteViews views =
        PrayerWidgets.render(
            this,
            id,
            descriptor,
            new WidgetConfigV2(draft, descriptor),
            size.getWidth(),
            size.getHeight(),
            true);
    preview.show(views, size);
  }

  private void save() {
    if (WidgetProviderDescriptor.instance(this, id) != descriptor) {
      finish();
      return;
    }
    WidgetConfigV2 cfg = new WidgetConfigV2(draft, descriptor);
    if (cfg.resolvedZone(this) == null) {
      Toast.makeText(
              this,
              I18n.choose(this, "Pilih zon yang sah", "Choose a valid zone"),
              Toast.LENGTH_LONG)
          .show();
      return;
    }
    cfg.save(this, id);
    PrayerWidgets.refreshAsync(this);
    WidgetScheduleRefreshWorker.cleanup(this);
    WidgetScheduleRefreshWorker.enqueue(this, cfg.resolvedZone(this).code);
    setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id));
    finish();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putString("draft", draft.toString());
    super.onSaveInstanceState(state);
  }
}
