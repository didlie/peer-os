package org.safehaus.subutai.core.peer.impl.entity;


import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.safehaus.subutai.common.host.Interface;
import org.safehaus.subutai.common.mdc.SubutaiExecutors;
import org.safehaus.subutai.common.network.Gateway;
import org.safehaus.subutai.common.network.Vni;
import org.safehaus.subutai.common.network.VniVlanMapping;
import org.safehaus.subutai.common.peer.PeerException;
import org.safehaus.subutai.common.protocol.Disposable;
import org.safehaus.subutai.common.settings.Common;
import org.safehaus.subutai.common.util.CollectionUtil;
import org.safehaus.subutai.common.util.NumUtil;
import org.safehaus.subutai.common.util.ServiceLocator;
import org.safehaus.subutai.core.hostregistry.api.ResourceHostInfo;
import org.safehaus.subutai.core.network.api.NetworkManager;
import org.safehaus.subutai.core.network.api.NetworkManagerException;
import org.safehaus.subutai.core.network.api.Tunnel;
import org.safehaus.subutai.core.peer.api.ManagementHost;
import org.safehaus.subutai.core.repository.api.RepositoryException;
import org.safehaus.subutai.core.repository.api.RepositoryManager;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;


@Entity
@Table( name = "management_host" )
@Access( AccessType.FIELD )
public class ManagementHostEntity extends AbstractSubutaiHost implements ManagementHost, Disposable
{
    private static final String GATEWAY_INTERFACE_NAME_REGEX = "^br-(\\d+)$";
    private static final Pattern GATEWAY_INTERFACE_NAME_PATTERN = Pattern.compile( GATEWAY_INTERFACE_NAME_REGEX );


    @Column
    String name = "Subutai Management Host";

    @Transient
    private ExecutorService singleThreadExecutorService = SubutaiExecutors.newSingleThreadExecutor();
    @Transient
    private ServiceLocator serviceLocator = new ServiceLocator();


    protected ManagementHostEntity()
    {
    }


    public ManagementHostEntity( final String peerId, final ResourceHostInfo resourceHostInfo )
    {
        super( peerId, resourceHostInfo );
    }


    public void init()
    {
        //for future use
    }


    public void dispose()
    {
        singleThreadExecutorService.shutdown();
    }


    public <T> Future<T> queueSequentialTask( Callable<T> callable )
    {
        return singleThreadExecutorService.submit( callable );
    }


    public String getName()
    {
        return name;
    }


    public void setName( final String name )
    {
        this.name = name;
    }


    public void addAptSource( final String hostname, final String ip ) throws PeerException
    {
        try
        {
            getRepositoryManager().addAptSource( hostname, ip );
        }
        catch ( RepositoryException e )
        {
            throw new PeerException( "Error adding apt source", e );
        }
    }


    public void removeAptSource( final String host, final String ip ) throws PeerException
    {
        try
        {
            getRepositoryManager().removeAptSource( ip );
        }
        catch ( RepositoryException e )
        {
            throw new PeerException( "Error removing apt source", e );
        }
    }


    @Override
    public String readFile( final String path ) throws IOException
    {
        byte[] encoded = Files.readAllBytes( Paths.get( path ) );
        return new String( encoded, Charset.defaultCharset() );
    }


    @Override
    public void createGateway( final String gatewayIp, final int vlan ) throws PeerException
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( gatewayIp ) && gatewayIp.matches( Common.IP_REGEX ),
                "Invalid gateway IP" );
        Preconditions.checkArgument( NumUtil.isIntBetween( vlan, Common.MIN_VLAN_ID, Common.MAX_VLAN_ID ),
                String.format( "VLAN must be in the range from %d to %d", Common.MIN_VLAN_ID, Common.MAX_VLAN_ID ) );

        //need to execute sequentially since other parallel executions can take the same gateway
        Future<Boolean> future = queueSequentialTask( new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {

                Gateway newGateway = new Gateway( vlan, gatewayIp );

                try
                {
                    Set<Gateway> existingGateways = getGateways();
                    for ( Gateway gateway : existingGateways )
                    {
                        if ( gateway.equals( newGateway ) )
                        {
                            return false;
                        }
                    }

                    getNetworkManager().setupGateway( gatewayIp, vlan );

                    return true;
                }
                catch ( NetworkManagerException e )
                {
                    throw new PeerException(
                            String.format( "Error creating gateway tap device with IP %s and VLAN %d", gatewayIp,
                                    vlan ), e );
                }
            }
        } );

        try
        {
            future.get();
        }
        catch ( InterruptedException e )
        {
            throw new PeerException( e );
        }
        catch ( ExecutionException e )
        {
            if ( e.getCause() instanceof PeerException )
            {
                throw ( PeerException ) e.getCause();
            }
            throw new PeerException( "Error creating gateway", e.getCause() );
        }
    }


    @Override
    public void removeGateway( final int vlan ) throws PeerException
    {
        Preconditions.checkArgument( NumUtil.isIntBetween( vlan, Common.MIN_VLAN_ID, Common.MAX_VLAN_ID ),
                String.format( "VLAN must be in the range from %d to %d", Common.MIN_VLAN_ID, Common.MAX_VLAN_ID ) );

        try
        {
            getNetworkManager().removeGateway( vlan );
        }
        catch ( NetworkManagerException e )
        {
            throw new PeerException( String.format( "Error removing gateway tap device with VLAN %d", vlan ), e );
        }
    }


    public void cleanupEnvironmentNetworkSettings( final UUID environmentId ) throws PeerException
    {
        Preconditions.checkNotNull( environmentId, "Invalid environment id" );

        try
        {
            getNetworkManager().cleanupEnvironmentNetworkSettings( environmentId );
        }
        catch ( NetworkManagerException e )
        {
            throw new PeerException(
                    String.format( "Error cleaning up environment %s network settings", environmentId ), e );
        }
    }


    private Set<Tunnel> listTunnels() throws PeerException
    {
        try
        {
            return getNetworkManager().listTunnels();
        }
        catch ( NetworkManagerException e )
        {
            throw new PeerException( "Error retrieving peer tunnels", e );
        }
    }


    @Override
    public void removeTunnel( final String peerIp ) throws PeerException
    {
        try
        {
            Set<Tunnel> tunnels = listTunnels();
            for ( final Tunnel tunnel : tunnels )
            {
                if ( tunnel.getTunnelIp().equalsIgnoreCase( peerIp ) )
                {
                    getNetworkManager().removeTunnel( tunnel.getTunnelId() );
                    break;
                }
            }
        }
        catch ( NetworkManagerException e )
        {
            throw new PeerException( "Error removing tunnel", e );
        }
    }


    protected NetworkManager getNetworkManager() throws PeerException
    {
        try
        {
            return serviceLocator.getService( NetworkManager.class );
        }
        catch ( NamingException e )
        {
            throw new PeerException( e );
        }
    }


    protected RepositoryManager getRepositoryManager() throws PeerException
    {
        try
        {
            return serviceLocator.getService( RepositoryManager.class );
        }
        catch ( NamingException e )
        {
            throw new PeerException( e );
        }
    }


    @Override
    public Set<Vni> getReservedVnis() throws PeerException
    {
        try
        {
            return getNetworkManager().getReservedVnis();
        }
        catch ( NetworkManagerException e )
        {
            throw new PeerException( e );
        }
    }


    @Override
    public Set<Gateway> getGateways() throws PeerException
    {
        Set<Gateway> gateways = Sets.newHashSet();

        for ( Interface iface : interfaces )
        {
            Matcher matcher = GATEWAY_INTERFACE_NAME_PATTERN.matcher( iface.getInterfaceName().trim() );
            if ( matcher.find() )
            {
                int vlan = Integer.parseInt( matcher.group( 1 ) );
                String ip = iface.getIp();

                gateways.add( new Gateway( vlan, ip ) );
            }
        }

        return gateways;
    }


    @Override
    public int reserveVni( final Vni vni ) throws PeerException
    {
        Preconditions.checkNotNull( vni, "Invalid vni" );

        //need to execute sequentially since other parallel executions can take the same VNI
        Future<Integer> future = queueSequentialTask( new Callable<Integer>()
        {
            @Override
            public Integer call() throws Exception
            {

                //check if vni is already reserved
                Vni existingVni = findVniByEnvironmentId( vni.getEnvironmentId() );
                if ( existingVni != null )
                {
                    return existingVni.getVlan();
                }

                //figure out available vlan
                int vlan = findAvailableVlanId();

                //reserve vni & vlan for environment

                getNetworkManager().reserveVni( new Vni( vni.getVni(), vlan, vni.getEnvironmentId() ) );

                return vlan;
            }
        } );

        try
        {
            return future.get();
        }
        catch ( InterruptedException e )
        {
            throw new PeerException( e );
        }
        catch ( ExecutionException e )
        {
            if ( e.getCause() instanceof PeerException )
            {
                throw ( PeerException ) e.getCause();
            }
            throw new PeerException( "Error reserving VNI", e.getCause() );
        }
    }


    @Override
    public int setupTunnels( final Set<String> peerIps, final UUID environmentId ) throws PeerException
    {
        Preconditions.checkArgument( !CollectionUtil.isCollectionEmpty( peerIps ), "Invalid peer ips set" );
        Preconditions.checkNotNull( environmentId, "Invalid environment id" );

        //need to execute sequentially since other parallel executions can setup the same tunnel
        Future<Integer> future = queueSequentialTask( new Callable<Integer>()
        {
            @Override
            public Integer call() throws Exception
            {

                NetworkManager networkManager = getNetworkManager();

                //fail if vni is not reserved
                Vni environmentVni = findVniByEnvironmentId( environmentId );

                if ( environmentVni == null )
                {
                    throw new PeerException(
                            String.format( "Error setting up tunnels: No reserved vni found for environment %s",
                                    environmentId ) );
                }


                //setup tunnels to each remote peer
                Set<Tunnel> tunnels = networkManager.listTunnels();

                //remove local IP, just in case
                peerIps.remove( getIpByInterfaceName( Common.MANAGEMENT_HOST_EXTERNAL_IP_INTERFACE ) );

                for ( String peerIp : peerIps )
                {
                    int tunnelId = findTunnel( peerIp, tunnels );
                    //tunnel not found, create new one
                    if ( tunnelId == -1 )
                    {
                        //calculate tunnel id
                        tunnelId = calculateNextTunnelId( tunnels );

                        //create tunnel
                        networkManager.setupTunnel( tunnelId, peerIp );
                    }

                    //create vni-vlan mapping
                    setupVniVlanMapping( tunnelId, environmentVni.getVni(), environmentVni.getVlan(),
                            environmentVni.getEnvironmentId() );
                }

                return environmentVni.getVlan();
            }
        } );

        try
        {
            return future.get();
        }
        catch ( InterruptedException e )
        {
            throw new PeerException( e );
        }
        catch ( ExecutionException e )
        {
            if ( e.getCause() instanceof PeerException )
            {
                throw ( PeerException ) e.getCause();
            }
            throw new PeerException( "Error setting up tunnels", e.getCause() );
        }
    }


    private Vni findVniByEnvironmentId( UUID environmentId ) throws PeerException
    {
        //check if vni is already reserved
        for ( Vni aVni : getReservedVnis() )
        {
            if ( aVni.getEnvironmentId().equals( environmentId ) )
            {
                return aVni;
            }
        }

        return null;
    }


    private void setupVniVlanMapping( final int tunnelId, final long vni, final int vlanId, final UUID environmentId )
            throws PeerException
    {
        try
        {
            Set<VniVlanMapping> mappings = getNetworkManager().getVniVlanMappings();

            for ( VniVlanMapping mapping : mappings )
            {
                if ( mapping.getTunnelId() == tunnelId && mapping.getEnvironmentId().equals( environmentId ) )
                {
                    return;
                }
            }

            getNetworkManager().setupVniVLanMapping( tunnelId, vni, vlanId, environmentId );
        }
        catch ( NetworkManagerException e )
        {
            throw new PeerException( e );
        }
    }


    protected int findTunnel( String peerIp, Set<Tunnel> tunnels )
    {
        for ( Tunnel tunnel : tunnels )
        {
            if ( tunnel.getTunnelIp().equals( peerIp ) )
            {
                return tunnel.getTunnelId();
            }
        }

        return -1;
    }


    protected int calculateNextTunnelId( Set<Tunnel> tunnels )
    {
        int maxTunnelId = 0;
        for ( Tunnel tunnel : tunnels )
        {
            if ( tunnel.getTunnelId() > maxTunnelId )
            {
                maxTunnelId = tunnel.getTunnelId();
            }
        }

        return maxTunnelId + 1;
    }


    protected int findAvailableVlanId() throws PeerException
    {
        SortedSet<Integer> takenIds = Sets.newTreeSet();

        for ( Vni vni : getReservedVnis() )
        {
            takenIds.add( vni.getVlan() );
        }

        for ( int i = Common.MIN_VLAN_ID; i <= Common.MAX_VLAN_ID; i++ )
        {
            if ( !takenIds.contains( i ) )
            {
                return i;
            }
        }

        throw new PeerException( "No available vlan found" );
    }
}