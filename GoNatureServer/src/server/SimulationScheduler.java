package server;

import java.sql.Timestamp;
import java.util.List;
import common.ChatIF;
import common.Reservation;
import db.DatabaseController;

public class SimulationScheduler implements Runnable {

	//here in this class we have alot of static variables and methods so they 
	//can bee called from anywhere without creating an object
    private final DatabaseController db;
    private final ChatIF serverUI;
    private boolean running = false;
    private Thread thread;

    // Simulation Constants
    // 1 hour = 3 seconds
    // 24 hours (1 day) = 72 seconds (1.2 minutes)
    // 2 hours = 6 seconds
    // 1 hour = 3 seconds
    private static final long SIM_DAY_MS = 72 * 1000L;            // 1.2 minutes in real time
    private static final long SIM_TWO_HOURS_MS = 6 * 1000L;       // 6 seconds in real time
    private static final long SIM_ONE_HOUR_MS = 3 * 1000L;        // 3 seconds in real time
    private static final long SIM_STAY_DURATION_MS = 4 * 3 * 1000L; // 4 hours = 12 seconds

    // Simulated Clock Fields
    private static long startTimeMs = 0;
    private static double speedup = 1200.0;

    public static long getStartTimeMs() { return startTimeMs; }
    public static double getSpeedup() { return speedup; }

    public static long getSimulatedTime() {
        if (startTimeMs == 0) return System.currentTimeMillis();
        long elapsedReal = System.currentTimeMillis() - startTimeMs;
        return startTimeMs + (long)(elapsedReal * speedup);
    }

    public static Timestamp getSimulatedTimestamp() {
        return new Timestamp(getSimulatedTime());
    }

    public SimulationScheduler(DatabaseController db, ChatIF serverUI) {
        this.db = db;
        this.serverUI = serverUI;
    }

    public synchronized void start() {
        if (!running) {
            running = true;
            startTimeMs = System.currentTimeMillis();
            speedup = 3600.0 / (SIM_ONE_HOUR_MS / 1000.0);
            thread = new Thread(this, "SimulationScheduler");
            thread.setDaemon(true);//setDaemon(true) means this thread won't keep the JVM alive on its own  when the server shuts down,
            //the daemon thread dies with it rather than hanging the process
            thread.start();// begins running run() on a separate thread
            log("Simulation Scheduler started (1 hour = 3 seconds).");
        }
    }

    public synchronized void stop() {
        if (running) {
            running = false;
            if (thread != null) {
                thread.interrupt();
            }
            log("Simulation Scheduler stopped.");
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Sleep for 2 seconds between cycles
                Thread.sleep(2000);
                
                // Process scheduler tasks
                processReminders();
                processConfirmations();
                processNoShows();
                processExits();
                
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.out.println("Error in SimulationScheduler loop: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void processReminders() {
        List<Reservation> activeRes = db.getPendingTimerReservations();
        long simNow = getSimulatedTime();

        for (Reservation r : activeRes) {
            if ("CONFIRMED".equals(r.getStatus()) && r.getReminderSentTime() == null) {
                long visitTime = r.getVisitDateTime().getTime();
                
                // If the visit is within 24 simulation hours (1 day)
                if (visitTime - simNow > 0 && visitTime - simNow <= 24 * 60 * 60 * 1000L) {
                    db.setReminderSent(r.getReservationId(), new Timestamp(simNow));
                    db.updateReservationStatus(r.getReservationId(), "PENDING_CONFIRMATION", new Timestamp(simNow));
                    
                    String alertMsg = "\n=== SIMULATION ALERT ===\n" +
                                      "Type: SMS & Email Reminder\n" +
                                      "To: " + r.getEmail() + " | Phone: " + r.getPhoneNumber() + "\n" +
                                      "Message: Hello! This is a reminder for your upcoming visit to " + r.getParkName() + "\n" +
                                      "Scheduled for: " + r.getVisitDateTime() + "\n" +
                                      "Please confirm your reservation within 6 seconds (2 simulation hours),\n" +
                                      "otherwise it will be automatically cancelled.\n" +
                                      "========================\n";
                    log(alertMsg);
                }
            }
        }
    }

    private void processConfirmations() {
        List<Reservation> activeRes = db.getPendingTimerReservations();
        long simNow = getSimulatedTime();

        for (Reservation r : activeRes) {
            if ("PENDING_CONFIRMATION".equals(r.getStatus())) {
                // Case A: Promoted from waiting list (has spotPromotedTime)
                if (r.getSpotPromotedTime() != null) {
                    long promotedTime = r.getSpotPromotedTime().getTime();
                    if (simNow - promotedTime >= 1 * 60 * 60 * 1000L) { // 1 simulated hour
                        db.updateReservationStatus(r.getReservationId(), "CANCELLED", new Timestamp(simNow));
                        log("Reservation #" + r.getReservationId() + " (Waiting List Promoted) AUTO-CANCELLED: No confirmation within 3 seconds (1 hour).");
                        
                        // Promote next in line for this park and slot
                        promoteNextWaiting(r.getParkId(), r.getVisitDateTime());
                    }
                } 
                // Case B: Normal reservation reminder sent (has reminderSentTime)
                else if (r.getReminderSentTime() != null) {
                    long reminderTime = r.getReminderSentTime().getTime();
                    if (simNow - reminderTime >= 2 * 60 * 60 * 1000L) { // 2 simulated hours
                        db.updateReservationStatus(r.getReservationId(), "CANCELLED", new Timestamp(simNow));
                        log("Reservation #" + r.getReservationId() + " AUTO-CANCELLED: No confirmation within 6 seconds (2 hours).");
                        
                        // Promote next in line since space opened up
                        promoteNextWaiting(r.getParkId(), r.getVisitDateTime());
                    }
                }
            }
        }
    }

    private void processNoShows() {
        List<Reservation> activeRes = db.getPendingTimerReservations();
        long simNow = getSimulatedTime();

        for (Reservation r : activeRes) {
            if ("CONFIRMED".equals(r.getStatus()) || "PENDING_CONFIRMATION".equals(r.getStatus())) {
                long visitTime = r.getVisitDateTime().getTime();
                
                // If they are more than 12 simulation hours late (36 seconds)
                if (simNow - visitTime >= 12 * 60 * 60 * 1000L) {
                    db.flagNoShow(r.getReservationId(), new Timestamp(simNow));
                    log("Reservation #" + r.getReservationId() + " marked as NO_SHOW (Auto-Cancelled).");
                    
                    // Promote next in line
                    promoteNextWaiting(r.getParkId(), r.getVisitDateTime());
                }
            }
        }
    }

    public void promoteNextWaiting(int parkId, Timestamp visitTime) {
        List<Reservation> waiting = db.getFirstInWaitingList(parkId, visitTime);
        if (waiting.isEmpty()) return;

        // Check if there is capacity available now to promote
        for (Reservation candidate : waiting) {
            if (db.checkCapacityAvailable(parkId, visitTime, candidate.getNumberOfVisitors())) {
                long simNow = getSimulatedTime();
                db.updateReservationStatus(candidate.getReservationId(), "PENDING_CONFIRMATION", new Timestamp(simNow));
                db.setSpotPromoted(candidate.getReservationId(), new Timestamp(simNow));
                //it promotes the first person whose group size still fits the freed space
                String alertMsg = "\n=== SIMULATION ALERT ===\n" +
                                  "Type: SMS & Email Promotion (Waiting List)\n" +
                                  "To: " + candidate.getEmail() + " | Phone: " + candidate.getPhoneNumber() + "\n" +
                                  "Message: Space is now available for your visit to " + candidate.getParkName() + "!\n" +
                                  "Scheduled for: " + candidate.getVisitDateTime() + "\n" +
                                  "Please confirm your reservation within 3 seconds (1 simulation hour),\n" +
                                  "otherwise the spot will pass to the next visitor in queue.\n" +
                                  "========================\n";
                log(alertMsg);
                break; // Only promote one that fits
            }
        }
    }

    private void processExits() {
        List<Reservation> activeRes = db.getPendingTimerReservations();
        long simNow = getSimulatedTime();

        for (Reservation r : activeRes) {
            if ("ACTIVE".equals(r.getStatus())) {
                Timestamp entryTime = r.getActualEntryTime();
                if (entryTime != null) {
                    common.Park p = db.getPark(r.getParkId());
                    if (p != null) {
                        long stayDurationMs = p.getStayDuration() * 60L * 60 * 1000L;
                        if (simNow - entryTime.getTime() >= stayDurationMs) {
                            db.updateReservationStatus(r.getReservationId(), "COMPLETED", new Timestamp(simNow));
                            
                            int parkId = r.getParkId();
                            int occupancy = db.getParkCurrentOccupancy(parkId);
                            db.logOccupancyChange(parkId, occupancy, new Timestamp(simNow));
                            
                            log("Reservation #" + r.getReservationId() + " (Visitor Exit) AUTO-COMPLETED: Stay duration of " + p.getStayDuration() + " hours expired.");
                            
                            // Promote next in waiting list since space freed
                            promoteNextWaiting(parkId, r.getVisitDateTime());
                        }
                    }
                }
            }
        }
    }

    private void log(String msg) {
        if (serverUI != null) {
            serverUI.display(msg);
        } else {
            System.out.println(msg);
        }
    }
}
