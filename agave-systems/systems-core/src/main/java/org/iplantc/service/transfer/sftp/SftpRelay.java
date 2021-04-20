package org.iplantc.service.transfer.sftp;

import com.sshtools.logging.LoggerFactory;
import com.sshtools.publickey.SshPrivateKeyFile;
import com.sshtools.publickey.SshPrivateKeyFileFactory;
import com.sshtools.sftp.*;
import com.sshtools.ssh.PasswordAuthentication;
import com.sshtools.ssh.*;
import com.sshtools.ssh.components.ComponentManager;
import com.sshtools.ssh.components.SshKeyPair;
import com.sshtools.ssh.components.jce.JCEComponentManager;
import com.sshtools.ssh2.KBIAuthentication;
import com.sshtools.ssh2.Ssh2Client;
import com.sshtools.ssh2.Ssh2Context;
import com.sshtools.ssh2.Ssh2PublicKeyAuthentication;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.agaveplatform.transfer.proto.sftp.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.remote.ssh.MaverickSSHSubmissionClient;
import org.iplantc.service.remote.ssh.shell.Shell;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.*;
import org.iplantc.service.transfer.exceptions.*;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.PermissionType;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Generic SFTP client to interact with remote systems.
 *
 * @author dooley
 */
public final class SftpRelay implements RemoteDataClient {
    // Logging.
    private static final Logger log = Logger.getLogger(SftpRelay.class);

    // Set the logging level for the maverick library code.
    // Comment out this static initializer to turn off
    // maverick library logging.
    static {
        initializeMaverickSFTPLogger();
    }

    private SftpClient sftpClient = null;
    private Ssh2Client ssh2 = null;
    private SshClient forwardedConnection = null;

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private String rootDir = "";
    private String homeDir = "";
    private final String proxyHost;
    private final int proxyPort;
    private final String publicKey;
    private final String privateKey;
    private SshConnector con;
    private SshAuthentication auth;
    private final Map<String, RemoteFileInfo> fileInfoCache = new ConcurrentHashMap<String, RemoteFileInfo>();

    // String fragment contained in SshException msg when problem was a socket read timeout
    public static final String CONN_TIMEDOUT = "connection timed out";

    // Socket timeouts
    public static final int CONNECT_TIMEOUT_MS = 20000;  // 20 seconds
    public static final int READ_TIMEOUT_MS    = 120000; // 2 minutes

    public static final int TEST_CONNECT_TIMEOUT_MS = 8000; // 8 seconds
    public static final int TEST_READ_TIMEOUT_MS    = 100;  // .1 seconds

    // Not clear what's going on here.  MAX_BUFFER_SIZE is commented out in all
    // but one place--the one place with external visibility.  Defined the jumbo
    // size here to avoid inline magic number usage.  No justification for value.
    private static final int MAX_BUFFER_SIZE = 32768 * 64;           // 2 MB
    private static final int DEFAULT_BUFFER_SIZE = -1;        // unlimited file size

    ManagedChannel sftpRelayServerManagedChannel;
    SftpRelayGrpc.SftpRelayBlockingStub sftpRelayGrpcClient;
    RemoteSystemConfig gprcRemoteSystemConfig;
    String sftpRelayServerHost;
    int sftpRelayServerPort;


    public SftpRelay(String host, int port, String username, String password, String rootDir, String homeDir) {
        this(host, port, username, password, rootDir, homeDir, null, null);
//        this.host = host;
//        this.port = port > 0 ? port : 22;
//        this.username = username;
//        this.password = password;
//
//        updateSystemRoots(rootDir, homeDir);
    }

    public SftpRelay(String host, int port, String username, String password, String rootDir, String homeDir, String proxyHost, int proxyPort) {
        this(host, port, username, password, rootDir, homeDir, proxyHost, proxyPort, null, null, Settings.SFTP_RELAY_HOST, Settings.SFTP_RELAY_PORT);
//        this.host = host;
//        this.port = port > 0 ? port : 22;
//        this.username = username;
//        this.password = password;
//        this.proxyHost = proxyHost;
//        this.proxyPort = proxyPort;
//
//        updateSystemRoots(rootDir, homeDir);
    }

    public SftpRelay(String host, int port, String username, String password, String rootDir, String homeDir, String publicKey, String privateKey) {
        this(host, port, username, password, rootDir, homeDir, null, 0, publicKey, privateKey, Settings.SFTP_RELAY_HOST, Settings.SFTP_RELAY_PORT);
//        this.host = host;
//        this.port = port > 0 ? port : 22;
//        this.username = username;
//        this.password = password;
//        this.publicKey = publicKey;
//        this.privateKey = privateKey;
//
//        updateSystemRoots(rootDir, homeDir);
    }

    public SftpRelay(String host, int port, String username, String password, String rootDir, String homeDir, String proxyHost, int proxyPort, String publicKey, String privateKey) {
        this(host, port, username, password, rootDir, homeDir, proxyHost, proxyPort, publicKey, privateKey, Settings.SFTP_RELAY_HOST, Settings.SFTP_RELAY_PORT);
//        this.host = host;
//        this.port = port > 0 ? port : 22;
//        this.username = username;
//        this.password = password;
//        this.proxyHost = proxyHost;
//        this.proxyPort = proxyPort;
//        this.publicKey = publicKey;
//        this.privateKey = privateKey;
//
//        updateSystemRoots(rootDir, homeDir);
    }

    public SftpRelay(String host, int port, String username, String password, String rootDir, String homeDir, String proxyHost, int proxyPort, String publicKey, String privateKey, String sftpRelayServerHost, int sftpRelayServerPort) {
        this.host = host;
        this.port = port > 0 ? port : 22;
        this.username = username;
        this.password = password;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.sftpRelayServerHost = sftpRelayServerHost;
        this.sftpRelayServerPort = sftpRelayServerPort;

        updateSystemRoots(rootDir, homeDir);
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#getHomeDir()
     */
    @Override
    public String getHomeDir() {
        return this.homeDir;
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#getRootDir()
     */
    @Override
    public String getRootDir() {
        return this.rootDir;
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#updateSystemRoots(java.lang.String, java.lang.String)
     */
    @Override
    public void updateSystemRoots(String rootDir, String homeDir) {
        rootDir = FilenameUtils.normalize(rootDir);
        rootDir = StringUtils.stripEnd(rootDir, " ");
        if (!StringUtils.isEmpty(rootDir)) {
            this.rootDir = rootDir;
            if (!this.rootDir.endsWith("/")) {
                this.rootDir += "/";
            }
        } else {
            this.rootDir = "/";
        }

        homeDir = FilenameUtils.normalize(homeDir);
        if (!StringUtils.isEmpty(homeDir)) {
            this.homeDir = this.rootDir + homeDir;
            if (!this.homeDir.endsWith("/")) {
                this.homeDir += "/";
            }
        } else {
            this.homeDir = this.rootDir;
        }

        this.homeDir = StringUtils.stripEnd(this.homeDir.replaceAll("/+", "/"), " ");
        this.rootDir = StringUtils.stripEnd(this.rootDir.replaceAll("/+", "/"), " ");
    }

    /**
     * Creates a single instance of a grpc channel to the relay service. We implement this as a class
     * level field rather than a static singleton because we are not entirely sure we'll have load balancing
     * in front of the grpc service. We may route instances of this class to different relay server instances
     * based on tenant, system, etc. This allows us to do that cleanly. If necessary, we can always use the
     * setter to assign the channel from an external pool later on.
     *
     * @return managed channel builder pointing at the sftp relay service loadbalancer with retry enabled
     */
    protected ManagedChannel getGrpcManagedChannel() {
        if (sftpRelayServerManagedChannel == null) {
            sftpRelayServerManagedChannel =
                    ManagedChannelBuilder.forAddress(sftpRelayServerHost, sftpRelayServerPort)
                            .enableRetry()
                            .usePlaintext()
                            .build();
        }

        return sftpRelayServerManagedChannel;
    }

    /**
     * Generates a single client to the relay server. We only need a single client per channel.
     * This is the way.
     *
     * @return
     */
    protected SftpRelayGrpc.SftpRelayBlockingStub getGrpcClient() {

        // Create an sfpt service client (blocking - synchronous)
        if (sftpRelayGrpcClient == null) {
            sftpRelayGrpcClient = SftpRelayGrpc.newBlockingStub(getGrpcManagedChannel());
        }

        return sftpRelayGrpcClient;
    }

    /**
     * Creates a RemoteSystemConfig used for authentication in the grpc requests.
     *
     * @return valid remote system config to pass to the grpc services
     */
    protected RemoteSystemConfig getRemoteSystemConfig() {
        if (gprcRemoteSystemConfig == null) {
            // create a protocol buffer sftp message
            RemoteSystemConfig.Builder builder = RemoteSystemConfig.newBuilder()
                    .setHost(host)
                    .setPort(port)
                    .setUsername(username);

            if (StringUtils.isNotEmpty(privateKey)) builder.setPrivateKey(privateKey);
            if (StringUtils.isNotEmpty(publicKey)) builder.setPublicKey(publicKey);
            if (StringUtils.isNotEmpty(password)) builder.setPassword(password);

            gprcRemoteSystemConfig = builder.build();
        }

        return gprcRemoteSystemConfig;
    }

    @Override
    public void authenticate() throws RemoteDataException {
        // clear cache here as we may have stale information in between authentications
        fileInfoCache.clear();

        SftpRelayGrpc.SftpRelayBlockingStub grpcClient = getGrpcClient();

        AuthenticationCheckRequest authenticationCheckRequest = AuthenticationCheckRequest.newBuilder()
                .setSystemConfig(getRemoteSystemConfig())
                .build();
        EmptyResponse authCheckResponse = grpcClient.authCheck(authenticationCheckRequest);

        if (StringUtils.isNotBlank(authCheckResponse.getError())) {
            throw new RemoteDataException(authCheckResponse.getError());
        }
    }

    public void nativeAuthenticate() throws RemoteDataException {
        // clear cache here as we may have stale information in between authentications
        fileInfoCache.clear();

        // Maybe we're already authenticated.
        if (ssh2 != null && ssh2.isConnected() && ssh2.isAuthenticated()) {
            return;
        }

        SshAuthentication auth;
        Socket sock = null;
        SocketAddress sockaddr = null;

        // Get a new authenticated session.
        try {
            // Make initial socket connection
            // This call can throw a recoverable exception AloeSSHConnection
            sock = connect(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS,
                    username, host, port, proxyHost, proxyPort, getMsgPrefix());
            // Create an SSH connector
            con = createSshConnector(getMsgPrefix());

            // Use the connected socket to perform the ssh handshake.
            try {
                ssh2 = con.connect(new com.sshtools.net.SocketWrapper(sock), username);
            } catch (Exception e) {
                String msg = String.format("Failure during SSH initialization for %s : %s", getMsgPrefix(), e.getMessage());
                log.error(msg, e);

                // We can retry connection failures.
                RemoteConnectionException rex = new RemoteConnectionException(e.getMessage(), e);
                throw rex;
            }

            // Authenticate using either PKI or password
            // This call can throw a recoverable exception AloeSSHAuth
            // If no exception thrown then ssh2.isAuthenticated() will be true
            auth = authenticateByPKIOrPassword(ssh2, username, publicKey, privateKey, password,
                    host, port, proxyHost, proxyPort, getMsgPrefix(), false);

            // Do we need to continue authentication using a proxy?
            if (useTunnel()) {
                try {
                    forwardedConnection = createProxyConnection(ssh2, con, host, port, proxyHost,
                            proxyPort, username, getMsgPrefix());

                    forwardedConnection.authenticate(auth);
                } catch (Exception e) {
                    String msg = String.format("Failure to authenticate using proxy for %s : %s",
                            getMsgPrefix(), e.getMessage());
                    log.error(msg, e);
                    // We can retry auth failures.
                    AuthenticationException rex = new AuthenticationException(e.getMessage(), e);
                    throw rex;
                }
            }
        } catch (Exception e) {
            closeConnections(ssh2, forwardedConnection, null, sock);

            ssh2 = null;
            forwardedConnection = null;

            // All exceptions have previously been caught and logged.
            // Throw the expected exception
            // Allow auth exception through so state can be added later.

//            if (e instanceof AuthenticationException) throw (AuthenticationException) e;
            if (e instanceof RemoteDataException) throw (RemoteDataException) e;
            else throw new RemoteDataException(e.getMessage(), e);
        }
    }

    /**
     * Create a socket, initialize it and connect.
     * If connect fails then throw RemoteConnectionException
     *
     * @param connectTimeout
     * @param socketTimeout
     * @param username
     * @param host
     * @param port
     * @param proxyHost
     * @param proxyPort
     * @param msgPrefix
     * @return
     * @throws RemoteConnectionException
     */
    public Socket connect(int connectTimeout, int socketTimeout, String username, String host,
                          int port, String proxyHost, int proxyPort, String msgPrefix)
            throws RemoteConnectionException {

        //Initialize socket.
        SocketAddress sockaddr;
        Socket retSock = new Socket();

        if (useTunnel()) {
            sockaddr = new InetSocketAddress(proxyHost, proxyPort);
        } else {
            sockaddr = new InetSocketAddress(host, port);
        }

        // Configure the socket and make the connection.
        //  - No delay means send each buffer without waiting to fill a packet.
        //  - The performance preferences mean bandwidth, latency, connection time
        //    are given that priority.
        //  - The connect timeout only applies to socket connection.
        //  - The SO timeout applies to any blocking call on the socket.
        //
        // Note the original Agave code connected before setting these options,
        // which at least in the case of the performance preferences caused them
        // to be ignored.
        // Timeouts may get adjusted on retries.
//        if (log.isDebugEnabled()) {
//            log.debug("Setting connect timeout to " + connectTimeout + "ms.");
//            log.debug("Setting socket timeout to " + socketTimeout + "ms.");
//        }
        try {
            retSock.setTcpNoDelay(true);
            retSock.setPerformancePreferences(0, 1, 2);
            retSock.setSoTimeout(socketTimeout);
            retSock.connect(sockaddr, connectTimeout);
        } catch (Exception e) {
            String msg = String.format("Socket connection failure for %s : %s", msgPrefix, e.getMessage());
            log.error(msg, e);
            // We can retry connection failures.
            RemoteConnectionException rex = new RemoteConnectionException(e.getMessage(), e);
            throw rex; // socket should be closed in a final catch clause.
        }
        return retSock;
    }

    /**
     * Create a Maverick SSH connector.
     *
     * @throws Exception
     */
    public static SshConnector createSshConnector(String msgPrefix)
            throws Exception {
        SshConnector retCon;
        try {
            retCon = SshConnector.createInstance();
        } catch (Exception e) {
            String msg = String.format("Unable to create SSH connector for %s : %s", msgPrefix, e.getMessage());
            log.error(msg, e);
            throw e;
        }

        // Get a component manager.
        JCEComponentManager cm;
        try {
            cm = (JCEComponentManager) ComponentManager.getInstance();
        } catch (Exception e) {
            String msg = String.format("nable to create a component manager for %s : %s", msgPrefix, e.getMessage());
            log.error(msg, e);
            throw e;
        }

        //Install some ciphers.
        cm.installArcFourCiphers(cm.supportedSsh2CiphersCS());
        cm.installArcFourCiphers(cm.supportedSsh2CiphersSC());

        // Set preferences
        try {
            Ssh2Context context = retCon.getContext();
            context.setPreferredKeyExchange(Ssh2Context.KEX_DIFFIE_HELLMAN_GROUP14_SHA1);

            context.setPreferredPublicKey(Ssh2Context.PUBLIC_KEY_SSHDSS);
            context.setPublicKeyPreferredPosition(Ssh2Context.PUBLIC_KEY_ECDSA_521, 1);

            context.setPreferredCipherCS(Ssh2Context.CIPHER_ARCFOUR_256);
            context.setCipherPreferredPositionCS(Ssh2Context.CIPHER_ARCFOUR, 1);
            context.setCipherPreferredPositionCS(Ssh2Context.CIPHER_AES128_CTR, 1);

            context.setPreferredCipherSC(Ssh2Context.CIPHER_ARCFOUR_256);
            context.setCipherPreferredPositionSC(Ssh2Context.CIPHER_ARCFOUR, 1);
            context.setCipherPreferredPositionCS(Ssh2Context.CIPHER_AES128_CTR, 1);

            context.setPreferredMacCS(Ssh2Context.HMAC_SHA256);
            context.setMacPreferredPositionCS(Ssh2Context.HMAC_SHA1, 1);
            context.setMacPreferredPositionCS(Ssh2Context.HMAC_MD5, 2);

            context.setPreferredMacSC(Ssh2Context.HMAC_SHA256);
            context.setMacPreferredPositionSC(Ssh2Context.HMAC_SHA1, 1);
            context.setMacPreferredPositionSC(Ssh2Context.HMAC_MD5, 2);
        } catch (Exception e) {
            String msg = String.format("Failure setting a cipher preference for %s : %s", msgPrefix, e.getMessage());
            log.error(msg, e);
            throw e;
        }
        return retCon;
    }

    /**
     * Given an ssh client perform authentication via PKI or password.
     * On return ssh2Client.isAuthenticated() will always be true.
     * If authentication fails then throw recoverable.AuthenticationException
     *
     * @param ssh2Client
     * @param username
     * @param publicKey
     * @param privateKey
     * @param password
     * @param host
     * @param port
     * @param proxyHost  - needed for capturing state in case of recoverable error
     * @param proxyPort  - needed for capturing state in case of recoverable error
     * @param msgPrefix
     * @param isProxy    - Used for logging only
     * @return
     * @throws Exception
     */
    public SshAuthentication authenticateByPKIOrPassword(Ssh2Client ssh2Client, String username,
                                                         String publicKey, String privateKey, String password,
                                                         String host, int port, String proxyHost, int proxyPort,
                                                         String msgPrefix, boolean isProxy)
            throws AuthenticationException {
        SshAuthentication retAuth;
        String[] authenticationMethods;

        try {
            authenticationMethods = ssh2Client.getAuthenticationMethods(username);
        } catch (Exception e) {
            String msg = String.format("Failure to get ssh2 authentication methods for %s : %s", msgPrefix, e.getMessage());
            log.error(msg, e);
            AuthenticationException rex = new AuthenticationException(e.getMessage(), e);
            throw rex;
        }

        int authStatus;

        if (!StringUtils.isEmpty(publicKey) && !StringUtils.isEmpty(privateKey)) {
            // Authenticate the user using pki authentication
            retAuth = new Ssh2PublicKeyAuthentication();

            SshPrivateKeyFile pkfile;
            try {
                pkfile = SshPrivateKeyFileFactory.parse(privateKey.getBytes());
            } catch (Exception e) {
                String msg = String.format("Failure to parse private key for %s : %s", msgPrefix, e.getMessage());
                log.error(msg, e);
                AuthenticationException rex = new AuthenticationException(e.getMessage(), e);
                throw rex;
            }

            // Create the key pair
            SshKeyPair pair;
            try {
                if (pkfile.isPassphraseProtected()) {
                    pair = pkfile.toKeyPair(password);
                } else {
                    pair = pkfile.toKeyPair(null);

                }
            } catch (Exception e) {
                String msg = String.format("Failed to create key pair for %s : %s", msgPrefix, e.getMessage());
                log.error(msg, e);
                AuthenticationException rex = new AuthenticationException(e.getMessage(), e);
                throw rex;
            }

            // Assign keys to auth object
            ((PublicKeyAuthentication) retAuth).setPrivateKey(pair.getPrivateKey());
            ((PublicKeyAuthentication) retAuth).setPublicKey(pair.getPublicKey());

            do {
                // Authenticate
                try {
                    authStatus = ssh2Client.authenticate(retAuth);
                } catch (Exception e) {
                    String msg;
                    if (!isProxy) {
                        msg = String.format("Failure to authenticate using key pair for %s : %s", msgPrefix, e.getMessage());
                    } else {
                        msg = String.format("Failure to authenticate using proxy for %s : %s", msgPrefix, e.getMessage());
                    }
                    log.error(msg, e);
                    AuthenticationException rex = new AuthenticationException(e.getMessage(), e);
                    throw rex;
                }

                // Try to handle interactive session
                if (authStatus == SshAuthentication.FURTHER_AUTHENTICATION_REQUIRED &&
                        Arrays.asList(authenticationMethods).contains("keyboard-interactive")) {
                    // Starting PKI multifactor authentication for user: <username> host: <host> port: <port>
                    String msg = String.format("Starting PKI multifactor authentication for user: %s host: %s port: %d", username, host, port);
                    log.info(msg);
                    // Set up MFA request handler
                    KBIAuthentication kbi = new KBIAuthentication();
                    kbi.setUsername(username);
                    kbi.setKBIRequestHandler(new MultiFactorKBIRequestHandler(password, null, username, host, port));
                    try {
                        authStatus = ssh2Client.authenticate(kbi);
                        // Multifactor authentication threw no exceptions for user: {0} host: {1} port: {2}
                        msg = String.format("Multifactor authentication threw no exceptions for user: %s host: %s port: %d", username, host, port);
                        log.info(msg);
                    } catch (Exception e) {
                        msg = String.format("Failure to MFA authenticate using key pair for %s : %s", msgPrefix, e.getMessage());
                        log.error(msg, e);
                        AuthenticationException rex = new AuthenticationException(e.getMessage(), e);
                        throw rex;
                    }
                }
            } while (authStatus != SshAuthentication.COMPLETE
                    && authStatus != SshAuthentication.FAILED
                    && authStatus != SshAuthentication.CANCELLED
                    && ssh2Client.isConnected());
        } else {
            // Authenticate the user using password authentication
            retAuth = new PasswordAuthentication();
            do {
                ((PasswordAuthentication) retAuth).setPassword(password);

                retAuth = checkForPasswordOverKBI(retAuth, authenticationMethods, username, password, host, port);
                boolean isMFA = !(retAuth instanceof PasswordAuthentication);

                try {
                    authStatus = ssh2Client.authenticate(retAuth);
                    if (isMFA) {
                        // Multifactor authentication threw no exceptions for user: {0} host: {1} port: {2}
                        String msg = String.format("Multifactor authentication threw no exceptions for user: %s host: %s port: %d ", username, host, port);
                        log.info(msg);
                    }
                } catch (Exception e) {
                    String msg;
                    if (isMFA) {
                        msg = String.format("Failure to MFA authenticate using password for %s : %s", msgPrefix, e.getMessage());
                    } else if (!isProxy) {
                        msg = String.format("Failure to authenticate using password for %s : %s", msgPrefix, e.getMessage());
                    } else {
                        msg = String.format("Failure to authenticate using proxy for %s : %s", msgPrefix, e.getMessage());
                    }
                    log.error(msg, e);
                    AuthenticationException rex = new AuthenticationException(e.getMessage(), e);
                    throw rex;
                }
            }
            while (authStatus != SshAuthentication.COMPLETE
                    && authStatus != SshAuthentication.FAILED
                    && authStatus != SshAuthentication.CANCELLED
                    && ssh2Client.isConnected());
        }

        // If auth has failed log a message and throw recoverable exception
        if (!ssh2Client.isAuthenticated()) {
            String msg = String.format("Authentication failed with session left in state \"%s\" for %s", msgPrefix,
                    getAuthStateText(authStatus));
            log.error(msg);
            AuthenticationException rex = new AuthenticationException(msg);
            throw rex;
        }

        return retAuth;

    }

    /**
     * Create a proxy connection.
     * If connect fails then throw RemoteConnectionException
     *
     * @param sshClient
     * @param con
     * @param host
     * @param port
     * @param proxyHost - needed for capturing state in case of recoverable error
     * @param proxyPort - needed for capturing state in case of recoverable error
     * @param username
     * @param msgPrefix
     * @return
     * @throws RemoteConnectionException
     */
    private SshClient createProxyConnection(SshClient sshClient, SshConnector con,
                                            String host, int port, String proxyHost, int proxyPort,
                                            String username, String msgPrefix)
            throws RemoteConnectionException {
        Ssh2Client retClient;
        SshTunnel tunnel;
        try {
            tunnel = sshClient.openForwardingChannel(host, port, "127.0.0.1", 22, "127.0.0.1", 22, null, null);
        } catch (Exception e) {
            String msg = String.format("Failure open forwarding channel using proxy for %s : %s", msgPrefix, e.getMessage());
            log.error(msg, e);
            RemoteConnectionException rex = new RemoteConnectionException(e.getMessage(), e);
            throw rex;
        }
        // Connect using the tunnel
        try {
            retClient = con.connect(tunnel, username);
        } catch (Exception e) {
            String msg = String.format("Failure to connect using proxy for %s : %s", msgPrefix, e.getMessage());
            log.error(msg, e);
            RemoteConnectionException rex = new RemoteConnectionException(e.getMessage(), e);
            throw rex;
        }
        return retClient;
    }

    /**
     * Capture all the SSH/SFTP connection information. This information can be used by
     * the recovery framework to reissue the connection call.
     *
     * @return map of connection info needed for recovery.
     */
    public static TreeMap<String, String> captureConnectionState(String username, String host, int port,
                                                                 String proxyHost, int proxyPort) {
        TreeMap<String, String> state = new TreeMap<>();
        state.put("loginProtocolType", LoginProtocolType.SSH.name());
        state.put("host", host);
        state.put("port", Integer.toString(port));
        state.put("proxyHost", proxyHost);
        state.put("proxyPort", Integer.toString(proxyPort));
        state.put("username", username);
        return state;
    }

    /**
     * Looks through the supported auth returned from the server and overrides the
     * password auth type if the server only lists keyboard-interactive. This acts
     * as a frontline check to override the default behavior and use our
     * {@link MultiFactorKBIRequestHandler}.
     *
     * @param authenticationMethods
     * @return a {@link SshAuthentication} based on the ordering and existence of auth methods returned from the server.
     */
    private static SshAuthentication checkForPasswordOverKBI(SshAuthentication existingAuth, String[] authenticationMethods, String username,
                                                             String password, String host, int port) {
        SshAuthentication retAuth = existingAuth;
        boolean kbiAuthenticationPossible = false;
        for (String authMethod : authenticationMethods) {
            if (authMethod.equals("password")) {
                return retAuth;
            }
            if (authMethod.equals("keyboard-interactive")) {
                kbiAuthenticationPossible = true;
            }
        }

        if (kbiAuthenticationPossible) {
            KBIAuthentication kbi = new KBIAuthentication();

            kbi.setUsername(username);

            kbi.setKBIRequestHandler(new MultiFactorKBIRequestHandler(password, null, username, host, port));

            // MultiFactorKBIRequestHandler created for user: {0} host: {1} port: {2}
            String msg = String.format("MultiFactorKBIRequestHandler created for user: %s host: %s port: %d", username, host, port);
            log.info(msg);

            return kbi;
        }
        return retAuth;
    }

    private boolean useTunnel() {
        return (!StringUtils.isBlank(proxyHost));
    }

    @Override
    public int getMaxBufferSize() {
        // Why is the max buffer size being returned when it is never used
        // elsewhere in the code?  The jumbo buffer size value that is used
        // below may have been intended here, but there's no indication one
        // way or the other.

        //******************** This is used by the new GRPC server *******************
        return MAX_BUFFER_SIZE;
    }

    /**
     * Returns the currently authenticated SftpClient object or creates a new one on demand.
     *
     * @return authenticated SftpClient
     * @throws RemoteDataException
     */
    protected SftpClient getClient() throws RemoteDataException {
        // Authenticate if necessary.
        if (ssh2 == null || !ssh2.isConnected()) {
            try {
                nativeAuthenticate();
            } catch (RemoteDataException aex) {
                throw aex;
            } catch (Exception e) {
                String msg = String.format("Failed to authenticate for %s", getMsgPrefix());
                throw new RemoteDataException(msg, e);
            }
        }

        try {
            if (sftpClient == null || sftpClient.isClosed()) {
                try {
                    if (useTunnel()) {
                        sftpClient = new SftpClient(forwardedConnection);
                    } else {
                        sftpClient = new SftpClient(ssh2);
                    }
                } catch (Exception e) {
                    String msg = String.format("Failure to create sftpClient for %s : %s", getMsgPrefix(), e.getMessage());
                    log.error(msg, e);
                    throw e;
                }

                // set only if the file size is larger than we're comfortable
                // putting in memory. by default this is -1, which means the
                // entire file is read into memory on a get/put
                sftpClient.setMaxAsyncRequests(256);
                sftpClient.setBufferSize(DEFAULT_BUFFER_SIZE);
                sftpClient.setTransferMode(SftpClient.MODE_BINARY);
            }

            return sftpClient;
        } catch (SshException e) {
            // Throw recoverable AloeSSHConnectionException. This is typically a socket timeout exception
            throw new RemoteConnectionException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RemoteDataException(e.getMessage(), e);
        }
    }

    /**
     * Try to tailor the buffer size the sftp client uses for local file data
     * to something that matches the file size.  We cap the buffer size at a
     * maximum that should give reasonable performance without blowing out JVM
     * memory.
     * <p>
     * This whole thing is a mess given the conflicting comments in the Maverick
     * source code and the lack of justification for buffer sizes in Agave.
     * Despite comments to the contrary, it does not appear that a buffer size
     * of -1 means "the size of the file".  Instead, the code appears to
     * "use the block size when the buffer size is -1".
     * <p>
     * The returned value is either -1 or n, where 0 < n <= MAX_BUFFER_SIZE.
     *
     * @param localFile non-null local file
     * @return the calculated buffer size for this file
     */
    private int getBufferSizeForLocalFile(File localFile) {
        // Default to the initial client buffer size.
        long size = DEFAULT_BUFFER_SIZE;

        // Get the size of the file if possible.
        try {
            size = localFile.length();
        } catch (Exception e) {
            String msg = String.format("Unable to get size of %s. Buffer size cannot be calculated. Using default.",
                    localFile.getAbsolutePath());
            log.error(msg, e);
        }

        // Make sure the value is reasonable.
        int isize = (int) Math.min(size, MAX_BUFFER_SIZE);
        if (isize <= 0) isize = DEFAULT_BUFFER_SIZE;

        return isize;
    }

    @Override
    public MaverickSFTPInputStream getInputStream(String path, boolean passive) throws IOException, RemoteDataException {
        try {
            path = resolvePath(path);
        } catch (Exception e) {
            String msg = getMsgPrefix() + "Failure to resolve input path: " + path + ": " + e.getMessage();
            log.error(msg, e);
            throw e;
        }

        MaverickSFTPInputStream ins;
        try {
            ins = new MaverickSFTPInputStream(getClient(), path);
        } catch (RemoteDataException | FileNotFoundException e) {
            String msg = getMsgPrefix() + "Failure to create inputstream for path: " + path + ": " + e.getMessage();
            log.error(msg, e);
            throw e;
        } catch (Exception e) {
            String msg = getMsgPrefix() + "Failure to create inputstream for path: " + path + ": " + e.getMessage();
            log.error(msg, e);
            throw new RemoteDataException("Failed to open input stream to " + path, e);
        }

        return ins;
    }

    @Override
    public MaverickSFTPOutputStream getOutputStream(String path, boolean passive, boolean append)
            throws IOException, RemoteDataException {
        String resolvedPath;
        try {
            resolvedPath = resolvePath(path);
        } catch (Exception e) {
            String msg = getMsgPrefix() + "Failure to resolve output path: " + path + ": " + e.getMessage();
            log.error(msg, e);
            throw e;
        }
        SftpClient client;
        try {
            client = getClient();
        } catch (Exception e) {
            String msg = getMsgPrefix() + "Failure to get output client. " + e.getMessage();
            log.error(msg, e);
            throw e;
        }

        // workaround because maverick throws an exception if an output stream is opened to
        // a file that does not exist.
        if (!doesExist(path)) {
            try {
                // Upload a file with the content of an empty string.
                ByteArrayInputStream ins = new ByteArrayInputStream("".getBytes());
                client.put(ins, resolvedPath);
            } catch (SftpStatusException e) {
                if (e.getMessage().toLowerCase().contains("no such file")) {
                    String msg = getMsgPrefix() + "No such file or directory: " + path + ": " + e.getMessage();
                    log.error(msg, e);
                    throw new FileNotFoundException(msg);
                } else {
                    String msg = getMsgPrefix() + "Failue to put file: " + path + ": " + e.getMessage();
                    log.error(msg, e);
                    throw new RemoteDataException(msg, e);
                }
            } catch (Exception e) {
                String msg = getMsgPrefix() + "Failue to put file: " + path + ": " + e.getMessage();
                log.error(msg, e);
                throw new RemoteDataException(msg, e);
            }
        }

        MaverickSFTPOutputStream outs;
        try {
            outs = new MaverickSFTPOutputStream(client, resolvedPath);
        } catch (Exception e) {
            String msg = getMsgPrefix() + "Failure to create outputstream for path: " + resolvedPath + ": " + e.getMessage();
            log.error(msg, e);
            throw e;
        }

        return outs;
    }

    @Override
    public List<RemoteFileInfo> ls(String remotedir)
            throws IOException, RemoteDataException {
        try {
            remotedir = resolvePath(remotedir);
        } catch (Exception e) {
            String msg = getMsgPrefix() + "Failure to resolve path: " + remotedir + ": " + e.getMessage();
            log.error(msg, e);
            throw e;
        }


        List<RemoteFileInfo> fileList = new ArrayList<RemoteFileInfo>();

        SftpRelayGrpc.SftpRelayBlockingStub grpcClient = getGrpcClient();

        RemoteSystemConfig remoteSystemConfig = getRemoteSystemConfig();

        // create remote directory listing request
        SrvListRequest relayListRequest = SrvListRequest.newBuilder()
                .setSystemConfig(remoteSystemConfig)
                .setRemotePath(remotedir)
                .build();

        try {
            // call the gRPC and get back a CopyLocalToRemoteResponse
            FileInfoListResponse relayListingResponse = grpcClient.list(relayListRequest);

            if (StringUtils.isNotBlank(relayListingResponse.getError())) {
                String errorMessage = relayListingResponse.getError();
                if (errorMessage.contains("does not exist") || errorMessage.contains("no such file or directory")) {
                    throw new FileNotFoundException(errorMessage);
                } else {
                    throw new RemoteDataException(errorMessage);
                }
            }

            // The exception handling here is confused and needs a redesign.
            // For now, we log what's going on where an error occurs.
            for (org.agaveplatform.transfer.proto.sftp.RemoteFileInfo relayFileInfo : relayListingResponse.getListingList()) {
                if (relayFileInfo.getName().equals(".") || relayFileInfo.getName().equals("..")) continue;
                try {
                    fileList.add(new RemoteFileInfo(relayFileInfo));
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "No such file or directory: " + relayFileInfo.getPath() +
                            ": " + e.getMessage();
                    throw e;
                }
            }
            Collections.sort(fileList);
            return fileList;
        } catch (FileNotFoundException | RemoteDataException e) {
            throw e;
        } catch (Throwable e) {
            throw new RemoteDataException("Failed to list directory " + remotedir, e);
        }
    }

    @Override
    public void get(String remotedir, String localdir)
            throws IOException, RemoteDataException {
        get(remotedir, localdir, null);
    }

    /**
     * Implements the {@link SftpClient#copyRemoteDirectory(String, String, boolean, boolean, boolean, FileTransferProgress)}
     * used to copy a remote directory to the local host. We reimplement the method to leverage the sftp-relay service
     * instead of the Java library.
     *
     * @param remotedir – the unresolved remote directory whose contents will be copied.
     * @param localdir relative local path. This needs to resolve to a path within a directory shared with the sftp-relay container
     * @param recurse – recurse into child folders
     * @param commit – actually perform the operation. If false the operation will be processed and a DirectoryOperation will be returned without actually transfering any files.
     * @param progress callback listener to receive progress updates
     * @return An object containing the results of the directory operation
     * @throws IOException
     * @throws FileNotFoundException
     * @throws SftpStatusException
     * @throws RemoteDataException
     */
    protected DirectoryOperation copyRemoteDirectory(String remotedir,
                                                  String localdir, boolean recurse, boolean commit,
                                                  RemoteTransferListener progress) throws IOException,
            FileNotFoundException, SftpStatusException, SshException, TransferCancelledException, RemoteDataException {

        // Create an operation object to hold the information
        DirectoryOperation op = new DirectoryOperation();

        // Setup the local cwd
        remotedir += ((remotedir.endsWith("/") ? "" : "/"));

        String base = remotedir;

        if (base.endsWith("/"))
            base = base.substring(0, base.length() - 1);

        int idx = base.lastIndexOf('/');

        if (idx != -1) {
            base = base.substring(idx + 1);
        }

        File local = new File(localdir);
        if (!local.exists() && commit) {
            local.mkdirs();
        }

        File f;

        for (RemoteFileInfo file: ls(remotedir)) {

            if (file.isDirectory() && !file.getName().equals(".")
                    && !file.getName().equals("..")) {
                if (recurse) {
                    f = new File(local, file.getName());
                    op.addDirectoryOperation(
                            copyRemoteDirectory(
                                    remotedir + file.getName(),
                                    local.getPath() + "/" + file.getName(), recurse,
                                    commit, progress), f);
                }
            } else if (file.isFile()) {
                f = new File(local, file.getName());

                if (f.exists()
                        && (f.length() == file.getSize())
                        && ((f.lastModified() / 1000) == (file.getLastModified().getTime() / 1000))) {
                    if (commit) {
                        op.getUnchangedFiles().addElement(f);
                    } else {
                        op.getUnchangedFiles().addElement(file);
                    }

                    continue;
                }

                try {

                    if (f.exists()) {
                        if (commit) {
                            op.getUpdatedFiles().addElement(f);
                        } else {
                            op.getUpdatedFiles().addElement(file);
                        }
                    } else {
                        if (commit) {
                            op.getNewFiles().addElement(f);
                        } else {
                            op.getNewFiles().addElement(file);
                        }
                    }

                    if (commit) {
                        // Get the file
                        get(remotedir + file.getName(), f.getPath(), progress);
                    }

                } catch (Exception ex) {
                    op.getFailedTransfers().put(f, ex);
                    throw ex;
                }
            }
        }
        return op;
    }

    @Override
    public void get(String remoteSource, String localdir, RemoteTransferListener listener)
            throws IOException, RemoteDataException {
        try {
            RemoteFileInfo remoteFileInfo = null;
            try {
                remoteFileInfo = getFileInfo(remoteSource);
            } catch (Exception e) {
                String msg = getMsgPrefix() + "Failure to access remote path " + remoteSource + ": " + e.getMessage();
                throw e;
            }


            if (remoteFileInfo.isDirectory()) {
                File localDirectory = new File(localdir);

                // if local directory is not there
                if (!localDirectory.exists()) {
                    // if parent is not there, throw exception
                    if (!localDirectory.getParentFile().exists()) {
                        String msg = getMsgPrefix() + "Parent directory doesn't exist for local directory " +
                                localDirectory.getPath() + ".";
                        throw new FileNotFoundException("No such file or directory");
                    }
                }
                // can't download folder to an existing file
                else if (!localDirectory.isDirectory()) {
                    String msg = getMsgPrefix() + "Cannot overwrite non-directory " + localDirectory.getPath() +
                            " with directory " + remoteSource;
                    throw new InvalidTransferException(msg);
                } else {
                    // downloading to existing directory and keeping name
                    localDirectory = new File(localDirectory, FilenameUtils.getName(remoteSource));
                }

                DirectoryOperation operation;
                try {
                    operation = copyRemoteDirectory(resolvePath(remoteSource), localDirectory.getPath(),
                            true, true, listener);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failed to copy remote directory " + remoteSource +
                            " to local directory " + localDirectory.getPath() + ": " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }

                if (!operation.getFailedTransfers().isEmpty()) {
                    String msg = getMsgPrefix() + "Failed to copy at least one file from remote directory " + remoteSource +
                            " to local directory " + localDirectory.getPath() + ".";
                    log.error(msg);
                    throw new RemoteDataException(msg);
                }
                if (!localDirectory.exists()) {
                    String msg = getMsgPrefix() + "Failed to copy remote directory " + remoteSource +
                            " to local directory " + localDirectory.getPath() + ".";
                    log.error(msg);
                    throw new RemoteDataException(msg);
                }
            } else {
                File localTarget = new File(localdir);

                // verify local path and explicity resolve target path
                if (!localTarget.exists()) {
                    if (!localTarget.getParentFile().exists()) {
                        String msg = getMsgPrefix() + "Parent directory doesn't exist for local file " +
                                localTarget.getPath() + ".";
                        log.error(msg);
                        throw new FileNotFoundException("No such file or directory");
                    }
                }
                // if a directory, resolve full path
                else if (localTarget.isDirectory()) {
                    localTarget = new File(localTarget, FilenameUtils.getName(remoteSource));
                } else {
                    // if not a directory, overwrite local file
                }

                try {
                    String resolvedPath = resolvePath(remoteSource);

                    SftpRelayGrpc.SftpRelayBlockingStub grpcClient = getGrpcClient();

                    RemoteSystemConfig remoteSystemConfig = getRemoteSystemConfig();

                    //create a CopyLocalToRemoteRequest
                    SrvGetRequest srvGetRequest = SrvGetRequest.newBuilder()
                            .setSystemConfig(remoteSystemConfig)
                            .setRemotePath(resolvedPath)
                            .setLocalPath(localTarget.getPath())
                            .setForce(true)
                            .build();

                    if (listener != null) listener.started(remoteFileInfo.getSize(), remoteFileInfo.getName());

                    // call the gRPC and get back a CopyLocalToRemoteResponse
                    TransferResponse transferResponse = grpcClient.get(srvGetRequest);

                    if (StringUtils.isNotBlank(transferResponse.getError())) {
                        String errorMessage = transferResponse.getError();
                        if (listener != null) listener.failed();
                        if (errorMessage.contains("does not exist") || errorMessage.contains("no such file or directory")) {
                            throw new FileNotFoundException(errorMessage);
                        } else {
                            throw new RemoteDataException(errorMessage);
                        }
                    } else {
                        if (listener != null) {
                            listener.progressed(transferResponse.getBytesTransferred());
                            listener.completed();
                        }
                    }
                } catch (FileNotFoundException | RemoteDataException e) {
                    throw e;
                } catch (Throwable e) {
                    String msg = getMsgPrefix() + "Failed to copy remote file " + remoteSource +
                            " to local target " + localTarget.getAbsolutePath() + ": " + e.getMessage();
                    throw new RemoteDataException(msg, e);
                }

                // make sure file transferred
                if (!localTarget.exists()) {
                    String msg = getMsgPrefix() + "Failed to copy remote file " + remoteSource +
                            " to local target " + localTarget.getAbsolutePath() + ".";
                    log.error(msg);
                    throw new RemoteDataException(msg);
                }
                // we could do a size check here...meah
            }
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to get data from " + remoteSource, e);
            }
        } catch (IOException | RemoteDataException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteDataException("Failed to get data from " + remoteSource, e);
        }
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#append(java.lang.String, java.lang.String)
     */
    @Override
    public void append(String localpath, String remotepath)
            throws IOException, RemoteDataException {
        append(localpath, remotepath, null);
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#append(java.lang.String, java.lang.String, org.iplantc.service.transfer.RemoteTransferListener)
     */
    @Override
    public void append(String localpath, String remotepath, RemoteTransferListener listener)
            throws IOException, RemoteDataException {
        File localFile = new File(localpath);
        if (!localFile.exists()) {
            String msg = getMsgPrefix() + "Local path " + localFile.getAbsolutePath() + " does not exist.";
            log.error(msg);
            throw new FileNotFoundException("No such file or directory");
        }

        try {
            boolean remoteExists;
            try {
                remoteExists = !doesExist(remotepath);
            } catch (Exception e) {
                String msg = getMsgPrefix() + "Remote path " + remotepath + " does not exist.";
                log.error(msg);
                throw e;
            }


            if (!remoteExists) {
                put(localpath, remotepath, listener);
            } else if (localFile.isDirectory()) {
                String msg = getMsgPrefix() + "Local path " + localFile.getAbsolutePath() + " cannot be a directory.";
                log.error(msg);
                throw new RemoteDataException(msg);
            } else {
                String resolvedPath;
                try {
                    resolvedPath = resolvePath(remotepath);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failure to resolve path: " + remotepath + ": " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }

                // bust cache since this file has now changed
                fileInfoCache.remove(resolvedPath);
                long position = Math.max(length(resolvedPath) - 1, 0);
                getClient().put(new FileInputStream(localFile), resolvedPath, listener, position);
            }
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to append data to " + remotepath, e);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteDataException("Failed to append data to " + remotepath, e);
        }
    }

    @Override
    public void put(String localdir, String remotedir)
            throws IOException, RemoteDataException {
        put(localdir, remotedir, null);
    }

    public DirectoryOperation copyLocalDirectory(String localFilePath,
                                                 String remotePath, boolean recurse, boolean commit,
                                                 RemoteTransferListener progress) throws IOException,
            SftpStatusException, SshException, TransferCancelledException, RemoteDataException {

        DirectoryOperation op = new DirectoryOperation();

        // ensure trailing slash on resolved path for dir copy
        String remoteResolvedPath = resolvePath(remotePath);
        remoteResolvedPath += (remoteResolvedPath.endsWith("/") ? "" : "/");

        // ensure trailing slash on unresolved path
        remotePath += (remotePath.endsWith("/") ? "" : "/");

        // Setup the remote directory if were committing
        if (commit) {
            try {
                stat(remoteResolvedPath);
            } catch (FileNotFoundException ex) {
                mkdirs(remotePath);
            }
        }

        // List the local files and verify against the remote server
        File localFileItem = new File(localFilePath);

        String[] ls = localFileItem.list();
        File source;
        if (ls != null) {
            for (String localChildPath: ls) {
                source = new File(localFileItem, localChildPath);
                if (source.isDirectory() && !source.getName().equals(".")
                        && !source.getName().equals("..")) {
                    if (recurse) {
                        // File f = new File(local, source.getName());
                        op.addDirectoryOperation(
                                copyLocalDirectory(source.getPath(),
                                        remotePath + source.getName(), recurse,
                                        commit, progress), source);
                    }
                } else if (source.isFile()) {

                    boolean newFile = false;
                    boolean unchangedFile = false;

                    try {
                        RemoteFileInfo remoteFileInfo = stat(remoteResolvedPath + source.getName());

                        // size and last modified must line up
                        unchangedFile = ((source.length() == remoteFileInfo.getSize()) &&
                                ((source.lastModified() / 1000) == (remoteFileInfo.getLastModified().getTime()  / 1000)));

                    } catch (FileNotFoundException | RemoteDataException ex) {
                        newFile = true;
                    }

                    try {

                        if (commit && !unchangedFile) { // BPS - Added
                            // !unChangedFile test.
                            // Why would want to
                            // copy that has been
                            // determined to be
                            // unchanged?
                            put(source.getPath(),
                                    remotePath + source.getName(), progress);
                        }

                        if (unchangedFile) {
                            op.getUnchangedFiles().addElement(source);
                        } else if (!newFile) {
                            op.getUpdatedFiles().addElement(source);
                        } else {
                            op.getNewFiles().addElement(source);
                        }

                    } catch (Exception ex) {
                        op.getFailedTransfers().put(source, ex);
                        throw ex;
                    }
                }
            }
        }

        // Return the operation details
        return op;
    }

    @Override
    public void put(String localdir, String remotedir, RemoteTransferListener listener)
            throws IOException, NotImplementedException, RemoteDataException {

        File localFile = new File(localdir);
        if (!localFile.exists()) {
            String msg = getMsgPrefix() + "Local path " + localdir + " does not exist.";
            throw new FileNotFoundException("No such file or directory");
        }

        try {
            if (localFile.isDirectory()) {
                boolean remoteExists;
                try {
                    remoteExists = doesExist(remotedir);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Remote directory " + remotedir + " does not exist.";
                    log.error(msg, e);
                    throw e;
                }

                // can't upload folder to an existing file
                if (remoteExists) {
                    // can't put dir to file
                    if (!isDirectory(remotedir)) {
                        String msg = getMsgPrefix() + "Cannot overwrite non-directory " + remotedir +
                                " with directory " + localdir;
                        log.error(msg);
                        throw new RemoteDataException(msg);
                    } else {
                        remotedir += (StringUtils.isEmpty(remotedir) ? "" : "/") + localFile.getName();
                    }
                } else if (doesExist(remotedir + (StringUtils.isEmpty(remotedir) ? ".." : "/.."))) {
                    // this folder will be created
                } else {
                    String msg = getMsgPrefix() + "Cannot write non-existent remote directory" + remotedir + ".";
                    log.error(msg);
                    throw new FileNotFoundException("No such file or directory");
                }

                // bust cache since this file has now changed
                String resolvedPath;
                try {
                    resolvedPath = resolvePath(remotedir);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failure to resolve remote path: " + remotedir + ": " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }

                fileInfoCache.remove(resolvedPath);

                DirectoryOperation operation;
                try {
                    operation = copyLocalDirectory(localFile.getPath(), remotedir,
                            true, true, listener);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failure to copy local directory " + localFile.getAbsolutePath() +
                            " to " + resolvedPath + ": " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }

                if (!operation.getFailedTransfers().isEmpty()) {
                    String msg = getMsgPrefix() + "One or more files failed to copy from local directory " +
                            localFile.getAbsolutePath() + " to " + resolvedPath + ".";
                    log.error(msg);
                    throw new RemoteDataException(msg);
                }
            } else {
                String resolvedPath;
                try {
                    resolvedPath = resolvePath(remotedir);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failure to resolve remote path: " + remotedir + ": " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }

                RemoteFileInfo remoteFileInfo = null;
                try {
                    remoteFileInfo = stat(resolvedPath);
                } catch (Exception ignored) {
                }

                // if the file is not present, check the parent directory
                if (remoteFileInfo == null) {
                    // if parent does not exist, throw a FileNotFoundException because the path is bad
                    if (!doesExist(remotedir + (StringUtils.isEmpty(remotedir) ? ".." : "/.."))) {
                        throw new FileNotFoundException("no such file or directory");
                    } else {
                        // if parent exists, dest file path is the new file name, we allow the write to continue as is
                    }
                } else if (remoteFileInfo.isDirectory()) {
                    // if the remote path is a directory, then we write the file into the directory
                    // preserving the original source file name
                    resolvedPath += "/" + localFile.getName();
                } else {
                    // the remote path exists, is a file, and we will just overwrite it.
                }

                // bust cache since this file has now changed
                fileInfoCache.remove(resolvedPath);

                try {
                    SftpRelayGrpc.SftpRelayBlockingStub grpcClient = getGrpcClient();

                    RemoteSystemConfig remoteSystemConfig = getRemoteSystemConfig();

                    //create a SrvPutRequest
                    SrvPutRequest srvPutRequest = SrvPutRequest.newBuilder()
                            .setSystemConfig(remoteSystemConfig)
                            .setRemotePath(resolvedPath)
                            .setLocalPath(localdir)
                            .setForce(true)
                            .setAppend(false)
                            .build();

                    if (listener != null) listener.started(localFile.length(), localdir);

                    // call the gRPC and get back a SrvPutResponse
                    TransferResponse transferResponse = grpcClient.put(srvPutRequest);

                    if (StringUtils.isNotBlank(transferResponse.getError())) {
                        String errorMessage = transferResponse.getError();

                        if (errorMessage.contains("does not exist") || errorMessage.contains("no such file or directory")) {
                            throw new FileNotFoundException(errorMessage);
                        } else {
                            throw new RemoteDataException(errorMessage);
                        }
                    } else {
                        // update the listener with final bytes transferred and mark complete
                        if (listener != null) {
                            listener.progressed(transferResponse.getBytesTransferred());
                            listener.completed();
                        }
                    }

                } catch (FileNotFoundException | RemoteDataException e) {
                    if (listener != null) listener.failed();
                    throw e;
                } catch (Throwable e) {

                    String msg = getMsgPrefix() + "Failure to write local file " + localFile.getAbsolutePath() +
                            " to " + resolvedPath + ": " + e.getMessage();
                    throw new RemoteDataException(msg, e);
                }
            }
        } catch (FileNotFoundException | RemoteDataException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteDataException("Failed to put data to " + remotedir, e);
        }
    }

    @Override
    public void syncToRemote(String localdir, String remotedir, RemoteTransferListener listener)
            throws IOException, RemoteDataException {
        File localFile = new File(localdir);
        if (!localFile.exists()) {
            throw new FileNotFoundException("No such file or directory");
        }

        if ((listener == null)) {
            listener = new RemoteTransferListenerImpl(null);
        }

        try {
            // invalidate this now so the existence check isn't stale
            fileInfoCache.remove(resolvePath(remotedir));
            if (!doesExist(remotedir)) {
                put(localdir, remotedir, listener);
                return;
            } else if (localFile.isDirectory()) {
                String adjustedRemoteDir = remotedir;

                // can't put dir to file
                if (!isDirectory(adjustedRemoteDir)) {
                    delete(adjustedRemoteDir);
                    put(localdir, adjustedRemoteDir, listener);
                    return;
                } else {
                    adjustedRemoteDir += (StringUtils.isEmpty(remotedir) ? "" : "/") + localFile.getName();
                }

                for (File child : localFile.listFiles()) {
                    String childRemotePath = adjustedRemoteDir + "/" + child.getName();
                    TransferTask childTask = null;
                    RemoteTransferListener childRemoteTransferListener = null;
                    if (listener.getTransferTask() != null) {
                        TransferTask parentTask = listener.getTransferTask();
                        String srcPath = parentTask.getSource() +
                                (StringUtils.endsWith(parentTask.getSource(), "/") ? "" : "/") +
                                child.getName();
                        childTask = listener.createAndPersistChildTransferTask(srcPath, resolvePath(childRemotePath));
                    }

                    childRemoteTransferListener = listener.createChildRemoteTransferListener(childTask);

                    if (child.isDirectory()) {
                        // local is a directory, remote is a file. delete remote file. we will replace with local directory
                        try {
                            if (isFile(childRemotePath)) {
                                delete(childRemotePath);
                            }
                        } catch (FileNotFoundException ignored) {}

                        // now create the remote directory
                        mkdir(childRemotePath);

                        // sync the folder now that we've cleaned up
                        syncToRemote(child.getPath(), adjustedRemoteDir, childRemoteTransferListener);
                    } else {
                        syncToRemote(child.getPath(), childRemotePath, childRemoteTransferListener);
                    }
                }
            } else {
                String resolvedPath = resolvePath(remotedir);

                // sync if file is not there
                if (!doesExist(remotedir)) {
                    // bust cache since this file has now changed
                    fileInfoCache.remove(resolvedPath);

                    put(localFile.getPath(), resolvePath(remotedir), listener);
                } else {
                    RemoteFileInfo fileInfo = getFileInfo(remotedir);

                    // if the types mismatch, delete remote, use current
                    if (localFile.isDirectory() && !fileInfo.isDirectory() ||
                            localFile.isFile() && !fileInfo.isFile()) {
                        delete(remotedir);

                        // bust cache since this file has now changed
                        fileInfoCache.remove(resolvedPath);

                        put(localFile.getPath(), resolvedPath, listener);
                    }
                    // or if the file sizes are different
                    else if (localFile.length() != fileInfo.getSize()) {
                        // bust cache since this file has now changed
                        fileInfoCache.remove(resolvedPath);

                        put(localFile.getPath(), resolvePath(remotedir), listener);
                    } else {
                        // manually update the listener since there will be no callback from the underlying
                        // client when we skip this transfer.
                        if (listener != null) {
                            listener.skipped(fileInfo.getSize(), resolvePath(remotedir));
                        }
                        log.debug("Skipping transfer of " + localFile.getPath() + " to " +
                                remotedir + " because file is present and of equal size.");
                    }
                }
            }
//        } catch (SftpStatusException e) {
//            if (e.getMessage().toLowerCase().contains("no such file")) {
//                throw new FileNotFoundException("No such file or directory");
//            } else {
//                throw new RemoteDataException("Failed to put data to " + remotedir, e);
//            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteDataException("Failed to put data to " + remotedir, e);
        }
    }

    /**
     * Internal method to fetch and cache metadata for a remote file/folder.
     * This speeds up most operations as the logic of this adaptor heavily
     * relies on explicit metadata checking for each operation.
     *
     * @param resolvedPath
     * @return
     * @throws FileNotFoundException
     * @throws RemoteDataException
     */
    protected RemoteFileInfo stat(String resolvedPath) throws FileNotFoundException, RemoteDataException {
        try {
            SftpRelayGrpc.SftpRelayBlockingStub grpcClient = getGrpcClient();

            RemoteSystemConfig remoteSystemConfig = getRemoteSystemConfig();

            //create a SrvStatRequest
            SrvStatRequest srvStatRequest = SrvStatRequest.newBuilder()
                    .setSystemConfig(remoteSystemConfig)
                    .setRemotePath(resolvedPath)
                    .build();

            // call the gRPC and get back a SrvPutResponse
            FileInfoResponse fileInfoResponse = grpcClient.stat(srvStatRequest);

            if (StringUtils.isNotBlank(fileInfoResponse.getError())) {
                String errorMessage = fileInfoResponse.getError();
                if (errorMessage.contains("does not exist") || errorMessage.contains("no such file or directory")) {
                    throw new FileNotFoundException(errorMessage);
                } else {
                    throw new RemoteDataException(errorMessage);
                }
            } else {

                //#**********************************************************************************
                // this has to be valid or the whole relay is busted
                RemoteFileInfo grpcFileInfo = new RemoteFileInfo(fileInfoResponse.getRemoteFileInfo());
                grpcFileInfo.setOwner(username);
                grpcFileInfo.updateMode(fileInfoResponse.getRemoteFileInfo().getMode());

                fileInfoCache.put(resolvedPath, grpcFileInfo);

                return grpcFileInfo;
            }
        } catch (FileNotFoundException | RemoteDataException e) {
            fileInfoCache.remove(resolvedPath);
            throw e;
        } catch (Throwable e) {
            fileInfoCache.remove(resolvedPath);
            throw new RemoteDataException("Unknown error calling stat on the relay server");
        }
    }

    @Override
    public boolean isDirectory(String remotepath)
            throws IOException, RemoteDataException {
        try {
            return stat(resolvePath(remotepath)).isDirectory();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
        }
    }

    @Override
    public boolean isFile(String remotepath)
            throws IOException, RemoteDataException {
        try {
            return stat(resolvePath(remotepath)).isFile();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
        }
    }

    @Override
    public long length(String remotepath)
            throws IOException, RemoteDataException {
        try {
            return stat(resolvePath(remotepath)).getSize();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
        }
    }

    @Override
    public String checksum(String remotepath)
            throws IOException, RemoteDataException, NotImplementedException {
        // Checksums are not calculated or guaranteed to be available via sftp.
        // we could calculate them on the fly, but that could be extremely expensive
        // and we would have to do it over and over again to guarantee correctness
        // upon subsequent requests. We punt here until assumptions and/or requirements
        // change for checksums via sftp.
        try {
            if (isDirectory(remotepath)) {
                throw new RemoteDataException("Directory cannot be checksummed.");
            } else {
                throw new NotImplementedException();
            }
        } catch (RemoteDataException | NotImplementedException | IOException e) {
            throw e;
        }
    }

    @Override
    public void doRename(String oldpath, String newpath)
            throws IOException, RemoteDataException, RemoteDataSyntaxException {
        String resolvedSourcePath = null;
        String resolvedDestPath = null;
        try {
            resolvedSourcePath = StringUtils.removeEnd(resolvePath(oldpath), "/");
            resolvedDestPath = StringUtils.removeEnd(resolvePath(newpath), "/");


            SftpRelayGrpc.SftpRelayBlockingStub grpcClient = getGrpcClient();

            RemoteSystemConfig remoteSystemConfig = getRemoteSystemConfig();

            //create a SrvRenameRequest
            SrvRenameRequest srvRenameRequest = SrvRenameRequest.newBuilder()
                    .setSystemConfig(remoteSystemConfig)
                    .setRemotePath(resolvedSourcePath)
                    .setNewName(resolvedDestPath)
                    .build();

            // call the gRPC and get back a SrvPutResponse
            FileInfoResponse renameResponse = grpcClient.rename(srvRenameRequest);

            if (StringUtils.isNotBlank(renameResponse.getError())) {
                String errorMessage = renameResponse.getError();
                if (errorMessage.contains("does not exist") || errorMessage.contains("no such file or directory")) {
                    throw new FileNotFoundException(errorMessage);
                } else if (errorMessage.contains("file or dir exists")) {
                    throw new RemoteDataException("Destination already exists: " + newpath);
                } else {
                    throw new RemoteDataException("Failed to rename " + oldpath + " to " + newpath,
                            new RemoteDataException(errorMessage));
                }
            }

            fileInfoCache.remove(resolvedSourcePath);

        } catch (RemoteDataException | FileNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            throw new RemoteDataException("Failed to rename " + oldpath + " to " + newpath, e);
        }
    }

    /*
     * The SFTP copy method performs a remote copy on a file or folder. Unlike
     * other RemoteDataClients, this call is done purely server side and as such
     * requires no data movement. It should be nearly instantaneous. The down side
     * is that it opens a session to the remote system, so this may run into
     * trouble if the environment isn't stable.
     *
     * @param remotefromdir remote source
     * @param remotetodir remote destination
     * @throws IOException, RemoteDataException
     */
    @Override
    public void copy(String remotefromdir, String remotetodir)
            throws IOException, RemoteDataException, RemoteDataSyntaxException {
        copy(remotefromdir, remotetodir, null);
    }

    /*
     * The SFTP copy method performs a remote copy on a file or folder. Unlike
     * other RemoteDataClients, this call is done purely server side and as such
     * requires no data movement. It should be nearly instantaneous. The down side
     * is that it opens a session to the remote system, so this may run into
     * trouble if the environment isn't stable.
     *
     * @param remotesrc remote source. If a folder, the contents will be copied to the {@code remotedest}
     * @param remotedest remote destination. If a folder, it will receive the contents of the {@code remotesrc}
     * @param listener The listener to update. This will be updated on start and
     * finish with no updated inbetween.
     * @throws IOException, RemoteDataException
     */
    @Override
    public void copy(String remotesrc, String remotedest, RemoteTransferListener listener)
            throws IOException, RemoteDataException, RemoteDataSyntaxException {

        // ensure remote path exists before attempting the copy
        if (!doesExist(remotesrc)) {
            throw new FileNotFoundException("No such file or directory");
        }

        String resolvedSrc = resolvePath(remotesrc);
        String resolvedDest = resolvePath(remotedest);

        // ensure the native streaming client is authenticated in case this is the first
        // use of the native maverick library.
        nativeAuthenticate();

        Shell shell = null;
        try {
            if (ssh2.isAuthenticated()) {
                long remoteDestLength = length(remotesrc);
                if (listener != null) {
                    listener.started(remoteDestLength, remotedest);
                }

                fileInfoCache.remove(resolvedDest);

                if (isDirectory(remotesrc)) {
                    resolvedSrc = StringUtils.stripEnd(resolvedSrc, "/") + "/.";
                }

                String copyCommand = String.format("cp -rLf \"%s\" \"%s\"", resolvedSrc, resolvedDest);
//                log.debug("Performing remote copy on " + host + ": " + copyCommand);

                MaverickSSHSubmissionClient proxySubmissionClient = null;
                String proxyResponse = null;
                try {
                    proxySubmissionClient = new MaverickSSHSubmissionClient(getHost(), port, username,
                            password, proxyHost, proxyPort, publicKey, privateKey);
                    proxyResponse = proxySubmissionClient.runCommand(copyCommand);
                } catch (RemoteDataException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RemoteDataException("Failed to connect to destination server " + getHost() + ":" + port, e);
                } finally {
                    try {
                        proxySubmissionClient.close();
                    } catch (Exception ignored) {
                    }
                }

                if (proxyResponse.length() > 0) {
                    if (listener != null) {
                        listener.failed();
                    }
                    if (StringUtils.containsIgnoreCase(proxyResponse, "No such file or directory")) {
                        throw new FileNotFoundException("No such file or directory");
                    } else if (StringUtils.startsWithIgnoreCase(proxyResponse, "cp:")) {
                        // We use the heuristic that a copy failure due to invalid
                        // user input produces a message that begins with 'cp:'.
                        throw new RemoteDataException("Copy failure: " + proxyResponse.substring(3));
                    } else {
                        throw new RemoteDataException("Failed to perform a remote copy command on " + host + ". " +
                                proxyResponse);
                    }
                } else {
                    if (listener != null) {
                        listener.progressed(remoteDestLength);
                        listener.completed();
                    }
                }

            } else {
                throw new RemoteDataException("Failed to authenticate to remote host");
            }
        } catch (FileNotFoundException | RemoteDataException e) {
            throw e;
        } catch (Throwable t) {
            throw new RemoteDataException("Failed to perform a remote copy command on " + host, t);
        } finally {
            try {
                shell.close();
            } catch (Throwable ignored) {
            }
        }
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#getUriForPath(java.lang.String)
     */
    @Override
    public URI getUriForPath(String path) throws IOException, RemoteDataException {
        try {
            return new URI("sftp://" + host + (port == 22 ? "" : ":" + port) + "/" + path);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void delete(String remotepath) throws IOException, RemoteDataException {
        try {
            String resolvedPath = resolvePath(remotepath);

            SftpRelayGrpc.SftpRelayBlockingStub grpcClient = getGrpcClient();

            RemoteSystemConfig remoteSystemConfig = getRemoteSystemConfig();

            //create a SrvRemoveRequest
            SrvRemoveRequest srvRemoveRequest = SrvRemoveRequest.newBuilder()
                    .setSystemConfig(remoteSystemConfig)
                    .setRemotePath(resolvedPath)
                    .build();

            // call the gRPC and get back a SrvPutResponse
            EmptyResponse emptyResponse = grpcClient.remove(srvRemoveRequest);

            if (StringUtils.isNotBlank(emptyResponse.getError())) {
                String errorMessage = emptyResponse.getError();
                if (errorMessage.contains("does not exist") || errorMessage.contains("no such file or directory")) {
                    throw new FileNotFoundException(errorMessage);
                } else {
                    throw new RemoteDataException(errorMessage);
                }
            }

            // bust cache since this file has now changed
            fileInfoCache.remove(resolvedPath);
            String prefixPath = StringUtils.removeEnd(resolvedPath, "/") + "/";
            for (String path : fileInfoCache.keySet()) {
                if (StringUtils.startsWith(path, prefixPath)) {
                    fileInfoCache.remove(path);
                }
            }
        } catch (FileNotFoundException e) {
            throw e;
        } catch (RemoteDataException e) {
            throw new RemoteDataException("Failed to delete " + remotepath + ": " + e.getMessage());
        } catch (Throwable e) {
            throw new RemoteDataException("Failed to delete " + remotepath, e);
        }
    }

    @Override
    public boolean isThirdPartyTransferSupported() {
        return false;
    }

    @Override
    public boolean mkdirs(String remotedir)
            throws IOException, RemoteDataException {
        String resolvedPath = null;
        try {
            resolvedPath = resolvePath(remotedir);

            SftpRelayGrpc.SftpRelayBlockingStub grpcClient = getGrpcClient();

            RemoteSystemConfig remoteSystemConfig = getRemoteSystemConfig();

            //create a SrvMkdirRequest
            SrvMkdirRequest srvMkdirsRequest = SrvMkdirRequest.newBuilder()
                    .setSystemConfig(remoteSystemConfig)
                    .setRemotePath(resolvedPath)
                    .setRecursive(true)
                    .build();

            // call the gRPC and get back a SrvPutResponse
            FileInfoResponse mkdirsResponse = grpcClient.mkdir(srvMkdirsRequest);

            if (StringUtils.isNotBlank(mkdirsResponse.getError())) {
                String errorMessage = mkdirsResponse.getError().toLowerCase();
                if (errorMessage.contains("does not exist") || errorMessage.contains("no such file or directory")) {
                    throw new FileNotFoundException(mkdirsResponse.getError());
                } else if (errorMessage.contains("not a directory")) {
                    // remote path was a file, so no directory was created
                    return false;
                } else if (errorMessage.contains("permission denied")) {
                    throw new RemoteDataException("Cannot create directory " + resolvedPath + ": " + mkdirsResponse.getError());
                } else {
                    throw new RemoteDataException("Failed to create " + remotedir + ": " + mkdirsResponse.getError());
                }
            } else {
                return mkdirsResponse.getRemoteFileInfo().getIsDirectory();
            }
        } catch (FileNotFoundException | RemoteDataException e) {
            throw e;
        } catch (Throwable e) {
            throw new RemoteDataException("Failed to create " + remotedir, e);
        }
    }

    @Override
    public boolean mkdirs(String remotepath, String authorizedUsername)
            throws IOException, RemoteDataException {
        if (isPermissionMirroringRequired() && StringUtils.isNotEmpty(authorizedUsername)) {
            String pathBuilder = "";
            for (String token : StringUtils.split(remotepath, "/")) {
                if (StringUtils.isEmpty(token)) {
                    continue;
                } else if (StringUtils.isNotEmpty(pathBuilder)) {
                    pathBuilder += "/" + token;
                } else {
                    pathBuilder = (StringUtils.startsWith(remotepath, "/") ? "/" : "") + token;
                }

                if (doesExist(pathBuilder)) {
                    continue;
                } else if (mkdir(pathBuilder)) {
                    setOwnerPermission(authorizedUsername, pathBuilder, true);
                } else {
                    return false;
                }
            }
            return true;
        } else {
            return mkdirs(remotepath);
        }
    }


    @Override
    public boolean mkdir(String remotedir) throws IOException, RemoteDataException {
        String resolvedPath = null;
        try {
            resolvedPath = resolvePath(remotedir);

            SftpRelayGrpc.SftpRelayBlockingStub grpcClient = getGrpcClient();

            RemoteSystemConfig remoteSystemConfig = getRemoteSystemConfig();

            //create a SrvMkdirRequest
            SrvMkdirRequest srvMkdirsRequest = SrvMkdirRequest.newBuilder()
                    .setSystemConfig(remoteSystemConfig)
                    .setRemotePath(resolvedPath)
                    .setRecursive(false)
                    .build();

            // call the gRPC and get back a SrvPutResponse
            FileInfoResponse mkdirsResponse = grpcClient.mkdir(srvMkdirsRequest);

            if (StringUtils.isNotBlank(mkdirsResponse.getError())) {
                String errorMessage = mkdirsResponse.getError().toLowerCase();
                if (errorMessage.contains("already exists")) {
                    return false;
                } else if (errorMessage.contains("does not exist") || errorMessage.contains("no such file or directory")) {
                    throw new FileNotFoundException(mkdirsResponse.getError());
                } else if (errorMessage.contains("not a directory")) {
                    // remote path was a file, so no directory was created
                    return false;
                } else if (errorMessage.contains("permission denied")) {
                    throw new RemoteDataException("Cannot create directory " + resolvedPath + ": " + mkdirsResponse.getError());
                } else {
                    throw new RemoteDataException("Failed to create " + remotedir + ": " + mkdirsResponse.getError());
                }
            } else {
                return mkdirsResponse.getRemoteFileInfo().getIsDirectory();
            }
        } catch (FileNotFoundException | RemoteDataException e) {
            throw e;
        } catch (Throwable e) {
            throw new RemoteDataException("Failed to create " + remotedir, e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (sftpClient != null) sftpClient.quit();
        } catch (Exception ignored) {
        }
        try {
            if (forwardedConnection != null) forwardedConnection.disconnect();
        } catch (Exception ignored) {
        }
        try {
            if (ssh2 != null) ssh2.disconnect();
        } catch (Exception ignored) {
        }

        ssh2 = null;
        forwardedConnection = null;
        sftpClient = null;
        try {
            if (sftpRelayServerManagedChannel != null) {
                sftpRelayServerManagedChannel.shutdownNow();
                sftpRelayServerManagedChannel.awaitTermination(13, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {}
        sftpRelayServerManagedChannel = null;
        sftpRelayGrpcClient = null;
        gprcRemoteSystemConfig = null;

        // clear the local file info cache
        fileInfoCache.clear();
    }

    @Override
    public boolean doesExist(String path) throws IOException, RemoteDataException {
        String resolvedPath;
        try {
            resolvedPath = resolvePath(path);
        } catch (Exception e) {
            String msg = getMsgPrefix() + "Failure to resolve path: " + path + ": " + e.getMessage();
            log.error(msg, e);
            throw e;
        }

        try {
            stat(resolvedPath);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    @Override
    public List<RemoteFilePermission> getAllPermissionsWithUserFirst(String path, String username)
            throws IOException, RemoteDataException {
        // Return an empty list
        return new ArrayList<RemoteFilePermission>();
    }

    @Override
    public List<RemoteFilePermission> getAllPermissions(String path)
            throws IOException, RemoteDataException {
        // Return an empty list
        return new ArrayList<RemoteFilePermission>();
    }

    @Override
    public PermissionType getPermissionForUser(String username, String path)
            throws IOException, RemoteDataException {
        int mode;
        try {
            RemoteFileInfo fileInfo = stat(resolvePath(path));
            PermissionType pem = PermissionType.NONE;

            if (fileInfo.userCanRead()) pem.add(PermissionType.READ);
            if (fileInfo.userCanWrite()) pem.add(PermissionType.WRITE);
            if (fileInfo.userCanExecute()) pem.add(PermissionType.EXECUTE);

            return pem;
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteDataException("Failed to retrieve permissions for user.", e);
        }
    }

    @SuppressWarnings("unused")
    @Override
    public boolean hasReadPermission(String path, String username)
            throws FileNotFoundException, RemoteDataException {
        // If the file is located under the root direectory and exists on the server, return true
        try {
            path = resolvePath(path);

            // check file exists
            stat(path);
            return true;
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasWritePermission(String path, String username)
            throws IOException, RemoteDataException {
        // If the file is located under the home direectory and exists on the server, return true
        try {
            // check file exists
            RemoteFileInfo fileInfo = getFileInfo(path);
            return fileInfo.userCanWrite();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasExecutePermission(String path, String username)
            throws IOException, RemoteDataException {
        // If the file is located under the home direectory and exists on the server, return true
        try {
            // check file exists
            RemoteFileInfo fileInfo = getFileInfo(path);
            return fileInfo.userCanExecute();

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setPermissionForUser(String username, String path, PermissionType type, boolean recursive)
            throws IOException, RemoteDataException {

    }

    @Override
    public void setOwnerPermission(String username, String path, boolean recursive)
            throws IOException, RemoteDataException {
    }

    @Override
    public void setReadPermission(String username, String path, boolean recursive)
            throws IOException, RemoteDataException {
    }

    @Override
    public void removeReadPermission(String username, String path, boolean recursive)
            throws IOException, RemoteDataException {
    }

    @Override
    public void setWritePermission(String username, String path, boolean recursive)
            throws IOException, RemoteDataException {
    }

    @Override
    public void removeWritePermission(String username, String path, boolean recursive)
            throws IOException, RemoteDataException {
    }

    @Override
    public void setExecutePermission(String username, String path, boolean recursive)
            throws IOException, RemoteDataException {
    }

    @Override
    public void removeExecutePermission(String username, String path, boolean recursive)
            throws IOException, RemoteDataException {
    }

    @Override
    public void clearPermissions(String username, String path, boolean recursive)
            throws IOException, RemoteDataException {
    }

    @Override
    public String getPermissions(String path)
            throws IOException, RemoteDataException {
        return null;
    }

    @Override
    public boolean isPermissionMirroringRequired() {
        return false;
    }

    public String escapeResolvedPath(String resolvedPath) {
        return StringUtils.replaceEach(resolvedPath, new String[]{" ", "$"}, new String[]{"\\ ", "\\$"});
    }

    @Override
    public String resolvePath(String path) throws FileNotFoundException {
        if (StringUtils.isEmpty(path)) {
            return StringUtils.stripEnd(getHomeDir(), " ");
        } else if (path.startsWith("/")) {
            path = getRootDir() + path.replaceFirst("/", "");
        } else {
            path = getHomeDir() + path;
        }

        String adjustedPath = path;
        if (adjustedPath.endsWith("/..") || adjustedPath.endsWith("/.")) {
            adjustedPath += File.separator;
        }

        if (adjustedPath.startsWith("/")) {
            path = org.codehaus.plexus.util.FileUtils.normalize(adjustedPath);
        } else {
            path = FilenameUtils.normalize(adjustedPath);
        }

        if (path == null) {
            throw new FileNotFoundException("The specified path " + path +
                    " does not exist or the user does not have permission to view it.");
        } else if (!path.startsWith(getRootDir())) {
            if (!path.equals(StringUtils.removeEnd(getRootDir(), "/"))) {
                throw new FileNotFoundException("The specified path " + path +
                        " does not exist or the user does not have permission to view it.");
            }
        }
        return StringUtils.stripEnd(path, " ");
    }

    @Override
    public RemoteFileInfo getFileInfo(String remotepath) throws RemoteDataException, FileNotFoundException {

        String resolvedPath = resolvePath(remotepath);

        return stat(resolvedPath);
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getHost() {
        return host;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SftpRelay other = (SftpRelay) obj;
        if (homeDir == null) {
            if (other.homeDir != null)
                return false;
        } else if (!homeDir.equals(other.homeDir))
            return false;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (port != other.port)
            return false;
        if (proxyHost == null) {
            if (other.proxyHost != null)
                return false;
        } else if (!proxyHost.equals(other.proxyHost))
            return false;
        if (proxyPort != other.proxyPort)
            return false;
        if (rootDir == null) {
            if (other.rootDir != null)
                return false;
        } else if (!rootDir.equals(other.rootDir))
            return false;
        if (username == null) {
            return other.username == null;
        } else return username.equals(other.username);
    }

    /**
     * Error message prefix generator that captures parameters to this
     * instance when an error occurs.
     *
     * @return the prefix to a log message
     */
    private String getMsgPrefix() {
        String s = this.getClass().getSimpleName() + " [";
        s += "host=" + host;
        s += ", port=" + port;
        s += ", usename=" + username;
        s += ", password=" + (StringUtils.isBlank(password) ? "" : "***");
        s += ", rootDir=" + rootDir;
        s += ", homeDir=" + homeDir;
        s += ", proxyHost=" + proxyHost;
        s += ", proxyPort=" + proxyPort;
        s += ", publicKey=" + (StringUtils.isBlank(publicKey) ? "" : "***");
        s += ", privateKey=" + (StringUtils.isBlank(privateKey) ? "" : "***");
        s += "]: ";
        return s;
    }

    /**
     * This method initializes maverick library logging to use the agave
     * log as the ultimate target.  By default, the maverick library does
     * not log.  Setting the log level to any of the 3 supported levels
     * (ERROR, INFO, DEBUG) enables logging in the library.
     */
    private static void initializeMaverickSFTPLogger() {
        // Create the object that bridges maverick logging to agave logging.
        // Assign a logger to the maverick logger factory has the side effect
        // of enabling maverick logging.
        LoggerFactory.setInstance(MaverickSFTPLogger.getInstance());
    }

    /**
     * Return the textual name associated with the states defined in the interface
     * com.sshtools.ssh.SshAuthentication.
     *
     * @param authState the authentication state returned from ssh2Client.nativeAuthenticate();
     * @return the text name associated with the state
     */
    public String getAuthStateText(int authState) {
        switch (authState) {
            case 1:
                return "COMPLETE";
            case 2:
                return "FAILED";
            case 3:
                return "FURTHER_AUTHENTICATION_REQUIRED";
            case 4:
                return "CANCELLED";
            case 5:
                return "PUBLIC_KEY_ACCEPTABLE";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Close all connections, log warnings for exceptions
     *
     * @param sshClient
     * @param forwardedConnection
     * @param sock
     */
    public void closeConnections(SshClient sshClient, SshClient forwardedConnection,
                                 SshSession sshSession, Socket sock) {
        if (sshClient != null) try {
            sshClient.disconnect();
        } catch (Throwable t) {
            String msg = "sshClient disconnect failure.";
            log.warn(msg, t);
        }
        if (forwardedConnection != null) try {
            forwardedConnection.disconnect();
        } catch (Throwable t) {
            String msg = "forwardedConnection disconnect failure.";
            log.warn(msg, t);
        }
        if (sshSession != null) try {
            sshSession.close();
        } catch (Throwable t) {
            String msg = "sshSession close failure.";
            log.warn(msg, t);
        }
        if (sock != null) try {
            sock.close();
        } catch (Throwable t) {
            String msg = "socket close failure.";
            log.warn(msg, t);
        }
    }

    public String getSftpRelayServerHost() {
        return sftpRelayServerHost;
    }

    public void setSftpRelayServerHost(String sftpRelayServerHost) {
        this.sftpRelayServerHost = sftpRelayServerHost;
    }

    public int getSftpRelayServerPort() {
        return sftpRelayServerPort;
    }

    public void setSftpRelayServerPort(int sftpRelayServerPort) {
        this.sftpRelayServerPort = sftpRelayServerPort;
    }

    public ManagedChannel getSftpRelayServerManagedChannel() {
        return sftpRelayServerManagedChannel;
    }

    public void setSftpRelayServerManagedChannel(ManagedChannel sftpRelayServerManagedChannel) {
        this.sftpRelayServerManagedChannel = sftpRelayServerManagedChannel;
    }

    public SftpRelayGrpc.SftpRelayBlockingStub getSftpRelayGrpcClient() {
        return sftpRelayGrpcClient;
    }

    public void setSftpRelayGrpcClient(SftpRelayGrpc.SftpRelayBlockingStub sftpRelayGrpcClient) {
        this.sftpRelayGrpcClient = sftpRelayGrpcClient;
    }
}
