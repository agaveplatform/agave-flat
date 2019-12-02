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
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.exceptions.RemoteDataSyntaxException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.PermissionType;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
        initializeSftpRelayLogger();
    }

    private SftpClient sftpClient = null;
    private Ssh2Client ssh2 = null;
    private SshClient forwardedConnection = null;

    private String host;
    private int port;
    private String username;
    private String password;
    private String rootDir = "";
    private String homeDir = "";
    private String proxyHost;
    private int proxyPort;
    private String publicKey;
    private String privateKey;
    private SshConnector con;
    private SshAuthentication auth;
    private Map<String, SftpFileAttributes> fileInfoCache = new ConcurrentHashMap<String, SftpFileAttributes>();

    // Not clear what's going on here.  MAX_BUFFER_SIZE is commented out in all
    // but one place--the one place with external visibility.  Defined the jumbo
    // size here to avoid inline magic number usage.  No justification for value.
    private static final int MAX_BUFFER_SIZE = 32768 * 64;           // 2 MB
    private static final int JUMBO_BUFFER_SIZE = 500 * 1024 * 1024;  // 500 MB

    public SftpRelay(String host, int port, String username, String password, String rootDir, String homeDir) {
        this.host = host;
        this.port = port > 0 ? port : 22;
        this.username = username;
        this.password = password;

        updateSystemRoots(rootDir, homeDir);
    }

    public SftpRelay(String host, int port, String username, String password, String rootDir, String homeDir, String proxyHost, int proxyPort) {
        this.host = host;
        this.port = port > 0 ? port : 22;
        this.username = username;
        this.password = password;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;

        updateSystemRoots(rootDir, homeDir);
    }

    public SftpRelay(String host, int port, String username, String password, String rootDir, String homeDir, String publicKey, String privateKey) {
        this.host = host;
        this.port = port > 0 ? port : 22;
        this.username = username;
        this.password = password;
        this.publicKey = publicKey;
        this.privateKey = privateKey;

        updateSystemRoots(rootDir, homeDir);
    }

    public SftpRelay(String host, int port, String username, String password, String rootDir, String homeDir, String proxyHost, int proxyPort, String publicKey, String privateKey) {
        this.host = host;
        this.port = port > 0 ? port : 22;
        this.username = username;
        this.password = password;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.publicKey = publicKey;
        this.privateKey = privateKey;

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

    @Override
    public void authenticate() throws RemoteDataException {
        // clear cache here as we may have stale information in between authentications
        fileInfoCache.clear();

        // Maybe we're already authenticated.
        if (ssh2 != null && ssh2.isConnected() && ssh2.isAuthenticated()) return;

        // Get a new authenticated session.
        Socket sock = null;
        try {
            // Get a connector.
            try {
                con = SshConnector.createInstance();
            } catch (Exception e) {
                String msg = getMsgPrefix() + "Unable to create SshConnector instance: " + e.getMessage();
                log.error(msg, e);
                throw e;
            }

            // Get a component manager.
            JCEComponentManager cm;
            try {
                cm = (JCEComponentManager) ComponentManager.getInstance();
            } catch (Exception e) {
                String msg = getMsgPrefix() + "Unable to create ComponentManager instance: " + e.getMessage();
                log.error(msg, e);
                throw e;
            }

            // Install some ciphers.
            cm.installArcFourCiphers(cm.supportedSsh2CiphersCS());
            cm.installArcFourCiphers(cm.supportedSsh2CiphersSC());

            // Set preferences.
            try {
                ((Ssh2Context) con.getContext()).setPreferredKeyExchange(Ssh2Context.KEX_DIFFIE_HELLMAN_GROUP14_SHA1);

                ((Ssh2Context) con.getContext()).setPreferredPublicKey(Ssh2Context.PUBLIC_KEY_SSHDSS);
                ((Ssh2Context) con.getContext()).setPublicKeyPreferredPosition(Ssh2Context.PUBLIC_KEY_ECDSA_521, 1);

                ((Ssh2Context) con.getContext()).setPreferredCipherCS(Ssh2Context.CIPHER_ARCFOUR_256);
                ((Ssh2Context) con.getContext()).setCipherPreferredPositionCS(Ssh2Context.CIPHER_ARCFOUR, 1);
                ((Ssh2Context) con.getContext()).setCipherPreferredPositionCS(Ssh2Context.CIPHER_AES128_CTR, 1);

                ((Ssh2Context) con.getContext()).setPreferredCipherSC(Ssh2Context.CIPHER_ARCFOUR_256);
                ((Ssh2Context) con.getContext()).setCipherPreferredPositionSC(Ssh2Context.CIPHER_ARCFOUR, 1);
                ((Ssh2Context) con.getContext()).setCipherPreferredPositionCS(Ssh2Context.CIPHER_AES128_CTR, 1);

                ((Ssh2Context) con.getContext()).setPreferredMacCS(Ssh2Context.HMAC_SHA256);
                ((Ssh2Context) con.getContext()).setMacPreferredPositionCS(Ssh2Context.HMAC_SHA1, 1);
                ((Ssh2Context) con.getContext()).setMacPreferredPositionCS(Ssh2Context.HMAC_MD5, 2);

                ((Ssh2Context) con.getContext()).setPreferredMacSC(Ssh2Context.HMAC_SHA256);
                ((Ssh2Context) con.getContext()).setMacPreferredPositionSC(Ssh2Context.HMAC_SHA1, 1);
                ((Ssh2Context) con.getContext()).setMacPreferredPositionSC(Ssh2Context.HMAC_MD5, 2);
            } catch (Exception e) {
                String msg = getMsgPrefix() + "Failure setting a cipher preference: " + e.getMessage();
                log.error(msg, e);
                throw e;
            }

            // Initialize socket.
            SocketAddress sockaddr = null;
            sock = new Socket();
            if (useTunnel()) sockaddr = new InetSocketAddress(proxyHost, proxyPort);
            else sockaddr = new InetSocketAddress(host, port);


            // Configure the socket.
            //  - No delay means send each buffer without waiting to fill a packet.
            //  - The performance preferences mean bandwidth, latency, connection time
            //    are given that priority.
            //
            // Note the original Agave code connected before setting these options,
            // which at least in the case of the performance preferences caused them
            // to be ignored.
            try {
                sock.setTcpNoDelay(true);
                sock.setPerformancePreferences(0, 1, 2);
                sock.connect(sockaddr, 15000);
            } catch (Exception e) {
                String msg = getMsgPrefix() + "Socket connection failure: " + e.getMessage();
                log.error(msg, e);
                throw e; // sock is closed in final catch clause.
            }

            // Use the connected socket to perform the ssh handshake.
            try {
                ssh2 = (Ssh2Client) con.connect(new com.sshtools.net.SocketWrapper(sock), username);
            } catch (Exception e) {
                String msg = getMsgPrefix() + "Failure during ssh initialization: " + e.getMessage();
                log.error(msg, e);
                throw e;
            }

            String[] authenticationMethods;
            try {
                authenticationMethods = ssh2.getAuthenticationMethods(username);
            } catch (Exception e) {
                String msg = getMsgPrefix() + "Failure to get ssh2 authentication methods: " + e.getMessage();
                log.error(msg, e);
                throw e;
            }

            int authStatus;
            if (!StringUtils.isEmpty(publicKey) && !StringUtils.isEmpty(privateKey)) {
                // Authenticate the user using pki authentication.
                auth = new Ssh2PublicKeyAuthentication();

                do {
                    SshPrivateKeyFile pkfile;
                    try {
                        pkfile = SshPrivateKeyFileFactory.parse(privateKey.getBytes());
                    } catch (Exception e) {
                        String msg = getMsgPrefix() + "Failure to parse private key: " + e.getMessage();
                        log.error(msg, e);
                        throw e;
                    }

                    // Create the key pair.
                    SshKeyPair pair;
                    try {
                        if (pkfile.isPassphraseProtected()) pair = pkfile.toKeyPair(password);
                        else pair = pkfile.toKeyPair(null);
                    } catch (Exception e) {
                        String msg = getMsgPrefix() + "Failure to create key pair: " + e.getMessage();
                        log.error(msg, e);
                        throw e;
                    }

                    // Assign keys to auth object.
                    ((PublicKeyAuthentication) auth).setPrivateKey(pair.getPrivateKey());
                    ((PublicKeyAuthentication) auth).setPublicKey(pair.getPublicKey());

                    // Authenticate.
                    try {
                        authStatus = ssh2.authenticate(auth);
                    } catch (Exception e) {
                        String msg = getMsgPrefix() + "Failure to authenticate using key pair: " + e.getMessage();
                        log.error(msg, e);
                        throw e;
                    }

                    // Try to handle interactive session.
                    if (authStatus == SshAuthentication.FURTHER_AUTHENTICATION_REQUIRED &&
                            Arrays.asList(authenticationMethods).contains("keyboard-interactive")) {
                        // Set up MFA request handler.
                        KBIAuthentication kbi = new KBIAuthentication();
                        kbi.setUsername(username);
                        kbi.setKBIRequestHandler(new MultiFactorKBIRequestHandler(password, null, username, host, port));
                        try {
                            authStatus = ssh2.authenticate(kbi);
                        } catch (Exception e) {
                            String msg = getMsgPrefix() + "Failure to MFA authenticate using key pair: " + e.getMessage();
                            log.error(msg, e);
                            throw e;
                        }
                    }
                } while (authStatus != SshAuthentication.COMPLETE &&
                        authStatus != SshAuthentication.FAILED &&
                        authStatus != SshAuthentication.CANCELLED &&
                        ssh2.isConnected());
            } else {
                //Authenticate the user using password authentication.
                auth = new PasswordAuthentication();
                do {
                    ((PasswordAuthentication) auth).setPassword(password);

                    auth = checkForPasswordOverKBI(authenticationMethods);

                    try {
                        authStatus = ssh2.authenticate(auth);
                    } catch (Exception e) {
                        String mfa = (auth instanceof PasswordAuthentication) ? "" : "MFA ";
                        String msg = getMsgPrefix() + "Failure to " + mfa + "authenticate using a password: " + e.getMessage();
                        log.error(msg, e);
                        throw e;
                    }
                } while (authStatus != SshAuthentication.COMPLETE &&
                        authStatus != SshAuthentication.FAILED &&
                        authStatus != SshAuthentication.CANCELLED &&
                        ssh2.isConnected());
            }

            // What happened?
            if (!ssh2.isAuthenticated()) {
                String msg = getMsgPrefix() + "Failed to authenticate.";
                log.error(msg);
                throw new RemoteDataException(msg);
            }

            // Do we need to continue authentication using a proxy?
            if (useTunnel()) {
                SshTunnel tunnel;
                try {
                    tunnel = ssh2.openForwardingChannel(host, port, "127.0.0.1", 22, "127.0.0.1", 22, null, null);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failure open forwarding channel using proxy: " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }

                // Connect using the tunnel.
                try {
                    forwardedConnection = con.connect(tunnel, username);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failure to connect using proxy: " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }

                try {
                    forwardedConnection.authenticate(auth);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failure to authenticate using proxy: " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }
            }
        } catch (Exception e) {
            // All exceptions have previously been caught and logged.
            // We just have to make sure all resources are cleaned up
            // and that the declared exception type is thrown.
            if (ssh2 != null) try {
                ssh2.disconnect();
            } catch (Exception e1) {
            }
            if (forwardedConnection != null) try {
                forwardedConnection.disconnect();
            } catch (Exception e1) {
            }
            if (sock != null) try {
                sock.close();
            } catch (Exception e1) {
            }

            // Null out fields.
            ssh2 = null;
            forwardedConnection = null;
            auth = null;
            con = null;

            // Throw the expected exception.
            if (e instanceof RemoteDataException) throw (RemoteDataException) e;
            else throw new RemoteDataException(e.getMessage(), e);
        }
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
    private SshAuthentication checkForPasswordOverKBI(String[] authenticationMethods) {
        boolean kbiAuthenticationPossible = false;
        for (int i = 0; i < authenticationMethods.length; i++) {
            if (authenticationMethods[i].equals("password")) {
                return auth;
            }
            if (authenticationMethods[i].equals("keyboard-interactive")) {
                kbiAuthenticationPossible = true;
                break;
            }
        }

        if (kbiAuthenticationPossible) {
            KBIAuthentication kbi = new KBIAuthentication();

            kbi.setUsername(username);

            kbi.setKBIRequestHandler(new MultiFactorKBIRequestHandler(password, null, username, host, port));

            return kbi;
        }

        return auth;
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
        return MAX_BUFFER_SIZE;
    }

    protected SftpClient getClient() throws RemoteDataException {
        // Authenticate if necessary.
        if (ssh2 == null || !ssh2.isConnected()) authenticate();

        try {
            if (sftpClient == null || sftpClient.isClosed()) {
                try {
                    if (useTunnel()) sftpClient = new SftpClient(forwardedConnection);
                    else sftpClient = new SftpClient(ssh2);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failure to create sftpClient: " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }

                // set only if the file size is larger than we're comfortable
                // putting in memory. by default this is -1, which means the
                // entire file is read into memory on a get/put
                sftpClient.setMaxAsyncRequests(256);
                sftpClient.setBufferSize(JUMBO_BUFFER_SIZE);
                sftpClient.setTransferMode(SftpClient.MODE_BINARY);
            }

            return sftpClient;
        } catch (Exception e) {
            throw new RemoteDataException(e.getMessage(), e);
        }
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
        } catch (Exception e) {
            String msg = getMsgPrefix() + "Failure to create inputstream for path: " + path + ": " + e.getMessage();
            log.error(msg, e);
            throw e;
        }

        return ins;
    }

    @Override
    public MaverickSFTPOutputStream getOutputStream(String path, boolean passive, boolean append)
            throws IOException, FileNotFoundException, RemoteDataException {
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
            throws IOException, FileNotFoundException, RemoteDataException {
        try {
            remotedir = resolvePath(remotedir);
        } catch (Exception e) {
            String msg = getMsgPrefix() + "Failure to resolve path: " + remotedir + ": " + e.getMessage();
            log.error(msg, e);
            throw e;
        }


        List<RemoteFileInfo> fileList = new ArrayList<RemoteFileInfo>();

        try {
            SftpFile[] files;
            try {
                files = getClient().ls(remotedir);
            } catch (Exception e) {
                String msg = getMsgPrefix() + "Failure to list directory: " + remotedir + ": " + e.getMessage();
                log.error(msg, e);
                throw e;
            }

            // The exception handling here is confused and needs a redesign.
            // For now, we log what's going on where an error occurs.
            for (SftpFile file : files) {
                if (file.getFilename().equals(".") || file.getFilename().equals("..")) continue;
                try {
                    fileList.add(new RemoteFileInfo(file));
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "No such file or directory: " + file.getAbsolutePath() + ": " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }
            }
            Collections.sort(fileList);
            return fileList;
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to list directory " + remotedir, e);
            }
        } catch (Exception e) {
            throw new RemoteDataException("Failed to list directory " + remotedir, e);
        }
    }

    @Override
    public void get(String remotedir, String localdir)
            throws IOException, FileNotFoundException, RemoteDataException {
        get(remotedir, localdir, null);
    }

    @Override
    public void get(String remoteSource, String localdir, RemoteTransferListener listener)
            throws IOException, FileNotFoundException, RemoteDataException {
        try {
            boolean isRemoteDir;
            try {
                isRemoteDir = isDirectory(remoteSource);
            } catch (Exception e) {
                String msg = getMsgPrefix() + "Failure to access remote path " + remoteSource + ": " + e.getMessage();
                log.error(msg, e);
                throw e;
            }


            if (isRemoteDir) {
                File localDirectory = new File(localdir);

                // if local directory is not there
                if (!localDirectory.exists()) {
                    // if parent is not there, throw exception
                    if (!localDirectory.getParentFile().exists()) {
                        String msg = getMsgPrefix() + "Parent directory doesn't exist for local directory " +
                                localDirectory.getAbsolutePath() + ".";
                        log.error(msg);
                        throw new FileNotFoundException("No such file or directory");
                    }
                }
                // can't download folder to an existing file
                else if (!localDirectory.isDirectory()) {
                    String msg = getMsgPrefix() + "Local target " + localDirectory.getAbsolutePath() +
                            " is not a directory to receive content from remote directory " + remoteSource + ".";
                    log.error(msg);
                    throw new RemoteDataException(msg);
                } else {
                    // downloading to existing directory and keeping name
                    localDirectory = new File(localDirectory, FilenameUtils.getName(remoteSource));
                }

                DirectoryOperation operation;
                try {
                    operation = getClient().copyRemoteDirectory(resolvePath(remoteSource), localDirectory.getAbsolutePath(),
                            true, false, true, listener);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failed to copy remote directory " + remoteSource +
                            " to local directory " + localDirectory.getAbsolutePath() + ": " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }

                if (operation != null && !operation.getFailedTransfers().isEmpty()) {
                    String msg = getMsgPrefix() + "Failed to copy at least one file from remote directory " + remoteSource +
                            " to local directory " + localDirectory.getAbsolutePath() + ".";
                    log.error(msg);
                    throw new RemoteDataException(msg);
                }
                if (!localDirectory.exists()) {
                    String msg = getMsgPrefix() + "Failed to copy remote directory " + remoteSource +
                            " to local directory " + localDirectory.getAbsolutePath() + ".";
                    log.error(msg);
                    throw new RemoteDataException(msg);
                }
            } else {
                File localTarget = new File(localdir);

                // verify local path and explicity resolve target path
                if (!localTarget.exists()) {
                    if (!localTarget.getParentFile().exists()) {
                        String msg = getMsgPrefix() + "Parent directory doesn't exist for local file " +
                                localTarget.getAbsolutePath() + ".";
                        log.error(msg);
                        throw new FileNotFoundException("No such file or directory");
                    }
                }
                // if not a directory, overwrite local file
                else if (!localTarget.isDirectory()) {

                }
                // if a directory, resolve full path
                else {
                    localTarget = new File(localTarget, FilenameUtils.getName(remoteSource));
                }

                try {
                    //getClient().get(resolvePath(remoteSource), localTarget.getAbsolutePath(), listener);
                    ManagedChannel channel = ManagedChannelBuilder.forAddress(remoteSource, 50051).usePlaintext().build();

                    // Create an sfpt service client (blocking - synchronous)
                    SftpRelayGrpc.SftpRelayBlockingStub sftpClient = SftpRelayGrpc.newBlockingStub(channel);

                    // create a protocol buffer sftp message
                    Sftp srceSftp = Sftp.newBuilder()
                            .setPassWord(password)
                            .setUsername(username)
                            .setSystemId(host)
                            .setHostPort(String.valueOf(port))
                            .setFileName(remoteSource)
                            .build();

                    Sftp destSftp = Sftp.newBuilder()
                            .setSystemId("localhost")
                            .setFileName(remoteSource)
                            .build();

                    //create a CopyLocalToRemoteRequest
                    SrvGetRequest srvGetRequest = SrvGetRequest.newBuilder()
                            .setSrceSftp(srceSftp)
                            .build();

                    // call the gRPC and get back a CopyLocalToRemoteResponse
                    SrvGetResponse srvGetResponseResponse = sftpClient.get(srvGetRequest);

                    String response = srvGetResponseResponse.getError();
                    String fileName = srvGetResponseResponse.getFileName();
                    String byteCount = srvGetResponseResponse.getBytesReturned();
                    log.info(fileName);
                    log.info(byteCount);
                    log.info(response);
                    if (response.contains("Dialing") || response.contains("creating new client") || response.contains("opening source file")) {
                        throw new RemoteDataException(response);
                    } else {
                        //TODO find another output for the response
                        System.out.println("File: " + srvGetResponseResponse.getFileName() + " Bytes: " + srvGetResponseResponse.getBytesReturned() +  " Errors: " + response);
                    }

                    channel.shutdown();
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failed to copy remote file " + remoteSource +
                            " to local target " + localTarget.getAbsolutePath() + ": " + e.getMessage();
                    log.error(msg, e);
                    throw e;
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
            throws IOException, FileNotFoundException, RemoteDataException {
        append(localpath, remotepath, null);
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#append(java.lang.String, java.lang.String, org.iplantc.service.transfer.RemoteTransferListener)
     */
    @Override
    public void append(String localpath, String remotepath, RemoteTransferListener listener)
            throws IOException, FileNotFoundException, RemoteDataException {
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
            throws IOException, FileNotFoundException, RemoteDataException {
        put(localdir, remotedir, null);
    }

    @Override
    public void put(String localdir, String remotedir, RemoteTransferListener listener)
            throws IOException, FileNotFoundException, RemoteDataException {
        File localFile = new File(localdir);
        if (!localFile.exists()) {
            String msg = getMsgPrefix() + "Local path " + localFile.getAbsolutePath() + " does not exist.";
            log.error(msg);
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
                        String msg = getMsgPrefix() + "Cannot overwrite non-directory " + remotedir + " with directory " + localFile.getAbsolutePath();
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
                    operation = getClient().copyLocalDirectory(localFile.getAbsolutePath(), resolvedPath,
                            true, false, true, listener);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failure to copy local directory " + localFile.getAbsolutePath() +
                            " to " + resolvedPath + ": " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }

                if (operation != null && !operation.getFailedTransfers().isEmpty()) {
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

                // bust cache since this file has now changed
                fileInfoCache.remove(resolvedPath);

                try {

                    //getClient().get(resolvePath(remoteSource), localTarget.getAbsolutePath(), listener);
                    ManagedChannel channel = ManagedChannelBuilder.forAddress("sftp-relay", 50051)
                            .usePlaintext()
                            .build();

                    // Create an sftp service client (blocking - synchronous)

                    SftpRelayGrpc.SftpRelayBlockingStub sftpClient = SftpRelayGrpc.newBlockingStub(channel);

                    // create a protocol buffer sftp message
                    Sftp putConfig = Sftp.newBuilder()
                            .setPassWord(password)
                            .setUsername(username)
                            .setClientKey(privateKey)
                            .setSystemId(host)
                            .setHostPort(String.valueOf(port))
                            .setFileName(localdir)
                            .setDestFileName(resolvedPath)
                            .build();

                    //create a CopyLocalToRemoteRequest
                    SrvPutRequest srvPutRequest = SrvPutRequest.newBuilder()
                            .setSrceSftp(putConfig)
                            .build();

                    // call the gRPC and get back a SrvPutResponse
                    SrvPutResponse copyResponse = sftpClient.put(srvPutRequest);

                    String response = copyResponse.getError();
                    String fileName = copyResponse.getFileName();
                    String byteCount = copyResponse.getBytesReturned();
                    log.info(fileName);
                    log.info(byteCount);
                    log.info(response);
                    if (response.contains("Dialing") || response.contains("creating new client") || response.contains("opening source file")) {
                        throw new RemoteDataException(response);
                    } else {
                        //TODO find another output for the response
                        System.out.println("Result: " + response);
                        System.out.println("File: " + copyResponse.getFileName() + " Bytes: " + copyResponse.getBytesReturned() +  " Errors: " + response);
                    }

                    channel.shutdown();

                    //getClient().put(localFile.getAbsolutePath(), resolvedPath, listener);
                } catch (Exception e) {
                    String msg = getMsgPrefix() + "Failure to write local file " + localFile.getAbsolutePath() +
                            " to " + resolvedPath + ": " + e.getMessage();
                    log.error(msg, e);
                    throw e;
                }
            }
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to put data to " + remotedir, e);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteDataException("Failed to put data to " + remotedir, e);
        }
    }

    @Override
    public void syncToRemote(String localdir, String remotedir, RemoteTransferListener listener)
            throws IOException, FileNotFoundException, RemoteDataException {
        File localFile = new File(localdir);
        if (!localFile.exists()) {
            throw new FileNotFoundException("No such file or directory");
        }

        try {
            // invalidate this now so the existence check isn't stale
            fileInfoCache.remove(resolvePath(remotedir));
            if (!doesExist(remotedir) || !doesExist(remotedir)) {
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
                    if (listener != null && listener.getTransferTask() != null) {
                        TransferTask parentTask = listener.getTransferTask();
                        String srcPath = parentTask.getSource() +
                                (StringUtils.endsWith(parentTask.getSource(), "/") ? "" : "/") +
                                child.getName();
                        childTask = new TransferTask(srcPath,
                                resolvePath(childRemotePath),
                                parentTask.getOwner(),
                                parentTask.getRootTask(),
                                parentTask);
                        TransferTaskDao.persist(childTask);
                    }

                    if (child.isDirectory()) {
                        // local is a directory, remote is a file. delete remote file. we will replace with local directory
                        try {
                            if (isFile(childRemotePath)) {
                                delete(childRemotePath);
                            }
                        } catch (FileNotFoundException e) {
                        }

                        // now create the remote directory
                        mkdir(childRemotePath);

                        // sync the folder now that we've cleaned up
                        syncToRemote(child.getAbsolutePath(), adjustedRemoteDir, childTask == null ? null : new RemoteTransferListener(childTask));
                    } else {
                        syncToRemote(child.getAbsolutePath(), childRemotePath, childTask == null ? null : new RemoteTransferListener(childTask));
                    }
                }
            } else {
                String resolvedPath = resolvePath(remotedir);

                // sync if file is not there
                if (!doesExist(remotedir)) {
                    // bust cache since this file has now changed
                    fileInfoCache.remove(resolvedPath);

                    getClient().put(localFile.getAbsolutePath(), resolvePath(remotedir), listener);
                } else {
                    RemoteFileInfo fileInfo = getFileInfo(remotedir);

                    // if the types mismatch, delete remote, use current
                    if (localFile.isDirectory() && !fileInfo.isDirectory() ||
                            localFile.isFile() && !fileInfo.isFile()) {
                        delete(remotedir);

                        // bust cache since this file has now changed
                        fileInfoCache.remove(resolvedPath);

                        getClient().put(localFile.getAbsolutePath(), resolvedPath, listener);
                    }
                    // or if the file sizes are different
                    else if (localFile.length() != fileInfo.getSize()) {
                        // bust cache since this file has now changed
                        fileInfoCache.remove(resolvedPath);

                        getClient().put(localFile.getAbsolutePath(), resolvePath(remotedir), listener);
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
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to put data to " + remotedir, e);
            }
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
     * @throws SftpStatusException
     * @throws SshException
     * @throws IOException
     * @throws RemoteDataException
     */
    protected SftpFileAttributes stat(String resolvedPath)
            throws SftpStatusException, SshException, IOException, RemoteDataException {
//	    String resolvedPath = resolvePath(remotepath);

        try {

            SftpFileAttributes atts = fileInfoCache.get(resolvedPath);
            if (atts == null) {
                atts = getClient().stat(StringUtils.removeEnd(resolvedPath, "/"));

                // adjust for links so we get info about the referenced file/folder
                if (atts != null && atts.isLink()) {
                    atts = getClient().statLink(resolvedPath);
                }

                if (atts != null) {
                    fileInfoCache.put(resolvedPath, atts);
                }
            }
            return atts;
        } catch (SftpStatusException | SshException | RemoteDataException e) {
            fileInfoCache.remove(resolvedPath);
            throw e;
        }
    }

    @Override
    public boolean isDirectory(String remotepath)
            throws IOException, FileNotFoundException, RemoteDataException {
        try {
            return stat(resolvePath(remotepath)).isDirectory();
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
            }
        } catch (Exception e) {
            throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
        }
    }

    @Override
    public boolean isFile(String remotepath)
            throws IOException, FileNotFoundException, RemoteDataException {
        try {
            return stat(resolvePath(remotepath)).isFile();
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
            }
        } catch (Exception e) {
            throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
        }
    }

    @Override
    public long length(String remotepath)
            throws IOException, FileNotFoundException, RemoteDataException {
        try {
            return stat(resolvePath(remotepath)).getSize().longValue();
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to retrieve length of " + remotepath, e);
            }
        } catch (Exception e) {
            throw new RemoteDataException("Failed to retrieve information about " + remotepath, e);
        }
    }

    @Override
    public String checksum(String remotepath)
            throws IOException, FileNotFoundException, RemoteDataException, NotImplementedException {
        //TODO: What does this do?  It looks like nothing is being done.
        // it most likely needs a checksum if it is not a DIR.
        try {
            if (isDirectory(remotepath)) {
                throw new RemoteDataException("Directory cannot be checksummed.");
            } else {
                throw new NotImplementedException();
            }
        } catch (RemoteDataException e) {
            throw e;
        } catch (NotImplementedException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public void doRename(String oldpath, String newpath)
            throws IOException, FileNotFoundException, RemoteDataException, RemoteDataSyntaxException {
        String resolvedSourcePath = null;
        String resolvedDestPath = null;
        try {
            resolvedSourcePath = resolvePath(oldpath);
            resolvedDestPath = resolvePath(newpath);

//			if (StringUtils.startsWith(resolvedDestPath, resolvedSourcePath)) {
//				throw new RemoteDataException("Cannot rename a file or director into its own subtree");
//			}
            resolvedSourcePath = StringUtils.removeEnd(resolvedSourcePath, "/");
            resolvedDestPath = StringUtils.removeEnd(resolvedDestPath, "/");

            getClient().rename(resolvedSourcePath, resolvedDestPath);
        } catch (RemoteDataException e) {
            throw e;
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else if (doesExistSafe(resolvedDestPath)) {
                throw new RemoteDataException("Destination already exists: " + newpath);
            } else {
                throw new RemoteDataException("Failed to rename " + oldpath + " to " + newpath, e);
            }
        } catch (Exception e) {
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
            throws IOException, FileNotFoundException, RemoteDataException, RemoteDataSyntaxException {
        if (!doesExist(remotesrc)) {
            throw new FileNotFoundException("No such file or directory");
        }

        String resolvedSrc = resolvePath(remotesrc);
        String resolvedDest = resolvePath(remotedest);

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
                log.debug("Performing remote copy on " + host + ": " + copyCommand);

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
                    } catch (Exception e) {
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
//			try { ssh2.disconnect(); } catch (Throwable t) {}
            try {
                shell.close();
            } catch (Throwable t) {
            }
        }
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClient#getUriForPath(java.lang.String)
     */
    @Override
    public URI getUriForPath(String path) throws IOException,
            RemoteDataException {
        try {
            return new URI("sftp://" + host + (port == 22 ? "" : ":" + port) + "/" + path);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void delete(String remotepath) throws IOException, FileNotFoundException, RemoteDataException {
        try {
            String resolvedPath = resolvePath(remotepath);

            // bust cache since this file has now changed
            fileInfoCache.remove(resolvedPath);
            String prefixPath = StringUtils.removeEnd(resolvedPath, "/") + "/";
            for (String path : fileInfoCache.keySet()) {
                if (StringUtils.startsWith(path, prefixPath)) {
                    fileInfoCache.remove(path);
                }
            }

            getClient().rm(resolvedPath, true, true);
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("permission denied")) {
                throw new RemoteDataException("The specified path " + remotepath +
                        " does not exist or the user does not have permission to view it.", e);
            } else if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to delete " + remotepath, e);
            }
        } catch (Exception e) {
            throw new RemoteDataException("Failed to delete " + remotepath, e);
        }
    }

    @Override
    public boolean isThirdPartyTransferSupported() {
        return false;
    }

    @Override
    public boolean mkdirs(String remotedir)
            throws IOException, FileNotFoundException, RemoteDataException {
        String resolvedPath = null;
        try {
            resolvedPath = resolvePath(remotedir);
            fileInfoCache.remove(resolvedPath);

            SftpClient client = getClient();
            client.mkdirs(resolvedPath);

            return client.stat(resolvedPath).isDirectory();
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else if (e.getMessage().toLowerCase().contains("directory already exists")) {
                return false;
            } else if (e.getMessage().toLowerCase().contains("file already exists")) {
                return false;
            } else if (e.getMessage().toLowerCase().contains("permission denied")) {
                throw new RemoteDataException("Cannot create directory " + resolvedPath + ": Permisison denied");
            } else {
                throw new RemoteDataException("Failed to create " + remotedir, e);
            }
        } catch (Exception e) {
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
    public boolean mkdir(String remotedir)
            throws IOException, FileNotFoundException, RemoteDataException {
        String resolvedPath = null;
        try {
            resolvedPath = resolvePath(remotedir);

            fileInfoCache.remove(resolvedPath);

            SftpClient client = getClient();
            client.mkdir(resolvedPath);

            return client.stat(resolvedPath).isDirectory();
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else if (e.getMessage().toLowerCase().contains("directory already exists")) {
                return false;
            } else if (e.getMessage().toLowerCase().contains("file already exists")) {
                return false;
            } else if (e.getMessage().toLowerCase().contains("permission denied")) {
                throw new RemoteDataException("Cannot create directory " + resolvedPath + ": Permisison denied");
            } else {
                throw new RemoteDataException("Failed to create " + remotedir, e);
            }
        } catch (Exception e) {
            throw new RemoteDataException("Failed to create " + remotedir, e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (sftpClient != null) sftpClient.quit();
        } catch (Exception e) {
        }
        try {
            if (forwardedConnection != null) forwardedConnection.disconnect();
        } catch (Exception e) {
        }
        try {
            if (ssh2 != null) ssh2.disconnect();
        } catch (Exception e) {
        }
        ssh2 = null;
        forwardedConnection = null;
        sftpClient = null;
    }

    /**
     * Determine if a resolved path exists without throwing exceptions.
     * If the path exists, true is returned.  If the path does not exist
     * or if the command fails for any reason, false is returned.
     *
     * @param resolvedPath a fully resolved pathname
     * @return true if there's positive confirmation that the path represents
     * an existing object, false otherwise.
     */
    private boolean doesExistSafe(String resolvedPath) {
        // Is it worth trying?
        if (resolvedPath == null) return false;

        // See if we get any attributes back.
        SftpFileAttributes atts = null;
        try {
            atts = stat(resolvedPath);
        } catch (Exception e) {
        }
        if (atts == null) return false;
        else return true;  // object found
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
            SftpFileAttributes atts = stat(resolvedPath);
            return atts != null;
        } catch (IOException e) {
            return false;
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                return false;
            } else {
                String msg = getMsgPrefix() + "stat command failure on path: " + resolvedPath + ": " + e.getMessage();
                log.error(msg, e);
                throw new RemoteDataException(msg, e);
            }
        } catch (SshException e) {
            String msg = getMsgPrefix() + "stat command exception on path: " + resolvedPath + ": " + e.getMessage();
            log.error(msg, e);
            return false;
        }
    }

    @Override
    public List<RemoteFilePermission> getAllPermissionsWithUserFirst(String path, String username)
            throws IOException, FileNotFoundException, RemoteDataException {
        // Return an empty list
        return new ArrayList<RemoteFilePermission>();
    }

    @Override
    public List<RemoteFilePermission> getAllPermissions(String path)
            throws IOException, FileNotFoundException, RemoteDataException {
        // Return an empty list
        return new ArrayList<RemoteFilePermission>();
    }

    @Override
    public PermissionType getPermissionForUser(String username, String path)
            throws IOException, FileNotFoundException, RemoteDataException {
        int mode;
        try {
            mode = stat(resolvePath(path)).getPermissions().intValue();
            Integer pem = Integer.parseInt(Integer.toString(mode, 8), 10);
            pem = pem % 1000;
            pem = pem / 100;

            for (PermissionType type : PermissionType.values()) {
                if (type.getUnixValue() == pem) {
                    return type;
                }
            }

            return PermissionType.NONE;
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to retrieve permissions for user.", e);
            }
        } catch (IOException e) {
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
            SftpFileAttributes attrs = stat(path);
            return true;
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to retrieve permissions for user.", e);
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasWritePermission(String path, String username)
            throws IOException, FileNotFoundException, RemoteDataException {
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
            throws IOException, FileNotFoundException, RemoteDataException {
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
            throws IOException, FileNotFoundException, RemoteDataException {

    }

    @Override
    public void setOwnerPermission(String username, String path, boolean recursive)
            throws IOException, FileNotFoundException, RemoteDataException {
    }

    @Override
    public void setReadPermission(String username, String path, boolean recursive)
            throws IOException, FileNotFoundException, RemoteDataException {
    }

    @Override
    public void removeReadPermission(String username, String path, boolean recursive)
            throws IOException, FileNotFoundException, RemoteDataException {
    }

    @Override
    public void setWritePermission(String username, String path, boolean recursive)
            throws IOException, FileNotFoundException, RemoteDataException {
    }

    @Override
    public void removeWritePermission(String username, String path, boolean recursive)
            throws IOException, FileNotFoundException, RemoteDataException {
    }

    @Override
    public void setExecutePermission(String username, String path, boolean recursive)
            throws IOException, FileNotFoundException, RemoteDataException {
    }

    @Override
    public void removeExecutePermission(String username, String path, boolean recursive)
            throws IOException, FileNotFoundException, RemoteDataException {
    }

    @Override
    public void clearPermissions(String username, String path, boolean recursive)
            throws IOException, FileNotFoundException, RemoteDataException {
    }

    @Override
    public String getPermissions(String path)
            throws IOException, FileNotFoundException, RemoteDataException {
        return null;
    }

    @Override
    public boolean isPermissionMirroringRequired() {
        return false;
    }

    public String escapeResolvedPath(String resolvedPath) {
        String escapedPath = StringUtils.replaceEach(resolvedPath, new String[]{" ", "$"}, new String[]{"\\ ", "\\$"});

        return escapedPath;
    }

    @Override
    public String resolvePath(String path) throws FileNotFoundException {
        if (StringUtils.isEmpty(path)) {
            return StringUtils.stripEnd(homeDir, " ");
        } else if (path.startsWith("/")) {
            path = rootDir + path.replaceFirst("/", "");
        } else {
            path = homeDir + path;
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
        } else if (!path.startsWith(rootDir)) {
            if (!path.equals(StringUtils.removeEnd(rootDir, "/"))) {
                throw new FileNotFoundException("The specified path " + path +
                        " does not exist or the user does not have permission to view it.");
            }
        }
        return StringUtils.stripEnd(path, " ");
    }

    @Override
    public RemoteFileInfo getFileInfo(String remotepath)
            throws RemoteDataException, FileNotFoundException, IOException {
        String resolvedPath = resolvePath(remotepath);

        try {
            SftpFileAttributes atts = stat(resolvedPath);
            return new RemoteFileInfo(resolvedPath, atts);
        } catch (SftpStatusException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                log.error("Failed to stat " + remotepath + " => " + resolvedPath, e);
                throw new FileNotFoundException("No such file or directory");
            } else {
                throw new RemoteDataException("Failed to retrieve information for " + remotepath, e);
            }
        } catch (Exception e) {
            throw new RemoteDataException("Failed to retrieve information for " + remotepath, e);
        }
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
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
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
    private static void initializeSftpRelayLogger() {
        // Create the object that bridges maverick logging to agave logging.
        // Assign a logger to the maverick logger factory has the side effect
        // of enabling maverick logging.
        LoggerFactory.setInstance(MaverickSFTPLogger.getInstance());
    }
}
