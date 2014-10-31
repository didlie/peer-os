package org.safehaus.subutai.plugin.spark.impl;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.tracker.OperationState;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.plugin.spark.api.SparkClusterConfig;
import org.safehaus.subutai.plugin.spark.impl.handler.StopNodeOperationHandler;
import org.safehaus.subutai.plugin.spark.impl.mock.SparkImplMock;


@Ignore
public class StopEnvironmentContainerOperationHandlerTest
{
    private SparkImplMock mock;
    private AbstractOperationHandler handler;


    @Before
    public void setUp()
    {
        mock = new SparkImplMock();
        handler = new StopNodeOperationHandler( mock, "test-cluster", "test-host", true );
    }


    @Test
    public void testWithoutCluster()
    {

        handler.run();

        TrackerOperation po = handler.getTrackerOperation();
        Assert.assertTrue( po.getLog().contains( "not exist" ) );
        Assert.assertEquals( po.getState(), OperationState.FAILED );
    }


    @Test
    public void testWithNotConnectedAgents()
    {
        SparkClusterConfig config = new SparkClusterConfig();
        mock.setClusterConfig( config );
        handler.run();

        TrackerOperation po = handler.getTrackerOperation();
        Assert.assertTrue( po.getLog().contains( "not connected" ) );
        Assert.assertEquals( po.getState(), OperationState.FAILED );
    }
}