/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.plugin.cassandra.ui.manager;


import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.naming.NamingException;

import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.util.ServiceLocator;
import org.safehaus.subutai.core.agent.api.AgentManager;
import org.safehaus.subutai.core.command.api.CommandRunner;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.cassandra.api.Cassandra;
import org.safehaus.subutai.plugin.cassandra.api.CassandraClusterConfig;
import org.safehaus.subutai.server.ui.component.ConfirmationDialog;
import org.safehaus.subutai.server.ui.component.ProgressWindow;
import org.safehaus.subutai.server.ui.component.TerminalWindow;

import com.google.common.collect.Sets;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.Sizeable;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.Window;


public class Manager
{
    protected final static String AVAILABLE_OPERATIONS_COLUMN_CAPTION = "AVAILABLE_OPERATIONS";
    protected final static String REFRESH_CLUSTERS_CAPTION = "Refresh Clusters";
    protected final static String CHECK_ALL_BUTTON_CAPTION = "Check All";
    protected final static String CHECK_BUTTON_CAPTION = "Check";
    protected final static String START_ALL_BUTTON_CAPTION = "Start All";
    protected final static String START_BUTTON_CAPTION = "Start";
    protected final static String STOP_ALL_BUTTON_CAPTION = "Stop All";
    protected final static String STOP_BUTTON_CAPTION = "Stop";
    protected final static String DESTROY_CLUSTER_BUTTON_CAPTION = "Destroy Cluster";
    protected final static String HOST_COLUMN_CAPTION = "Host";
    protected final static String IP_COLUMN_CAPTION = "IP List";
    protected final static String NODE_ROLE_COLUMN_CAPTION = "Node Role";
    protected final static String STATUS_COLUMN_CAPTION = "Status";
    protected final static String BUTTON_STYLE_NAME = "default";

    private static final Embedded PROGRESS_ICON = new Embedded( "", new ThemeResource( "img/spinner.gif" ) );
    private static final String MESSAGE = "No cluster is installed !";
    final Button refreshClustersBtn, startAllBtn, stopAllBtn, checkAllBtn, destroyClusterBtn;
    private final ExecutorService executorService;
    private final Tracker tracker;
    private final AgentManager agentManager;
    private final Cassandra cassandra;
    private final CommandRunner commandRunner;
    private final Table nodesTable;
    private GridLayout contentRoot;
    private ComboBox clusterCombo;
    private CassandraClusterConfig config;


    public Manager( final ExecutorService executorService, ServiceLocator serviceLocator ) throws NamingException
    {

        this.cassandra = serviceLocator.getService( Cassandra.class );
        this.executorService = executorService;
        this.tracker = serviceLocator.getService( Tracker.class );
        this.agentManager = serviceLocator.getService( AgentManager.class );
        this.commandRunner = serviceLocator.getService( CommandRunner.class );

        contentRoot = new GridLayout();
        contentRoot.setSpacing( true );
        contentRoot.setMargin( true );
        contentRoot.setSizeFull();
        contentRoot.setRows( 10 );
        contentRoot.setColumns( 1 );

        //tables go here
        nodesTable = createTableTemplate( "Cluster nodes" );

        HorizontalLayout controlsContent = new HorizontalLayout();
        controlsContent.setSpacing( true );

        controlsContent = new HorizontalLayout();
        controlsContent.setSpacing( true );
        controlsContent.setHeight( 100, Sizeable.Unit.PERCENTAGE );


        Label clusterNameLabel = new Label( "Select the cluster" );
        controlsContent.addComponent( clusterNameLabel );
        controlsContent.setComponentAlignment( clusterNameLabel, Alignment.MIDDLE_CENTER );

        clusterCombo = new ComboBox();
        clusterCombo.setImmediate( true );
        clusterCombo.setTextInputAllowed( false );
        clusterCombo.setWidth( 200, Sizeable.Unit.PIXELS );
        clusterCombo.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                config = ( CassandraClusterConfig ) event.getProperty().getValue();
                refreshUI();
                checkAllNodes();
            }
        } );
        controlsContent.addComponent( clusterCombo );
        controlsContent.setComponentAlignment( clusterCombo, Alignment.MIDDLE_CENTER );

        /** Refresh button */
        refreshClustersBtn = new Button( REFRESH_CLUSTERS_CAPTION );
        addClickListener( refreshClustersBtn );
        controlsContent.addComponent( refreshClustersBtn );
        controlsContent.setComponentAlignment( refreshClustersBtn, Alignment.MIDDLE_CENTER );


        /** Check all button */
        checkAllBtn = new Button( CHECK_ALL_BUTTON_CAPTION );
        addClickListener( checkAllBtn );
        controlsContent.addComponent( checkAllBtn );
        controlsContent.setComponentAlignment( checkAllBtn, Alignment.MIDDLE_CENTER );


        /** Start all button */
        startAllBtn = new Button( START_ALL_BUTTON_CAPTION );
        addClickListener( startAllBtn );
        controlsContent.addComponent( startAllBtn );
        controlsContent.setComponentAlignment( startAllBtn, Alignment.MIDDLE_CENTER );


        /** Stop all button */
        stopAllBtn = new Button( STOP_ALL_BUTTON_CAPTION );
        addClickListener( stopAllBtn );
        controlsContent.addComponent( stopAllBtn );
        controlsContent.setComponentAlignment( stopAllBtn, Alignment.MIDDLE_CENTER );


        /** Destroy Cluster button */
        destroyClusterBtn = new Button( DESTROY_CLUSTER_BUTTON_CAPTION );
        addClickListenerToDestroyClusterButton();
        controlsContent.addComponent( destroyClusterBtn );
        controlsContent.setComponentAlignment( destroyClusterBtn, Alignment.MIDDLE_CENTER );

        addStyleNameToButtons( refreshClustersBtn, checkAllBtn, startAllBtn, stopAllBtn, destroyClusterBtn );

        PROGRESS_ICON.setVisible( false );
        controlsContent.addComponent( PROGRESS_ICON );
        contentRoot.addComponent( controlsContent, 0, 0 );
        contentRoot.addComponent( nodesTable, 0, 1, 0, 9 );
    }


    public void addClickListenerToDestroyClusterButton(){
        destroyClusterBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                if ( config != null )
                {
                    ConfirmationDialog alert = new ConfirmationDialog(
                            String.format( "Do you want to destroy the %s cluster?", config.getClusterName() ), "Yes",
                            "No" );
                    alert.getOk().addClickListener( new Button.ClickListener()
                    {
                        @Override
                        public void buttonClick( Button.ClickEvent clickEvent )
                        {
                            /** before destroying cluster, stop it first to not leave background zombie processes **/
                            stopAllBtn.click();
                            UUID trackID = cassandra.uninstallCluster( config.getClusterName() );

                            ProgressWindow window = new ProgressWindow( executorService, tracker, trackID,
                                    CassandraClusterConfig.PRODUCT_KEY );
                            window.getWindow().addCloseListener( new Window.CloseListener()
                            {
                                @Override
                                public void windowClose( Window.CloseEvent closeEvent )
                                {
                                    refreshClustersInfo();
                                }
                            } );
                            contentRoot.getUI().addWindow( window.getWindow() );
                        }
                    } );

                    contentRoot.getUI().addWindow( alert.getAlert() );
                }
                else
                {
                    show( "Please, select cluster" );
                }
            }
        } );
    }


    public void addClickListener( Button button )
    {
        if ( button.getCaption().equals( REFRESH_CLUSTERS_CAPTION ) )
        {
            button.addClickListener( new Button.ClickListener()
            {
                @Override
                public void buttonClick( final Button.ClickEvent event )
                {
                    refreshClustersInfo();
                }
            } );
            return;
        }
        switch ( button.getCaption() )
        {
            case CHECK_ALL_BUTTON_CAPTION:
                button.addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( final Button.ClickEvent event )
                    {
                        if ( config == null )
                        {
                            show( MESSAGE );
                        }
                        else
                        {
                            checkAllNodes();
                        }
                    }
                } );
                break;

            case START_ALL_BUTTON_CAPTION:
                button.addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( final Button.ClickEvent event )
                    {
                        if ( config == null )
                        {
                            show( MESSAGE );
                        }
                        else
                        {
                            startAllNodes();
                        }

                    }
                } );
                break;
            case STOP_ALL_BUTTON_CAPTION:
                button.addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( final Button.ClickEvent event )
                    {
                        if ( config == null )
                        {
                            show( MESSAGE );
                        }
                        else
                        {
                            stopAllNodes();
                        }
                    }
                } );
                break;
        }
    }


    public void addStyleNameToButtons( Button... buttons )
    {
        for ( Button b : buttons )
        {
            b.addStyleName( BUTTON_STYLE_NAME );
        }
    }

    public void disableOREnableAllButtonsOnTable( Table table, boolean value )
    {
        if ( table != null )
        {
            for ( Object o : table.getItemIds() )
            {
                int rowId = ( Integer ) o;
                Item row = table.getItem( rowId );
                HorizontalLayout availableOperationsLayout =
                        ( HorizontalLayout ) ( row.getItemProperty( AVAILABLE_OPERATIONS_COLUMN_CAPTION ).getValue() );
                if ( availableOperationsLayout != null )
                {
                    for ( Component component : availableOperationsLayout )
                    {
                        component.setEnabled( value );
                    }
                }
            }
        }
    }


    /**
     * Creates table in which all nodes in the cluster are listed.
     *
     * @param caption title of table
     *
     * @return nodes table
     */
    private Table createTableTemplate( String caption )
    {
        final Table table = new Table( caption );
        table.addContainerProperty( HOST_COLUMN_CAPTION, String.class, null );
        table.addContainerProperty( IP_COLUMN_CAPTION, String.class, null );
        table.addContainerProperty( NODE_ROLE_COLUMN_CAPTION, String.class, null );
        table.addContainerProperty( STATUS_COLUMN_CAPTION, Label.class, null );
        table.addContainerProperty( AVAILABLE_OPERATIONS_COLUMN_CAPTION, HorizontalLayout.class, null );

        table.setSizeFull();
        table.setPageLength( 10 );
        table.setSelectable( false );
        table.setImmediate( true );
        table.setColumnCollapsingAllowed( true );
        table.addItemClickListener( new ItemClickEvent.ItemClickListener()
        {
            @Override
            public void itemClick( ItemClickEvent event )
            {
                if ( event.isDoubleClick() )
                {
                    String lxcHostname =
                            ( String ) table.getItem( event.getItemId() ).getItemProperty( HOST_COLUMN_CAPTION )
                                            .getValue();
                    Agent lxcAgent = agentManager.getAgentByHostname( lxcHostname );
                    if ( lxcAgent != null )
                    {
                        TerminalWindow terminal =
                                new TerminalWindow( Sets.newHashSet( lxcAgent ), executorService, commandRunner,
                                        agentManager );
                        contentRoot.getUI().addWindow( terminal.getWindow() );
                    }
                    else
                    {
                        show( "Agent is not connected" );
                    }
                }
            }
        } );
        return table;
    }


    /**
     * Shows notification with the given argument
     *
     * @param notification notification which will shown.
     */
    private void show( String notification )
    {
        Notification.show( notification );
    }


    /**
     * Fill out the table in which all nodes in the cluster are listed.
     *
     * @param table table to be filled
     * @param agents nodes
     */
    private void populateTable( final Table table, Set<Agent> agents )
    {
        table.removeAllItems();
        for ( final Agent agent : agents )
        {
            final Label resultHolder = new Label();
            final Button checkButton = new Button( CHECK_BUTTON_CAPTION );
            final Button startButton = new Button( START_BUTTON_CAPTION );
            final Button stopButton = new Button( STOP_BUTTON_CAPTION );

            addStyleNameToButtons( checkButton, startButton, stopButton );

            disableButtons( startButton, stopButton );
            PROGRESS_ICON.setVisible( false );

            final HorizontalLayout availableOperations = new HorizontalLayout();
            availableOperations.addStyleName( "default" );
            availableOperations.setSpacing( true );

            addGivenComponents( availableOperations, checkButton, startButton, stopButton );

            String isSeed = checkIfSeed( agent );

            table.addItem( new Object[] {
                    agent.getHostname(), agent.getListIP().get( 0 ), isSeed, resultHolder, availableOperations
            }, null );

            addClickListenerToCheckButton( agent, resultHolder, checkButton, startButton, stopButton );
            addClickListenerToStartButton( agent, checkButton, startButton, stopButton );
            addClickListenerToStopButton( agent, checkButton, startButton, stopButton );
        }
    }

    public Button getButton( String caption, Button... buttons )
    {
        for ( Button b : buttons )
        {
            if ( b.getCaption().equals( caption ) )
            {
                return b;
            }
        }
        return null;
    }


    public void addClickListenerToStopButton( final Agent agent, final Button ... buttons ){
        getButton( STOP_BUTTON_CAPTION, buttons ).addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                PROGRESS_ICON.setVisible( true );
                disableButtons( buttons );
                executorService.execute(
                        new StopTask( cassandra, tracker, config.getClusterName(), agent.getHostname(),
                                new CompleteEvent()
                                {
                                    @Override
                                    public void onComplete( String result )
                                    {
                                        synchronized ( PROGRESS_ICON )
                                        {
                                            getButton( CHECK_BUTTON_CAPTION, buttons ).setEnabled( true );
                                            getButton( CHECK_BUTTON_CAPTION, buttons ).click();
                                        }
                                    }
                                } ) );
            }
        } );
    }

    public void addClickListenerToStartButton( final Agent agent, final Button ... buttons ){
        getButton( START_BUTTON_CAPTION, buttons ).addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                PROGRESS_ICON.setVisible( true );
                disableButtons( buttons );
                executorService.execute(
                        new StartTask( cassandra, tracker, config.getClusterName(), agent.getHostname(),
                                new CompleteEvent()
                                {
                                    @Override
                                    public void onComplete( String result )
                                    {
                                        synchronized ( PROGRESS_ICON )
                                        {
                                            getButton( CHECK_BUTTON_CAPTION, buttons ).setEnabled( true );
                                            getButton( CHECK_BUTTON_CAPTION, buttons ).click();
                                        }
                                    }
                                } ) );
            }
        } );
    }


    public void addClickListenerToCheckButton( final Agent agent, final Label resultHolder, final Button ... buttons ){
        getButton( CHECK_BUTTON_CAPTION, buttons ).addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent event )
            {
                PROGRESS_ICON.setVisible( true );
                disableButtons( buttons );
                executorService.execute(
                        new CheckTask( cassandra, tracker, config.getClusterName(), agent.getHostname(),
                                new CompleteEvent()
                                {
                                    public void onComplete( String result )
                                    {
                                        synchronized ( PROGRESS_ICON )
                                        {
                                            resultHolder.setValue( result );
                                            if ( result.contains( "not" ) )
                                            {
                                                getButton( START_BUTTON_CAPTION, buttons ).setEnabled( true );
                                                getButton( STOP_BUTTON_CAPTION, buttons ).setEnabled( false );
                                            }
                                            else
                                            {
                                                getButton( START_BUTTON_CAPTION, buttons ).setEnabled( false );
                                                getButton( STOP_BUTTON_CAPTION, buttons ).setEnabled( true );
                                            }
                                            PROGRESS_ICON.setVisible( false );
                                            getButton( CHECK_BUTTON_CAPTION, buttons ).setEnabled( true );
                                        }
                                    }
                                } ) );
            }
        } );
    }

    public void addGivenComponents( HorizontalLayout layout, Button... buttons )
    {
        for ( Button b : buttons )
        {
            layout.addComponent( b );
        }
    }



    public void disableButtons( Button... buttons )
    {
        for ( Button b : buttons )
        {
            b.setEnabled( false );
        }
    }


    public void enableButtons( Button... buttons )
    {
        for ( Button b : buttons )
        {
            b.setEnabled( true );
        }
    }

    public void stopAllNodes()
    {
        for ( Agent agent : config.getNodes() )
        {
            PROGRESS_ICON.setVisible( true );
            disableOREnableAllButtonsOnTable( nodesTable, false );
            executorService.execute(
                    new StopTask( cassandra, tracker, config.getClusterName(), agent.getHostname(), new CompleteEvent()
                    {
                        @Override
                        public void onComplete( String result )
                        {
                            synchronized ( PROGRESS_ICON )
                            {
                                disableOREnableAllButtonsOnTable( nodesTable, true );
                                checkAllNodes();
                            }
                        }
                    } ) );
        }
    }


    public void startAllNodes()
    {
        for ( Agent agent : config.getNodes() )
        {
            PROGRESS_ICON.setVisible( true );
            disableOREnableAllButtonsOnTable( nodesTable, false );
            executorService.execute(
                    new StartTask( cassandra, tracker, config.getClusterName(), agent.getHostname(), new CompleteEvent()
                    {
                        @Override
                        public void onComplete( String result )
                        {
                            synchronized ( PROGRESS_ICON )
                            {
                                disableOREnableAllButtonsOnTable( nodesTable, true );
                                checkAllNodes();
                            }
                        }
                    } ) );
        }
    }


    public void checkAllNodes()
    {
        if ( nodesTable != null )
        {
            for ( Object o : nodesTable.getItemIds() )
            {
                int rowId = ( Integer ) o;
                Item row = nodesTable.getItem( rowId );
                HorizontalLayout availableOperationsLayout =
                        ( HorizontalLayout ) ( row.getItemProperty( AVAILABLE_OPERATIONS_COLUMN_CAPTION ).getValue() );
                if ( availableOperationsLayout != null )
                {
                    Button checkBtn = getButton( availableOperationsLayout, CHECK_BUTTON_CAPTION );
                    if ( checkBtn != null )
                    {
                        checkBtn.click();
                    }
                }
            }
        }
    }


    protected Button getButton( final HorizontalLayout availableOperationsLayout, String caption )
    {
        if ( availableOperationsLayout == null )
        {
            return null;
        }
        else
        {
            for ( Component component : availableOperationsLayout )
            {
                if ( component.getCaption().equals( caption ) )
                {
                    return ( Button ) component;
                }
            }
            return null;
        }
    }


    /**
     * Removes all items in the table, and populate it again with {@link #populateTable(com.vaadin.ui.Table,
     * java.util.Set)}.
     */
    private void refreshUI()
    {
        if ( config != null )
        {
            populateTable( nodesTable, config.getNodes() );
        }
        else
        {
            nodesTable.removeAllItems();
        }
    }


    /**
     * @param agent agent
     *
     * @return Yes if give agent is among seeds, otherwise returns No
     */
    public String checkIfSeed( Agent agent )
    {
        if ( config.getSeedNodes().contains( agent ) )
        {
            return "Seed";
        }
        return "Not Seed";
    }


    /**
     * Refreshes combo box which lists available clusters in DB
     */
    public void refreshClustersInfo()
    {
        List<CassandraClusterConfig> info = cassandra.getClusters();
        CassandraClusterConfig clusterInfo = ( CassandraClusterConfig ) clusterCombo.getValue();
        clusterCombo.removeAllItems();
        if ( info != null && !info.isEmpty() )
        {
            for ( CassandraClusterConfig cassandraInfo : info )
            {
                clusterCombo.addItem( cassandraInfo );
                clusterCombo.setItemCaption( cassandraInfo, cassandraInfo.getClusterName() );
            }
            if ( clusterInfo != null )
            {
                for ( CassandraClusterConfig cassandraInfo : info )
                {
                    if ( cassandraInfo.getClusterName().equals( clusterInfo.getClusterName() ) )
                    {
                        clusterCombo.setValue( cassandraInfo );
                        return;
                    }
                }
            }
            else
            {
                clusterCombo.setValue( info.iterator().next() );
            }
        }
    }


    public Component getContent()
    {
        return contentRoot;
    }
}