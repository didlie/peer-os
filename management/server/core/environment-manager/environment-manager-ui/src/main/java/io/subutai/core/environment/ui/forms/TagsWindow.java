package io.subutai.core.environment.ui.forms;


import io.subutai.common.environment.Environment;
import io.subutai.common.peer.ContainerHost;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.common.peer.HostNotFoundException;
import io.subutai.core.peer.api.LocalPeer;

import com.google.common.base.Strings;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;


public class TagsWindow extends Window
{


    public TagsWindow( final EnvironmentManager environmentManager, final Environment environment, final ContainerHost containerHost, LocalPeer localPeer )
    {
        ContainerHost localContainer = null;
        try
        {
            localContainer = localPeer.getContainerHostById( containerHost.getId() );
        }
        catch ( HostNotFoundException e )
        {
            //ignore, this is a remote container
        }

        final ContainerHost finalLocalContainer = localContainer;

        setCaption( containerHost.getHostname() );
        setWidth( "350px" );
        setHeight( "300px" );
        setModal( true );
        setClosable( true );

        GridLayout content = new GridLayout( 2, 2 );
        content.setSpacing( true );
        content.setMargin( true );
        content.setStyleName( "default" );
        content.setSizeFull();

        final ListSelect tagsSelect = new ListSelect( "Tags", containerHost.getTags() );
        tagsSelect.setNullSelectionAllowed( false );
        //        tagsSelect.setRows( 5 );
        tagsSelect.setWidth( "150px" );
        tagsSelect.setMultiSelect( false );

        content.addComponent( tagsSelect, 0, 0 );

        content.setComponentAlignment( tagsSelect, Alignment.BOTTOM_LEFT );

        Button removeTagBtn = new Button( "Remove" );

        removeTagBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent event )
            {
                if ( tagsSelect.getValue() != null )
                {
                    String tag = String.valueOf( tagsSelect.getValue() ).trim();
                    containerHost.removeTag( tag );
                    tagsSelect.setContainerDataSource( new IndexedContainer( containerHost.getTags() ) );
                    if ( finalLocalContainer != null )
                    {
                        finalLocalContainer.removeTag( tag );
                    }

                    environmentManager.notifyOnContainerStateChanged( environment, containerHost );
                }
                else
                {
                    Notification.show( "Please, select a tag" );
                }
            }
        } );

        content.addComponent( removeTagBtn, 1, 0 );
        content.setComponentAlignment( removeTagBtn, Alignment.BOTTOM_LEFT );


        final TextField tagTxt = new TextField();
        tagTxt.setWidth( "150px" );

        content.addComponent( tagTxt, 0, 1 );

        Button addTagBtn = new Button( "Add" );

        addTagBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent event )
            {

                if ( !Strings.isNullOrEmpty( tagTxt.getValue() ) )
                {
                    String tag = tagTxt.getValue().trim();
                    containerHost.addTag( tag );
                    tagsSelect.setContainerDataSource( new IndexedContainer( containerHost.getTags() ) );
                    if ( finalLocalContainer != null )
                    {
                        finalLocalContainer.addTag( tag );
                    }

                    environmentManager.notifyOnContainerStateChanged( environment, containerHost );
                }
                else
                {
                    Notification.show( "Please, enter valid tag" );
                }
            }
        } );

        content.addComponent( addTagBtn, 1, 1 );


        setContent( content );
    }
}