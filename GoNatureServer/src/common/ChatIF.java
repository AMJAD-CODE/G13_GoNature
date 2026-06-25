package common;

/**
 * Defines a simple interface for displaying messages.
 *
 * <p>Classes implementing this interface provide a mechanism
 * for presenting messages to users, such as through a graphical
 * user interface or a console window.</p>
 *
 * @author Rahaf Mreh
 * @version 1.0
 * @since 1.0
 */
public interface ChatIF {

    /**
     * Displays the specified message.
     *
     * @param message the message object to display
     */
    void display(Object message);
}

