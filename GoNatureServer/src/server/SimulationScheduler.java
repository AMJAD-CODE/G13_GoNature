package server;

import java.sql.Timestamp;
import java.util.List;
import common.ChatIF;
import common.Reservation;
import db.DatabaseController;

/**
 * Manages all simulation-based tasks in the GoNature system.
 *
 * <p>This scheduler runs in a background thread and simulates the
 * passage of time at an accelerated rate. It automatically processes
 * reservation reminders, confirmation deadlines, waiting-list
 * promotions, no-show visitors, and visitor exits.</p>
 *
 * <p>The scheduler communicates with the database to update
 * reservation statuses and uses the server user interface to
 * display simulation events and notifications.</p>
 *
 * <p>Simulation timing is accelerated so that hours and days
 * pass within seconds, allowing system behavior to be tested
 * without waiting for real-time events.</p>
 *
 * @author Rahaf Mreh
 * @version 1.0
 * @since 1.0
 */
public class SimulationScheduler implements Runnable  {

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

	/**
	 * Returns the simulation start time.
	 *
	 * @return the simulation start time in milliseconds
	 */
	public static long getStartTimeMs() {
		return startTimeMs;
	}

	/**
	 * Returns the simulation speed multiplier.
	 *
	 * @return the simulation speedup factor
	 */
	public static double getSpeedup() { 
		return speedup; 
	}

	/**
	 * Calculates the current simulated time based on the
	 * simulation speed multiplier.
	 *
	 * @return the current simulated time in milliseconds
	 */
	public static long getSimulatedTime(){
		if (startTimeMs == 0) return System.currentTimeMillis();
		long elapsedReal = System.currentTimeMillis() - startTimeMs;
		return startTimeMs + (long)(elapsedReal * speedup);
	}

	/**
	 * Returns the current simulated time as a timestamp.
	 *
	 * @return a timestamp representing the current simulated time
	 */
	public static Timestamp getSimulatedTimestamp() {
		return new Timestamp(getSimulatedTime());
	}

	/**
	 * Creates a new simulation scheduler.
	 *
	 * @param db the database controller used to access and update data
	 * @param serverUI the server user interface used for displaying
	 *                 simulation messages and alerts
	 */
	public SimulationScheduler(DatabaseController db, ChatIF serverUI) {
		this.db = db;
		this.serverUI = serverUI;
	}

	/**
	 * Starts the simulation scheduler thread.
	 *
	 * <p>The simulation clock is initialized and a background
	 * daemon thread is created to process scheduled tasks.</p>
	 */
	public synchronized void start(){
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

	/**
	 * Stops the simulation scheduler thread.
	 *
	 * <p>The scheduler loop is terminated and the running
	 * thread is interrupted.</p>
	 */
	public synchronized void stop(){
		if (running) {
			running = false;
			if (thread != null) {
				thread.interrupt();
			}
			log("Simulation Scheduler stopped.");
		}
	}

	/**
	 * Executes the scheduler loop.
	 *
	 * <p>Periodically checks reservations and processes
	 * reminders, confirmations, no-shows, and visitor exits
	 * while the scheduler is running.</p>
	 */
	@Override
	public void run(){
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

	/**
	 * Sends reminders for upcoming visits and changes eligible
	 * reservations to pending confirmation status.
	 *
	 * <p>Visitors are notified when their reservation is within
	 * 24 simulated hours of the scheduled visit time.</p>
	 */
	private void processReminders(){
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

	/**
	 * Processes confirmation deadlines for reservations.
	 *
	 * <p>Reservations that are not confirmed within the allowed
	 * time period are automatically cancelled and their places
	 * may be offered to visitors on the waiting list.</p>
	 */
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

	/**
	 * Detects visitors who did not arrive for their reservation.
	 *
	 * <p>Reservations that exceed the no-show threshold are
	 * automatically cancelled and marked as no-shows.</p>
	 */
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

	/**
	 * Promotes the next eligible reservation from the waiting list.
	 *
	 * <p>If capacity becomes available, the first waiting-list
	 * reservation that fits the available space is offered the
	 * newly available spot and moved to pending confirmation.</p>
	 *
	 * @param parkId the park identifier
	 * @param visitTime the visit date and time of the reservation slot
	 */
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

	/**
	 * Automatically completes active visits whose allowed stay
	 * duration has expired.
	 *
	 * <p>When a visit is completed, park occupancy is updated and
	 * waiting-list visitors may be promoted.</p>
	 */
	private void processExits(){
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

	/**
	 * Displays a scheduler message through the server user interface.
	 *
	 * <p>If no user interface is available, the message is printed
	 * to the console.</p>
	 *
	 * @param msg the message to display
	 */
	private void log(String msg){
		if (serverUI != null) {
			serverUI.display(msg);
		} else {
			System.out.println(msg);
		}
	}
}
