package com.example.waktusolatmalaysia.views;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.*;
import androidx.core.content.ContextCompat;
import androidx.core.view.*;
import androidx.navigation.*;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.*;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.databinding.ShellMainBinding;
import com.example.waktusolatmalaysia.services.ScheduleRefreshWorker;
import com.example.waktusolatmalaysia.ui.*;

public final class MainActivity extends AppCompatActivity {
  private ShellMainBinding binding;
  private NavController navigation;
  private AppBarConfiguration appBar;
  private int layoutMode = -1;
  private int insetTop, insetBottom;
  private java.util.function.Consumer<Boolean> pendingPermission;
  private final ActivityResultLauncher<String> permission =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          ok -> {
            if (pendingPermission != null) {
              pendingPermission.accept(ok);
              pendingPermission = null;
            }
          });

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(I18n.wrap(base));
  }

  @Override
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    binding = ShellMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    boolean night =
        (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
            == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    WindowInsetsControllerCompat controller =
        WindowCompat.getInsetsController(getWindow(), binding.getRoot());
    controller.setAppearanceLightStatusBars(!night);
    controller.setAppearanceLightNavigationBars(!night);
    setSupportActionBar(binding.toolbar);
    NavHostFragment host =
        (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
    navigation = host.getNavController();
    appBar =
        new AppBarConfiguration.Builder(
                R.id.prayerFragment,
                R.id.quranFragment,
                R.id.worshipFragment,
                R.id.exploreFragment,
                R.id.savedFragment)
            .build();
    NavigationUI.setupActionBarWithNavController(this, navigation, appBar);
    binding.bottomNav.setOnItemSelectedListener(
        item -> {
          navigateHub(item.getItemId());
          return true;
        });
    // This shell owns the safe area. Material's default inset padding would apply it twice.
    ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav, (v, insets) -> insets);
    binding.bottomNav.setItemActiveIndicatorEnabled(true);
    binding.bottomNav.setItemActiveIndicatorWidth(Ui.dp(this, 52));
    binding.bottomNav.setItemActiveIndicatorHeight(Ui.dp(this, 32));
    binding.bottomNav.setItemActiveIndicatorShapeAppearance(
        com.google.android.material.shape.ShapeAppearanceModel.builder()
            .setAllCornerSizes(Ui.dp(this, 16))
            .build());
    binding.bottomNav.setItemActiveIndicatorColor(
        android.content.res.ColorStateList.valueOf(Ui.color(this, R.color.prayer_selected)));
    binding.bottomNav.setItemPaddingTop(Ui.dp(this, 10));
    binding.bottomNav.setItemPaddingBottom(Ui.dp(this, 8));
    binding.bottomNav.setLabelVisibilityMode(
        getResources().getConfiguration().fontScale > 1.5f
            ? com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_UNLABELED
            : com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED);
    for (Menu menu : new Menu[] {binding.bottomNav.getMenu(), binding.navigationRail.getMenu()}) {
      menu.findItem(R.id.quranFragment).setTitle("Quran");
      menu.findItem(R.id.exploreFragment).setTitle(I18n.choose(this, "Teroka", "Explore"));
      menu.findItem(R.id.savedFragment).setTitle(I18n.choose(this, "Simpan", "Saved"));
    }
    binding.navigationRail.setOnItemSelectedListener(
        item -> {
          navigateHub(item.getItemId());
          return true;
        });
    binding.bottomNav.setOnItemReselectedListener(item -> navigateHub(item.getItemId()));
    binding.navigationRail.setOnItemReselectedListener(item -> navigateHub(item.getItemId()));
    binding.permanentNavigation.setNavigationItemSelectedListener(
        item -> {
          openDestination(item.getItemId(), null);
          return true;
        });
    binding.railSettings.setOnClickListener(v -> showSettings());
    binding.sidebarSettings.setOnClickListener(v -> showSettings());
    binding.mainSurface.addOnLayoutChangeListener(
        (v, l, t, r, b, ol, ot, or, ob) -> adaptive(r - l, b - t));
    ViewCompat.setOnApplyWindowInsetsListener(
        binding.getRoot(),
        (v, insets) -> {
          androidx.core.graphics.Insets bars =
              insets.getInsets(
                  WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
          insetTop = bars.top;
          insetBottom = bars.bottom;
          binding.mainSurface.setPadding(bars.left, 0, bars.right, 0);
          binding.toolbar.setPadding(0, bars.top, 0, 0);
          binding.toolbar.getLayoutParams().height = Ui.dp(this, 64) + bars.top;
          binding.permanentSidebar.setPadding(0, bars.top, 0, bars.bottom);
          binding.navigationRailShell.setPadding(0, bars.top, 0, bars.bottom);
          binding.bottomNavShell.setPadding(0, 0, 0, bars.bottom);
          binding.bottomNavShell.getLayoutParams().height = Ui.dp(this, 88) + bars.bottom;
          binding.navHost.setPadding(0, 0, 0, layoutMode > 0 ? bars.bottom : 0);
          boolean keyboard = insets.isVisible(WindowInsetsCompat.Type.ime());
          if (layoutMode == 0)
            binding.bottomNavShell.setVisibility(keyboard ? View.GONE : View.VISIBLE);
          adaptive(binding.mainSurface.getWidth(), binding.mainSurface.getHeight());
          return insets;
        });
    navigation.addOnDestinationChangedListener(
        (c, d, args) -> {
          syncSelection();
          updateChrome();
          I18n.localize(binding.getRoot());
        });
    ScheduleRefreshWorker.enqueue(this, false);
    com.example.waktusolatmalaysia.widgets.PrayerWidgets.refreshAsync(this);
    if (saved == null) handleIntent(getIntent());
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();
    // Restore the route's selection after the navigation views restore their saved state.
    syncSelection();
  }

  private void syncSelection() {
    if (navigation == null || navigation.getCurrentDestination() == null) return;
    int destination = navigation.getCurrentDestination().getId();
    int selected = owner(destination);
    for (Menu menu :
        new Menu[] {
          binding.bottomNav.getMenu(),
          binding.navigationRail.getMenu(),
          binding.permanentNavigation.getMenu()
        }) {
      // Utility pages belong outside the five hubs. Allow an empty selection and
      // manage exclusivity here; exclusive menu groups cannot uncheck their last item.
      menu.setGroupCheckable(0, true, false);
      for (int i = 0; i < menu.size(); i++)
        menu.getItem(i).setChecked(menu.getItem(i).getItemId() == selected);
    }
    boolean settings = selected == 0;
    binding.railSettings.setSelected(settings);
    binding.sidebarSettings.setSelected(settings);
    CharSequence state = settings ? I18n.choose(this, "Sedang dibuka", "Current page") : null;
    ViewCompat.setStateDescription(binding.railSettings, state);
    ViewCompat.setStateDescription(binding.sidebarSettings, state);
  }

  private int owner(int id) {
    if (id == R.id.quranReaderFragment || id == R.id.downloadsFragment) return R.id.quranFragment;
    if (id == R.id.readingFragment
        || id == R.id.qiblaFragment
        || id == R.id.tasbihFragment
        || id == R.id.collectionFragment
        || id == R.id.calendarFragment
        || id == R.id.tasksFragment
        || id == R.id.widgetsFragment) return R.id.worshipFragment;
    if (id == R.id.videoFragment) return R.id.exploreFragment;
    if (id == R.id.backupFragment || id == R.id.settingsFragment || id == R.id.aboutFragment)
      return 0;
    if (id == R.id.scheduleFragment || id == R.id.locationFragment || id == R.id.onboardingFragment)
      return R.id.prayerFragment;
    return id;
  }

  private void adaptive(int px, int height) {
    if (px <= 0 || height <= 0) return;
    float density = getResources().getDisplayMetrics().density;
    int
        dp =
            Math.round(
                (px - binding.mainSurface.getPaddingLeft() - binding.mainSurface.getPaddingRight())
                    / density),
        availableHeight = Math.round((height - insetTop - insetBottom) / density),
        mode = dp < 600 || availableHeight < 480 ? 0 : dp < 1200 ? 1 : 2;
    if (mode == layoutMode) return;
    layoutMode = mode;
    binding.bottomNavShell.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
    binding.navigationRail.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
    binding.navigationRailShell.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
    binding.permanentSidebar.setVisibility(mode == 2 ? View.VISIBLE : View.GONE);
    ConstraintSet set = new ConstraintSet();
    set.clone(binding.mainSurface);
    int start =
        mode == 2
            ? R.id.permanent_sidebar
            : mode == 1 ? R.id.navigation_rail_shell : ConstraintSet.PARENT_ID;
    int edge = mode == 0 ? ConstraintSet.START : ConstraintSet.END;
    set.connect(R.id.toolbar, ConstraintSet.START, start, edge);
    set.connect(R.id.nav_host, ConstraintSet.START, start, edge);
    set.applyTo(binding.mainSurface);
    updateChrome();
    ViewCompat.requestApplyInsets(binding.getRoot());
  }

  private boolean isHub() {
    return navigation != null
        && navigation.getCurrentDestination() != null
        && binding.bottomNav.getMenu().findItem(navigation.getCurrentDestination().getId()) != null;
  }

  private void updateChrome() {
    binding.toolbar.setLogo(
        layoutMode == 0 && isHub()
            ? ContextCompat.getDrawable(this, R.drawable.ic_brand_toolbar)
            : null);
    invalidateOptionsMenu();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.toolbar_menu, menu);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.action_settings).setVisible(layoutMode == 0 && isHub());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_settings) {
      showSettings();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onSupportNavigateUp() {
    NavHostFragment host =
        (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
    androidx.fragment.app.Fragment page =
        host.getChildFragmentManager().getPrimaryNavigationFragment();
    if (page instanceof com.example.waktusolatmalaysia.fragments.LibraryFragment
        && ((com.example.waktusolatmalaysia.fragments.LibraryFragment) page)
            .closeSinglePaneDetail()) return true;
    return NavigationUI.navigateUp(navigation, appBar) || super.onSupportNavigateUp();
  }

  private void navigateHub(int id) {
    navigation.navigate(
        id,
        null,
        new NavOptions.Builder()
            .setPopUpTo(navigation.getGraph().getStartDestinationId(), false)
            .setLaunchSingleTop(true)
            .setRestoreState(false)
            .build());
  }

  public void openDestination(int destination, Bundle args) {
    if (args == null && binding.bottomNav.getMenu().findItem(destination) != null)
      navigateHub(destination);
    else
      navigation.navigate(
          destination, args, new NavOptions.Builder().setLaunchSingleTop(true).build());
  }

  public void showSettings() {
    if (getSupportFragmentManager().findFragmentByTag("floating-settings") == null)
      new com.example.waktusolatmalaysia.fragments.SettingsPanel()
          .show(getSupportFragmentManager(), "floating-settings");
  }

  public void notificationPermission(java.util.function.Consumer<Boolean> result) {
    if (Build.VERSION.SDK_INT < 33
        || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
      result.accept(true);
      return;
    }
    pendingPermission = result;
    permission.launch(Manifest.permission.POST_NOTIFICATIONS);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntent(intent);
  }

  private void handleIntent(Intent intent) {
    if (intent != null && intent.getBooleanExtra("open_tasks", false))
      binding.navHost.post(() -> openDestination(R.id.tasksFragment, null));
    if (intent != null && intent.getBooleanExtra("open_schedule", false)) {
      Bundle b = new Bundle();
      b.putString("date", intent.getStringExtra("date"));
      b.putString("zone", intent.getStringExtra("zone"));
      b.putString("zone_name", intent.getStringExtra("zone_name"));
      binding.navHost.post(() -> openDestination(R.id.scheduleFragment, b));
    }
  }
}
