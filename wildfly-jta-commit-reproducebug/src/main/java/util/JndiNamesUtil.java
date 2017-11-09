package util;

/**
 * Static utility class.
 *
 * Create strings that can be used in JNDI context lookups.
 *
 */
public final class JndiNamesUtil {

    /**
     * static util class has no constructor.
     */
    private JndiNamesUtil() {
    }

    /**
     * Produce a string of the form "queue/ReproduceBug052Queue"
     *
     * <P>
     * NOTE: <br>
     * Currently in this sample application we use a single queue So we do not need a dynamic queue name. The only Mdb
     * we are using is the: <br>
     * "ReproduceBugMdb".
     *
     * @param queueNumber
     *            a number from 1 to 52 in this sample application
     * @return The process queue name that will trigger some business logic if a message is put on the queue.
     */

    public static String getJndiQueueNameForReproduceBugQueue(int queueNumber) {
        // return String.format("queue/ReproduceBug%1$03dQueue", queueNumber);
        return "queue/ReproduceBugQueue";
    }

    /**
     * Produce a string of the form "queue/DefaultWorkDoneQueue"
     *
     * @return the queue name where we expect MdBs to report back stating that they are done with their work.
     */
    public static String getJndiQueueNameForWorkDoneMessage() {
        return "queue/DefaultWorkDoneQueue";
    }

}
