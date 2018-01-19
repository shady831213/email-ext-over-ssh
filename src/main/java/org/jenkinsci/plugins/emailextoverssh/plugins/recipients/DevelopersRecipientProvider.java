package org.jenkinsci.plugins.emailextoverssh.plugins.recipients;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.User;
import org.jenkinsci.plugins.emailextoverssh.ExtendedEmailPublisherContext;
import org.jenkinsci.plugins.emailextoverssh.ExtendedEmailPublisherDescriptor;
import org.jenkinsci.plugins.emailextoverssh.plugins.RecipientProvider;
import org.jenkinsci.plugins.emailextoverssh.plugins.RecipientProviderDescriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.mail.internet.InternetAddress;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Set;

/**
 * Created by acearl on 12/25/13.
 */
public class DevelopersRecipientProvider extends RecipientProvider {
    
    @DataBoundConstructor
    public DevelopersRecipientProvider() {
        
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
        Set<User> users = RecipientProviderUtilities.getChangeSetAuthors(Collections.<Run<?, ?>>singleton(context.getRun()), debug);
        RecipientProviderUtilities.addUsers(users, context, env, to, cc, bcc, debug);
    }

    @Extension
    public static final class DescriptorImpl extends RecipientProviderDescriptor {
        @Override
        public String getDisplayName() {
            return "Developers";
        }
    }
}
