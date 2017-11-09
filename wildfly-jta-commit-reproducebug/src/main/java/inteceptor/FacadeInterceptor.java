package inteceptor;

import java.io.Serializable;

/**
 * An implementation of the abstract interceptor that is causing problems in weblogic.
 *
 */
public class FacadeInterceptor extends GenericLoggingInterceptor implements Serializable {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** {@inheritDoc} */
    @Override
    protected String getLogContext() {
        return "Facade";
    }

}
