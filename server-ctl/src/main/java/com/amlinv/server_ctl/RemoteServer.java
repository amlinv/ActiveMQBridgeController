package com.amlinv.server_ctl;

import com.amlinv.server_ctl.jsch.Log4jAdapter;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by art on 5/4/15.
 */
public class RemoteServer {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RemoteServer.class);

    private Logger log = DEFAULT_LOGGER;
    private Logger jschLogger = LoggerFactory.getLogger("com.jcraft.jsch");

    private String server;
    private String username;
    private int sshPort = 22;

    private String command;
//    private String executable;

    private int connectTimeout = 60000;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void start () throws JSchException, IOException {
        JSch jsch = new JSch();

        jsch.setKnownHosts(new File(new File(System.getProperty("user.home"), ".ssh"), "known_hosts").getAbsolutePath());
        jsch.setLogger(new Log4jAdapter(this.jschLogger));

        this.addIdentityIfExists(new File(new File(System.getProperty("user.home"), ".ssh"), "id_dsa"), jsch);
        this.addIdentityIfExists(new File(new File(System.getProperty("user.home"), ".ssh"), "id_rsa"), jsch);


        Session sshSession = jsch.getSession(this.username, this.server, this.sshPort);

        sshSession.connect(connectTimeout);

        boolean shell = false;

        if ( shell ) {
            this.startShell(sshSession);
        } else {
            this.execCommand(sshSession);
            sshSession.disconnect();
        }
    }

    protected void startShell (Session sshSession) throws JSchException {
        ChannelShell channelShell = (ChannelShell) sshSession.openChannel("shell");

        channelShell.setInputStream(System.in);
        channelShell.setOutputStream(System.out);

        channelShell.connect();
        channelShell.getExitStatus();
    }

    protected void execCommand (Session sshSession) throws JSchException, IOException {
        ChannelExec channelExec = (ChannelExec) sshSession.openChannel("exec");
        channelExec.setCommand(this.command);

        InputStream inputFromCmd = channelExec.getInputStream();

        Thread remoteOutputProcessThread = new Thread(new RemoteServerOutputProcessor(inputFromCmd));
        remoteOutputProcessThread.start();

        channelExec.setInputStream(System.in);
        channelExec.setOutputStream(System.out);

        channelExec.connect();

        try {
            remoteOutputProcessThread.join();
        } catch ( InterruptedException intExc ) {
            intExc.printStackTrace();
        }

        channelExec.disconnect();
    }

    protected void addIdentityIfExists (File path, JSch jsch) throws JSchException {
        if ( path.exists() ) {
            jsch.addIdentity(path.getAbsolutePath());
        }
    }

    protected class RemoteServerOutputProcessor implements Runnable {
        private final InputStream receiveFromRemoteInputStream;

        public RemoteServerOutputProcessor(InputStream receiveFromRemoteInputStream) {
            this.receiveFromRemoteInputStream = receiveFromRemoteInputStream;
        }

        @Override
        public void run() {
            try {
                IOUtils.copy(receiveFromRemoteInputStream, System.out);
            } catch (IOException ioExc) {
                ioExc.printStackTrace();
            }
        }
    }

    // Quick test
    public static void main (String[] args) {
        RemoteServer mainObj = new RemoteServer();

        try {
            mainObj.setServer("10.171.106.174");
            mainObj.setCommand("date >/tmp/asn.001; vmstat 3 2");
            mainObj.setUsername("ec2-user");
            mainObj.start();
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
