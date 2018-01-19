def l = namespace(lib.LayoutTagLib)

def requiresAdmin = app.getDescriptor('org.jenkinsci.plugins.emailextoverssh.ExtendedEmailPublisher').adminRequiredForTemplateTesting
def hasPermission = requiresAdmin ? h.hasPermission(app.ADMINISTER) : h.hasPermission(this.action.project, this.action.project.CONFIGURE)

if(hasPermission) {
    l.task(icon: "/plugin/email-ext/images/template-debugger.png", title: this.action.displayName, 
        href: h.getActionUrl(my.url, this.action), contextMenu: h.isContextMenuVisible(this.action))
}