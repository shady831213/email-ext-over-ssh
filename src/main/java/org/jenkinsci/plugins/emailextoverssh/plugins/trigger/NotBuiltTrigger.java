package org.jenkinsci.plugins.emailextoverssh.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.emailextoverssh.plugins.EmailTrigger;
import org.jenkinsci.plugins.emailextoverssh.plugins.EmailTriggerDescriptor;
import org.jenkinsci.plugins.emailextoverssh.plugins.RecipientProvider;
import org.jenkinsci.plugins.emailextoverssh.plugins.recipients.DevelopersRecipientProvider;
import org.jenkinsci.plugins.emailextoverssh.plugins.recipients.ListRecipientProvider;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

public class NotBuiltTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Not Built";
    
    @DataBoundConstructor
    public NotBuiltTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Deprecated
    public NotBuiltTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        return build.getResult() == Result.NOT_BUILT;
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        public DescriptorImpl() {
            addDefaultRecipientProvider(new DevelopersRecipientProvider());
            addDefaultRecipientProvider(new ListRecipientProvider());
        }
        
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