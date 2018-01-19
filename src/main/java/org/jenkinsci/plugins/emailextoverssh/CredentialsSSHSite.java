package org.jenkinsci.plugins.emailextoverssh;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.StreamTaskListener;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.jenkinsci.plugins.jsch.JSchConnector;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class CredentialsSSHSite {

    String hostname;
    String username;
    int port;
    String credentialId;
    int serverAliveInterval = 0;
    int timeout = 0;
    Boolean pty = Boolean.FALSE;

    transient String resolvedHostname = null;

    public static final Logger LOGGER = Logger.getLogger(CredentialsSSHSite.class.getName());

    public static final List<DomainRequirement> NO_REQUIREMENTS = Collections.<DomainRequirement> emptyList();

    private CredentialsSSHSite() {
    }

    @DataBoundConstructor
    public CredentialsSSHSite(final String hostname, final String port, final String credentialId,
                              final String serverAliveInterval, final String timeout, final Boolean pty) {
        final StandardUsernameCredentials credentials = lookupCredentialsById(credentialId);
        this.username = credentials.getUsername();
        this.pty = pty;
        this.hostname = hostname;
        try {
            this.port = Integer.parseInt(port);
        } catch (final NumberFormatException e) {
            this.port = 22;
        }
        this.credentialId = credentialId;
        this.setServerAliveInterval(serverAliveInterval);
        this.setTimeout(timeout);
    }

    private void closeSession(final Session session) {
        if (session != null) {
            session.disconnect();
        }
    }


    public Session createSession(final PrintStream logger) throws JSchException, IOException, InterruptedException {
        final StandardUsernameCredentials user = lookupCredentialsById(credentialId);
        if (user == null) {
            String message = "Credentials with id '" + credentialId + "', no longer exist!";
            logger.println(message);
            throw new InterruptedException(message);
        }

        final JSchConnector connector = new JSchConnector(user.getUsername(), getResolvedHostname(), port);

        final SSHAuthenticator<JSchConnector, StandardUsernameCredentials> authenticator = SSHAuthenticator
                .newInstance(connector, user);
        authenticator.authenticate(new StreamTaskListener(logger, Charset.defaultCharset()));

        final Session session = connector.getSession();

        session.setServerAliveInterval(serverAliveInterval);

        final Properties config = new Properties();
        //TODO put this as configuration option instead of ignoring by default
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(timeout);

        return session;
    }


    public String getCredentialId() {
        return credentialId;
    }

    public String getHostname() {
        return hostname;
    }

    public int getIntegerPort() {
        return port;
    }

    public String getPort() {
        return "" + port;
    }

    public Boolean getPty() {
        return pty;
    }

    public String getServerAliveInterval() {
        return "" + serverAliveInterval;
    }

    /** Returns &quot;identifier&quot; for ssh site: <strong>username@hostname:port</strong> */
    public String getSitename() {
        return username + "@" + hostname + ":" + port;
    }

    public String getTimeout() {
        return "" + timeout;
    }

    private StandardUsernameCredentials lookupCredentialsById(final String credentialId) {
        final List<StandardUsernameCredentials> all = CredentialsProvider.lookupCredentials(
                StandardUsernameCredentials.class, (Item) null, ACL.SYSTEM, NO_REQUIREMENTS);

        return CredentialsMatchers.firstOrNull(all, CredentialsMatchers.withId(credentialId));
    }

    public void setCredentialId(final String credentialId) {
        this.credentialId = credentialId;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    public void setPort(final String port) {
        try {
            this.port = Integer.parseInt(port);
        } catch (final NumberFormatException e) {
            this.port = 22;
        }
    }

    @DataBoundSetter
    public void setPty(final Boolean pty) {
        this.pty = pty;
    }

    public void setResolvedHostname(final String hostname) {
        this.resolvedHostname = hostname;
    }

    public void setServerAliveInterval(final String serverAliveInterval) {
        try {
            this.serverAliveInterval = Integer.parseInt(serverAliveInterval);
        } catch (final NumberFormatException e) {
            this.serverAliveInterval = 0;
        }
    }

    public void setTimeout(final String timeout) {
        try {
            this.timeout = Integer.parseInt(timeout);
        } catch (final NumberFormatException e) {
            this.timeout = 0;
        }
    }

    private String getResolvedHostname() {
        return resolvedHostname == null ? hostname : resolvedHostname;
    }

    public void testConnection(final PrintStream logger) throws JSchException, IOException, InterruptedException {
        final Session session = createSession(logger);
        closeSession(session);
    }

    @Override
    public String toString() {
        return "SSHSite [username=" + username + ", hostname=" + hostname + ",port=" + port + ", credentialId="
                + credentialId + ", pty=" + pty + "]";
    }

}