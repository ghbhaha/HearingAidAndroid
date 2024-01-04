package com.github.vatbub.hearingaid.fragments;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.github.vatbub.hearingaid.BuildConfig;
import com.github.vatbub.hearingaid.CustomApplication;
import com.github.vatbub.hearingaid.FeedbackPrivacyActivity;
import com.github.vatbub.hearingaid.MainActivity;
import com.github.vatbub.hearingaid.ProfileManager;
import com.github.vatbub.hearingaid.R;
import com.github.vatbub.hearingaid.RemoteConfig;
import com.github.vatbub.hearingaid.profileeditor.ProfileEditorActivity;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends CustomFragment implements ProfileManager.ProfileManagerListener, AdapterView.OnItemSelectedListener {
    private ArrayAdapter<ProfileManager.Profile> profileAdapter;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initVersionLabel();
        updateEqSwitch();
        loadEqSettings();
        updateCrashReportingCheckBox();

        initButtonHandlers();
        initFrequencyLabelsAndSeekbars();
    }

    private void updateCrashReportingCheckBox() {
        AppCompatCheckBox crashReportsEnabledCheckBox = findViewById(R.id.enableCrashReportsCheckBox);
        crashReportsEnabledCheckBox.setChecked(CustomApplication.isBugSnagEnabled(getContext()));
    }

    @Override
    public void onResume() {
        super.onResume();
        ProfileManager.getInstance(getActivity()).getChangeListeners().add(this);
        initProfileSelector();
    }

    private void initProfileSelector() {
        Spinner profileSelector = findViewById(R.id.fragment_settings_profile_selector);
        profileSelector.setAdapter(getProfileAdapter(true));
        profileSelector.setOnItemSelectedListener(this);
        if (ProfileManager.getInstance(getContext()).getCurrentlyActiveProfile() != null)
            profileSelector.setSelection(ProfileManager.getInstance(getContext()).getPosition(ProfileManager.getInstance(getContext()).getCurrentlyActiveProfile()));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_settings_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.fragment_settings_launch_profile_editor:
                Intent intent = new Intent(getContext(), ProfileEditorActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initButtonHandlers() {
        Switch eqSwitch = findViewById(R.id.eq_on_off_switch);
        eqSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
            ProfileManager.Profile currentProfile = ProfileManager.getInstance(getActivity()).getCurrentlyActiveProfile();
            if (currentProfile != null)
                currentProfile.setEqEnabled(checked);

            updateEQViewEnabledStatus(checked);

            StreamingFragment streamingFragment = (StreamingFragment) getActivity().getSupportFragmentManager().findFragmentByTag(FragmentTag.STREAMING_FRAGMENT.toString());
            if (streamingFragment != null)
                streamingFragment.notifyEQEnabledSettingChanged();
        });

        AppCompatCheckBox crashReportsEnabledCheckBox = findViewById(R.id.enableCrashReportsCheckBox);
        crashReportsEnabledCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CustomApplication.setBugSnagEnabled(getContext(), isChecked);
            if (isChecked)
                CustomApplication.initializeBugSnag(getContext());
            else
                showRestartDialog();
        });

        Button viewPrivacyStatementButton = findViewById(R.id.fragment_settings_view_privacy_button);
        viewPrivacyStatementButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FeedbackPrivacyActivity.class);
            startActivity(intent);
        });
    }

    private void showRestartDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());

        // set title
        alertDialogBuilder.setTitle(R.string.fragment_settings_restart_alert_title);

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.fragment_settings_restart_alert_message)
                .setCancelable(true)
                .setPositiveButton(R.string.fragment_settings_restart_alert_restart_button, (dialog, id) -> restartApp())
                .setNegativeButton(R.string.fragment_settings_restart_alert_cancel_button, (dialog, id) -> dialog.cancel());

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void restartApp() {
        Context context = getContext();
        if (context == null) return;
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    private void initFrequencyLabelsAndSeekbars() {
        double lowerFreq = Double.parseDouble(RemoteConfig.getConfig().getValue(RemoteConfig.Keys.MIN_EQ_FREQUENCY));
        double higherFreq = Double.parseDouble(RemoteConfig.getConfig().getValue(RemoteConfig.Keys.MAX_EQ_FREQUENCY));
        double numberOfChannels = Double.parseDouble(RemoteConfig.getConfig().getValue(RemoteConfig.Keys.NUMBER_OF_EQ_BINS));
        double hzPerChannel = (higherFreq - lowerFreq) / numberOfChannels;
        final int[] textViewIds = {R.id.text_view_bin_1, R.id.text_view_bin_2, R.id.text_view_bin_3, R.id.text_view_bin_4, R.id.text_view_bin_5, R.id.text_view_bin_6};

        for (int channel = 1; channel <= numberOfChannels; channel++) {
            double meanBinFreq = lowerFreq + (channel - 0.5) * hzPerChannel;
            // double higherBinFreq = lowerFreq + channel * hzPerChannel;

            String textToShow = getString(R.string.fragment_settings_frequency_bin_pattern_abbreviated).replace("{abbreviatedFrequency}", getStringForFrequency(meanBinFreq));

            ((TextView) findViewById(textViewIds[channel - 1])).setText(textToShow);

            VerticalSeekBar seekBar = findViewById(getSeekbarIDs()[channel - 1]);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser)
                        saveEqSettings();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
    }

    private void saveEqSettings() {
        List<Float> eqSettings = new ArrayList<>(getSeekbarIDs().length);
        for (int seekbarId : getSeekbarIDs()) {
            VerticalSeekBar seekBar = findViewById(seekbarId);
            eqSettings.add(seekBar.getProgress() / (float) seekBar.getMax());
        }

        ProfileManager.getInstance(getContext()).getCurrentlyActiveProfile().setEQSettings(eqSettings);
    }

    private void loadEqSettings() {
        Context context = getContext();
        if (context == null) return;
        ProfileManager.Profile currentProfile = ProfileManager.getInstance(context).getCurrentlyActiveProfile();
        if (currentProfile == null) return;
        List<Float> eqSettings = currentProfile.getEQSettings();
        int[] seekbarIDs = getSeekbarIDs();
        if (seekbarIDs.length != eqSettings.size()) {
            // setting was probably never set
            eqSettings = new ArrayList<>();
            for (int ignored : seekbarIDs) {
                eqSettings.add(0f);
            }
        }

        for (int i = 0; i < seekbarIDs.length; i++) {
            VerticalSeekBar seekBar = findViewById(seekbarIDs[i]);
            // seekBar.setProgress((int) (eqSettings.get(i) * seekBar.getMax()));
            animateSeekbar(seekBar, (int) (eqSettings.get(i) * seekBar.getMax()));
        }
    }

    private void animateSeekbar(final SeekBar seekBar, int toValue) {
        ValueAnimator anim = ValueAnimator.ofInt(seekBar.getProgress(), toValue);
        anim.setDuration(400);
        anim.addUpdateListener(animation -> {
            int animProgress = (Integer) animation.getAnimatedValue();
            seekBar.setProgress(animProgress);
        });
        anim.start();
    }

    private String getStringForFrequency(double frequency) {
        int mhzFactor = 1000000;
        int khzFactor = 1000;
        if (frequency >= mhzFactor) {
            return Long.toString(Math.round(frequency / mhzFactor)) + " " + getString(R.string.fragment_settings_MHz);
        } else if (frequency >= khzFactor) {
            return Long.toString(Math.round(frequency / khzFactor)) + " " + getString(R.string.fragment_settings_kHz);
        } else {
            return Long.toString(Math.round(frequency)) + " " + getString(R.string.fragment_settings_Hz);
        }
    }

    @Override
    public void onProfileApplied(@Nullable ProfileManager.Profile oldProfile, @Nullable ProfileManager.Profile newProfile) {
        if (newProfile == null)
            return;

        Spinner profileSelector = findViewById(R.id.fragment_settings_profile_selector);
        int position = ProfileManager.getInstance(getActivity()).getPosition(newProfile);
        profileSelector.setSelection(position);
        updateEqSwitch();
        loadEqSettings();
    }

    @Override
    public void onProfileCreated(ProfileManager.Profile newProfile) {
        getProfileAdapter().add(newProfile);
    }

    @Override
    public void onProfileDeleted(ProfileManager.Profile deletedProfile) {
        getProfileAdapter().remove(deletedProfile);
    }

    @Override
    public void onSortOrderChanged(List<ProfileManager.Profile> previousOrder, List<ProfileManager.Profile> newOrder) {
        initProfileAdapter();
    }

    private void initVersionLabel() {
        ((TextView) findViewById(R.id.version_text_view)).setText(String.format(getString(R.string.fragment_settings_version_text_view), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
    }

    private void updateEqSwitch() {
        Switch eqSwitch = findViewById(R.id.eq_on_off_switch);
        ProfileManager.Profile currentProfile = ProfileManager.getInstance(getContext()).getCurrentlyActiveProfile();
        if (eqSwitch == null) return;
        if (currentProfile == null) return;
        eqSwitch.setChecked(currentProfile.isEqEnabled());
        updateEQViewEnabledStatus(currentProfile.isEqEnabled());
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        ProfileManager.Profile selectedProfile = (ProfileManager.Profile) adapterView.getItemAtPosition(pos);
        ProfileManager.getInstance(getActivity()).applyProfile(selectedProfile);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        System.out.println("Nothing selected");
    }

    public ArrayAdapter<ProfileManager.Profile> getProfileAdapter() {
        return getProfileAdapter(false);
    }

    public ArrayAdapter<ProfileManager.Profile> getProfileAdapter(boolean forceRefresh) {
        if (profileAdapter == null) {
            profileAdapter = new ArrayAdapter<>(getContext(), R.layout.simple_spinner_item_black);
            profileAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            initProfileAdapter();
        } else if (forceRefresh)
            initProfileAdapter();
        return profileAdapter;
    }

    private void initProfileAdapter() {
        getProfileAdapter().clear();
        getProfileAdapter().addAll(ProfileManager.getInstance(getActivity()).listProfiles());
    }

    private int[] getSeekbarIDs() {
        return new int[]{R.id.eq_channel_1, R.id.eq_channel_2, R.id.eq_channel_3, R.id.eq_channel_4, R.id.eq_channel_5, R.id.eq_channel_6};
    }

    private void updateEQViewEnabledStatus(boolean enabled) {
        for (int seekbarId : getSeekbarIDs()) {
            findViewById(seekbarId).setEnabled(enabled);
        }
    }
}
