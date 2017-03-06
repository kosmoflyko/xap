package org.openspaces.launcher;

import com.gigaspaces.lrmi.nio.filters.SelfSignedCertificate;
import com.gigaspaces.start.SystemInfo;
import com.gigaspaces.start.manager.XapManagerClusterInfo;
import com.gigaspaces.start.manager.XapManagerConfig;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yohana Khoury
 * @since 12.1
 */
public class Starter implements Closeable {
    private static final Logger logger = Logger.getLogger("com.gigaspaces.rest");

    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final String DEFAULT_PORT = "8091";
    private static final String DEFAULT_HTTPS_PORT = "8090";
    private static final String KEYSTORE_FILE_NAME = System.getenv("XAP_HOME") + "/keystore.ks";

    private ClassPathXmlApplicationContext application;

    public static void main(String[] args) {
        final Starter starter = new Starter();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                starter.close();
            }
        });
    }

    /**
     * NOTE: This ctor is also called via reflection from SystemConfig
     */
    @SuppressWarnings("WeakerAccess")
    public Starter() {
        try {
            generateKeyStore();
            String host = DEFAULT_HOST;
            String port = DEFAULT_PORT;
            final XapManagerClusterInfo managerClusterInfo = SystemInfo.singleton().getManagerClusterInfo();
            if (managerClusterInfo.getServers().length != 0) {
                final XapManagerConfig manager = managerClusterInfo.getCurrServer();
                if (manager == null) {
                    logger.severe("Cannot start server  - this host is not part of the xap managers configuration");
                    System.exit(1);
                }
                host = manager.getHost();
                port = manager.getProperties().getProperty("api", DEFAULT_PORT);
            }
            System.setProperty("com.gs.manager.api.host", host);
            System.setProperty("com.gs.manager.api.port", port);
            System.setProperty("com.gs.manager.api.https.port", DEFAULT_HTTPS_PORT);
            this.application = new ClassPathXmlApplicationContext("jetty.xml");
            Server jetty = (Server) this.application.getBean("jetty");
            for (Connector connector : jetty.getConnectors()) {
                if (connector instanceof SslSelectChannelConnector) {
                    logger.info("REST service running at https://" + connector.getHost() + ":" + connector.getLocalPort() + "/v1");
                } else {
                    logger.info("REST service running at http://" + connector.getHost() + ":" + connector.getLocalPort() + "/v1");
                }
            }
        }catch(Exception e){
            logger.log(Level.SEVERE, e.toString(), e);
            System.exit(-1);
        }
    }

    private void generateKeyStore() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        File keyStoreFile = new File(KEYSTORE_FILE_NAME);
        if (!keyStoreFile.exists()) {
            KeyStore keyStore = SelfSignedCertificate.keystore();
            if (keyStore != null) {
                FileOutputStream fos = new FileOutputStream(KEYSTORE_FILE_NAME);
                keyStore.store(fos, "foo".toCharArray());
                fos.close();
            }
        }
    }

    @Override
    public void close() {
        if (this.application != null) this.application.destroy();
    }
}