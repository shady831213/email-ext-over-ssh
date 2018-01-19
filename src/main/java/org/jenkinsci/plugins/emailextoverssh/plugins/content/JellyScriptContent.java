package org.jenkinsci.plugins.emailextoverssh.plugins.content;

import hudson.FilePath;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.emailextoverssh.ExtendedEmailPublisherDescriptor;
import org.jenkinsci.plugins.emailextoverssh.JellyTemplateConfig.JellyTemplateConfigProvider;
import org.jenkinsci.plugins.emailextoverssh.plugins.EmailToken;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.Script;
import org.apache.commons.jelly.XMLOutput;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.JellyLanguage;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.xml.sax.InputSource;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.attribute.UserDefinedFileAttributeView;

@EmailToken
public class JellyScriptContent extends AbstractEvalContent {

    public static final String MACRO_NAME = "JELLY_SCRIPT";
    private static final String DEFAULT_HTML_TEMPLATE_NAME = "html";
    private static final String DEFAULT_TEMPLATE_NAME = DEFAULT_HTML_TEMPLATE_NAME;
    public static final String JELLY_EXTENSION = ".jelly";

    @Parameter
    public String template = DEFAULT_TEMPLATE_NAME;

    public JellyScriptContent() {
        super(MACRO_NAME);
    }

    @Override
    public String evaluate(@Nonnull Run<?, ?> run, FilePath workspace, @Nonnull TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        InputStream inputStream = null;

        try {
            inputStream = getFileInputStream(workspace, template, JELLY_EXTENSION);
            return renderContent(run, inputStream, listener);
        } catch (JellyException e) {
            return "JellyException: " + e.getMessage();
        } catch (FileNotFoundException e) {
            return generateMissingFile("Jelly", template);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    protected Class<? extends ConfigProvider> getProviderClass() {
        return JellyTemplateConfigProvider.class;
    }

    private String renderContent(@Nonnull Run<?, ?> build, InputStream inputStream, @Nonnull TaskListener listener)
            throws JellyException, IOException {
        String rawScript = IOUtils.toString(inputStream);
        if (inputStream instanceof UserProvidedContentInputStream) {
            Item parent = build.getParent();
            ScriptApproval.get().configuring(rawScript, JellyLanguage.get(), ApprovalContext.create().withItem(parent));
            ScriptApproval.get().using(rawScript, JellyLanguage.get());
        }

        JellyContext context = createContext(new ScriptContentBuildWrapper(build), build, listener);
        Script script = context.compileScript(new InputSource(new StringReader(rawScript)));

        if (script != null) {
            return convert(build, context, script);
        }
        return null;
    }

    private String convert(Run<?, ?> build, JellyContext context, Script script)
            throws JellyTagException, IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(16 * 1024);
        XMLOutput xmlOutput = XMLOutput.createXMLOutput(output);
        script.run(context, xmlOutput);
        xmlOutput.flush();
        xmlOutput.close();
        output.close();
        return output.toString(getCharset(build));
    }

    private JellyContext createContext(Object it, @Nonnull Run<?, ?> build, @Nonnull TaskListener listener) {
        JellyContext context = new JellyContext();
        ExtendedEmailPublisherDescriptor descriptor = Jenkins.getActiveInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        context.setVariable("it", it);
        context.setVariable("build", build);
        context.setVariable("project", build.getParent());
        context.setVariable("logger", listener.getLogger());
        context.setVariable("rooturl", descriptor.getHudsonUrl());
        return context;
    }
}
