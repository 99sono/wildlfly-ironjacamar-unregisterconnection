
import java.io.Serializable;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.deltaspike.core.api.scope.ViewAccessScoped;
import org.apache.deltaspike.core.spi.scope.window.WindowContext;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author b7godin
 */
@Named("cr100")
@ViewAccessScoped
public class ViewAccessedBean implements Serializable {

    String pageId = "cr100";

    String selectedTab = "default";

    String backViewId = "index.xhtml?faces-redirect=true";

    String nextView = "pageNavigatedFromIndex.xhtml?faces-redirect=true";

    @Inject
    WindowContext windowContext;

    public String back() {
        return backViewId;
    }

    public String nextView() {
        return nextView;
    }

    /**
     * Listener method when user selects a different tab.
     */
    public void selectedTabChanged() {
        System.out.println("Select tab changed" + selectedTab);
    }

    public String getSelectedTab() {
        return selectedTab;
    }

    public void setSelectedTab(String selectedTab) {
        this.selectedTab = selectedTab;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public void refresh() {
        selectedTab = "default";
    }

    public String getWindowId() {
        return windowContext.getCurrentWindowId();
    }

}
