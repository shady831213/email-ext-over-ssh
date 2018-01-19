// Namespaces
f = namespace("/lib/form")
d = namespace("jelly:define")
j = namespace("jelly:core")
l = namespace("/lib/layout")
st = namespace("jelly:stapler")
t = namespace("/lib/hudson")
c = namespace("/lib/credentials")

f.section(title: _("Extended E-mail Notification")) {
    f.entry(title: _("SSH sites"), description: _("SSH sites that projects will want to connect")) {
        f.entry(title: _("Hostname"), help: "/plugin/email-ext-over-ssh/help-hostname.html") {
            f.textbox(name:"ext_mailer_ssh_hostname", value: descriptor.sshHostname)
        }
        f.entry(title: _("Port"), help: "/plugin/email-ext-over-ssh/help-port.html") {
            f.textbox(name:"ext_mailer_ssh_port", value: descriptor.sshPort)
        }
        f.entry(title: _("ForwardPort"), help: "/plugin/email-ext-over-ssh/help-forwardport.html") {
            f.textbox(name:"ext_mailer_ssh_forwardport", value: descriptor.sshForwardPort)
        }
        f.entry(title: _("Credentials")) {
            c.select(name: "ext_mailer_ssh_credentialId", field:"credentialId", value: descriptor.credentialId)
        }
        f.entry(title: _("Pty"), help: "/plugin/email-ext-over-ssh/help-pty.html") {
            f.checkbox(class: "setting-input", name: "ext_mailer_ssh_pty", checked: descriptor.sshPty)
        }
        f.entry(title: _("serverAliveInterval"), help: "/plugin/email-ext-over-ssh/help-serverAliveInterval.html") {
            f.textbox(name: "ext_mailer_ssh_serverAliveInterval", value: descriptor.sshServerAliveInterval)
        }
        f.entry(title: _("timeout"), help: "/plugin/email-ext-over-ssh/help-timeout.html") {
            f.textbox(name: "ext_mailer_ssh_timeout", value: descriptor.sshTimeout)
        }
        f.validateButton(title: _("Check connection"), progress: "Checking...",
                method: "loginCheck", with: "ext_mailer_ssh_hostname,ext_mailer_ssh_port,credentialId,ext_mailer_ssh_serverAliveInterval,ext_mailer_ssh_timeout")

    }
    f.entry(title: _("from")) {
        input(type: "text", class: "setting-input", value: descriptor.from, name: "ext_mailer_from")
    }
    f.entry(help: "/descriptor/hudson.tasks.Mailer/help/smtpServer", title: _("SMTP server")) {
        input(type: "text", class: "setting-input", value: descriptor.smtpServer, name: "ext_mailer_smtp_server")
    }
    f.entry(help: "/descriptor/hudson.tasks.Mailer/help/defaultSuffix", title: _("Default user E-mail suffix")) {
        input(type: "text", class: "setting-input", value: descriptor.defaultSuffix, name: "ext_mailer_default_suffix")
    }
    f.advanced() {
        f.optionalBlock(help: "/help/tasks/mailer/smtpAuth.html", checked: descriptor.smtpAuthUsername != null, name: "ext_mailer_use_smtp_auth", title: _("Use SMTP Authentication")) {
            f.entry(title: _("User Name")) {
                input(type: "text", class: "setting-input", value: descriptor.smtpAuthUsername, name: "ext_mailer_smtp_username")
            }
            f.entry(title: _("Password")) {
                input(type: "password", class: "setting-input", value: descriptor.smtpAuthPassword, name: "ext_mailer_smtp_password")
            }
        }
        f.entry(help: "/descriptor/hudson.tasks.Mailer/help/useSsl", title: _("Use SSL")) {
            f.checkbox(checked: descriptor.useSsl, name: "ext_mailer_smtp_use_ssl")
        }
        f.entry(help: "/descriptor/hudson.tasks.Mailer/help/smtpPort", title: _("SMTP port")) {
            input(type: "text", class: "setting-input", value: descriptor.smtpPort, name: "ext_mailer_smtp_port")
        }
        f.entry(title: _("Charset")) {
            input(type: "text", class: "setting-input", value: descriptor.charset, name: "ext_mailer_charset")
        }
    }
    f.entry(help: "/plugin/email-ext-over-ssh/help/globalConfig/contentType.html", title: _("Default Content Type")) {
        select(class: "setting-input", name: "ext_mailer_default_content_type") {
            f.option(selected: 'text/plain' == descriptor.defaultContentType, value: "text/plain", _("contentType.plainText"))
            f.option(selected: 'text/html' == descriptor.defaultContentType, value: "text/html", _("contentType.html"))
        }
    }
    f.optionalBlock(help: "/plugin/email-ext-over-ssh/help/globalConfig/listId.html", checked: descriptor.listId != null, name: "ext_mailer_use_list_id", title: _("Use List-ID Email Header")) {
        f.entry(title: _("List ID")) {
            input(type: "text", class: "setting-input", value: descriptor.listId, name: "ext_mailer_list_id")
        }
    }
    f.optionalBlock(help: "/plugin/email-ext-over-ssh/help/globalConfig/precedenceBulk.html", checked: descriptor.precedenceBulk, name: "ext_mailer_add_precedence_bulk", title: _("Add 'Precedence: bulk' Email Header"))
    f.entry(field: "recipients", help: "/plugin/email-ext-over-ssh/help/globalConfig/defaultRecipients.html", title: _("Default Recipients")) {
        input(type: "text", class: "setting-input", value: descriptor.defaultRecipients, name: "ext_mailer_default_recipients")
    }
    f.entry(field: "replyTo", help: "/plugin/email-ext-over-ssh/help/globalConfig/replyToList.html", title: _("Reply To List")) {
        input(type: "text", class: "setting-input", value: descriptor.defaultReplyTo, name: "ext_mailer_default_replyto")
    }
    f.entry(help: "/plugin/email-ext-over-ssh/help/globalConfig/emergencyReroute.html", title: _("Emergency reroute")) {
        input(type: "text", class: "setting-input", value: descriptor.emergencyReroute, name: "ext_mailer_emergency_reroute")
    }
    f.entry(help: "/plugin/email-ext-over-ssh/help/globalConfig/excludedRecipients.html", title: _("Excluded Recipients")) {
        input(type: "text", class: "setting-input", value: descriptor.excludedCommitters, name: "ext_mailer_excluded_committers")
    }
    f.entry(help: "/plugin/email-ext-over-ssh/help/globalConfig/defaultSubject.html", title: _("Default Subject")) {
        input(type: "text", class: "setting-input", value: descriptor.defaultSubject, name: "ext_mailer_default_subject")
    }
    f.entry(help: "/plugin/email-ext-over-ssh/help/globalConfig/maxAttachmentSize.html", title: _("Maximum Attachment Size")) {
        if (descriptor.maxAttachmentSize > 0) {
            input(checkUrl: "'${rootURL}/publisher/ExtendedEmailPublisher/maxAttachmentSizeCheck?value='+encodeURIComponent(this.value)", type: "text", class: "setting-input", value: descriptor.maxAttachmentSizeMb, name: "ext_mailer_max_attachment_size")
        } else {
            input(checkUrl: "'${rootURL}/publisher/ExtendedEmailPublisher/maxAttachmentSizeCheck?value='+encodeURIComponent(this.value)", type: "text", class: "setting-input", value: "", name: "ext_mailer_max_attachment_size")
        }
    }
    f.entry(help: "/plugin/email-ext-over-ssh/help/globalConfig/defaultBody.html", title: _("Default Content")) {
        f.textarea(class: "setting-input", value: descriptor.defaultBody, name: "ext_mailer_default_body")
    }
    f.entry(help: "/plugin/email-ext-over-ssh/help/globalConfig/defaultPresendScript.html", title: _("Default Pre-send Script")) {
        f.textarea(class: "setting-input", value: descriptor.defaultPresendScript, name: "ext_mailer_default_presend_script")
    }
    f.entry(help: "/plugin/email-ext-over-ssh/help/globalConfig/defaultPostsendScript.html", title: _("Default Post-send Script")) {
        f.textarea(class: "setting-input", value: descriptor.defaultPostsendScript, name: "ext_mailer_default_postsend_script")
    }
    f.entry(title: _("Additional groovy classpath"), help: "/plugin/email-ext-over-ssh/help/globalConfig/defaultClasspath.html") {
        f.repeatable(field: "defaultClasspath") {
            f.textbox(field: "path", name: "ext_mailer_default_classpath")
            div(align: "right") {
                f.repeatableDeleteButton()
            }
        }
    }
    f.optionalBlock(help: "/plugin/email-ext-over-ssh/help/globalConfig/debugMode.html", checked: descriptor.isDebugMode(), name: "ext_mailer_debug_mode", title: _("Enable Debug Mode"))
    f.optionalBlock(help: "/plugin/email-ext-over-ssh/help/globalConfig/requireAdmin.html", checked: descriptor.isAdminRequiredForTemplateTesting(), name: "ext_mailer_require_admin_for_template_testing", title: _("Require Administrator for Template Testing"))
    f.optionalBlock(help: "/plugin/email-ext-over-ssh/help/globalConfig/watching.html", checked: descriptor.isWatchingEnabled(), name: "ext_mailer_watching_enabled", title: _("Enable watching for jobs"))
    f.optionalBlock(help: "/plugin/email-ext-over-ssh/help/globalConfig/allowUnregistered.html", checked: descriptor.isAllowUnregisteredEnabled(), name: "ext_mailer_allow_unregistered_enabled", title: _("Allow sending to unregistered users"))

    f.advanced(title: _("Default Triggers")) {
        f.entry(title: _("Default Triggers"), help: "/plugin/email-ext-over-ssh/help/globalConfig/defaultTriggers.html") {
            org.jenkinsci.plugins.emailextoverssh.plugins.EmailTrigger.all().each { t ->
                f.checkbox(name: "defaultTriggers", title: t.displayName, checked: descriptor.defaultTriggerIds.contains(t.id), json: t.id)
                br()
            }
        }
    }

    f.entry(title: _("Content Token Reference"), field: "tokens")
}