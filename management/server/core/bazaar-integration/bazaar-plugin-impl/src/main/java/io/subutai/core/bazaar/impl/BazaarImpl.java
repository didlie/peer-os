package io.subutai.core.bazaar.impl;


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.subutai.common.dao.DaoManager;
import io.subutai.core.bazaar.api.Bazaar;
import io.subutai.core.bazaar.api.dao.ConfigDataService;
import io.subutai.core.bazaar.api.model.Plugin;
import io.subutai.core.bazaar.impl.dao.ConfigDataServiceImpl;
import io.subutai.core.hubmanager.api.HubPluginException;
import io.subutai.core.hubmanager.api.Integration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BazaarImpl implements Bazaar
{
	private static final Logger LOG = LoggerFactory.getLogger( BazaarImpl.class );
    private Integration integration;
    private DaoManager daoManager;
    private ConfigDataService configDataService;
    private String checksum = "";
	private ScheduledExecutorService sumChecker = Executors.newSingleThreadScheduledExecutor();

    public BazaarImpl( final Integration integration, final DaoManager daoManager )
    {
        this.daoManager = daoManager;
        this.configDataService = new ConfigDataServiceImpl( this.daoManager );
        this.integration = integration;
        LOG.info ("Starting sumchecker");
        this.sumChecker.scheduleWithFixedDelay (new Runnable ()
		{
			@Override
			public void run ()
			{
				try
				{
					LOG.info ("Generating plugins list md5 checksum");
					String productList = getProducts ();
					MessageDigest md = MessageDigest.getInstance ("MD5");
					byte[] bytes = md.digest (productList.getBytes ("UTF-8"));
					StringBuilder hexString = new StringBuilder();

					for (int i = 0; i < bytes.length; i++) {
						String hex = Integer.toHexString(0xFF & bytes[i]);
						if (hex.length() == 1) {
							hexString.append('0');
						}
						hexString.append(hex);
					}

					checksum = hexString.toString();
					LOG.info ("Checksum generated: " + checksum);
				}
				catch (NoSuchAlgorithmException | UnsupportedEncodingException e)
				{
					LOG.error (e.getMessage ());
					e.printStackTrace ();
				}
			}
		}, 1, 3600000, TimeUnit.MILLISECONDS);
	}


	@Override
	public String getChecksum ()
	{
		return this.checksum;
	}

	@Override
    public String getProducts()
    {
        try
        {
            String result = this.integration.getProducts();
            return result;
        }
        catch ( HubPluginException e )
        {
            e.printStackTrace();
        }
        return "";
    }


    @Override
    public List<Plugin> getPlugins()
    {
        return this.configDataService.getPlugins();
    }


    @Override
    public void installPlugin( String name, String version, String kar, String url, String uid ) throws HubPluginException
    {
		this.integration.installPlugin (kar);
        this.configDataService.savePlugin( name, version, kar, url, uid );
    }


    @Override
    public void uninstallPlugin( Long id, String kar, String name )
    {
        this.integration.uninstallPlugin( kar, name );
        this.configDataService.deletePlugin( id );
    }

	@Override
	public void restorePlugin (Long id, String name, String version, String kar, String url, String uid) throws HubPluginException
	{
		this.integration.uninstallPlugin (kar, name);
		this.integration.installPlugin (kar);
		this.configDataService.deletePlugin (id);
		this.configDataService.savePlugin( name, version, kar, url, uid );
	}
}
