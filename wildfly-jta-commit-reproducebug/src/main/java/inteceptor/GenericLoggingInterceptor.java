package inteceptor;

import java.io.Serializable;
import java.util.Date;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * <p>
 * This interceptor base class is used around facades. Around each method invocation log out the method call, its
 * arguments, duration, return value or exception message.
 * </p>
 * <ul>
 * <li>It logs the parameters, return value, exception and execution time.</li>
 * </ul>
 *
 * <P>
 * The exception translation feature is particularly useful when the ejb being intercepted is prone to blow up due to,
 * for example, javax.validation.ConstraintViolationException which normally do not inform what particular constraint is
 * violated. The translation mechanism is able to discover the specific JPA validation that failed.
 *
 *
 */
public abstract class GenericLoggingInterceptor implements Serializable {
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static long countOfInterceptions = 0;

    /**
     * The log context. Usually the log appender (handler) creates a log file per context. (e.g Facade, MdB)
     * <P>
     * For example, in the glassfish log folder a file named "whatever-MdB.log" will be created when context is MdB.
     */
    protected abstract String getLogContext();

    /**
     * The intercepting method. It assigns a log context, adds log messages around the call and translates exceptions.
     *
     * @param invocation
     *            The invocation context within the method is called
     * @throws Exception
     *             InvocationContext.proceed() throws Exception
     */
    @AroundInvoke
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public Object aroundInvoke(InvocationContext invocation) throws Exception {
        incrementCountOfInterpcetions();
        Logger logger = LoggerFactory.getLogger(invocation.getTarget().getClass());

        // get more information for the trace message
        String clazzName = invocation.getTarget().getClass().getSimpleName();
        String methodName = invocation.getMethod().getName();
        Object[] parameters = invocation.getParameters();

        Exception exception = null;
        Object returnValue = null;
        String entryPoint = clazzName + "." + methodName;
        MDC.put("LogContext", entryPoint);
        long startTime = System.currentTimeMillis();
        try {
            writeBeginTrace(logger, entryPoint, parameters, true);
            returnValue = invocation.proceed();
        } catch (Exception e) {
            exception = e;
        } finally {
            writeEndTrace(logger, invocation, entryPoint, returnValue, exception, startTime, true);

            MDC.remove("LogContext");
        }

        //
        if (exception != null) {
            throw new RuntimeException("Something blew up in method being intercepted. ", exception);
        } else {
            return returnValue;
        }
    }

    /**
     * Write a start transaction trace.
     */
    private void writeBeginTrace(Logger logger, String entryPoint, Object[] parameters, boolean tracked) {
        logger.debug(String.format("------- >: STARTED %1$s :   ", entryPoint));
    }

    /**
     * Write a trace message describing how long the transaction intercepted took to execute.
     */
    private void writeEndTrace(Logger logger, InvocationContext invocation, String entryPoint, Object returnValue,
            Exception exception, long startTime, boolean tracked) {
        if (exception != null) {
            logger.error("Exception that in real life would have been transalated was: " + exception.getMessage(),
                    exception);
        }
        long numberOfMsToExecuteTransaction = (new Date()).getTime() - startTime;
        logger.debug(
                String.format("<------- : FINISHED %1$s in %2$s (ms)", entryPoint, numberOfMsToExecuteTransaction));
    }

    private static synchronized void incrementCountOfInterpcetions() {
        countOfInterceptions++;
    }

}
