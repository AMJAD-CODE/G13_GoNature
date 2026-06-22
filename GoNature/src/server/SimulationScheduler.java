package server;

import java.sql.Timestamp;

import common.ChatIF;
import db.DatabaseController;

public class SimulationScheduler {

    private static final long startTimeMs =
            System.currentTimeMillis();

    private static final double speedup = 1.0;

    private DatabaseController db;
    private ChatIF ui;

    public SimulationScheduler(
            DatabaseController db,
            ChatIF ui) {

        this.db = db;
        this.ui = ui;
    }

    public void start() {

        ui.display(
            "SimulationScheduler started");
    }

    public void stop() {

        ui.display(
            "SimulationScheduler stopped");
    }

    public void promoteNextWaiting(
            int parkId,
            Timestamp visitDateTime) {

        ui.display(
            "Checking waiting list for park "
            + parkId);
    }

    public static Timestamp getSimulatedTimestamp() {

        return new Timestamp(
                System.currentTimeMillis());
    }

    public static long getStartTimeMs() {

        return startTimeMs;
    }

    public static double getSpeedup() {

        return speedup;
    }
}