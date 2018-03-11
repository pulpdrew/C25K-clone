package com.pulpdrew.c25k;

import android.os.CountDownTimer;

/**
 * Class representing a timer with second-accuracy. Accepts a listener that
 * it will update when the timer is reset to the beginning of its time,
 * runs out, or changes by a second.
 */
public class Timer {

    /*
     * Instance and state variables
     */
    private CountDownTimer cdt;
    private int duration, remaining;
    private boolean isRunning;
    private TimerListener listener;

    /**
     * Creates a new timer that is not running and has duration 0 seconds
     */
    public Timer() {
        isRunning = false;
        duration = 0;
        remaining = 0;
    }

    /**
     * Adds a minute to the timer, unless the timer is closer
     * than a minute to its duration, in which
     * case the timer is just set to its full duration.
     */
    public void addMinute() {

        // Stop the timer
        cdt.cancel();

        // add a minute to cdt
        remaining += 60;
        cdt = getCDT(remaining);

        // notify listeners if cdt is above original duration
        if (remaining > duration) {
            notifyRestartListener();
        }

        // start the timer if it was running before
        if (isRunning) {
            cdt.start();
        }

        // tick so that the updated time is sent to listeners
        tick(remaining * 1000);

    }

    /**
     * Subtracts a minute from the timer. If the timer
     * has less than a minute left, then just end the timer.
     */
    public void subtractMinute() {

        // Stop the timer
        cdt.cancel();

        // subtract a minute to cdt
        remaining -= 60;

        // If the timer has less than 0 seconds left, end the timer.
        if (remaining <= 0) {
            remaining = 0;
            finish();
        } else {

            // Update the countdown timer to match the new remaining time.
            cdt = getCDT(remaining);

            // start the timer if it was running before
            if (isRunning) {
                cdt.start();
            }
        }

        // tick so that the updated time is sent to listeners
        tick(remaining * 1000);

    }

    /**
     * Pauses or plays the timer depending on the value of run.
     */
    public void setRunning(boolean run) {

        isRunning = run;

        if (run) {
            // Get a new countdown timer with the appropriate remaining time
            cdt = getCDT(remaining);
            cdt.start();
        } else {

            // Stop the current countdown timer.
            cdt.cancel();
        }

    }

    /**
     * Resets the timer to the given time. The timer will start running regardless of
     * the current value of isRunning.
     */
    public void resetTo(int seconds) {
        if (cdt != null) {
            cdt.cancel();
        }
        cdt = getCDT(seconds);
        cdt.start();
        duration = seconds;
        isRunning = true;
    }

    /*
     * Accessor methods
     */

    public int getDuration() {
        return duration;
    }

    public int getRemainingTime() {
        return remaining;
    }

    /*
     * Utility methods
     */
    private CountDownTimer getCDT(int seconds) {

        CountDownTimer CDT = new CountDownTimer(seconds * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                tick(millisUntilFinished);
            }

            public void onFinish() {
                finish();
            }
        };
        return CDT;
    }

    private void finish() {
        this.isRunning = false;
        notifyFinishListener();
    }

    private void tick(long millisecondsLeft) {
        notifyTickListener((int) (millisecondsLeft / 1000));
        this.remaining = (int) (millisecondsLeft / 1000);
    }

    /*
     * Methods for listeners
     */

    public void setTimerListener(TimerListener listener) {

        this.listener = listener;

    }

    private void notifyTickListener(int seconds) {
        if (listener != null) {
            this.listener.onTimerTick(seconds);
        }
    }

    private void notifyFinishListener() {
        if (listener != null) {
            this.listener.onTimerFinish();
        }
    }

    private void notifyRestartListener() {
        if (listener != null) {
            this.listener.onTimerRestart();
        }
    }

    public interface TimerListener {
        void onTimerRestart();

        void onTimerFinish();

        void onTimerTick(int secondsRemaining);
    }

}
