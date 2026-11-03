package com.example.waktusolatmalaysia.fragments;

import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.activity.OnBackPressedCallback;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.ui.*;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.util.concurrent.*;
import java.util.function.Consumer;

public abstract class LibraryFragment extends Fragment {
  protected LinearLayout root, head, top;
  protected RecyclerView list;
  protected FrameLayout detail;
  protected TextView status;
  private LinearLayout left, body;
  private NestedScrollView headScroll;
  private ExecutorService worker;
  private int token;
  private boolean wide, narrowReading;
  private String detailKey = "";
  private OnBackPressedCallback back;

  protected boolean inlineReader() { return false; }
  @Override public View onCreateView(@NonNull LayoutInflater i, ViewGroup parent, Bundle state) {
    worker = Executors.newSingleThreadExecutor(); token++;
    narrowReading = state != null && state.getBoolean("library_detail_open");
    detailKey = state == null ? "" : state.getString("library_detail_key", "");
    root = Ui.column(requireContext()); root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
    int edge = Ui.dp(requireContext(), 20); root.setPadding(edge, Ui.dp(requireContext(), 12), edge, 0);
    top = Ui.column(requireContext()); root.addView(top);
    body = Ui.row(requireContext()); body.setGravity(Gravity.TOP);
    root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
    left = Ui.column(requireContext()); body.addView(left, new LinearLayout.LayoutParams(-1, -1));
    head = Ui.column(requireContext()); head.setPadding(0, 0, 0, Ui.dp(requireContext(), 12));
    headScroll = new NestedScrollView(requireContext()); headScroll.setFillViewport(false); headScroll.addView(head);
    left.addView(headScroll, new LinearLayout.LayoutParams(-1, -2));
    status = Ui.text(requireContext(), "", 13, false, R.color.text_secondary);
    status.setId(R.id.library_status); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    left.addView(status, new LinearLayout.LayoutParams(-1, -2));
    list = new RecyclerView(requireContext()); list.setLayoutManager(new LinearLayoutManager(requireContext()));
    list.setClipToPadding(false); list.setPadding(0, 0, 0, Ui.dp(requireContext(), 24));
    left.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
    detail = new FrameLayout(requireContext()); detail.setId(R.id.library_detail); detail.setVisibility(View.GONE);
    body.addView(detail, new LinearLayout.LayoutParams(0, -1, 1));
    root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> adapt());
    return root;
  }
  @Override public void onViewCreated(@NonNull View v, Bundle state) {
    back = new OnBackPressedCallback(false) { public void handleOnBackPressed() { closeSinglePaneDetail(); } };
    requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), back);
    build(state);
  }
  private void adapt() {
    if (root == null || root.getWidth() == 0) return;
    float density = getResources().getDisplayMetrics().density;
    boolean previous = wide;
    wide = root.getWidth()/density >= 680 && root.getHeight()/density >= 480
        && getResources().getConfiguration().fontScale <= 1.3f;
    boolean reading = !detailKey.isEmpty() && inlineReader() && narrowReading;
    left.setVisibility(!wide && reading ? View.GONE : View.VISIBLE);
    detail.setVisibility(wide || reading ? View.VISIBLE : View.GONE);
    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) left.getLayoutParams();
    int width = wide ? Ui.dp(requireContext(), Math.max(240, Math.min(320, (root.getWidth()/density - 64) * .34f))) : -1;
    if (lp.width != width) { lp.width = width; left.setLayoutParams(lp); }
    LinearLayout.LayoutParams dp = (LinearLayout.LayoutParams) detail.getLayoutParams();
    int margin = wide ? Ui.dp(requireContext(), 24) : 0;
    if (dp.leftMargin != margin) { dp.leftMargin = margin; detail.setLayoutParams(dp); }
    int headHeight = Math.min(head.getMeasuredHeight(), Math.max(Ui.dp(requireContext(), 72), body.getHeight()/2));
    if (headHeight > 0 && headScroll.getLayoutParams().height != headHeight) {
      headScroll.getLayoutParams().height = headHeight; headScroll.requestLayout();
    }
    back.setEnabled(!wide && reading);
    if (wide != previous) onPaneChanged(wide);
  }
  protected void onPaneChanged(boolean wide) {}
  protected abstract void build(Bundle state);
  protected boolean embedded() { return wide; }
  protected void open(int dest, Bundle args, Fragment child) {
    if ((wide || inlineReader()) && child != null) {
      String key = dest + ":" + args.getString("kind", "") + ":" + args.getString("reading_id", "") + ":" + args.getInt("surah") + ":" + args.getInt("verse");
      if (!detailKey.equals(key) || getChildFragmentManager().findFragmentByTag("library-reading") == null) {
        if (getChildFragmentManager().isStateSaved()) return;
        child.setArguments(args);
        getChildFragmentManager().beginTransaction().replace(detail.getId(), child, "library-reading").commit();
        detailKey = key;
      }
      narrowReading = true; adapt();
    } else ((MainActivity) requireActivity()).openDestination(dest, args);
  }
  protected void clearDetail() {
    detailKey = ""; narrowReading = false;
    Fragment child = getChildFragmentManager().findFragmentByTag("library-reading");
    if (child != null && !getChildFragmentManager().isStateSaved()) getChildFragmentManager().beginTransaction().remove(child).commit();
    adapt();
  }
  public boolean closeSinglePaneDetail() {
    if (wide || !inlineReader() || !narrowReading) return false;
    narrowReading = false; adapt(); return true;
  }
  protected <T> void task(Callable<T> call, Consumer<T> done, Consumer<Exception> error) {
    int generation = token;
    worker.execute(() -> { try { T value = call.call(); new Handler(Looper.getMainLooper()).post(() -> {
      if (root != null && isAdded() && generation == token) done.accept(value);
    }); } catch (Exception e) { new Handler(Looper.getMainLooper()).post(() -> {
      if (root != null && isAdded() && generation == token) error.accept(e);
    }); } });
  }
  protected void heading(String title, String sub) {
    head.addView(Ui.text(requireContext(), title, 25, true, R.color.text_primary));
    if (!sub.isEmpty()) Ui.add(head, Ui.text(requireContext(), sub, 12, false, R.color.text_secondary), 6);
  }
  @Override public void onSaveInstanceState(@NonNull Bundle out) {
    super.onSaveInstanceState(out); out.putBoolean("library_detail_open", narrowReading); out.putString("library_detail_key", detailKey);
  }
  @Override public void onDestroyView() {
    token++; worker.shutdownNow(); root = null; head = null; list = null; detail = null; super.onDestroyView();
  }
  public static final class Holder extends RecyclerView.ViewHolder {
    final FrameLayout frame;
    public Holder(FrameLayout f) {
      super(f); frame = f;
      frame.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
  }
}
