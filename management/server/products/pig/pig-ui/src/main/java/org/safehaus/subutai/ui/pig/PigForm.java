/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.ui.pig;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Runo;
import org.safehaus.subutai.ui.pig.manager.Manager;
import org.safehaus.subutai.ui.pig.wizard.Wizard;

/**
 *
 * @author dilshat
 */
public class PigForm extends CustomComponent {

    public PigForm() {
        setSizeFull();
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSpacing(true);
        verticalLayout.setSizeFull();
        TabSheet mongoSheet = new TabSheet();
        mongoSheet.setStyleName(Runo.TABSHEET_SMALL);
        mongoSheet.setSizeFull();
        Manager manager = new Manager();
        Wizard wizard = new Wizard();
        mongoSheet.addTab(wizard.getContent(), "Install");
        mongoSheet.addTab(manager.getContent(), "Manage");
        verticalLayout.addComponent(mongoSheet);
        setCompositionRoot(verticalLayout);
        manager.refreshClustersInfo();
    }

}
