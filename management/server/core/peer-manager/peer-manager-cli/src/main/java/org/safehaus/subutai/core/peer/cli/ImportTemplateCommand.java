package org.safehaus.subutai.core.peer.cli;


import org.safehaus.subutai.common.util.UUIDUtil;
import org.safehaus.subutai.core.peer.api.Peer;
import org.safehaus.subutai.core.peer.api.PeerManager;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;


/**
 *
 */
@Command( scope = "peer", name = "export-template" )
public class ImportTemplateCommand extends OsgiCommandSupport
{

    private PeerManager peerManager;


    public PeerManager getPeerManager()
    {
        return peerManager;
    }


    public void setPeerManager( final PeerManager peerManager )
    {
        this.peerManager = peerManager;
    }


    @Override
    protected Object doExecute() throws Exception
    {
        Peer peer = getSamplePeer();

        if ( peerManager.register( peer ) )
        {
            System.out.println( "Peer registered." );
        }
        else
        {
            System.out.println( "Failed to register peer." );
        }

        return null;
    }


    private Peer getSamplePeer()
    {
        Peer peer = new Peer();
        peer.setName( "Peer name" );
        peer.setIp( "10.10.10.10" );
        peer.setId( UUIDUtil.generateTimeBasedUUID() );
        return peer;
    }
}
