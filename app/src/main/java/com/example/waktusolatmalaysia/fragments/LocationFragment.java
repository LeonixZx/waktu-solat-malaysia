package com.example.waktusolatmalaysia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.databinding.ScreenZoneBinding;
import com.example.waktusolatmalaysia.model.PrayerZone;
import com.example.waktusolatmalaysia.preferences.AppPreferences;
import com.example.waktusolatmalaysia.repo.ZoneRepository;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LocationFragment extends Fragment {
  private ScreenZoneBinding binding;
  private Map<String, List<PrayerZone>> states;
  private List<PrayerZone> districts;
  private PrayerZone selected;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = ScreenZoneBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    AppPreferences preferences = new AppPreferences(requireContext());
    try {
      states = ZoneRepository.load(requireContext());
    } catch (IllegalStateException failure) {
      binding.hint.setText(R.string.missing_zones);
      binding.save.setEnabled(false);
      return;
    }
    List<String> names = new ArrayList<>(states.keySet());
    binding.state.setAdapter(
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names));
    PrayerZone current = preferences.zone();
    String stateName =
        savedInstanceState == null
            ? current.state
            : savedInstanceState.getString("state", current.state);
    if (!states.containsKey(stateName)) stateName = names.get(0);
    selectState(stateName);
    String code =
        savedInstanceState == null
            ? current.code
            : savedInstanceState.getString("code", current.code);
    String districtName =
        savedInstanceState == null
            ? current.name
            : savedInstanceState.getString("district", current.name);
    for (PrayerZone district : districts)
      if (district.code.equals(code)) {
        selected = district;
        if (district.name.equals(districtName)) break;
      }
    binding.district.setText(selected.toString(), false);
    binding.state.setOnItemClickListener(
        (parent, child, position, id) -> selectState((String) parent.getItemAtPosition(position)));
    binding.district.setOnItemClickListener(
        (parent, child, position, id) ->
            selected = (PrayerZone) parent.getItemAtPosition(position));
    binding.save.setOnClickListener(
        v -> {
          if (selected == null) return;
          preferences.setZone(selected);
          Snackbar message = Snackbar.make(
                  requireActivity().findViewById(R.id.main_surface),
                  R.string.location_saved,
                  Snackbar.LENGTH_SHORT);
          View bottom = requireActivity().findViewById(R.id.bottom_nav_shell);
          if (bottom.getVisibility() == View.VISIBLE) message.setAnchorView(bottom);
          message.show();
          NavHostFragment.findNavController(this).popBackStack(R.id.prayerFragment, false);
        });
  }

  private void selectState(String name) {
    districts = states.get(name);
    if (districts == null || districts.isEmpty()) return;
    selected = districts.get(0);
    binding.state.setText(name, false);
    binding.district.setAdapter(
        new ArrayAdapter<>(
            requireContext(), android.R.layout.simple_dropdown_item_1line, districts));
    binding.district.setText(selected.toString(), false);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle state) {
    if (selected != null) {
      state.putString("state", selected.state);
      state.putString("code", selected.code);
      state.putString("district", selected.name);
    }
    super.onSaveInstanceState(state);
  }

  @Override
  public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }
}
