package server;

/**
 * Launches the GoNature Server application.
 *
 * <p>This class serves as a lightweight entry point that delegates
 * application startup to the {@link ServerUI} class.</p>
 *
 * @author Rahaf Mreh
 * @version 1.0
 * @since 1.0
 */
public class ServerLauncher {
	/**
	 * Starts the GoNature Server user interface.
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
        ServerUI.main(args);
    }
}
