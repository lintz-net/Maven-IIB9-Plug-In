package ch.sbb.maven.plugins.iib.mojos;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import ch.sbb.maven.plugins.iib.utils.ConfigurationValidator;
import ch.sbb.maven.plugins.iib.utils.SkipUtil;

/**
 * Initializes the Zip Build Workspace.
 */
@Mojo(name = "initialize-zip-build-workspace", requiresDependencyResolution = ResolutionScope.TEST)
public class InitializeZipBuildWorkspaceMojo extends AbstractMojo {


    /**
     * The Maven Project Object
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (new SkipUtil().isSkip(this.getClass())) {
            return;
        }

        ConfigurationValidator.validadeAndFixConfigurationNatureOfProject(new File(project.getBasedir(), ".project"));

    }
}
