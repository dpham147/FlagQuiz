package edu.orangecoastcollege.cs273.dpham147.flagquiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class QuizActivity extends AppCompatActivity {

    // keys for reading data from shared preferences
    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    private boolean phoneDevice = true;
    private boolean preferencesChanged = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set default values in the apps SharedPreferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // register listener for SharedPreferences changes
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(preferencesChangeListener);

        // determine screen size
        int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        // if device is a tablet, set phoneDevice to false
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
            phoneDevice = false;

        // if running on phone-sized device, allow only portrait orientation
        if (phoneDevice)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (preferencesChanged){
            // init QuizActivityFragment and start quiz
            QuizActivityFragment quizFragment = (QuizActivityFragment)
                    getSupportFragmentManager().findFragmentById(R.id.quizFragment);
            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            preferencesChanged = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int orientation = getResources().getConfiguration().orientation;

        // display menu only if in portrait orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT){
            //inflate the menu
            getMenuInflater().inflate(R.menu.menu_quiz, menu);
            return true;
        }
        else
            return false;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);
        return super.onOptionsItemSelected(item);
    }

    private onSharedPreferenceChangeListener preferencesChangeListener =
            new onSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPreferences, String key) {
                preferencesChanged = true; // user changed setting

                QuizActivityFragment quizFragment = (QuizActivityFragment)
                        getSupportFragmentManager().findFragmentById(
                                R.id.quizFragment);

                if (key.equals(CHOICES)) { // # of choices to display changed
                    quizFragment.updateGuessRows(sharedPreferences);
                    quizFragment.resetQuiz();
                } else if (key.equals(REGIONS)) { // regions to include changed
                    Set<String> regions = sharedPreferences.getStringSet(REGIONS, null);

                    if (regions != null && regions.size() > 0) {
                        quizFragment.updateRegions(sharedPreferences);
                        quizFragment.resetQuiz();
                    } else {
                        // must select one region -- NA set as default
                        SharedPreferences.Editor editor =
                                sharedPreferences.edit();
                        regions.add(getString(R.string.default_region));
                        editor.putStringSet(REGIONS, regions);
                        editor.apply();

                        Toast.makeText(QuizActivity.this,
                                R.string.default_region_message,
                                Toast.LENGTH_SHORT).show();
                    }
                }

                Toast.makeText(QuizActivity.this,
                        R.string.restarting_quiz,
                        Toast.LENGTH_SHORT).show();
            }
        };
}