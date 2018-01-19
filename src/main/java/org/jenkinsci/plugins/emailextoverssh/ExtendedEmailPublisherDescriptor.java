package org.jenkinsci.plugins.emailextoverssh;

import com.jcraft.jsch.JSchException;
import hudson.Extension;
import hudson.Util;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.ItemGroup;
import org.jenkinsci.plugins.emailextoverssh.plugins.EmailTriggerDescriptor;
import org.jenkinsci.plugins.emailextoverssh.plugins.trigger.FailureTrigger;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Mailer;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.jsch.JSchConnector;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import hudson.security.ACL;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import org.kohsuke.stapler.AncestorInPath;

/**
 * These settings are global configurations
 */
@Extension
public final class ExtendedEmailPublisherDescriptor extends BuildStepDescriptor<Publisher> {

    public static final Logger LOGGER = Logger.getLogger(ExtendedEmailPublisherDescriptor.class.getName());
    /**
     * The default e-mail address suffix appended to the user name found from
     * changelog, to send e-mails. Null if not configured.
     */
    private String defaultSuffix;

    /**
     * Jenkins's own URL, to put into the e-mail.
     */
    private transient String hudsonUrl;

    /**
     * o
     * If non-null, use SMTP-AUTH
     */
    private String smtpAuthUsername;

    private Secret smtpAuthPassword;

    /**
     * The e-mail address that Jenkins puts to "From:" field in outgoing
     * e-mails. Null if not configured.
     */
    private transient String adminAddress;

    /**
     * SSH sshHostname
     */
    private String sshHostname;

    /**
     * SSH sshPort
     */
    private String sshPort = "22";

    /**
     * SSH sshForwardPort
     */
    private String sshForwardPort = "4567";

    /**
    /**
     * SSH credentialId
     */
    private String credentialId;

    /**
     * SSH pty
     */
    private Boolean sshPty = Boolean.FALSE;

    /**
     * SSH sshServerAliveInterval
     */
    private String sshServerAliveInterval = "0";

    /**
     * SSH sshTimeout
     */
    private String sshTimeout = "0";

    /**
     * The SMTP server to use for sending e-mail. Null for default to the
     * environment, which is usually <tt>localhost</tt>.
     */
    private String smtpHost;

    /**
     * If true use SSL on port 465 (standard SMTPS) unless <code>smtpPort</code>
     * is set.
     */
    private boolean useSsl;

    /**
     * The SMTP port to use for sending e-mail. Null for default to the
     * environment, which is usually <tt>25</tt>.
     */
    private String smtpPort;

    private String charset;

    private String from;

    /**
     * This is a global default content type (mime type) for emails.
     */
    private String defaultContentType;

    /**
     * This is a global default subject line for sending emails.
     */
    private String defaultSubject;

    /**
     * This is a global default body for sending emails.
     */
    private String defaultBody;

    /**
     * This is the global default pre-send script.
     */
    private String defaultPresendScript = "";

    /**
     * This is the global default post-send script.
     */
    private String defaultPostsendScript = "";

    private List<GroovyScriptPath> defaultClasspath = new ArrayList<>();

    private transient List<EmailTriggerDescriptor> defaultTriggers = new ArrayList<>();

    private List<String> defaultTriggerIds = new ArrayList<>();

    /**
     * This is the global emergency email address
     */
    private String emergencyReroute;

    /**
     * The maximum size of all the attachments (in bytes)
     */
    private long maxAttachmentSize = -1;

    /*
     * This is a global default recipient list for sending emails.
     */
    private String recipientList = "";

    /*
     * The default Reply-To header value
     */
    private String defaultReplyTo = "";

    /*
     * This is a global excluded committers list for not sending commit emails.
     */
    private String excludedCommitters = "";

    private boolean overrideGlobalSettings;

    /**
     * If non-null, set a List-ID email header.
     */
    private String listId;

    private boolean precedenceBulk;

    private boolean debugMode = false;

    private transient boolean enableSecurity = false;

    /**
     * If true, then the 'Email Template Testing' link will only be displayed
     * for users with ADMINISTER privileges.
     */
    private boolean requireAdminForTemplateTesting = false;

    /**
     * Enables the "Watch This Job" feature
     */
    private boolean enableWatching;

    /**
     * Enables the "Allow Unregistered Emails" feature
     */
    private boolean enableAllowUnregistered;

    public ExtendedEmailPublisherDescriptor() {
        super(ExtendedEmailPublisher.class);
        load();
        if (defaultBody == null && defaultSubject == null && emergencyReroute == null) {
            defaultBody = ExtendedEmailPublisher.DEFAULT_BODY_TEXT;
            defaultSubject = ExtendedEmailPublisher.DEFAULT_SUBJECT_TEXT;
            emergencyReroute = ExtendedEmailPublisher.DEFAULT_EMERGENCY_REROUTE_TEXT;
        }

        if (Jenkins.getActiveInstance().isUseSecurity()
                && (!StringUtils.isBlank(this.defaultPostsendScript)) || !StringUtils.isBlank(this.defaultPresendScript)) {
            setDefaultPostsendScript(this.defaultPostsendScript);
            setDefaultPresendScript(this.defaultPresendScript);
            try {
                setDefaultClasspath(this.defaultClasspath);
            } catch (FormException e) {
                //Some of the old configured classpaths probably used some environment variable, let's clean those out
                List<GroovyScriptPath> newList = new ArrayList<>();
                for (GroovyScriptPath path : defaultClasspath) {
                    URL u = path.asURL();
                    if (u != null) {
                        try {
                            new ClasspathEntry(u.toString());
                            newList.add(path);
                        } catch (MalformedURLException mfue) {
                            LOGGER.log(Level.WARNING, "The default classpath contained a malformed url, will be ignored.", mfue);
                        }
                    }
                }
                try {
                    setDefaultClasspath(newList);
                } catch (FormException e1) {
                    assert false : e1;
                }
            }
        }
    }

    @Override
    public String getDisplayName() {
        return Messages.ExtendedEmailPublisherDescriptor_DisplayName();
    }

    public String getAdminAddress() {
        JenkinsLocationConfiguration configuration = JenkinsLocationConfiguration.get();
        assert configuration != null;
        return configuration.getAdminAddress();
    }

    public String getDefaultSuffix() {
        return defaultSuffix;
    }

    public void setDefaultSuffix(String defaultSuffix) {
        this.defaultSuffix = defaultSuffix;
    }

    public Session createSession() {
        Properties props = new Properties(System.getProperties());
        if (smtpHost != null) {
            //props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.host", "localhost");
        }
        if (smtpPort != null) {
            //props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.port", sshForwardPort);
        }
        if (useSsl) {
            /* This allows the user to override settings by setting system properties but
             * also allows us to use the default SMTPs port of 465 if no port is already set.
             * It would be cleaner to use smtps, but that's done by calling session.getTransport()...
             * and thats done in mail sender, and it would be a bit of a hack to get it all to
             * coordinate, and we can make it work through setting mail.smtp properties.
             */
            if (props.getProperty("mail.smtp.socketFactory.port") == null) {
                props.put("mail.smtp.port", sshForwardPort);
                props.put("mail.smtp.socketFactory.port", sshForwardPort);
            }
            if (props.getProperty("mail.smtp.socketFactory.class") == null) {
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }
            props.put("mail.smtp.socketFactory.fallback", "false");
        }
        if (smtpAuthUsername != null) {
            props.put("mail.smtp.auth", "true");
        }

        // avoid hang by setting some timeout.
        props.put("mail.smtp.timeout", "60000");
        props.put("mail.smtp.connectiontimeout", "60000");

        return Session.getInstance(props, getAuthenticator());
    }

    private Authenticator getAuthenticator() {
        final String un = getSmtpAuthUsername();
        if (un == null) {
            return null;
        }
        return new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(getSmtpAuthUsername(), getSmtpAuthPassword());
            }
        };
    }

    public String getHudsonUrl() {
        return Jenkins.getActiveInstance().getRootUrl();
    }

    public String getSshHostname() {
        return sshHostname;
    }

    public void setSshHostname(String sshHostname) {
        this.sshHostname = sshHostname;
    }

    public String getSshPort() {
        return sshPort;
    }

    public void setSshPort(String sshPort) {
        this.sshPort = sshPort;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public Boolean getSshPty() {
        return sshPty;
    }

    public void setSshPty(Boolean sshPty) {
        this.sshPty = sshPty;
    }

    public String getSshServerAliveInterval() {
        return sshServerAliveInterval;
    }

    public void setSshServerAliveInterval(String sshServerAliveInterval) {
        this.sshServerAliveInterval = sshServerAliveInterval;
    }

    public String getSshForwardPort() {
        return sshForwardPort;
    }

    public void setSshForwardPort(String sshForwardPort) {
        this.sshForwardPort = sshForwardPort;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSshTimeout() {
        return sshTimeout;
    }

    public void setSshTimeout(String sshTimeout) {
        this.sshTimeout = sshTimeout;
    }

    public String getSmtpServer() {
        return smtpHost;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpHost = smtpServer;
    }

    public String getSmtpAuthUsername() {
        return smtpAuthUsername;
    }

    @SuppressWarnings("unused")
    public void setSmtpAuthUsername(String username) {
        this.smtpAuthUsername = username;
    }

    public String getSmtpAuthPassword() {
        return Secret.toString(smtpAuthPassword);
    }

    @SuppressWarnings("unused")
    public void setSmtpAuthPassword(String password) {
        this.smtpAuthPassword = Secret.fromString(password);
    }

    // Make API match Mailer plugin
    @SuppressWarnings("unused")
    public void setSmtpAuth(String userName, String password) {
        setSmtpAuthUsername(userName);
        setSmtpAuthPassword(password);
    }

    public boolean getUseSsl() {
        return useSsl;
    }

    @SuppressWarnings("unused")
    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    @SuppressWarnings("unused")
    public void setSmtpPort(String port) {
        this.smtpPort = nullify(port);
    }

    public String getCharset() {
        String c = charset;
        if (StringUtils.isBlank(c)) {
            c = "UTF-8";
        }
        return c;
    }

    @SuppressWarnings("unused")
    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getDefaultContentType() {
        return defaultContentType;
    }

    @SuppressWarnings("unused")
    public void setDefaultContentType(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            this.defaultContentType = "text/plain";
        } else {
            this.defaultContentType = contentType;
        }
    }

    public String getDefaultSubject() {
        return defaultSubject;
    }

    @SuppressWarnings("unused")
    public void setDefaultSubject(String subject) {
        if (subject == null) {
            this.defaultSubject = ExtendedEmailPublisher.DEFAULT_SUBJECT_TEXT;
        } else {
            this.defaultSubject = subject;
        }
    }

    public String getDefaultBody() {
        return defaultBody;
    }

    @SuppressWarnings("unused")
    public void setDefaultBody(String body) {
        if (body == null) {
            this.defaultBody = ExtendedEmailPublisher.DEFAULT_BODY_TEXT;
        } else {
            this.defaultBody = body;
        }
    }

    public String getEmergencyReroute() {
        return emergencyReroute;
    }

    protected void setEmergencyReroute(String emergencyReroute) {
        if (emergencyReroute == null) {
            this.emergencyReroute = ExtendedEmailPublisher.DEFAULT_EMERGENCY_REROUTE_TEXT;
        } else {
            this.emergencyReroute = emergencyReroute;
        }
    }

    public long getMaxAttachmentSize() {
        return maxAttachmentSize;
    }

    public void setMaxAttachmentSize(long bytes) {
        if (bytes < 0) {
            bytes = -1; // set to default "empty" value
        }
        this.maxAttachmentSize = bytes;
    }

    public long getMaxAttachmentSizeMb() {
        return maxAttachmentSize / (1024 * 1024);
    }

    @SuppressWarnings("unused")
    public void setMaxAttachmentSizeMb(long mb) {
        setMaxAttachmentSize(mb * (1024 * 1024));
    }

    public String getDefaultRecipients() {
        return recipientList;
    }

    @SuppressWarnings("unused")
    public void setDefaultRecipients(String recipients) {
        this.recipientList = ((recipients == null) ? "" : recipients);
    }

    public String getExcludedCommitters() {
        return excludedCommitters;
    }

    @SuppressWarnings("unused")
    public void setExcludedCommitters(String excluded) {
        this.excludedCommitters = ((excluded == null) ? "" : excluded);
    }

    public boolean getOverrideGlobalSettings() {
        return overrideGlobalSettings;
    }

    public String getListId() {
        return listId;
    }

    @SuppressWarnings("unused")
    public void setListId(String id) {
        this.listId = nullify(id);
    }

    public boolean getPrecedenceBulk() {
        return precedenceBulk;
    }

    @SuppressWarnings("unused")
    public void setPrecedenceBulk(boolean bulk) {
        this.precedenceBulk = bulk;
    }

    public String getDefaultReplyTo() {
        return defaultReplyTo;
    }

    @SuppressWarnings("unused")
    public void setDefaultReplyTo(String to) {
        this.defaultReplyTo = ((to == null) ? "" : to);
    }

    public boolean isSecurityEnabled() {
        return false;
    }

    public boolean isAdminRequiredForTemplateTesting() {
        return requireAdminForTemplateTesting;
    }

    @SuppressWarnings("unused")
    public void setAdminRequiredForTemplateTesting(boolean requireAdmin) {
        this.requireAdminForTemplateTesting = requireAdmin;
    }

    public boolean isWatchingEnabled() {
        return enableWatching;
    }

    public boolean isAllowUnregisteredEnabled() {
        return enableAllowUnregistered;
    }

    @SuppressWarnings("unused")
    public void setWatchingEnabled(boolean enabled) {
        this.enableWatching = enabled;
    }

    @SuppressWarnings("unused")
    public void setAllowUnregisteredEnabled(boolean enabled) {
        this.enableAllowUnregistered = enabled;
    }

    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }

    public String getDefaultPresendScript() {
        return defaultPresendScript;
    }

    @SuppressWarnings("unused")
    public void setDefaultPresendScript(String script) {
        script = StringUtils.trim(script);
        this.defaultPresendScript = ScriptApproval.get().configuring(((script == null) ? "" : script),
                GroovyLanguage.get(),
                ApprovalContext.create().withCurrentUser());
    }

    public String getDefaultPostsendScript() {
        return defaultPostsendScript;
    }

    @SuppressWarnings("unused")
    public void setDefaultPostsendScript(String script) {
        script = StringUtils.trim(script);
        this.defaultPostsendScript = ScriptApproval.get().configuring(((script == null) ? "" : script),
                GroovyLanguage.get(),
                ApprovalContext.create().withCurrentUser());
    }

    public List<GroovyScriptPath> getDefaultClasspath() {
        return defaultClasspath;
    }

    public void setDefaultClasspath(List<GroovyScriptPath> defaultClasspath) throws FormException {
        if (Jenkins.getActiveInstance().isUseSecurity()) {
            ScriptApproval approval = ScriptApproval.get();
            ApprovalContext context = ApprovalContext.create().withCurrentUser();
            for (GroovyScriptPath path : defaultClasspath) {
                URL u = path.asURL();
                if (u != null) {
                    try {
                        approval.configuring(new ClasspathEntry(u.toString()), context);
                    } catch (MalformedURLException e) {
                        throw new FormException(e, "ext_mailer_default_classpath");
                    }
                }
            }
        }
        this.defaultClasspath = defaultClasspath;
    }

    public List<String> getDefaultTriggerIds() {
        if (defaultTriggerIds.isEmpty()) {
            if (!defaultTriggers.isEmpty()) {
                defaultTriggerIds.clear();
                for (EmailTriggerDescriptor t : this.defaultTriggers) {
                    // we have to do the below because a bunch of stuff is not serialized for the Descriptor
                    EmailTriggerDescriptor d = Jenkins.getActiveInstance().getDescriptorByType(t.getClass());
                    if (d != null && !defaultTriggerIds.contains(d.getId())) {
                        defaultTriggerIds.add(d.getId());
                    }
                }
            } else {
                FailureTrigger.DescriptorImpl f = Jenkins.getActiveInstance().getDescriptorByType(FailureTrigger.DescriptorImpl.class);
                if (f != null) {
                    defaultTriggerIds.add(f.getId());
                }
            }
            save();
        }
        return defaultTriggerIds;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
            throws FormException {

        //configure fro ssh
        sshHostname = nullify(req.getParameter("ext_mailer_ssh_hostname"));
        sshPort = nullify(req.getParameter("ext_mailer_ssh_port"));
        sshForwardPort = nullify(req.getParameter("ext_mailer_ssh_forwardport"));
        credentialId = nullify(req.getParameter("_.credentialId"));
        sshPty = req.hasParameter("ext_mailer_ssh_pty");
        sshServerAliveInterval = nullify(req.getParameter("ext_mailer_ssh_serverAliveInterval"));
        sshTimeout = nullify(req.getParameter("ext_mailer_ssh_timeout"));

        // Configure the smtp server
        smtpHost = nullify(req.getParameter("ext_mailer_smtp_server"));
        defaultSuffix = nullify(req.getParameter("ext_mailer_default_suffix"));

        // specify authentication information
        if (req.hasParameter("ext_mailer_use_smtp_auth")) {
            smtpAuthUsername = nullify(req.getParameter("ext_mailer_smtp_username"));
            smtpAuthPassword = Secret.fromString(nullify(req.getParameter("ext_mailer_smtp_password")));
        } else {
            smtpAuthUsername = null;
            smtpAuthPassword = null;
        }

        // specify if the mail server uses ssl for authentication
        useSsl = req.hasParameter("ext_mailer_smtp_use_ssl");
        from =  nullify(req.getParameter("ext_mailer_from"));
        // specify custom smtp port
        smtpPort = nullify(req.getParameter("ext_mailer_smtp_port")) != null
                ? req.getParameter("ext_mailer_smtp_port") : useSsl ? "465" : "25";

        charset = nullify(req.getParameter("ext_mailer_charset"));

        defaultContentType = nullify(req.getParameter("ext_mailer_default_content_type"));

        // Allow global defaults to be set for the subject and body of the email
        defaultSubject = nullify(req.getParameter("ext_mailer_default_subject"));
        defaultBody = nullify(req.getParameter("ext_mailer_default_body"));
        emergencyReroute = nullify(req.getParameter("ext_mailer_emergency_reroute"));
        defaultReplyTo = nullify(req.getParameter("ext_mailer_default_replyto")) != null
                ? req.getParameter("ext_mailer_default_replyto") : "";
        setDefaultPresendScript(nullify(req.getParameter("ext_mailer_default_presend_script")) != null
                ? req.getParameter("ext_mailer_default_presend_script") : "");
        setDefaultPostsendScript(nullify(req.getParameter("ext_mailer_default_postsend_script")) != null
                ? req.getParameter("ext_mailer_default_postsend_script") : "");
        if (req.hasParameter("ext_mailer_default_classpath")) {
            List<GroovyScriptPath> cp = new ArrayList<>();
            for (String s : req.getParameterValues("ext_mailer_default_classpath")) {
                cp.add(new GroovyScriptPath(s));
            }
            setDefaultClasspath(cp);
        }
        debugMode = req.hasParameter("ext_mailer_debug_mode");

        // convert the value into megabytes (1024 * 1024 bytes)
        maxAttachmentSize = nullify(req.getParameter("ext_mailer_max_attachment_size")) != null
                ? Long.parseLong(req.getParameter("ext_mailer_max_attachment_size")) * 1024 * 1024 : -1;
        recipientList = nullify(req.getParameter("ext_mailer_default_recipients")) != null
                ? req.getParameter("ext_mailer_default_recipients") : "";

        precedenceBulk = req.hasParameter("ext_mailer_add_precedence_bulk");

        excludedCommitters = req.getParameter("ext_mailer_excluded_committers");

        requireAdminForTemplateTesting = req.hasParameter("ext_mailer_require_admin_for_template_testing");

        enableWatching = req.hasParameter("ext_mailer_watching_enabled");

        enableAllowUnregistered = req.hasParameter("ext_mailer_allow_unregistered_enabled");

        // specify List-ID information
        if (req.hasParameter("ext_mailer_use_list_id")) {
            listId = nullify(req.getParameter("ext_mailer_list_id"));
        } else {
            listId = null;
        }

        List<String> ids = new ArrayList<>();
        if (formData.optJSONArray("defaultTriggers") != null) {
            for (Object id : formData.getJSONArray("defaultTriggers")) {
                ids.add(id.toString());
            }
        } else if (StringUtils.isNotEmpty(formData.optString("defaultTriggers"))) {
            ids.add(formData.getString("defaultTriggers"));
        }

        if (!ids.isEmpty()) {
            defaultTriggerIds.clear();
            for (String id : ids) {
                EmailTriggerDescriptor d = (EmailTriggerDescriptor) Jenkins.getActiveInstance().getDescriptor(id);
                if (d != null) {
                    defaultTriggerIds.add(id);
                }
            }
        }

        if (!overrideGlobalSettings) {
            upgradeFromMailer();
        }

        save();
        return super.configure(req, formData);
    }

    private String nullify(String v) {
        if (v != null && v.length() == 0) {
            v = null;
        }
        return v;
    }

    void upgradeFromMailer() {
        // get the data from Mailer and then set override to true
        this.defaultSuffix = Mailer.descriptor().getDefaultSuffix();
        this.defaultReplyTo = Mailer.descriptor().getReplyToAddress();
        this.useSsl = Mailer.descriptor().getUseSsl();
        if (StringUtils.isNotBlank(Mailer.descriptor().getSmtpAuthUserName())) {
            this.smtpAuthPassword = Secret.fromString(Mailer.descriptor().getSmtpAuthPassword());
            this.smtpAuthUsername = Mailer.descriptor().getSmtpAuthUserName();
        }
        this.smtpPort = Mailer.descriptor().getSmtpPort();
        this.smtpHost = Mailer.descriptor().getSmtpServer();
        this.charset = Mailer.descriptor().getCharset();
        this.overrideGlobalSettings = true;
    }

    @Override
    public String getHelpFile() {
        return "/plugin/email-ext-over-ssh/help/main.html";
    }

    public FormValidation doAddressCheck(@QueryParameter final String value)
            throws IOException, ServletException {
        try {
            new InternetAddress(value);
            return FormValidation.ok();
        } catch (AddressException e) {
            return FormValidation.error(e.getMessage());
        }
    }

    public FormValidation doRecipientListRecipientsCheck(@QueryParameter final String value)
            throws IOException, ServletException {
        return new EmailRecipientUtils().validateFormRecipientList(value);
    }

    public FormValidation doMaxAttachmentSizeCheck(@QueryParameter final String value)
            throws IOException, ServletException {
        try {
            String testValue = value.trim();
            // we support an empty value (which means default)
            // or a number
            if (testValue.length() > 0) {
                Long.parseLong(testValue);
            }
            return FormValidation.ok();
        } catch (Exception e) {
            return FormValidation.error(e.getMessage());
        }
    }

    public boolean isMatrixProject(Object project) {
        return project instanceof MatrixProject;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void debug(PrintStream logger, String format, Object... args) {
        if (debugMode) {
            logger.format(format, args);
            logger.println();
        }
    }

    /**
     * for ssh
     */
    public ListBoxModel doFillCredentialIdItems(final @AncestorInPath ItemGroup<?> context) {
        return new StandardUsernameListBoxModel().includeEmptyValue().
               includeMatchingAs(ACL.SYSTEM, context, StandardUsernameCredentials.class, CredentialsSSHSite.NO_REQUIREMENTS,
                       SSHAuthenticator.matcher(JSchConnector.class));
    }

    public FormValidation doLoginCheck(@QueryParameter("ext_mailer_ssh_hostname") String hostname,
                                       @QueryParameter("ext_mailer_ssh_port") String port,
                                       @QueryParameter("credentialId") String credentialId,
                                       @QueryParameter("ext_mailer_ssh_serverAliveInterval") String serverAliveInterval,
                                       @QueryParameter("ext_mailer_ssh_timeout") String timeout) {
        hostname = hudson.Util.fixEmpty(hostname);
        port = hudson.Util.fixEmpty(port);
        credentialId = Util.fixEmpty(credentialId);

        if (hostname == null || port == null || credentialId == null) {// all fields not entered yet
            return FormValidation.warning("Please fill host, port and credentials.");
        }

        final CredentialsSSHSite site = new CredentialsSSHSite(hostname, port, credentialId,
                serverAliveInterval, timeout, false);
        try {
            try {
                site.testConnection(System.out);
            } catch (JSchException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                throw new IOException("Can't connect to server");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            return FormValidation.error(e.getMessage());
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            return FormValidation.error(e.getMessage());
        }
        return FormValidation.ok("Successfull connection");
    }
}
