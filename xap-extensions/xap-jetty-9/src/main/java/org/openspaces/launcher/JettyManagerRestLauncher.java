package org.openspaces.launcher;

import com.gigaspaces.logger.Constants;
import com.gigaspaces.lrmi.nio.filters.SelfSignedCertificate;
import com.gigaspaces.start.SystemInfo;
import com.gigaspaces.start.manager.XapManagerConfig;
import com.j_spaces.kernel.SystemProperties;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.MovedContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yohana Khoury
 * @since 12.1
 */
public class JettyManagerRestLauncher implements Closeable {
    private static final Logger logger = Logger.getLogger(Constants.LOGGER_MANAGER);

    private AbstractXmlApplicationContext application;
    private Server server;

    public static void main(String[] args) {
        final JettyManagerRestLauncher starter = new JettyManagerRestLauncher();
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
    public JettyManagerRestLauncher() {
        try {
            final XapManagerConfig config = SystemInfo.singleton().getManagerClusterInfo().getCurrServer();
            if (config == null) {
                logger.severe("Cannot start server  - this host is not part of the xap managers configuration");
                System.exit(1);
            }
            String customJettyPath = System.getProperty(SystemProperties.MANAGER_REST_JETTY_CONFIG);
            if (customJettyPath != null) {
                logger.info("Loading jetty configuration from " + customJettyPath);
                this.application = new FileSystemXmlApplicationContext(customJettyPath);
                this.server = this.application.getBean(Server.class);
            } else {
                this.server = new Server();
            }
            if (!server.isStarted()) {
                if (server.getConnectors() == null || server.getConnectors().length == 0) {
                    initConnectors(server, config);
                }
                if (server.getHandler() == null) {
                    initWebApps(server);
                }
                server.start();
            }
            if (logger.isLoggable(Level.INFO)) {
                String connectors = "";
                for (Connector connector : server.getConnectors()) {
                    if (connector instanceof ServerConnector) {
                        String connectorDesc = JettyUtils.toUrlPrefix((ServerConnector) connector);
                        connectors = connectors.isEmpty() ? connectorDesc : connectors + ", " + connectorDesc;
                    }
                }
                logger.info("Started at " + connectors);
            }
        }catch(Exception e){
            logger.log(Level.SEVERE, e.toString(), e);
            System.exit(-1);
        }
    }

    private void initConnectors(Server server, XapManagerConfig config)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        final String host = config.getHost();
        final int port = Integer.parseInt(config.getAdminRest());
        SslContextFactory sslContextFactory = createSslContextFactoryIfNeeded();
        JettyUtils.createConnector(server, host, port, sslContextFactory);
    }

    private void initWebApps(Server server) {
        ContextHandlerCollection handler = new ContextHandlerCollection();
        File webApps = new File(SystemInfo.singleton().locations().getLibPlatform() + "/manager/webapps");
        FilenameFilter warFilesFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".war");
            }
        };

        WebAppContext defaultWebApp = null;
        for (File file : webApps.listFiles(warFilesFilter)) {
            WebAppContext webApp = new WebAppContext();
            webApp.setContextPath("/" + file.getName().replace(".war", ""));
            webApp.setWar(file.getAbsolutePath());
            webApp.setThrowUnavailableOnStartupException(true);
            handler.addHandler(webApp);
            if (defaultWebApp == null)
                defaultWebApp = webApp;
        }

        if (defaultWebApp != null) {
            MovedContextHandler redirectHandler = new MovedContextHandler();
            redirectHandler.setContextPath("/");
            redirectHandler.setNewContextURL(defaultWebApp.getContextPath());
            redirectHandler.setPermanent(true);
            redirectHandler.setDiscardPathInfo(true);
            redirectHandler.setDiscardQuery(true);
            handler.addHandler(redirectHandler);
        }

        server.setHandler(handler);
    }

    private SslContextFactory createSslContextFactoryIfNeeded()
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        boolean sslEnabled = Boolean.getBoolean(SystemProperties.MANAGER_REST_SSL_ENABLED);
        if (!sslEnabled) {
            boolean isSecured = Boolean.getBoolean(SystemProperties.SECURITY_ENABLED);
            if (isSecured) {
                if (System.getProperty(SystemProperties.MANAGER_REST_SSL_ENABLED) == null)
                    throw new SecurityException("Cannot start a secured system without SSL (passwords will be sent over the network without encryption). Please configure SSL (or disable it explicitly)");
                logger.warning("NOTE: Environment security is enabled but SSL was explicitly disabled - passwords will be sent over the network without encryption");
            }
            return null;
        }
        SslContextFactory sslContextFactory = new SslContextFactory();
        String keyStorePath = System.getProperty(SystemProperties.MANAGER_REST_SSL_KEYSTORE_PATH);
        String password = System.getProperty(SystemProperties.MANAGER_REST_SSL_KEYSTORE_PASSWORD);

        if (keyStorePath != null && new File(keyStorePath).exists()) {
            sslContextFactory.setKeyStorePath(keyStorePath);
            sslContextFactory.setKeyStorePassword(password);
        } else {
            sslContextFactory.setKeyStore(SelfSignedCertificate.keystore());
            sslContextFactory.setKeyStorePassword("foo");
            logger.info("SSL Keystore was not provided - Self-signed certificate was generated");
        }

        return sslContextFactory;
    }

    @Override
    public void close() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                logger.warning("Failed to stop server: " + e);
            }
        }
        if (this.application != null)
            this.application.destroy();
    }
}
