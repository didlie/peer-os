package io.subutai.common.environment;


import java.util.UUID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import io.subutai.common.util.JsonUtil;
import io.subutai.hub.share.quota.ContainerCpuResource;
import io.subutai.hub.share.quota.ContainerDiskResource;
import io.subutai.hub.share.quota.ContainerHomeResource;
import io.subutai.hub.share.quota.ContainerQuota;
import io.subutai.hub.share.quota.ContainerRamResource;
import io.subutai.hub.share.quota.ContainerSize;
import io.subutai.hub.share.quota.Quota;
import io.subutai.hub.share.resource.ContainerResourceType;

import static junit.framework.TestCase.assertEquals;


@RunWith( MockitoJUnitRunner.class )
@Ignore
public class NodeSchemeJsonTest
{
    private static final String ENVIRONMENT_ID = UUID.randomUUID().toString();
    private static final String CONTAINER_NAME = "Container1";
    private static final String TEMPLATE_NAME = "master";
    private static final String TEMPLATE_ID = "templateId";

    private String JSON = "{\"quota\":{\"containerSize\":\"CUSTOM\",\"cpuQuota\":25,\"ramQuota\":\"1024MB\","
            + "\"homeQuota\":\"1GB\",\"rootQuota\":\"2GB\",\"varQuota\":\"2GB\",\"optQuota\":\"1GB\"},"
            + "\"templateName\":\"master\",\"name\":\"Container 1\","
            + "\"templateId\":\"a697e70f3fc538b4f4763588a7868388\",\"position\":{\"x\":20,\"y\":20}}";

    private NodeSchema nodeSchema;
    ContainerQuota containerQuota;
    Quota cpuQuota;
    private Quota ramQuota;
    private Quota homeQuota;


    @Before
    public void setUp() throws Exception
    {
        containerQuota = new ContainerQuota( ContainerSize.CUSTOM );
        cpuQuota = new Quota( new ContainerCpuResource( 22 ), 50 );
        containerQuota.add( cpuQuota );
        ramQuota = new Quota( new ContainerRamResource( "1GiB" ), 51 );
        containerQuota.add( ramQuota );
        homeQuota = new Quota( new ContainerHomeResource( "10GiB" ), 52 );
        containerQuota.add( homeQuota );
        nodeSchema = new NodeSchema( CONTAINER_NAME, containerQuota, TEMPLATE_NAME, TEMPLATE_ID );
    }


    @Test
    public void testSerializationDeserialization() throws Exception
    {
        String jsonString = JsonUtil.toJsonString( nodeSchema );
        System.out.println( jsonString );

        final NodeSchema object = JsonUtil.fromJsonString( jsonString, NodeSchema.class );

        assertEquals( "22", object.getQuota().get( ContainerResourceType.CPU ).getAsCpuResource().getWriteValue() );
        assertEquals( 50, object.getQuota().get( ContainerResourceType.CPU ).getThreshold().intValue() );
        assertEquals( "1024", object.getQuota().get( ContainerResourceType.RAM ).getAsRamResource().getWriteValue() );
        assertEquals( 51, object.getQuota().get( ContainerResourceType.RAM ).getThreshold().intValue() );
        assertEquals( "10", object.getQuota().get( ContainerResourceType.HOME ).getAsDiskResource().getWriteValue() );
        assertEquals( 52, object.getQuota().get( ContainerResourceType.HOME ).getThreshold().intValue() );
    }
}