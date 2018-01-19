/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jenkinsci.plugins.emailextoverssh.plugins.recipients;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Job;
import org.jenkinsci.plugins.emailextoverssh.EmailRecipientUtils;
import org.jenkinsci.plugins.emailextoverssh.ExtendedEmailPublisherContext;
import org.jenkinsci.plugins.emailextoverssh.ExtendedEmailPublisherDescriptor;
import org.jenkinsci.plugins.emailextoverssh.plugins.RecipientProvider;
import org.jenkinsci.plugins.emailextoverssh.plugins.RecipientProviderDescriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author acearl
 */
public class ListRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public ListRecipientProvider() {
        
    }
    
    @Override
    public void addRecipients(ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        try {
            ExtendedEmailPublisherDescriptor descriptor = Jenkins.getActiveInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
            descriptor.debug(context.getListener().getLogger(), "Adding recipients from project recipient list");
            EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc, EmailRecipientUtils.getRecipientList(context, context.getPublisher().recipientList), env, context.getListener());
        } catch (MessagingException ex) {
            Logger.getLogger(ListRecipientProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Extension
    public static final class DescriptorImpl extends RecipientProviderDescriptor {
        
        @Override
        public String getDisplayName() {
            return "Recipient List";
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return !jobType.getName().equals("org.jenkinsci.plugins.workflow.job.WorkflowJob");
        }
    }
}
