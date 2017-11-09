/*
 * -----------------------------------------------------------------------------
 * Application     : WM 6
 * Revision        : $Revision:  $
 * Revision date   : $Date:  $
 * Last changed by : $Author:  $
 * URL             : $URL:  $
 *
 * -----------------------------------------------------------------------------
 * Copyright
 * This software module including the design and software principals used
 * is and remains the property of Swisslog and is submitted with the
 * understanding that it is not to be reproduced nor copied in whole or in
 * part, nor licensed or otherwise provided or communicated to any third
 * party without Swisslog's prior written consent.
 * It must not be used in any way detrimental to the interests of Swisslog.
 * Acceptance of this module will be construed as an agreement to the above.
 *
 * All rights of Swisslog remain reserved. Swisslog and WarehouseManager
 * are trademarks or registered trademarks of Swisslog. Other products
 * and company names mentioned herein may be trademarks or trade names of
 * their respective owners. Specifications are subject to change without
 * notice.
 * -----------------------------------------------------------------------------
 */
package constants;

/**
 * A bundle of constants.
 */
public interface ApplicationConstants {

    String LONG_SLASH_LINE = "------------------------------------------------------------";

    long MAX_ALLOWED_NUMBER_OF_MS_BETWEEN_SEND_AND_RECEIVE = 500;

    /**
     * Should be equal to the number of QUeues and MDBs defined ine the entrypoint.mdb package.
     */
    int NUMBER_OF_MDBS_TO_PUBLISH_MESSAGE_TO = 1;

    boolean MDB_EXECUTION_LOG_HISTORY_ENABLED = false;

    boolean PUMP_MESSAGE_TIMER_ENABLED = false;

    int NUMBER_OF_SOME_ENTITY_ENTITIES_TO_CREATE = 100;

    /**
     * When the system test awaits for the job by the back-end to be done, how long is it willing to wait to send a JMS
     * reply go into the work done reply queue?
     */
    long DEFAULT_WAIT_TIME_FOR_BACK_END_BUSINESS_LOGIC_TO_COMMIT_MS = 10 * 1000;
}
