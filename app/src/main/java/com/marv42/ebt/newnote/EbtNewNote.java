/*
 Copyright (c) 2010 - 2024 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import static android.Manifest.permission.POST_NOTIFICATIONS;
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
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
    public static final int NOTIFICATIONS_PERMISSION_REQUEST_CODE = 5;
    static final int SUBMIT_FRAGMENT_INDEX = 0;
    private static final int RESULTS_FRAGMENT_INDEX = 1;
    private static final int DATA_RESULTS_FRAGMENT_INDEX = 2;
    private static final int VIBRATION_MS = 150;
    @Inject
    EncryptedPreferenceDataStore dataStore;
    @Inject
    SharedPreferencesHandler sharedPreferencesHandler;
    @Inject
    MySharedPreferencesListener sharedPreferencesListener;
    @Inject
    ViewModelProvider viewModelProvider;
    private SubmitFragment submitFragment = null;
    private ResultsFragment resultsFragment = null;
    private boolean isResultsEmpty = true;
    private String[] commentSuggestionsUntilFragmentAdded;
    private String ocrResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout();
        setupFragmentsInitially();
        setupResultsObserver();
        requestNotificationsPermissions();
        sharedPreferencesListener.register();
    }

    private void requestNotificationsPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                PermissionChecker.checkSelfPermission(this, NOTIFICATION_SERVICE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, NOTIFICATIONS_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isDualPane()) {
            submitFragment = (SubmitFragment) getSupportFragmentManager().findFragmentById(R.id.submit_fragment_container_view);
            resultsFragment = (ResultsFragment) getSupportFragmentManager().findFragmentById(R.id.results_fragment_container_view);
        }
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

    private boolean isDualPane() {
        return getViewPager() == null;
    }

    private ViewPager2 getViewPager() {
        return findViewById(R.id.view_pager);
    }

    private void setupFragmentsInitially() {
        if (isDualPane())
            setupFragmentsInitiallyDualPane();
        else
            setupFragmentsInitiallyNoDualPane();
    }

    private void setupFragmentsInitiallyDualPane() {
        addSubmitFragment();
        addResultsFragment();
    }

    private String getFragmentTag(int fragmentIndex) {
        return "f" + fragmentIndex;
    }

    private void addSubmitFragment() {
        final String tag = getFragmentTag(SUBMIT_FRAGMENT_INDEX);
        commitFragmentManagerAddTransaction(R.id.submit_fragment_container_view, SubmitFragment.class, tag);
    }

    private void addResultsFragment() {
        final String tag = getFragmentTag(RESULTS_FRAGMENT_INDEX);
        commitFragmentManagerAddTransaction(R.id.results_fragment_container_view, ResultsFragmentEmpty.class, tag);
    }

    private void commitFragmentManagerAddTransaction(int containerViewId, Class<? extends Fragment> fragmentClass, String tag) {
//        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
//        if (fragment != null)
//            throw new IllegalArgumentException("fragment already added");
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(containerViewId, fragmentClass, null, tag)
                .commit();
    }

    private void setupFragmentsInitiallyNoDualPane() {
        setupViewPager();
        setupTabLayout();
    }

    private void setupViewPager() {
        MyFragmentStateAdapter adapter = new MyFragmentStateAdapter();
        ViewPager2 viewPager = getViewPager();
        viewPager.setAdapter(adapter);
    }

    private void setupTabLayout() {
        new TabLayoutMediator(getTabLayout(), getViewPager(), (tab, position) -> {
            switch (position) {
                case SUBMIT_FRAGMENT_INDEX -> tab.setText(getString(R.string.submit_fragment_title));
                case RESULTS_FRAGMENT_INDEX -> tab.setText(getString(R.string.results_fragment_title));
                default -> throw new IllegalArgumentException("position");
            }
        }).attach();
    }

    private TabLayout getTabLayout() {
        return findViewById(R.id.tab_layout);
    }

    private void setupResultsObserver() {
        ResultsViewModel viewModel = viewModelProvider.get(ResultsViewModel.class);
        viewModel.getResults().observe(this, resultsObserver -> {
            if (! resultsObserver.isEmpty())
                if (isDualPane())
                    replaceResultsFragment();
                else
                    isResultsEmpty = false;
        });
    }

    private void replaceResultsFragment() {
        if (resultsFragment instanceof ResultsFragmentData)
            return;
        getSupportFragmentManager().beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.results_fragment_container_view, ResultsFragmentData.class, null)
            .commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!isDualPane() && shouldSwitchToResults(intent))
            switchFragment(RESULTS_FRAGMENT_INDEX);
    }

    private boolean shouldSwitchToResults(Intent intent) {
        final String extra = intent.getStringExtra(FRAGMENT_TYPE);
        return extra != null && extra.equals(ResultsFragment.class.getSimpleName());
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
        if (dataStore.get(R.string.pref_settings_ocr_online_key, false)) {
            String apiKey = dataStore.get(R.string.pref_settings_ocr_service_key, "");
            new OcrHandlerOnline(this, photoPath, photoUri, getContentResolver(), apiKey).execute();
        } else
            new OcrHandlerLocal(this, this, photoPath).execute();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.no_permission, LENGTH_LONG).show();
            return;
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, R.string.permission_location, LENGTH_LONG).show();
            submitFragment.locationButtonClicked();
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, R.string.permission_camera, LENGTH_LONG).show();
            submitFragment.takePhoto();
        }
    }

    @Override
    public void onSubmitFragmentAdded() {
        if (commentSuggestionsUntilFragmentAdded != null && commentSuggestionsUntilFragmentAdded.length > 0) {
            submitFragment.setCommentsAdapter(commentSuggestionsUntilFragmentAdded);
            commentSuggestionsUntilFragmentAdded = null;
        }
    }

    @Override
    public void switchFragment(int fragmentIndex) {
        if (isDualPane())
            return;
        ViewPager2 viewPager = getViewPager();
        viewPager.setCurrentItem(fragmentIndex);
    }

    @Override
    public void onSuggestions(String[] suggestions) {
        if (submitFragment != null && submitFragment.isAdded())
            submitFragment.setCommentsAdapter(suggestions);
        else
            commentSuggestionsUntilFragmentAdded = suggestions;
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
        if (submitFragment == null)
            return;
        final boolean serialNumberNotShortCode = ocrResult.length() >= LENGTH_THRESHOLD_SERIAL_NUMBER;
        submitFragment.checkClipboardManager(serialNumberNotShortCode);
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        if (serialNumberNotShortCode)
            viewModel.setSerialNumber(ocrResult);
        else
            viewModel.setShortCode(ocrResult);
    }

    private void vibrate() {
        VibratorManager manager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
        Vibrator v = manager.getDefaultVibrator();
        v.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, DEFAULT_AMPLITUDE));
    }

//    @Override
//    public void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner, @NonNull Lifecycle.State state) {
//        // TODO
//    }

    private class MyFragmentStateAdapter extends FragmentStateAdapter {
        private static final int NUM_TABS = 2;

        MyFragmentStateAdapter() {
            super(getSupportFragmentManager(), EbtNewNote.this.getLifecycle());
        }

        @Override
        public int getItemCount() {
            return NUM_TABS;
        }

        @Override
        public long getItemId(int position) {
            if (position == RESULTS_FRAGMENT_INDEX && ! isResultsEmpty)
                position = DATA_RESULTS_FRAGMENT_INDEX; // This triggers a createFragment call for a ResultsFragmentData
            return position;
        }

        @Override
        public boolean containsItem(long itemId) {
            return switch ((int) itemId) {
                case SUBMIT_FRAGMENT_INDEX -> submitFragment != null;
                case RESULTS_FRAGMENT_INDEX -> resultsFragment != null;
                case DATA_RESULTS_FRAGMENT_INDEX -> ! isResultsEmpty;
                default -> throw new IllegalArgumentException("itemId");
            };
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case SUBMIT_FRAGMENT_INDEX -> {
                    submitFragment = new SubmitFragment(); return submitFragment; }
                case RESULTS_FRAGMENT_INDEX -> {
                    resultsFragment = isResultsEmpty ? new ResultsFragmentEmpty() : new ResultsFragmentData();
                    return resultsFragment; }
                default -> throw new IllegalArgumentException("position");
            }
        }
    }
}
