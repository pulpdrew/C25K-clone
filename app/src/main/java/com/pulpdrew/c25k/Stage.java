package com.pulpdrew.c25k;

import android.graphics.Color;

/**
 * Class representing a single stage in the day, such as
 * RUN FOR 2 MINUTES, or COOLDOWN FOR 5 MINUTES.
 */
class Stage {

    /**
     * Enum representing the possible types of stages and the colors and
     * string values associated with them.
     */
    public static enum StageType {

        WARM_UP("Warm Up", Color.BLUE),
        COOL_DOWN("Cool down", Color.BLUE),
        RUN("Run", Color.RED),
        WALK("Walk", Color.GREEN);

        private String name;
        private int color;
        StageType(String name, int color) {
            this.name = name;
            this.color = color;
        }

        public String toString() {
            return name;
        }

        public int getColor() {
            return color;
        }
    }

    private int length;
    private StageType stageType;

    Stage(StageType stageType, int length) {
        this.length = length;
        this.stageType = stageType;
    }

    /**
     * Returns the length of the stage, in seconds.
     */
    int getLength() {
        return length;
    }

    StageType getStageType() {
        return stageType;
    }

}
