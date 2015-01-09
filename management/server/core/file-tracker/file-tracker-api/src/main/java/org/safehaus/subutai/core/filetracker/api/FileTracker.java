package org.safehaus.subutai.core.filetracker.api;


import java.util.Set;

import org.safehaus.subutai.core.peer.api.Host;


public interface FileTracker
{

    /**
     * Adds listener to be notified on i_notify events
     */
    public void addListener( ConfigPointListener listener );

    /**
     * Removes listener
     */
    public void removeListener( ConfigPointListener listener );

    /**
     * Enables config points to be monitored
     *
     * @param host - target host
     * @param configPoints - set of config points
     */
    public void createConfigPoints( Host host, Set<String> configPoints ) throws FileTrackerException;

    /**
     * Disables config points from being monitored
     *
     * @param host - target host
     * @param configPoints - set of config points
     */
    public void removeConfigPoints( Host host, Set<String> configPoints ) throws FileTrackerException;

    /**
     * Returns monitored config points on host
     *
     * @param host - target host
     *
     * @return - set of monitored config points
     */
    public Set<String> listConfigPoints( Host host ) throws FileTrackerException;
}