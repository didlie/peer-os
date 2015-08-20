package io.subutai.core.security.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.subutai.common.dao.DaoManager;
import io.subutai.core.keyserver.api.KeyServer;
import io.subutai.core.security.api.SecurityManager;
import io.subutai.core.security.api.crypto.EncryptionTool;
import io.subutai.core.security.api.crypto.KeyManager;
import io.subutai.core.security.api.dao.SecurityManagerDAO;
import io.subutai.core.security.impl.crypto.EncryptionToolImpl;
import io.subutai.core.security.impl.crypto.KeyManagerImpl;
import io.subutai.core.security.impl.dao.SecurityManagerDAOImpl;


/**
 * Implementation of SecurityManager
 */
public class SecurityManagerImpl implements SecurityManager
{
    private static final Logger LOG = LoggerFactory.getLogger( SecurityManagerImpl.class );

    private KeyManager keyManager = null;
    private DaoManager daoManager = null;
    private EncryptionTool encryptionTool = null;
    private SecurityManagerDAO securityManagerDAO = null;
    private KeyServer keyServer = null;
    private String secretKeyring;
    private String secretKeyringPwd;

    /********************************
     *
     */
    public void init()
    {
        securityManagerDAO = new SecurityManagerDAOImpl( daoManager );
        encryptionTool     = new EncryptionToolImpl();
        keyManager = new KeyManagerImpl(securityManagerDAO,keyServer,secretKeyring,secretKeyringPwd);
    }


    /********************************
     *
     */
    public void destroy()
    {

    }


    /********************************
     *
     */
    @Override
    public KeyManager getKeyManager()
    {
        return keyManager;
    }


    /********************************
     *
     */
    @Override
    public void setKeyManager( KeyManager keyManager )
    {
        this.keyManager = keyManager;
    }


    /********************************
     *
     */
    public DaoManager getDaoManager()
    {
        return daoManager;
    }


    /********************************
     *
     */
    public void setDaoManager( final DaoManager daoManager )
    {
        this.daoManager = daoManager;
    }


    /********************************
     *
     */
    public SecurityManagerDAO getSecurityManagerDAO()
    {
        return securityManagerDAO;
    }


    /********************************
     *
     */
    public void setSecurityManagerDAO( final SecurityManagerDAO securityManagerDAO )
    {
        this.securityManagerDAO = securityManagerDAO;
    }


    /********************************
     *
     */
    public KeyServer getKeyServer()
    {
        return keyServer;
    }


    /********************************
     *
     */
    public void setKeyServer( final KeyServer keyServer )
    {
        this.keyServer = keyServer;
    }



    /********************************
     *
     */
    public String getSecretKeyring()
    {
        return secretKeyring;
    }



    /********************************
     *
     */
    public void setSecretKeyring( final String secretKeyring )
    {
        this.secretKeyring = secretKeyring;
    }



    /********************************
     *
     */
    public String getSecretKeyringPwd()
    {
        return secretKeyringPwd;
    }


    /********************************
     *
     */
    public void setSecretKeyringPwd( final String secretKeyringPwd )
    {
        this.secretKeyringPwd = secretKeyringPwd;
    }


    /********************************
     *
     */
    public EncryptionTool getEncryptionTool()
    {
        return encryptionTool;
    }


    /********************************
     *
     */
    public void setEncryptionTool( final EncryptionTool encryptionTool )
    {
        this.encryptionTool = encryptionTool;
    }
}
