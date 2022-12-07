/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
import static android.widget.Toast.LENGTH_LONG;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR;
import static com.marv42.ebt.newnote.scanning.Corrections.LENGTH_THRESHOLD_SERIAL_NUMBER;
import static com.marv42.ebt.newnote.scanning.TextProcessor.NEW_LINE;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.marv42.ebt.newnote.exceptions.ErrorMessage;
import com.marv42.ebt.newnote.preferences.EncryptedPreferenceDataStore;
import com.marv42.ebt.newnote.preferences.MySharedPreferencesListener;
import com.marv42.ebt.newnote.preferences.SettingsActivity;
import com.marv42.ebt.newnote.preferences.SharedPreferencesHandler;
import com.marv42.ebt.newnote.scanning.IOcrHandler;
import com.marv42.ebt.newnote.scanning.OcrHandlerLocal;
import com.marv42.ebt.newnote.scanning.OcrHandlerOnline;
import com.marv42.ebt.newnote.scanning.OcrNotifier;
import com.marv42.ebt.newnote.ui.ResultsViewModel;
import com.marv42.ebt.newnote.ui.SubmitViewModel;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;

public class EbtNewNote extends DaggerAppCompatActivity
        implements SubmitFragment.Callback, ResultsFragmentData.Callback, CommentSuggestion.Callback,
        IOcrHandler.Callback, ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String FRAGMENT_TYPE = "fragment_type";
    public static final int IMAGE_CAPTURE_REQUEST_CODE = 2;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 3;
    public static final int CAMERA_PERMISSION_REQUEST_CODE = 4;
    static final int SUBMIT_FRAGMENT_INDEX = 0;
    private static final int RESULTS_FRAGMENT_INDEX = 1;
    private static final int VIBRATION_MS = 150;
    private static final int NUM_TABS = 2;
    @Inject
    EncryptedPreferenceDataStore dataStore;
    @Inject
    SharedPreferencesHandler sharedPreferencesHandler;
    @Inject
    MySharedPreferencesListener sharedPreferencesListener;
    @Inject
    SubmissionResultHandler submissionResultHandler;
    @Inject
    ViewModelProvider viewModelProvider;
    private SubmitFragment submitFragment = null;
    private String[] commentSuggestions;
    private boolean isDualPane = false;
    private boolean isResultsEmpty = true;
    private String ocrResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout();
        setDualPane();
        setFragmentsInitially();
        setupViewModel();
        sharedPreferencesListener.register();
    }

    private void setLayout() {
        applyStyle();
        setContentView(R.layout.main);
    }

    private void applyStyle() {
        Resources.Theme theme = getTheme();
        TypedValue colorAccentValue = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.colorAccent, colorAccentValue, true)) {
            @ColorRes int resourceId = colorAccentValue.resourceId != 0 ? colorAccentValue.resourceId : colorAccentValue.data;
            @ColorInt int color = ContextCompat.getColor(this, resourceId);
            theme.applyStyle(color, true);
        }
    }

    private void setDualPane() {
        ViewPager2 pager = findViewById(R.id.view_pager);
        if (pager == null)
            isDualPane = true;
    }

    private void setFragmentsInitially() {
        if (isDualPane)
            setupFragmentsInitiallyDualPane();
        else
            setupFragmentsInitiallyNoDualPane();
    }

    private void setupFragmentsInitiallyDualPane() {
        final FragmentManager manager = getSupportFragmentManager();
        submitFragment = (SubmitFragment) manager.findFragmentById(R.id.submit_fragment);
        addResultsFragment();
    }

    private void addResultsFragment() {
        final FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .add(R.id.results_holder, ResultsFragmentEmpty.class, null)
                .commit();
    }

    private void setupFragmentsInitiallyNoDualPane() {
        setupViewPager();
        setupTabLayout();
    }

    private void setupViewPager() {
        ViewPager2 pager = findViewById(R.id.view_pager);
        MyFragmentStateAdapter adapter = new MyFragmentStateAdapter();
        pager.setAdapter(adapter);
    }

    private void setupTabLayout() {
        ViewPager2 pager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, pager, (tab, position) -> {
            if (position == SUBMIT_FRAGMENT_INDEX)
                tab.setText(getString(R.string.submit_fragment_title));
            else if (position == RESULTS_FRAGMENT_INDEX)
                tab.setText(getString(R.string.results_fragment_title));
            else
                throw new IllegalArgumentException("position");
        }).attach();
    }

    private void setupViewModel() {
        ResultsViewModel viewModel = viewModelProvider.get(ResultsViewModel.class);
        viewModel.getResults().observe(this, observer -> {
            isResultsEmpty = observer.isEmpty();
            if (! isResultsEmpty)
                // TODO we don't need to do this, if we already have the ResultsFragmentData
                if (isDualPane)
                    replaceResultsFragment();
                else
                    invalidateResultsView();
        });
    }

    private void replaceResultsFragment() {
        final FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.results_holder, ResultsFragmentData.class, null)
                .commit();
    }

    private void invalidateResultsView() {
        ViewPager2 pager = findViewById(R.id.view_pager);
        RecyclerView.Adapter<?> adapter = pager.getAdapter();
        if (adapter != null)
            adapter.notifyItemChanged(RESULTS_FRAGMENT_INDEX);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        submissionResultHandler.reset();
        if (!isDualPane)
            switchFragment(RESULTS_FRAGMENT_INDEX);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        inflateMenu(menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    private void inflateMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        menu.findItem(R.id.settings).setIntent(new Intent(this, SettingsActivity.class));
        menu.findItem(R.id.licenses).setIntent(new Intent(this, LicensesActivity.class));
        menu.findItem(R.id.about).setOnMenuItemClickListener(new About(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == IMAGE_CAPTURE_REQUEST_CODE)
            if (resultCode == RESULT_OK)
                processPhoto();
    }

    private void processPhoto() {
        Toast.makeText(this, R.string.processing, LENGTH_LONG).show();
        String photoPath = sharedPreferencesHandler.get(R.string.pref_photo_path_key, "");
        Uri photoUri = Uri.parse(sharedPreferencesHandler.get(R.string.pref_photo_uri_key, ""));
        if (/*localOcrSetInPreferences*/true) {
            new OcrHandlerLocal(this, this, photoPath).execute();
        }
        else {
            String apiKey = dataStore.get(R.string.pref_settings_ocr_key, "");
            new OcrHandlerOnline(this, photoPath, photoUri, getContentResolver(), apiKey).execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.no_permission, LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, R.string.permission, LENGTH_LONG).show();
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE)
            submitFragment.locationButtonClicked();
        else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE)
            submitFragment.takePhoto();
    }

    @Override
    public void onSubmitFragmentAdded() {
        if (commentSuggestions != null && commentSuggestions.length > 0) {
            submitFragment.setCommentsAdapter(commentSuggestions);
            commentSuggestions = null;
        }
    }

    @Override
    public void switchFragment(int fragmentIndex) {
        if (!isDualPane) {
            ViewPager2 viewPager = findViewById(R.id.view_pager);
            viewPager.setCurrentItem(fragmentIndex);
        }
    }

    @Override
    public void onSuggestions(String[] suggestions) {
        if (submitFragment != null && submitFragment.isAdded())
            submitFragment.setCommentsAdapter(suggestions);
        else
            commentSuggestions = suggestions;
    }

    @Override
    protected void onDestroy() {
        sharedPreferencesListener.unregister();
        super.onDestroy();
    }

    @Override
    public void onOcrResult(@NonNull String result) {
        if (result.isEmpty())
            OcrNotifier.showDialog(this, getString(R.string.ocr_dialog_empty));
        else if (result.startsWith(ERROR))
            OcrNotifier.showDialog(this, new ErrorMessage(this).getErrorMessage(result));
        else
            checkMultipleOcrResults(result);
    }

    private void checkMultipleOcrResults(@NonNull String result) {
        if (result.contains(NEW_LINE))
            letUserChoose(result);
        else {
            ocrResult = result;
            vibrate();
            replaceShortCodeOrSerialNumber();
            Toast.makeText(this, R.string.ocr_return, LENGTH_LONG).show();
        }
    }

    private void letUserChoose(String ocrResults) {
        String[] allResults = ocrResults.split(NEW_LINE);
        ocrResult = "";
        new AlertDialog.Builder(this)
                .setTitle(R.string.ocr_multiple_results)
//                .setMessage(R.string.ocr_multiple_results)  // https://developer.android.com/develop/ui/views/components/dialogs#AddingAList
                .setItems(allResults, (dialog, item) -> {
                    ocrResult = allResults[item];
                    replaceShortCodeOrSerialNumber();
                })
                .setCancelable(false)
                .setNegativeButton(getString(android.R.string.cancel), null)
                .create()
                .show();
    }

    private void replaceShortCodeOrSerialNumber() {
        final boolean serialNumberOrShortCode = ocrResult.length() >= LENGTH_THRESHOLD_SERIAL_NUMBER;
        submitFragment.checkClipboardManager(serialNumberOrShortCode);
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        if (serialNumberOrShortCode)
            viewModel.setSerialNumber(ocrResult);
        else
            viewModel.setShortCode(ocrResult);
    }

    private void vibrate() {
        Vibrator v;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            v = manager.getDefaultVibrator();
        }
        else
            v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null)
            v.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, DEFAULT_AMPLITUDE));
    }

    private class MyFragmentStateAdapter extends FragmentStateAdapter {
        MyFragmentStateAdapter() {
            super(getSupportFragmentManager(), EbtNewNote.this.getLifecycle());
        }

        @Override
        public int getItemCount() {
            return NUM_TABS;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment;
            if (position == SUBMIT_FRAGMENT_INDEX) {
                submitFragment = new SubmitFragment();
                fragment = submitFragment;
            } else if (position == RESULTS_FRAGMENT_INDEX)
                if (isResultsEmpty)
                    fragment = new ResultsFragmentEmpty();
                else
                    fragment = new ResultsFragmentData();
            else
                throw new IllegalArgumentException("position");
            return fragment;
        }
    }
}
