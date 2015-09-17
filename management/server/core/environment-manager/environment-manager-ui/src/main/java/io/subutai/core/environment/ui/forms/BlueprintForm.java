package io.subutai.core.environment.ui.forms;


import java.util.UUID;

import io.subutai.common.environment.Blueprint;
import io.subutai.common.environment.NodeGroup;
import io.subutai.common.protocol.PlacementStrategy;
import io.subutai.common.util.CollectionUtil;
import io.subutai.common.util.JsonUtil;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.environment.api.exception.EnvironmentManagerException;
import io.subutai.core.peer.api.PeerManager;
import io.subutai.core.registry.api.TemplateRegistry;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;


public class BlueprintForm
{
    private static final String BLUEPRINT = "Blueprint";
    private static final String SAVE = "Save";
    private static final String PLEASE_PROVIDE_A_BLUEPRINT = "Please provide a blueprint";
    private static final String ERROR_SAVING_BLUEPRINT = "Error saving blueprint";
    private static final String BLUEPRINT_SAVED = "Blueprint saved";
    private static final String VIEW_BLUEPRINTS = "View blueprints";
    private static final String NAME = "Name";
    private static final String BUILD = "Build";
    private static final String VIEW = "View";
    private static final String DELETE = "Delete";
    private static final String GROW = "Grow";


    private final VerticalLayout contentRoot;
    private final EnvironmentManager environmentManager;
    private final PeerManager peerManager;
    private final TemplateRegistry templateRegistry;
    private TextArea blueprintTxtArea;
    private Table blueprintsTable;
    private Gson gson =
            new GsonBuilder().setExclusionStrategies( new FieldExclusionStrategy( "id" ) ).setPrettyPrinting().create();


    public BlueprintForm( EnvironmentManager environmentManager, PeerManager peerManager,
                          TemplateRegistry templateRegistry )
    {
        this.environmentManager = environmentManager;
        this.peerManager = peerManager;
        this.templateRegistry = templateRegistry;
        contentRoot = new VerticalLayout();

        contentRoot.setSpacing( true );
        contentRoot.setMargin( true );

        blueprintTxtArea = getBlueprintTxtArea();

        Button saveBlueprintBtn = new Button( SAVE );
        saveBlueprintBtn.setId( "saveBlueprintButton" );

        saveBlueprintBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                saveBlueprint();
            }
        } );

        Button putSampleBlueprint = new Button( "Get sample" );
        putSampleBlueprint.setId( "putSampleBlueprint" );
        putSampleBlueprint.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                String blueprintStr = gson.toJson( getSampleBlueprint() );
                blueprintTxtArea.setValue( blueprintStr );
            }
        } );

        contentRoot.addComponent( blueprintTxtArea );
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing( true );
        buttons.addComponent( saveBlueprintBtn );
        buttons.addComponent( putSampleBlueprint );
        contentRoot.addComponent( buttons );

        blueprintsTable = createBlueprintsTable( "Blueprints" );
        blueprintsTable.setId( "blueprintsTable" );

        final Button viewBlueprintsButton = new Button( VIEW_BLUEPRINTS );
        viewBlueprintsButton.setId( "viewBlueprintsButton" );
        viewBlueprintsButton.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                updateBlueprintsTable();
            }
        } );

        contentRoot.addComponent( viewBlueprintsButton );
        contentRoot.addComponent( blueprintsTable );

        updateBlueprintsTable();
    }


    private void updateBlueprintsTable()
    {
        blueprintsTable.removeAllItems();
        try
        {
            for ( final Blueprint blueprint : environmentManager.getBlueprints() )
            {
                final Button view = new Button( VIEW );
                view.setId( blueprint.getName() + "-view" );
                view.addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( final Button.ClickEvent clickEvent )
                    {
                        blueprintTxtArea.setValue( gson.toJson( blueprint ) );
                    }
                } );
                final Button delete = new Button( DELETE );
                delete.setId( blueprint.getName() + "-delete" );
                delete.addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( final Button.ClickEvent clickEvent )
                    {
                        try
                        {
                            environmentManager.removeBlueprint( blueprint.getId() );
                            updateBlueprintsTable();
                        }
                        catch ( EnvironmentManagerException e )
                        {
                            Notification.show( "Error removing blueprint", e.getMessage(),
                                    Notification.Type.ERROR_MESSAGE );
                        }
                    }
                } );
                final Button build = new Button( BUILD );
                build.setId( blueprint.getName() + "-build" );
                build.addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( final Button.ClickEvent clickEvent )
                    {
                        buildBlueprint( blueprint, false );
                    }
                } );

                final Button grow = new Button( GROW );
                grow.setId( blueprint.getName() + "-grow" );
                grow.addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( final Button.ClickEvent event )
                    {
                        buildBlueprint( blueprint, true );
                    }
                } );


                blueprintsTable.addItem( new Object[] {
                        blueprint.getName(), view, delete, build, grow
                }, null );
            }
        }
        catch ( EnvironmentManagerException e )
        {
            Notification.show( "Error loading blueprints", e.getMessage(), Notification.Type.ERROR_MESSAGE );
        }
        blueprintsTable.refreshRowCache();
    }


    private void buildBlueprint( Blueprint blueprint, boolean grow )
    {
        contentRoot.getUI().addWindow( new TopologyWindow( blueprint, peerManager, environmentManager, grow ) );
    }


    private Table createBlueprintsTable( String caption )
    {
        Table table = new Table( caption );
        table.addContainerProperty( NAME, String.class, null );
        table.addContainerProperty( VIEW, Button.class, null );
        table.addContainerProperty( DELETE, Button.class, null );
        table.addContainerProperty( BUILD, Button.class, null );
        table.addContainerProperty( GROW, Button.class, null );
        table.setPageLength( 10 );
        table.setSelectable( false );
        table.setEnabled( true );
        table.setImmediate( true );
        table.setSizeFull();
        return table;
    }


    private Blueprint getSampleBlueprint()
    {
        NodeGroup nodeGroup =
                new NodeGroup( "Sample node group", "master", 2, 0, 0, new PlacementStrategy( "ROUND_ROBIN" ) );
        return new Blueprint( "Sample blueprint", Sets.newHashSet( nodeGroup ) );
    }


    private TextArea getBlueprintTxtArea()
    {
        blueprintTxtArea = new TextArea( BLUEPRINT );
        blueprintTxtArea.setId( "blueprintTxtArea" );
        blueprintTxtArea.setSizeFull();
        blueprintTxtArea.setRows( 20 );
        blueprintTxtArea.setImmediate( true );
        blueprintTxtArea.setWordwrap( false );
        return blueprintTxtArea;
    }


    private void saveBlueprint()
    {
        String content = blueprintTxtArea.getValue().trim();
        if ( content.length() > 0 )
        {
            try
            {
                Blueprint blueprint = JsonUtil.fromJson( content, Blueprint.class );

                if ( Strings.isNullOrEmpty( blueprint.getName() ) )
                {
                    Notification.show( "Invalid blueprint name", Notification.Type.ERROR_MESSAGE );
                }
                else if ( CollectionUtil.isCollectionEmpty( blueprint.getNodeGroups() ) )
                {
                    Notification.show( "Invalid node group set", Notification.Type.ERROR_MESSAGE );
                }
                else
                {
                    for ( NodeGroup nodeGroup : blueprint.getNodeGroups() )
                    {
                        if ( Strings.isNullOrEmpty( nodeGroup.getName() ) )
                        {
                            Notification.show( "Invalid node group name", Notification.Type.ERROR_MESSAGE );
                            return;
                        }
                        //                        else if ( Strings.isNullOrEmpty( nodeGroup.getDomainName() ) )
                        //                        {
                        //                            Notification.show( "Invalid domain name", Notification.Type
                        // .ERROR_MESSAGE );
                        //                            return;
                        //                        }
                        else if ( nodeGroup.getNumberOfContainers() <= 0 )
                        {
                            Notification.show( "Invalid number of containers", Notification.Type.ERROR_MESSAGE );
                            return;
                        }
                        else if ( Strings.isNullOrEmpty( nodeGroup.getTemplateName() ) )
                        {
                            Notification.show( "Invalid templateName", Notification.Type.ERROR_MESSAGE );
                            return;
                        }
                        else if ( templateRegistry.getTemplate( nodeGroup.getTemplateName() ) == null )
                        {
                            Notification
                                    .show( String.format( "Template %s does not exist", nodeGroup.getTemplateName() ),
                                            Notification.Type.ERROR_MESSAGE );
                            return;
                        }
                        else if ( nodeGroup.getContainerPlacementStrategy() == null )
                        {
                            Notification.show( "Invalid node container placement strategy",
                                    Notification.Type.ERROR_MESSAGE );
                            return;
                        }
                    }

                    blueprint.setId( UUID.randomUUID() );

                    environmentManager.saveBlueprint( blueprint );

                    updateBlueprintsTable();

                    Notification.show( BLUEPRINT_SAVED );
                }
            }
            catch ( Exception e )
            {
                Notification.show( ERROR_SAVING_BLUEPRINT, e.getMessage(), Notification.Type.ERROR_MESSAGE );
            }
        }
        else
        {
            Notification.show( PLEASE_PROVIDE_A_BLUEPRINT );
        }
    }


    public VerticalLayout getContentRoot()
    {
        return this.contentRoot;
    }
}
