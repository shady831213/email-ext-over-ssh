package org.jenkinsci.plugins.emailextoverssh.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.emailextoverssh.ExtendedEmailPublisher;
import org.jenkinsci.plugins.emailextoverssh.plugins.EmailTrigger;
import org.jenkinsci.plugins.emailextoverssh.plugins.EmailTriggerDescriptor;
import org.jenkinsci.plugins.emailextoverssh.plugins.RecipientProvider;
import org.jenkinsci.plugins.emailextoverssh.plugins.recipients.DevelopersRecipientProvider;
import org.jenkinsci.plugins.emailextoverssh.plugins.recipients.ListRecipientProvider;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

public class FixedUnhealthyTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Unstable (Test Failures)/Failure -> Success";

    @DataBoundConstructor
    public FixedUnhealthyTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Deprecated
    public FixedUnhealthyTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {

        Result buildResult = build.getResult();

        if (buildResult == Result.SUCCESS) {
            Run<?, ?> prevBuild = getPreviousRun(build, listener);
            if (prevBuild != null && (prevBuild.getResult() == Result.UNSTABLE || prevBuild.getResult() == Result.FAILURE)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find most recent previous build matching certain criteria.
     */
    private Run<?, ?> getPreviousRun(Run<?, ?> build, TaskListener listener) {

        Run<?, ?> prevBuild = ExtendedEmailPublisher.getPreviousRun(build, listener);

        // Skip ABORTED builds
        if (prevBuild != null && prevBuild.getResult() == Result.ABORTED) {
            return getPreviousRun(prevBuild, listener);
        }

        return prevBuild;
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        public DescriptorImpl() {
            addTriggerNameToReplace(SuccessTrigger.TRIGGER_NAME);
            
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
