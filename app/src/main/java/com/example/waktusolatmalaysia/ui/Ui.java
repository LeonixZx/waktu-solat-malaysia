package com.example.waktusolatmalaysia.ui;

import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.example.waktusolatmalaysia.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public final class Ui {
  private Ui() {}

  public static void setTextChanged(TextView view, String value) {
    if (!android.text.TextUtils.equals(view.getText(), value)) view.setText(value);
  }

  public static int dp(Context c, float value) {
    return Math.round(value * c.getResources().getDisplayMetrics().density);
  }

  public static int color(Context c, int id) {
    return ContextCompat.getColor(c, id);
  }

  public static TextView text(Context c, String value, float size, boolean bold, int color) {
    TextView view = new TextView(c);
    view.setText(I18n.t(c, value));
    view.setTextSize(size);
    view.setTextColor(color(c, color));
    view.setTypeface(
        ResourcesCompat.getFont(c, bold ? R.font.jakarta_bold : R.font.jakarta_regular));
    view.setLineSpacing(dp(c, 3), 1);
    return view;
  }

  public static TextView arabic(Context c, String value, int size) {
    TextView view = text(c, value, size, false, R.color.text_primary);
    view.setTypeface(ResourcesCompat.getFont(c, R.font.arabic_reading));
    view.setGravity(Gravity.RIGHT);
    view.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
    view.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
    view.setTextDirection(View.TEXT_DIRECTION_RTL);
    view.setTextLocale(java.util.Locale.forLanguageTag("ar"));
    view.setLineSpacing(dp(c, 9), 1.15f);
    view.setTextIsSelectable(true);
    return view;
  }

  public static LinearLayout column(Context c) {
    LinearLayout result = new LinearLayout(c);
    result.setOrientation(LinearLayout.VERTICAL);
    result.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
    return result;
  }

  public static LinearLayout row(Context c) {
    LinearLayout result = new LinearLayout(c);
    result.setOrientation(LinearLayout.HORIZONTAL);
    result.setGravity(Gravity.CENTER_VERTICAL);
    result.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
    return result;
  }

  public static void add(LinearLayout parent, View child, int top) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.topMargin = dp(parent.getContext(), top);
    parent.addView(child, p);
  }

  public static void gap(LinearLayout parent, int height) {
    View view = new View(parent.getContext());
    parent.addView(view, new LinearLayout.LayoutParams(1, dp(parent.getContext(), height)));
  }

  public static GradientDrawable background(Context c, int color, int radius) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color(c, color));
    drawable.setCornerRadius(dp(c, radius));
    return drawable;
  }

  public static ImageView icon(Context c, int id, int size) {
    ImageView result = new ImageView(c);
    result.setImageResource(id);
    result.setImageTintList(ColorStateList.valueOf(color(c, R.color.prayer_text)));
    result.setLayoutParams(new LinearLayout.LayoutParams(dp(c, size), dp(c, size)));
    result.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    return result;
  }

  public static MaterialCardView card(Context c, View child, int padding) {
    MaterialCardView result = new MaterialCardView(c);
    result.setRadius(dp(c, 20));
    result.setCardElevation(0);
    result.setCardBackgroundColor(color(c, R.color.surface));
    result.setStrokeWidth(0);
    child.setPadding(dp(c, padding), dp(c, padding), dp(c, padding), dp(c, padding));
    result.addView(child);
    return result;
  }

  public static MaterialButton button(Context c, String label, int icon) {
    MaterialButton button = new MaterialButton(c);
    button.setText(I18n.t(c, label));
    button.setTextSize(13);
    button.setAllCaps(false);
    button.setLetterSpacing(0);
    button.setCornerRadius(dp(c, 12));
    button.setStateListAnimator(null);
    button.setElevation(0);
    button.setMinHeight(dp(c, 48));
    button.setMinimumHeight(dp(c, 48));
    button.setInsetTop(0);
    button.setInsetBottom(0);
    button.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 10));
    button.setBackgroundTintList(ColorStateList.valueOf(color(c, R.color.colorPrimary)));
    button.setTextColor(color(c, R.color.on_primary));
    if (icon != 0) {
      button.setIconResource(icon);
      button.setIconTintResource(R.color.on_primary);
      button.setIconSize(dp(c, 18));
      button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
      button.setIconPadding(dp(c, 8));
    }
    return button;
  }

  public static MaterialButton quiet(Context c, String label, int icon) {
    MaterialButton button = button(c, label, icon);
    button.setBackgroundTintList(
        new ColorStateList(
            new int[][] {new int[] {android.R.attr.state_selected}, new int[0]},
            new int[] {color(c, R.color.prayer_selected), color(c, R.color.soft_gold)}));
    button.setTextColor(color(c, R.color.prayer_text));
    if (icon != 0) button.setIconTintResource(R.color.prayer_text);
    button.setElevation(0);
    return button;
  }

  public static View banner(Context c, int image, String title, String subtitle, Runnable action) {
    MaterialCardView card = new MaterialCardView(c);
    card.setRadius(dp(c, 20));
    card.setCardElevation(0);
    card.setStrokeWidth(0);
    androidx.constraintlayout.widget.ConstraintLayout frame =
        new androidx.constraintlayout.widget.ConstraintLayout(c);
    frame.setMinHeight(dp(c, 148));
    card.setClipToOutline(true);
    card.addView(frame, new ViewGroup.LayoutParams(-1, -2));
    ImageView art = new ImageView(c);
    Artwork.bind(art, image);
    art.setScaleType(ImageView.ScaleType.CENTER_CROP);
    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams ap =
        new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(0, 0);
    ap.leftToLeft = ap.rightToRight = ap.topToTop = ap.bottomToBottom = 0;
    frame.addView(art, ap);
    View wash = new View(c);
    wash.setBackgroundResource(R.drawable.bg_art_scrim);
    frame.addView(wash, new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(ap));
    LinearLayout words = column(c);
    words.setPadding(dp(c, 20), dp(c, 20), dp(c, 16), dp(c, 20));
    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams wp =
        new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(0, -2);
    wp.leftToLeft = wp.topToTop = wp.bottomToBottom = 0;
    wp.rightToRight = 0;
    wp.horizontalBias = 0;
    wp.matchConstraintDefaultWidth = 2;
    wp.matchConstraintPercentWidth = .72f;
    frame.addView(words, wp);
    words.addView(text(c, title, 19, true, R.color.art_text));
    gap(words, 8);
    words.addView(text(c, subtitle, 12, false, R.color.art_text));
    if (action != null) {
      card.setClickable(true);
      card.setOnClickListener(v -> action.run());
      card.setContentDescription(I18n.t(c, title) + ". " + I18n.t(c, subtitle));
    }
    return card;
  }

  public static View line(Context c, String title, String subtitle, int icon, Runnable click) {
    LinearLayout row = row(c);
    row.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
    ImageView symbol = icon(c, icon, 28);
    symbol.setImageTintList(ColorStateList.valueOf(color(c, R.color.prayer_text)));
    row.addView(symbol);
    LinearLayout words = column(c);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
    p.leftMargin = dp(c, 14);
    row.addView(words, p);
    words.addView(text(c, title, 14, true, R.color.text_primary));
    if (!subtitle.isEmpty()) {
      gap(words, 4);
      words.addView(text(c, subtitle, 11, false, R.color.text_secondary));
    }
    row.addView(icon(c, R.drawable.ic_chevron, 17));
    row.setBackground(background(c, R.color.surface, 18));
    if (click != null) {
      row.setOnClickListener(v -> click.run());
      row.setClickable(true);
    }
    return row;
  }

  public static android.widget.ImageButton iconButton(
      Context c, int icon, String label, Runnable action) {
    android.widget.ImageButton button = new android.widget.ImageButton(c);
    button.setImageResource(icon);
    button.setImageTintList(ColorStateList.valueOf(color(c, R.color.prayer_text)));
    button.setContentDescription(I18n.t(c, label));
    button.setBackground(background(c, R.color.soft_gold, 12));
    button.setPadding(dp(c, 12), dp(c, 12), dp(c, 12), dp(c, 12));
    button.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 48), dp(c, 48)));
    button.setOnClickListener(v -> action.run());
    return button;
  }

  public static void share(Context c, String text) {
    Intent intent =
        new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text);
    c.startActivity(Intent.createChooser(intent, I18n.choose(c, "Kongsi bacaan", "Share reading")));
  }
}
