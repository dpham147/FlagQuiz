package edu.orangecoastcollege.cs273.dpham147.flagquiz;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * QuizActivityFragment contains the Flag Quiz Logic
 */
public class QuizActivityFragment extends Fragment {
    // String used when logging error messages
    private static final String TAG = "FlagQuiz Activity";

    private static final int FLAGS_IN_QUIZ = 10;

    private List<String> fileNameList; // flag file names
    private List<String> quizCountriesList; // countries in current quiz
    private Set<String> regionSet; // world regions in current quiz
    private String correctAnswer; // correct country for the current flag
    private int totalGuesses; // number of guesses made
    private int correctAnswers; // number of correct guesses
    private int guessRows; // #of rows displaying guess buttons
    private SecureRandom random; // used to randomize the quiz
    private Handler handler; // used to delay loading the next flag

    private TextView questionNumberTextView; // shows current question #
    private ImageView flagImageView; // displays a flag
    private LinearLayout[] guessLinearLayouts; // rows of answer buttons
    private TextView answerTextView; // displays correct answer

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        // get references to GUI components
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        // config listeners for the guess buttons
        for (LinearLayout row : guessLinearLayouts) {
            for (int col = 0; col < row.getChildCount(); col++) {
                Button button = (Button) row.getChildAt(col);
                button.setOnClickListener(guessButtonListener);
            }
        }

        // set questionNumberTextView's text
        questionNumberTextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));
        return view;
    }

    public void updateGuessRows(SharedPreferences sharedPreferences) {
        // get the number of buttons that should be displayed
        String choices = sharedPreferences.getString(QuizActivity.CHOICES, null);
        guessRows = Integer.parseInt(choices) / 2;

        // hide all guess button Linear Layouts
        for (LinearLayout layout : guessLinearLayouts) {
            layout.setVisibility(View.GONE);
        }

        // display appropriate guess button LinearLayouts
        for (int row = 0; row < guessRows; row++) {
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
        }
    }

    public void updateRegions(SharedPreferences sharedPreferences) {
        regionSet = sharedPreferences.getStringSet(QuizActivity.REGIONS, null);
    }

    public void resetQuiz() {
        // use assetmanager to get image file names for enabled regions
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear(); // empty list of file names

        try {
            // loop through each region
            for (String region : regionSet) {
                // get a list of all flag image files in the region
                String[] paths = assets.list(region);

                for (String path : paths) {
                    fileNameList.add(path.replace(".png", ""));
                }
            }
        } catch (IOException exception) {
            Log.e(TAG, "Error loading file names", exception);
        }

        correctAnswers = 0;
        totalGuesses = 0;
        quizCountriesList.clear();

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        // add FLAGS_IN_QUIZ random file names to the quizCountriesList
        while (flagCounter <= FLAGS_IN_QUIZ) {
            int randomIndex = random.nextInt(numberOfFlags);

            // get a random filename
            String filename = fileNameList.get(randomIndex);

            // if the region is enabled and not already chosen
            if (!quizCountriesList.contains(filename)) {
                quizCountriesList.add(filename);
                ++flagCounter;
            }
        }

        loadNextFlag(); // start quiz by loading first flag
    }

    public void loadNextFlag() {
        // get the filename of the next flag and remove it from the list
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage; // update the correct answer
        answerTextView.setText(""); // clear answer textview

        // display current question number
        questionNumberTextView.setText(getString(R.string.question, correctAnswers + 1, FLAGS_IN_QUIZ));

        // extract the region from the next image's name
        String region = nextImage.substring(0, nextImage.indexOf("_"));

        // use assetmanager to load the next image from assets folder
        AssetManager assets = getActivity().getAssets();

        // get an InputStream to the asset representing the next flag
        // and try to use the input stream
        try (InputStream stream = assets.open(region + "/" + nextImage + ".png")) {
            // load the asset as a drawable and display on flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);
        } catch (IOException exception) {
            Log.e(TAG, "Error Loading" + nextImage, exception);
        }

        Collections.shuffle(fileNameList); // shuffle file names

        // put correct answer at the end of fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        // add 2, 4, 6, or 8 guess buttons based on value of guessRows
        for (int row = 0; row < guessRows; row++) {
            // place buttons in curretTableRow
            for (int col = 0; col < guessLinearLayouts[row].getChildCount(); col++) {
                // get reference to button to configure
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(col);
                newGuessButton.setEnabled(true);

                // get country name and set it as button text
                String filename = fileNameList.get((row * 2) + col);
                newGuessButton.setText(getCountryName(filename));
            }
        }

        // randomly replace one button with correct answer
        int row = random.nextInt(guessRows); // pick random row
        int col = random.nextInt(2); // pick random column
        LinearLayout randomRow = guessLinearLayouts[row]; // get the row
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(col)).setText(countryName);
    }

    private String getCountryName(String name) {
        String countryName = name.substring(name.indexOf('-' + 1));
        return countryName.replace('_', ' ');
    }


    private View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button guessButton = ((Button) v);
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++ totalGuesses;

            if (guess.equals(correctAnswer)) { // if guess is correct
                ++correctAnswers; // increment # of correct answers

                //display correct answer in green text
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer,
                        getContext().getTheme()));

                disableButtons(); // disable guess buttons

                // if the user has correctly identified FLAGS_IN_QUIZ flags
                if (correctAnswers == FLAGS_IN_QUIZ) {
                    // Dialogfragment to display quiz results and start a new quiz
                    DialogFragment quizResults =
                            new DialogFragment() {
                                // create an alert dialog and return it
                                @Override
                                public Dialog onCreateDialog(Bundle bundle) {
                                    AlertDialog.Builder builder =
                                            new AlertDialog.Builder(getActivity());
                                    builder.setMessage(getString(R.string.results,
                                            totalGuesses, (1000 / (double) totalGuesses)));

                                    // reset quiz button
                                    builder.setPositiveButton(R.string.reset_quiz,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id){
                                                    resetQuiz();
                                                }
                                            }
                                    );

                                    return builder.create();
                                }
                            };
                    quizResults.setCancelable(false);
                    quizResults.show(getFragmentManager(), "quiz results");
                }
                else { // answer is correct but quiz not over
                    // load next flag after 2s delay
                    handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                loadNextFlag();
                                            }
                                        }, 2000); // 2000ms for a 2s delay
                }
            }
            else { //answer was incorrect
                // display "Incorrect!" in red
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer,
                        getContext().getTheme()));
                guessButton.setEnabled(false);
            }
        }
    };

    private void disableButtons() {
        for (int row = 0; row < guessRows; row++) {
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }
}
