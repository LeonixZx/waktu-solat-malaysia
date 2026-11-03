package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.preferences.UserStore;
import com.example.waktusolatmalaysia.ui.*;

public final class WorshipFragment extends FeatureFragment {
  private int columns = -1;
  @Override protected void build(Bundle saved) {
    page.setId(R.id.worship_list);
    page.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
      int next = r-l >= Ui.dp(requireContext(),680) && getResources().getConfiguration().fontScale <= 1.3f ? 4 : 2;
      if (columns != next) { columns = next; render(); }
    });
    render();
  }
  private void render() {
    page.removeAllViews();
    heading(I18n.choose(requireContext(), "Amalan dan bacaan", "Practice and reading"),
        I18n.choose(requireContext(), "Pilih bacaan atau alat untuk rutin anda.", "Reading and tools for your daily practice."));
    label("Bacaan", "Reading");
    GridLayout readings = new GridLayout(requireContext()); readings.setColumnCount(Math.max(2, columns));
    String[] kinds={"doa","wirid","niat","hadith"}, names={"Doa","Wirid","Niat","Hadis"};
    int[] icons={R.drawable.ic_doa,R.drawable.ic_tasbih,R.drawable.ic_task,R.drawable.ic_hadith};
    for (int i=0;i<kinds.length;i++) {
      String kind=kinds[i];
      LinearLayout card=Ui.column(requireContext()); card.setGravity(Gravity.CENTER); card.setMinimumHeight(Ui.dp(requireContext(),104));
      card.setPadding(Ui.dp(requireContext(),12),Ui.dp(requireContext(),16),Ui.dp(requireContext(),12),Ui.dp(requireContext(),16));
      card.setBackground(Ui.background(requireContext(),R.color.surface,20)); card.setFocusable(true);
      card.addView(Ui.icon(requireContext(),icons[i],28));
      TextView text=Ui.text(requireContext(),names[i],14,true,R.color.text_primary); text.setGravity(Gravity.CENTER); Ui.add(card,text,10);
      card.setContentDescription(I18n.t(requireContext(),names[i]));
      card.setOnClickListener(v -> { new UserStore(requireContext()).set("collection_tab",kind); open(R.id.collectionFragment); });
      gridAdd(readings,card);
    }
    Ui.add(page,readings,8);
    label("Amalan", "Practice");
    GridLayout practice=tools();
    tool(practice,"Tasbih digital","Amalan hari ini",R.drawable.ic_tasbih,R.id.tasbihFragment);
    tool(practice,"Peringatan peribadi",I18n.choose(requireContext(),"Sebelum atau selepas solat","Before or after prayers"),R.drawable.ic_task,R.id.tasksFragment);
    Ui.add(page,practice,8);
    label("Panduan", "Guidance");
    GridLayout guidance=tools();
    tool(guidance,"Kiblat",I18n.choose(requireContext(),"Arah dan keyakinan sensor","Direction and sensor confidence"),R.drawable.ic_compass,R.id.qiblaFragment);
    tool(guidance,"Kalendar Islam",I18n.choose(requireContext(),"Tarikh, jadual dan eksport","Dates, schedules and export"),R.drawable.ic_calendar,R.id.calendarFragment);
    Ui.add(page,guidance,8);
    label("Skrin utama", "Home screen");
    Ui.add(page,Ui.line(requireContext(),"Widget waktu solat","Panduan widget",R.drawable.ic_widget,() -> open(R.id.widgetsFragment)),8);
  }
  private void label(String ms,String en) { Ui.add(page,Ui.text(requireContext(),I18n.choose(requireContext(),ms,en),18,true,R.color.text_primary),24); }
  private GridLayout tools() { GridLayout g=new GridLayout(requireContext()); g.setColumnCount(columns==4?2:1); return g; }
  private void tool(GridLayout grid,String name,String subtitle,int icon,int destination) {
    gridAdd(grid,Ui.line(requireContext(),name,subtitle,icon,() -> open(destination)));
  }
  private void gridAdd(GridLayout grid,View v) {
    GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED),GridLayout.spec(GridLayout.UNDEFINED,1f));
    p.width=0; p.height=-2; p.setGravity(Gravity.FILL_HORIZONTAL); int margin=Ui.dp(requireContext(),4); p.setMargins(margin,margin,margin,margin); grid.addView(v,p);
  }
}
