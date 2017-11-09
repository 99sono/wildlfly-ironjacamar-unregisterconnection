
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Specializes;
import javax.faces.context.FacesContext;
import org.apache.deltaspike.jsf.spi.scope.window.DefaultClientWindowConfig;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author b7godin
 */
@Specializes
@ApplicationScoped
public class CodiJSFConfig extends DefaultClientWindowConfig {

    /**
     * tells delta spike to on page load to provide a temporary page do run javascript logic based on window id
     * management.
     *
     * @return
     */
    public ClientWindowRenderMode getClientWindowRenderMode(
            FacesContext facesContext) {
		// with each page load returns a dummy page that redirects to true content with the correct window id
        // this is necessary because with delta spike the browser checks the window.name against the windowid
        // and it does not allow for a given window or iframe to take more than one windowid during its life
        // each iframe has its own constant windowid
        // return ClientWindowRenderMode.CLIENTWINDOW;
        return ClientWindowRenderMode.LAZY;
    }

}
