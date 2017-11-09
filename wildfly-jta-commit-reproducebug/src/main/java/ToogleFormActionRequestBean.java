import java.util.logging.Logger;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

@ManagedBean(name = "toggleFromRequesttBean", eager = true)
@RequestScoped
public class ToogleFormActionRequestBean {
    private static final Logger LOGGER = Logger.getLogger(ToogleFormActionRequestBean.class.getCanonicalName());
    private static final String P_SELECT_ONE_DEFAULT_VALUE = "on";

    // ///////////////////////////////////////////////
    // BEGIN: STATE
    // ///////////////////////////////////////////////
    private String selectValue = P_SELECT_ONE_DEFAULT_VALUE;

    private String executeOutput = null;

    // ///////////////////////////////////////////////
    // BEGIN: ACTION
    // ///////////////////////////////////////////////

    /** form submission */
    public void execute() {
        LOGGER.info("The system is alive with select value: " + getSelectValue());
        setExecuteOutput(getSelectValue());
    }

    // ///////////////////////////////////////////////
    // BEGIN: BOILER PLATE
    // ///////////////////////////////////////////////
    public String getSelectValue() {
        return selectValue;
    }

    public void setSelectValue(String selectValue) {
        this.selectValue = selectValue;
    }

    public String getExecuteOutput() {
        return executeOutput;
    }

    public void setExecuteOutput(String executeOutput) {
        this.executeOutput = executeOutput;
    }

}
