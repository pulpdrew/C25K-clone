package com.pulpdrew.c25k;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Class representing a single day in teh C25K program, which includes stages for
 * each stage of the day. Reads in data for each day from the assets folder.
 */
class Day {

    /*
     * Member variables
     */
    private ArrayList<Stage> stages;
    private int numStages;
    private int currentStage;

    private int week, day;
    private boolean complete;
    private String description;

    private Context context;

    /**
     * Constructor to create a new Day object.
     *
     * @param week the week to which the day belongs.
     * @param day the day of the given week.
     * @param context the app's context.
     */
    Day(int week, int day, Context context) {

        this.context = context;
        this.week = week;
        this.day = day;

        readInDay(week, day);
        numStages = stages.size();
        currentStage = -1;

    }

    /**
     * Read in the day from a file and update the instance variables to
     * match the data that is read in.
     */
    private void readInDay(int week, int day) {

        // Read in the completeness of the day from shared preferences. This is persistent data.
        complete = context.getSharedPreferences(context.getString(R.string.pref_key), Context.MODE_PRIVATE)
                .getBoolean(context.getString(R.string.pref_complete_prefix) + week + "_" + day, false);

        // Initialize stages as an empty ArrayList
        stages = new ArrayList<>();

        // The data for the day will be in a file with named "[week]_[day].c25k"
        String filename = week + "_" + day + context.getString(R.string.file_extension);

        // Open up the file
        try (DataInputStream fileIn = new DataInputStream(context.getAssets().open(filename))) {

            Scanner scanner = new Scanner(fileIn);

            // Read the description from the first line of the file.
            description = scanner.nextLine();

            /*
             * Read each line of the file to get the stages in the day
             * Each line will be in the format [StageType] [length in seconds]
             */
            while (scanner.hasNext()) {

                // Split the line at the space
                String[] line = scanner.nextLine().split(" ");

                // Update the length in seconds
                int length = Integer.parseInt(line[1]);

                // Match the stage type to a StageType
                Stage.StageType type;
                switch (line[0]) {
                    case "WARMUP":
                        type = Stage.StageType.WARM_UP;
                        break;
                    case "COOLDOWN":
                        type = Stage.StageType.COOL_DOWN;
                        break;
                    case "RUN":
                        type = Stage.StageType.RUN;
                        break;
                    case "WALK":
                        type = Stage.StageType.WALK;
                        break;
                    default:
                        type = Stage.StageType.WALK;
                }

                // Add the stage given by the line to the list of stages in the day.
                stages.add(new Stage(type, length));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Return the next stage of the day, or the last stage if the day is
     * already on the last stage.
     */
    Stage getNext() {
        currentStage = Math.min(numStages - 1, currentStage + 1);
        return stages.get(currentStage);
    }

    /**
     * Returns the previous stage of the day, or the first stage if the
     * day is already at the beginning.
     */
    Stage getPrevious() {
        currentStage = Math.max(0, currentStage - 1);
        return stages.get(currentStage);
    }

    int numberOfStages() {
        return numStages;
    }

    /**
     * Returns the index of the stage that is currently represented by the day instance.
     */
    int getStageNumber() {
        return currentStage;
    }

    String getDescription() {
        return description;
    }

    boolean isComplete() {
        return complete;
    }

    /**
     * Sets the day's completedness to the value of complete.
     */
    void setComplete(boolean complete) {
        this.complete = complete;

        // Update shared preferences to reflect the updated completedness, so that the data is saved.
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.pref_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(context.getString(R.string.pref_complete_prefix) + week + "_" + day, complete);
        editor.apply();
    }

    void reset() {
        this.currentStage = -1;
    }
}
