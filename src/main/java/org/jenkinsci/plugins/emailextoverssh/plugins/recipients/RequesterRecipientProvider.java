package org.jenkinsci.plugins.emailextoverssh.plugins.recipients;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import org.jenkinsci.plugins.emailextoverssh.ExtendedEmailPublisherContext;
import org.jenkinsci.plugins.emailextoverssh.ExtendedEmailPublisherDescriptor;
import org.jenkinsci.plugins.emailextoverssh.plugins.RecipientProvider;
import org.jenkinsci.plugins.emailextoverssh.plugins.RecipientProviderDescriptor;
import java.io.PrintStream;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.mail.internet.InternetAddress;
import java.util.Set;

/**
 * Created by acearl on 12/25/13.
 */

public class RequesterRecipientProvider extends RecipientProvider {
    @DataBoundConstructor
    public RequesterRecipientProvider() {
        
    }
    
    @Override
    public void addRecipients(final ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        final class Debug implements RecipientProviderUtilities.IDebug {
            private final ExtendedEmailPublisherDescriptor descriptor
                    = Jenkins.getActiveInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

            private final PrintStream logger = context.getListener().getLogger();

            public void send(final String format, final Object... args) {
                descriptor.debug(logger, format, args);
            }
        }
        final Debug debug = new Debug();
        // looking for Upstream build.
        Run<?, ?> cur = context.getRun();
        Cause.UpstreamCause upc = cur.getCause(Cause.UpstreamCause.class);
        while (upc != null) {
            // UpstreamCause.getUpStreamProject() returns the full name, so use getItemByFullName
            Job<?, ?> p = (Job<?, ?>) Jenkins.getActiveInstance().getItemByFullName(upc.getUpstreamProject());
            if (p == null) {
                context.getListener().getLogger().print("There is a break in the project linkage, could not retrieve upstream project information");
                break;
            }
            cur = p.getBuildByNumber(upc.getUpstreamBuild());
            if (cur == null) {
                context.getListener().getLogger().print("There is a break in the build linkage, could not retrieve upstream build information");
                break;
            }
            upc = cur.getCause(Cause.UpstreamCause.class);
        }
        addUserTriggeringTheBuild(cur, to, cc, bcc, env, context, debug);
    }

    private static void addUserTriggeringTheBuild(Run<?, ?> run, Set<InternetAddress> to,
        Set<InternetAddress> cc, Set<InternetAddress> bcc, EnvVars env, final ExtendedEmailPublisherContext context, RecipientProviderUtilities.IDebug debug) {

        final User user = RecipientProviderUtilities.getUserTriggeringTheBuild(run);
        if (user != null) {
            RecipientProviderUtilities.addUsers(Collections.singleton(user), context, env, to, cc, bcc, debug);
        }
    }

    @SuppressWarnings("unchecked")

    
    @Extension
    public static final class DescriptorImpl extends RecipientProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "Requestor";
        }
        
    }
}
