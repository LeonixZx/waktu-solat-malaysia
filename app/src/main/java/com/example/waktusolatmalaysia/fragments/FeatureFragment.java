package com.example.waktusolatmalaysia.fragments;

import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.ui.*;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.util.concurrent.*;
import java.util.function.Consumer;

public abstract class FeatureFragment extends Fragment {
  protected LinearLayout page;
  protected NestedScrollView scroll;
  private ExecutorService worker;
  private int generation;
  private final Handler main = new Handler(Looper.getMainLooper());

  protected int maxContentWidth() {
    return 820;
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater i, @Nullable ViewGroup parent, @Nullable Bundle saved) {
    worker = Executors.newSingleThreadExecutor();
    generation++;
    scroll = new NestedScrollView(requireContext());
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    FrameLayout frame = new FrameLayout(requireContext());
    scroll.addView(frame, new ViewGroup.LayoutParams(-1, -2));
    page = Ui.column(requireContext());
    page.setPadding(
        Ui.dp(requireContext(), 20),
        Ui.dp(requireContext(), 12),
        Ui.dp(requireContext(), 20),
        Ui.dp(requireContext(), 28));
    frame.addView(page, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_HORIZONTAL));
    frame.addOnLayoutChangeListener(
        (v, l, t, r, b, ol, ot, or, ob) -> {
          if (page == null) return;
          int width = Math.min(r - l, Ui.dp(requireContext(), maxContentWidth()));
          if (page.getLayoutParams().width != width) {
            page.getLayoutParams().width = width;
            page.requestLayout();
          }
        });
    frame
        .getViewTreeObserver()
        .addOnGlobalLayoutListener(
            () -> {
              if (page != null) I18n.localize(page);
            });
    return scroll;
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
    build(s);
  }

  protected abstract void build(@Nullable Bundle saved);

  protected void heading(String title, String description) {
    TextView heading = Ui.text(requireContext(), title, 25, true, R.color.text_primary);
    androidx.core.view.ViewCompat.setAccessibilityHeading(heading,true);
    Ui.add(page, heading, 4);
    if (!description.isEmpty())
      Ui.add(page, Ui.text(requireContext(), description, 13, false, R.color.text_secondary), 8);
  }

  protected void open(int id) {
    ((MainActivity) requireActivity()).openDestination(id, null);
  }

  protected void open(int id, Bundle b) {
    ((MainActivity) requireActivity()).openDestination(id, b);
  }

  protected <T> void task(Callable<T> call, Consumer<T> success, Consumer<Exception> failure) {
    int token = generation;
    worker.execute(
        () -> {
          try {
            T value = call.call();
            main.post(
                () -> {
                  if (page != null && isAdded() && generation == token) success.accept(value);
                });
          } catch (Exception e) {
            main.post(
                () -> {
                  if (page != null && isAdded() && generation == token) failure.accept(e);
                });
          }
        });
  }

  @Override
  public void onDestroyView() {
    generation++;
    worker.shutdownNow();
    page = null;
    scroll = null;
    super.onDestroyView();
  }
}
