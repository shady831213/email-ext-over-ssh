package org.jenkinsci.plugins.emailextoverssh.plugins.trigger;

import hudson.Extension;
import org.jenkinsci.plugins.emailextoverssh.plugins.EmailTrigger;
import org.jenkinsci.plugins.emailextoverssh.plugins.RecipientProvider;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.List;

/**
 * @author Kanstantsin Shautsou
 */
public class XNthFailureTrigger extends NthFailureTrigger {
    public static final String TRIGGER_NAME = "Failure - X";

    private int requiredFailureCount = 3;

    @DataBoundConstructor
    public XNthFailureTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo,
                              String subject, String body, String attachmentsPattern, int attachBuildLog,
                              String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public int getRequiredFailureCount() {
        return requiredFailureCount;
    }

    @DataBoundSetter
    public void setRequiredFailureCount(int requiredFailureCount) {
        this.requiredFailureCount = requiredFailureCount;
    }

    @Extension
    public static final class DescriptorImpl extends NthFailureTrigger.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }

        @Override
        public EmailTrigger createDefault() {
            return _createDefault();
        }
    }
}
