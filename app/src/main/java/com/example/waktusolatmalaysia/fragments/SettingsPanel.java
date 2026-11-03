package com.example.waktusolatmalaysia.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.view.*;
import androidx.fragment.app.DialogFragment;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.ui.*;
import com.google.android.material.card.MaterialCardView;

/** A bounded end sheet that retains the current hub underneath it. */
public final class SettingsPanel extends DialogFragment {
  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle saved) {
    Dialog d = new Dialog(requireContext(), getTheme());
    d.requestWindowFeature(Window.FEATURE_NO_TITLE);
    d.setCanceledOnTouchOutside(true);
    return d;
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle saved) {
    LinearLayout content = Ui.column(requireContext());
    content.setId(R.id.settings_panel);
    LinearLayout header = Ui.row(requireContext());
    header.setPadding(
        Ui.dp(requireContext(), 20),
        Ui.dp(requireContext(), 12),
        Ui.dp(requireContext(), 12),
        Ui.dp(requireContext(), 8));
    header.addView(
        Ui.text(
            requireContext(),
            I18n.choose(requireContext(), "Tetapan", "Settings"),
            22,
            true,
            R.color.text_primary),
        new LinearLayout.LayoutParams(0, -2, 1));
    ImageButton close =
        Ui.iconButton(
            requireContext(),
            R.drawable.ic_close,
            I18n.choose(requireContext(), "Tutup tetapan", "Close settings"),
            this::dismiss);
    close.setId(R.id.settings_close);
    header.addView(close);
    content.addView(header);
    FrameLayout frame = new FrameLayout(requireContext());
    frame.setId(R.id.settings_content);
    content.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));
    MaterialCardView card = new MaterialCardView(requireContext());
    card.setRadius(Ui.dp(requireContext(), 28));
    card.setCardElevation(0);
    card.setStrokeWidth(Ui.dp(requireContext(), 1));
    card.setStrokeColor(Ui.color(requireContext(), R.color.divider));
    card.setCardBackgroundColor(Ui.color(requireContext(), R.color.background));
    card.addView(content, new ViewGroup.LayoutParams(-1, -1));
    return card;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle state) {
    if (getChildFragmentManager().findFragmentById(R.id.settings_content) == null)
      getChildFragmentManager()
          .beginTransaction()
          .replace(R.id.settings_content, new SettingsFragment())
          .commitNow();
    ViewCompat.setAccessibilityPaneTitle(
        view, I18n.choose(requireContext(), "Tetapan", "Settings"));
  }

  @Override
  public void onStart() {
    super.onStart();
    Window w = requireDialog().getWindow();
    if (w == null) return;
    w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    w.setDimAmount(.32f);
    w.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    android.graphics.Rect bounds = new android.graphics.Rect();
    requireActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(bounds);
    int margin = Ui.dp(requireContext(), 12);
    w.setLayout(
        Math.min(Ui.dp(requireContext(), 460), Math.max(1, bounds.width() - margin * 2)),
        Math.max(1, bounds.height() - margin * 2));
    WindowManager.LayoutParams p = w.getAttributes();
    p.x = margin;
    w.setAttributes(p);
    boolean motion =
        android.os.Build.VERSION.SDK_INT < 26
            || android.animation.ValueAnimator.areAnimatorsEnabled();
    w.setWindowAnimations(motion ? R.style.Animation_SettingsPanel : 0);
  }
}
