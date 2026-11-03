package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.Nullable;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.ui.*;

public final class VideoFragment extends FeatureFragment {
  private WebView player;
  private View fullscreen;
  private TextView status;

  @Override
  protected void build(@Nullable Bundle s) {
    try {
      String id = requireArguments().getString("id");
      CatalogEntry entry = null;
      for (CatalogEntry e : CatalogRepository.load(requireContext()))
        if (e.id.equals(id)) entry = e;
      if (entry == null) throw new IllegalArgumentException();
      CatalogEntry selected = entry;
      heading(
          entry.title,
          entry.provider + " · " + (entry.language.equals("en") ? "English" : "Bahasa Melayu"));
      status =
          Ui.text(
              requireContext(),
              I18n.choose(
                  requireContext(),
                  "Tekan untuk memuatkan pemain YouTube. Jika tidak tersedia, buka sumber asal.",
                  "Tap to load the YouTube player. If embedding is unavailable, open the original"
                      + " source."),
              13,
              false,
              R.color.text_secondary);
      status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
      Ui.add(page, status, 16);
      android.view.View play = Ui.button(requireContext(), "Tonton", R.drawable.ic_play);
      Ui.add(page, play, 16);
      play.setOnClickListener(
          v -> {
            if (player != null) return;
            play.setVisibility(View.GONE);
            player = new WebView(requireContext());
            player.getSettings().setJavaScriptEnabled(true);
            player.getSettings().setDomStorageEnabled(true);
            player.getSettings().setMediaPlaybackRequiresUserGesture(true);
            player.getSettings().setAllowFileAccess(false);
            player.setWebViewClient(
                new WebViewClient() {
                  @Override
                  public boolean shouldOverrideUrlLoading(WebView w, WebResourceRequest r) {
                    String h = r.getUrl().getHost();
                    return h == null
                        || !(h.endsWith("youtube.com")
                            || h.endsWith("youtube-nocookie.com")
                            || h.equals("appassets.androidplatform.net"));
                  }
                });
            player.setWebChromeClient(
                new WebChromeClient() {
                  @Override
                  public boolean onConsoleMessage(ConsoleMessage m) {
                    if (m.message().equals("wsm-player-unavailable") && status != null)
                      status.setText(
                          I18n.choose(
                              requireContext(),
                              "Pemain tidak tersedia pada peranti ini. Gunakan Buka sumber untuk"
                                  + " menonton.",
                              "The player is unavailable on this device. Use Open source to"
                                  + " watch."));
                    return true;
                  }

                  @Override
                  public void onShowCustomView(View view, CustomViewCallback callback) {
                    if (fullscreen != null) {
                      callback.onCustomViewHidden();
                      return;
                    }
                    fullscreen = view;
                    requireActivity().addContentView(view, new ViewGroup.LayoutParams(-1, -1));
                  }

                  @Override
                  public void onHideCustomView() {
                    if (fullscreen != null) {
                      ((ViewGroup) fullscreen.getParent()).removeView(fullscreen);
                      fullscreen = null;
                    }
                  }
                });
            String html =
                "<!doctype html><meta name='viewport'"
                    + " content='width=device-width,initial-scale=1'><style>html,body{margin:0;height:100%;background:#171410}#player{width:100%;height:100%;border:0}</style><div"
                    + " id='player'></div><script"
                    + " src='https://www.youtube.com/iframe_api'></script><script>function"
                    + " onYouTubeIframeAPIReady(){window.wsmPlayer=new"
                    + " YT.Player('player',{host:'https://www.youtube-nocookie.com',videoId:'"
                    + selected.youtubeId
                    + "',playerVars:{playsinline:1,autoplay:0,origin:'https://appassets.androidplatform.net'},events:{onError:function(){console.log('wsm-player-unavailable')}}})}setTimeout(function(){if(!window.YT)console.log('wsm-player-unavailable')},15000)</script>";
            player.loadDataWithBaseURL(
                "https://appassets.androidplatform.net/assets/", html, "text/html", "UTF-8", null);
            page.addView(player, new LinearLayout.LayoutParams(-1, Ui.dp(requireContext(), 260)));
          });
      android.view.View source = Ui.quiet(requireContext(), "Buka sumber", R.drawable.ic_video);
      source.setOnClickListener(v -> CatalogRepository.external(requireContext(), selected.url));
      Ui.add(page, source, 16);
    } catch (Exception e) {
      heading(
          "Video",
          I18n.choose(requireContext(), "Video ini belum tersedia.", "This video is unavailable."));
    }
  }

  @Override
  public void onPause() {
    if (player != null) {
      player.evaluateJavascript("window.wsmPlayer&&window.wsmPlayer.pauseVideo()", null);
      player.onPause();
    }
    if (fullscreen != null) {
      ((ViewGroup) fullscreen.getParent()).removeView(fullscreen);
      fullscreen = null;
    }
    super.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (player != null) player.onResume();
  }

  @Override
  public void onDestroyView() {
    if (player != null) {
      player.destroy();
      player = null;
    }
    status = null;
    super.onDestroyView();
  }
}
