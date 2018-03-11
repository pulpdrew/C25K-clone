package com.pulpdrew.c25k;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements Timer.TimerListener {

    /*
     * Identification constants
     */
    private static String CHANNEL_ID = "125348";
    private static int NOTIFICATION_ID = 123;

    private static int PROGRESS_SUBDIVISIONS = 1000;

    /*
     * Layout View variables
     */
    private ProgressBar timerProgressBar;
    private SeekBar stageSeekBar;
    private Button plusButton, minusButton, pauseButton, completeButton, startButton;
    private Spinner weekSpinner, daySpinner;
    private TextView completeTV, summaryTV, timerTV, stageTV;

    /*
     * Class variables
     */

    // Timer variables
    private Timer timer;
    private boolean timerRunning;

    // State variables
    private boolean started;
    private int dayIndex, weekIndex;
    private Day day;
    private Stage stage;

    // Used for the notification
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
         * Set up the notification channel on Android O and above
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Create the channel with low importance to avoid vibration or sounds
            int importance = NotificationManager.IMPORTANCE_LOW;
            CharSequence name = getString(R.string.channel_name);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(getString(R.string.channel_description));

            // Register the channel with the system
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        /*
         * Find the views from the layout.
         */
        timerProgressBar = findViewById(R.id.pb_timer);
        stageSeekBar = findViewById(R.id.sb_stage);
        plusButton = findViewById(R.id.bt_rewind);
        minusButton = findViewById(R.id.bt_ff);
        pauseButton = findViewById(R.id.bt_pause);
        completeButton = findViewById(R.id.bt_complete);
        startButton = findViewById(R.id.bt_start);
        weekSpinner = findViewById(R.id.week_spinner);
        daySpinner = findViewById(R.id.day_spinner);
        completeTV = findViewById(R.id.tv_complete);
        summaryTV = findViewById(R.id.tv_summary);
        timerTV = findViewById(R.id.tv_timer);
        stageTV = findViewById(R.id.tv_mode);

        // Setup buttons
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer.addMinute();
            }
        });
        plusButton.setEnabled(false);
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer.subtractMinute();
            }
        });
        minusButton.setEnabled(false);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePause();
            }
        });
        pauseButton.setEnabled(false);
        completeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleComplete();
            }
        });
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleStart();
            }
        });

        // setup progress bar with 0 progress and grey color
        timerProgressBar.setMax(PROGRESS_SUBDIVISIONS);
        timerProgressBar.setProgress(0);
        timerProgressBar.getProgressDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);

        // Set up seek bar, disabled so that the user cannot change it
        stageSeekBar.setEnabled(false);
        stageSeekBar.setProgress(0);

        // Setup the day and week number dropdowns to change the day and week when selected
        weekSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                // Change the week to the selected week
                weekIndex = i + 1;

                // Whenever the week is changed, change the day to day 1
                dayIndex = 1;
                daySpinner.setSelection(dayIndex - 1);

                // Set the day state variable to the new day
                setDay(weekIndex, dayIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        daySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                // Change the day to the selected day and update the day state variable to the new day
                dayIndex = i + 1;
                setDay(weekIndex, dayIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // setup timer with this as its listener
        timer = new Timer();
        timer.setTimerListener(this);

        // Setup the initial day to the most recent day that the app was set to
        SharedPreferences preferences = getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE);
        weekIndex = preferences.getInt(getString(R.string.pref_week), 1);
        dayIndex = preferences.getInt(getString(R.string.pref_day), 2);
        weekSpinner.setSelection(weekIndex - 1);
        daySpinner.setSelection(dayIndex - 1);
        setDay(weekIndex, dayIndex);

        // When the app loads, no day should be running
        started = false;

        // setup a pending intent for the notification that will open the already-running app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    /**
     * Changes the day to the given week/day.
     *
     * @param week the week to select the day from.
     * @param day  the day to select.
     */
    private void setDay(int week, int day) {

        /*
         * If there was a day already running when the day was changed, stop the other day before
         * opening the new one.
         */
        if (started) {
            toggleStart();
        }

        /*
         * Update the shared preferences day and week items. This will save the most recent day and week
         * so that the next time the app opens, the same week and day will be opened.
         */
        SharedPreferences preferences = getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getString(R.string.pref_day), day);
        editor.putInt(getString(R.string.pref_week), week);
        editor.apply();

        // Update the day and week state variables.
        weekIndex = week;
        dayIndex = day;

        // Change the day and update the summary and the seekbar to reflect the new number of stages.
        this.day = new Day(week, day, this);
        summaryTV.setText(this.day.getDescription());
        stageSeekBar.setMax(this.day.numberOfStages() - 1);

        // Update the complete/incomplete button and textview to match the completedness of the new day.
        if (this.day.isComplete()) {
            completeTV.setText(R.string.complete);
            completeButton.setText(R.string.mark_incomplete);
        } else {
            completeTV.setText(R.string.incomplete);
            completeButton.setText(R.string.mark_complete);
        }

    }

    /**
     * Moves on to the next stage. If there are no more stages, ends the day and marks it complete.
     */
    private void increaseStage() {

        // Vibrate for 500 milliseconds (Different methods for newer OS levels)
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(500);
        }

        // If the current stage is the last stage of the day, then finish the day.
        if (day.getStageNumber() + 1 == day.numberOfStages()) {

            // Stop the day
            if (started) {
                toggleStart();
            }

            // If the day was incomplete, mark it completed.
            if (!day.isComplete()) {
                toggleComplete();
            }

        } else {

            // get the next stage
            stage = day.getNext();

            // Update the seek bar, stage textview, and the timer to the new stage
            stageSeekBar.setProgress(day.getStageNumber());
            timer.resetTo(stage.getLength());
            timerRunning = true;
            this.stageTV.setText(stage.getStageType().toString());

            // Set the progress bar to the appropriate color
            timerProgressBar.getProgressDrawable().setColorFilter(stage.getStageType().getColor(), PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * Moves to the previous stage.
     */
    private void decreaseStage() {

        // Get the previous stage
        stage = day.getPrevious();

        // Update the seek bar, stage textview, and the timer to the new stage
        stageSeekBar.setProgress(day.getStageNumber());
        timer.resetTo(stage.getLength());
        timerRunning = true;
        this.stageTV.setText(stage.getStageType().toString());

        // Set the progress bar to the appropriate color
        timerProgressBar.getProgressDrawable().setColorFilter(stage.getStageType().getColor(), PorterDuff.Mode.SRC_IN);
    }

    /**
     * Pauses or plays the timer, depending on the state of timerRunning. Updates the pauseButton's
     * text and the progress bar appropriately.
     */
    private void togglePause() {

        // Toggle the timer
        timerRunning = !timerRunning;
        timer.setRunning(timerRunning);

        // Update the pausebutton text and the color of the progress bar
        if (timerRunning) {
            pauseButton.setText(R.string.pause);
            timerProgressBar.getProgressDrawable().setColorFilter(stage.getStageType().getColor(), PorterDuff.Mode.SRC_IN);
        } else {
            pauseButton.setText(R.string.resume);
            timerProgressBar.getProgressDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        }

    }

    /**
     * Toggles the completeness of a day. Updates the completeButton and complete textview text appropriately.
     */
    private void toggleComplete() {

        if (day.isComplete()) {
            day.setComplete(false);
            completeButton.setText(R.string.mark_complete);
            completeTV.setText(R.string.incomplete);
        } else {
            day.setComplete(true);
            completeButton.setText(R.string.mark_incomplete);
            completeTV.setText(R.string.complete);
        }

    }

    /**
     * Starts or stops the current day, depending on the current state of 'started.'
     */
    private void toggleStart() {

        // Toggle started and reset the day
        started = !started;
        day.reset();

        if (started) {

            /*
             * If the day is now started, increase stage to get the first stage, then update the start
             * button to say stop, and enable the timer buttons.
             */
            increaseStage();
            startButton.setText(R.string.stop);
            pauseButton.setEnabled(true);
            plusButton.setEnabled(true);
            minusButton.setEnabled(true);
        } else {

            /*
             * If the day is now stopped, then update the start button to say start, and disable the
             * timer buttons. Stop the timer, reset the progress bar and cancel the notification.
             */
            startButton.setText(R.string.start);
            timer.setRunning(false);
            timerRunning = false;
            timerTV.setText(R.string.zero_time);
            pauseButton.setEnabled(false);
            plusButton.setEnabled(false);
            minusButton.setEnabled(false);

            // Animate the progress bar if the API level is high enough
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                this.timerProgressBar.setProgress(0, true);
            } else {
                this.timerProgressBar.setProgress(0);
            }

            timerProgressBar.getProgressDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancelAll();
        }
    }

    /**
     * Updates the notification (Or creates one if there is not yet a notification) to match the current
     * timer state and stage.
     * @param secondsRemaining
     */
    private void buildNotification(int secondsRemaining) {

        // Calculate the minutes and seconds remaining from the seconds remaining.
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;

        // Build the notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds))
                .setContentText(stage.getStageType().toString())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setShowWhen(false)
                .setContentIntent(pendingIntent);

        // Send the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @Override
    public void onTimerRestart() {
        decreaseStage();
    }

    @Override
    public void onTimerFinish() {
        increaseStage();
    }

    @Override
    public void onTimerTick(long millisLeft) {
        long minutes = millisLeft / 1000 / 60;
        long seconds = (millisLeft / 1000) % 60;

        // Update the timer text view to match the timer
        this.timerTV.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        // Animate the progress bar if the API level is high enough
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.timerProgressBar.setProgress((int) (PROGRESS_SUBDIVISIONS - PROGRESS_SUBDIVISIONS * (millisLeft / 1000) / stage.getLength()), true);
        } else {
            this.timerProgressBar.setProgress((int) (PROGRESS_SUBDIVISIONS - PROGRESS_SUBDIVISIONS * (millisLeft / 1000) / stage.getLength()));
        }

        // Update the notification.
        buildNotification((int) (millisLeft / 1000));
    }
}
