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
package messages;

import java.io.Serializable;
import java.util.Date;

/**
 * Just a simple message to bet set as payload of a JMS object message.
 */
public class DoWorkObjectMessage implements Serializable {
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    final String messageText;

    final Date messageSendDate = new Date();

    /**
     * Create a new SimpleObjectMessage.
     *
     * @param messageText
     *            the message text
     */
    public DoWorkObjectMessage(String messageText) {
        super();
        this.messageText = messageText;
    }

    public String getMessageText() {
        return messageText;
    }

    public Date getMessageSendDate() {
        return messageSendDate;
    }

}
