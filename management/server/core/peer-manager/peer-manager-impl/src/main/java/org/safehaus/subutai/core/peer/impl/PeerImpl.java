package org.safehaus.subutai.core.peer.impl;


import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.safehaus.subutai.common.exception.HTTPException;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.util.HttpUtil;
import org.safehaus.subutai.common.util.UUIDUtil;
import org.safehaus.subutai.core.agent.api.AgentManager;
import org.safehaus.subutai.core.container.api.ContainerCreateException;
import org.safehaus.subutai.core.container.api.ContainerManager;
import org.safehaus.subutai.core.db.api.DBException;
import org.safehaus.subutai.core.db.api.DbManager;
import org.safehaus.subutai.core.peer.api.Peer;
import org.safehaus.subutai.core.peer.api.PeerException;
import org.safehaus.subutai.core.peer.api.PeerManager;
import org.safehaus.subutai.core.peer.api.message.Common;
import org.safehaus.subutai.core.peer.api.message.PeerMessageException;
import org.safehaus.subutai.core.peer.api.message.PeerMessageListener;
import org.safehaus.subutai.core.peer.impl.dao.PeerDAO;

import com.google.common.base.Strings;


/**
 * Created by bahadyr on 8/28/14.
 */
public class PeerImpl implements PeerManager {

    private final static Logger LOG = Logger.getLogger( PeerImpl.class.getName() );
    private final Queue<PeerMessageListener> peerMessageListeners = new ConcurrentLinkedQueue<>();


    private final String SOURCE = "PEER_MANAGER";
    private DbManager dbManager;
    private AgentManager agentManager;
    private PeerDAO peerDAO;
    private ContainerManager containerManager;


    public void init() {
        peerDAO = new PeerDAO( dbManager );
    }


    public void destroy() {
    }


    public void setDbManager( final DbManager dbManager ) {
        this.dbManager = dbManager;
    }


    public void setAgentManager( final AgentManager agentManager ) {
        this.agentManager = agentManager;
    }


    public ContainerManager getContainerManager() {
        return containerManager;
    }


    public void setContainerManager( final ContainerManager containerManager ) {
        this.containerManager = containerManager;
    }


    @Override
    public String register( final Peer peer ) {

        try
        {
            String peerId = peer.getId().toString();
            peerDAO.saveInfo( SOURCE, peerId, peer );
            return peerId;
        }
        catch ( DBException e )
        {
            LOG.info( e.getMessage() );
        }
        return null;
    }


    @Override
    public UUID getSiteId() {
        return UUIDUtil.generateMACBasedUUID();
    }


    @Override
    public List<Peer> peers() {
        List<Peer> peers = null;
        try
        {
            peers = peerDAO.getInfo( SOURCE, Peer.class );
        }
        catch ( DBException e )
        {
            LOG.info( e.getMessage() );
        }
        return peers;
    }


    @Override
    public boolean unregister( final String uuid ) {
        try
        {
            peerDAO.deleteInfo( SOURCE, uuid );
            return true;
        }
        catch ( DBException e )
        {
            LOG.info( e.getMessage() );
        }
        return false;
    }


    @Override
    public Peer getPeerByUUID( UUID uuid ) {
        if ( getSiteId().compareTo( uuid ) == 0 )
        {
            Peer peer = new Peer();
            peer.setId( uuid );
            peer.setIp( getLocalIp() );
            peer.setName( "Me" );
            return peer;
        }

        try
        {
            return peerDAO.getInfo( SOURCE, uuid.toString(), Peer.class );
        }
        catch ( DBException e )
        {
            LOG.info( e.getMessage() );
        }
        return null;
    }


    @Override
    public void addPeerMessageListener( PeerMessageListener listener ) {
        try
        {
            if ( !peerMessageListeners.contains( listener ) )
            {
                peerMessageListeners.add( listener );
            }
        }
        catch ( Exception ex )
        {
            LOG.log( Level.SEVERE, "Error in addPeerMessageListener", ex );
        }
    }


    @Override
    public void removePeerMessageListener( PeerMessageListener listener ) {
        try
        {
            peerMessageListeners.remove( listener );
        }
        catch ( Exception ex )
        {
            LOG.log( Level.SEVERE, "Error in removePeerMessageListener", ex );
        }
    }


    @Override
    public String sendPeerMessage( final Peer peer, String recipient, final String message )
            throws PeerMessageException {
        if ( peer == null )
        {
            throw new PeerMessageException( "Peer is null" );
        }
        if ( Strings.isNullOrEmpty( recipient ) )
        {
            throw new PeerMessageException( "Recipient is null or empty" );
        }
        if ( Strings.isNullOrEmpty( message ) )
        {
            throw new PeerMessageException( "Message is null or empty" );
        }

        try
        {
            if ( isPeerReachable( peer ) )
            {
                String ip = peer.getIp();

                Map<String, String> params = new HashMap<>();
                params.put( Common.RECIPIENT_PARAM_NAME, recipient );
                params.put( Common.PEER_ID_PARAM_NAME, getSiteId().toString() );
                params.put( Common.MESSAGE_PARAM_NAME, message );
                try
                {
                    return HttpUtil.request( HttpUtil.RequestType.POST, String.format( Common.MESSAGE_REQUEST_URL, ip ),
                            params );
                }
                catch ( HTTPException e )
                {
                    LOG.log( Level.SEVERE, "Error in sendPeerMessage", e );
                    throw new PeerMessageException( e.getMessage() );
                }
            }
            else
            {
                String err = "Peer is not reachable";
                LOG.log( Level.SEVERE, "Error in sendPeerMessage", err );
                throw new PeerMessageException( err );
            }
        }
        catch ( PeerException e )
        {
            LOG.log( Level.SEVERE, "Error in sendPeerMessage", e );
            throw new PeerMessageException( e.getMessage() );
        }
    }


    @Override
    public String processPeerMessage( final String peerId, final String recipient, final String message )
            throws PeerMessageException {
        if ( Strings.isNullOrEmpty( peerId ) )
        {
            throw new PeerMessageException( "Peer id is null or empty" );
        }
        if ( Strings.isNullOrEmpty( recipient ) )
        {
            throw new PeerMessageException( "Recipient is null or empty" );
        }
        if ( Strings.isNullOrEmpty( message ) )
        {
            throw new PeerMessageException( "Message is null or empty" );
        }
        try
        {
            UUID peerUUID = UUID.fromString( peerId );
            Peer senderPeer = getPeerByUUID( peerUUID );
            if ( senderPeer != null )
            {
                try
                {
                    if ( isPeerReachable( senderPeer ) )
                    {
                        for ( PeerMessageListener listener : peerMessageListeners )
                        {
                            if ( listener.getName().equalsIgnoreCase( recipient ) )
                            {
                                try
                                {
                                    return listener.onMessage( senderPeer, message );
                                }
                                catch ( Exception e )
                                {
                                    LOG.log( Level.SEVERE, "Error in processPeerMessage", e );
                                    throw new PeerMessageException( e.getMessage() );
                                }
                            }
                        }
                        String err = String.format( "Recipient %s not found", recipient );
                        LOG.log( Level.SEVERE, "Error in processPeerMessage", err );
                        throw new PeerMessageException( err );
                    }
                    else
                    {
                        String err = String.format( "Peer is not reachable %s", senderPeer );
                        LOG.log( Level.SEVERE, "Error in processPeerMessage", err );
                        throw new PeerMessageException( err );
                    }
                }
                catch ( PeerException e )
                {
                    LOG.log( Level.SEVERE, "Error in processPeerMessage", e );
                    throw new PeerMessageException( e.getMessage() );
                }
            }
            else
            {
                String err = String.format( "Peer %s not found", peerId );
                LOG.log( Level.SEVERE, "Error in processPeerMessage", err );
                throw new PeerMessageException( err );
            }
        }
        catch ( IllegalArgumentException e )
        {
            LOG.log( Level.SEVERE, "Error in processPeerMessage", e );
            throw new PeerMessageException( e.getMessage() );
        }
    }


    @Override
    public boolean isPeerReachable( final Peer peer ) throws PeerException {
        if ( peer == null )
        {
            throw new PeerException( "Peer is null" );
        }
        if ( getPeerByUUID( peer.getId() ) != null )
        {
            try
            {
                HttpUtil.request( HttpUtil.RequestType.GET, String.format( Common.PING_URL, peer.getIp() ), null );
                return true;
            }
            catch ( HTTPException e )
            {
                return false;
            }
        }
        else
        {
            throw new PeerException( "Peer not found" );
        }
    }


    @Override
    public Set<Agent> getConnectedAgents( String environmentId ) throws PeerException {
        try
        {
            UUID envId = UUID.fromString( environmentId );
            return agentManager.getAgentsByEnvironmentId( envId );
        }
        catch ( IllegalArgumentException e )
        {
            throw new PeerException( e.getMessage() );
        }
    }


    @Override
    public Set<Agent> createContainers( final UUID envId, final String template, final int numberOfNodes,
                                        final String strategy, final List<String> criteria ) {

        try
        {
            // TODO remote subutai or local
            return containerManager.clone( envId, template, numberOfNodes, strategy, null );
        }
        catch ( ContainerCreateException e )
        {
            LOG.severe( e.getMessage() );
        }
        return null;
    }


    private String getLocalIp() {
        Enumeration<NetworkInterface> n;
        try
        {
            n = NetworkInterface.getNetworkInterfaces();
            for (; n.hasMoreElements(); )
            {
                NetworkInterface e = n.nextElement();

                Enumeration<InetAddress> a = e.getInetAddresses();
                for (; a.hasMoreElements(); )
                {
                    InetAddress addr = a.nextElement();
                    if ( addr.getHostAddress().startsWith( "172" ) )
                    {
                        return addr.getHostAddress();
                    }
                }
            }
        }
        catch ( SocketException e )
        {
            LOG.severe( e.getMessage() );
        }


        return "127.0.0.1";
    }


    public Collection<PeerMessageListener> getPeerMessageListeners() {
        return Collections.unmodifiableCollection( peerMessageListeners );
    }
}