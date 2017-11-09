
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import facade.SendJmsMessageFacadeLocal;

@RequestScoped
@Named(value = "sendJmsMessageRequestBean")
public class SendJmsMessageRequestBean {
    private static final Logger LOGGER = Logger.getLogger(SendJmsMessageRequestBean.class.getCanonicalName());
    private static final String CLIENT_ID_GLOBAL = null;

    // ///////////////////////////////////////////////
    // BEGIN: STATE
    // ///////////////////////////////////////////////
    private String message = "DEFAULT-JMS-MESSAGE";

    @Inject
    private transient SendJmsMessageFacadeLocal sendJmsMessageFacade;

    @Inject
    private transient FacesContext facesContext;

    // ///////////////////////////////////////////////
    // BEGIN: ACTION
    // ///////////////////////////////////////////////

    /** form submission */
    public void execute() {
        try {
            // JMS transaction to put a message on a queue
            // sendJmsMessageFacade.sendJmsMessage(message);

            // just some trivial info message to the screen
            facesContext.addMessage(CLIENT_ID_GLOBAL,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Message Pumped OK.", null));
        } catch (Exception e) {
            facesContext.addMessage(CLIENT_ID_GLOBAL,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), e.getStackTrace().toString()));
        }
    }

    // ///////////////////////////////////////////////
    // BEGIN: BOILER PLATE CODE
    // ///////////////////////////////////////////////
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
