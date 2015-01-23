package org.safehaus.subutai.core.env.impl;


import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.safehaus.subutai.common.peer.Peer;
import org.safehaus.subutai.core.env.api.build.NodeGroup;
import org.safehaus.subutai.core.env.api.build.Topology;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


/**
 * Environment Topology
 */
public class TopologyImpl implements Topology
{
    Map<Peer, Set<NodeGroup>> nodeGroupPlacement = Maps.newHashMap();


    @Override
    public Map<Peer, Set<NodeGroup>> getNodeGroupPlacement()
    {
        return Collections.unmodifiableMap( nodeGroupPlacement );
    }


    @Override
    public void addNodeGroupPlacement( Peer peer, NodeGroup nodeGroup )
    {
        Preconditions.checkNotNull( peer, "Invalid peer" );
        Preconditions.checkNotNull( nodeGroup, "Invalid node group" );

        Set<NodeGroup> peerNodeGroups = nodeGroupPlacement.get( peer );

        if ( peerNodeGroups == null )
        {
            peerNodeGroups = Sets.newHashSet();
            nodeGroupPlacement.put( peer, peerNodeGroups );
        }

        peerNodeGroups.add( nodeGroup );
    }
}
