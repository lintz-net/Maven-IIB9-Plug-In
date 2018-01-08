package ch.sbb.maven.plugins.iib.mojos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import ch.sbb.maven.plugins.iib.utils.MqsiCommand;
import ch.sbb.maven.plugins.iib.utils.MqsiCommandLauncher;
import ch.sbb.maven.plugins.iib.utils.SkipUtil;


/**
 * Deploys a bar to the designated broker
 */

@Mojo(name = "mqsideploy", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class MqsiDeployMojo extends AbstractMojo {


    /**
     * sends internal debug trace information to the specified file.
     */
    @Parameter(property = "mqsiDeployTraceFileName", defaultValue = "${project.build.directory}/mqsideploytracefile.txt", required = true)
    protected File mqsiDeployTraceFileName;


    /**
     * indicates the absolute file path to the location of the mqsiprofile command or shell script.
     */
    @Parameter(property = "pathToMqsiProfileScript", defaultValue = "\"C:\\Program Files\\IBM\\MQSI\\9.0.0.2\\bin\\mqsiprofile.cmd\"", required = false)
    protected String pathToMqsiProfileScript;


    /**
     * a comma-separated list of commands that will be issued to the underlying os before launching the mqsi* command.
     * This will substitute for the Windows approach covered by the 'pathToMqsiProfileScript' value. These
     * commands should be operating system specific and
     * execute the mqsiprofile command as well as setup the launch of the followup mqsi command
     * 
     */
    @Parameter(property = "mqsiPrefixCommands", required = false)
    protected String mqsiPrefixCommands;

    @Parameter(property = "mqsiDeployReplacementCommand", required = false, defaultValue = "")
    protected String mqsiDeployReplacementCommand;


    /**
     * The path of the workspace
     */
    @Parameter(property = "workspace", required = true)
    protected File workspace;


    /**
     * The Maven Project Object
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;


    private void addParamFor(Object entry, String paramMarker, List<String> params)
    {
        if (entry == null)
        {
            return;
        }
        else if (entry.getClass().equals(String.class))
        {
            String s = (String) entry;
            if (s != null && !s.trim().isEmpty())
            {
                if (paramMarker != null) {
                    params.add(paramMarker);
                }
                params.add(s);
            }
        }
        else {
            if (entry != null)
            {
                if (paramMarker != null) {
                    params.add(paramMarker);
                }
                params.add("" + entry.toString());
            }

        }
    }


    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {

        if (new SkipUtil().isSkip(this.getClass())) {
            return;
        }

        List<String> environments = getValidEnvironments();
        if (environments.isEmpty())
        {
            String message = "Unable to deploy bar files in iib-overrides directory";
            getLog().warn(message);
            return;
        }

        String buildDir = project.getBuild().getDirectory();
        File buildDirectory = new File(buildDir);
        File overridesDir = new File(buildDirectory, "iib-overrides");
        File resultsFile = new File(overridesDir, "deployment.results");
        Properties resultsProperties = new Properties();

        for (String environment : environments)
        {


            String barFilename = project.getArtifactId() + "-" + project.getVersion() + "-" + environment + ".bar";
            File barFile = new File(overridesDir, barFilename);
            String barFilePath = barFile.getAbsolutePath();
            String brokerFileName = environment + ".broker";
            File brokerFile = new File(overridesDir, brokerFileName);
            String brokerFilePath = brokerFile.getAbsolutePath();
            String traceFileName = project.getArtifactId() + "-" + project.getVersion() + "-" + environment + "-deploy-trace.txt";
            String traceFilePath = new File(overridesDir, traceFileName).getAbsolutePath();

            String deployConfigFileName = environment + ".deploy-config";
            File deployConfigFile = new File(overridesDir, deployConfigFileName);
            Properties deployConfigProperties = new Properties();
            if (deployConfigFile.exists())
            {
                try {
                    deployConfigProperties.load(new FileInputStream(deployConfigFile));
                } catch (Exception e) {
                    throw new MojoFailureException("unable to load property file at :" + deployConfigFile + ";" + e);
                }
            }


            List<String> params = new ArrayList<String>();
            addBrokerSpecParams(brokerFile, brokerFilePath, deployConfigProperties, params);
            String integrationServerName = getProperty("integrationServerName", deployConfigProperties);
            addParamFor(integrationServerName, "-e", params);
            addParamFor(barFilePath, "-a", params);

            addParamFor(getProperty("deployedObjects", deployConfigProperties), "-d", params);
            String completeDeployment = getProperty("completeDeployment", deployConfigProperties);
            if (completeDeployment != null && completeDeployment.equalsIgnoreCase("true"))
            {
                params.add("-m");
            }

            String timeoutSecs = getProperty("timeoutSecs", deployConfigProperties);
            if (timeoutSecs != null)
            {
                addParamFor(timeoutSecs, "-s", params);
                addParamFor(timeoutSecs, "-w", params);
            }
            addParamFor(traceFilePath, "-v", params);

            try {

                new MqsiCommandLauncher().execute(
                        getLog(),
                        pathToMqsiProfileScript,
                        mqsiPrefixCommands,
                        MqsiCommand.mqsideploy,
                        params.toArray(new String[params.size()]),
                        mqsiDeployReplacementCommand
                        );
                resultsProperties.setProperty(environment, barFilePath);

            } catch (Exception e)
            {

                throw new MojoFailureException(e.toString());
            } finally
            {

                try
                {
                    FileOutputStream fos = new FileOutputStream(resultsFile);
                    getLog().info("Storing deployment summary results to " + resultsFile.getAbsolutePath());
                    String comments = "The key value represents the environmentId while the value represents the file location of the successfully deployed bar file";
                    resultsProperties.store(fos, comments);
                    fos.flush();
                    fos.close();

                } catch (Exception ignore)
                {

                }


            }

        }


    }

    private void addBrokerSpecParams(File brokerFile, String brokerFilePath, Properties deployConfigProperties, List<String> params) {
        String brokerName = getProperty("brokerName", deployConfigProperties);

        if (brokerName != null)
        {
            addParamFor(brokerName, null, params);
        }
        else if (brokerFile.exists())
        {
            addParamFor(brokerFilePath, "-n", params);
        }
        else
        {
            String ipAddress = getProperty("ipAddress", deployConfigProperties);
            String port = getProperty("port", deployConfigProperties);
            String qMgr = getProperty("qMgr", deployConfigProperties);
            addParamFor(ipAddress, "-i", params);
            addParamFor(port, "-p", params);
            addParamFor(qMgr, "-q", params);
        }
    }


    private String getProperty(String key, Properties properties)
    {
        if (properties == null) {
            return null;
        }
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }


    private List<String> getValidEnvironments()
    {
        List<String> validEnvironmentNames = new ArrayList<String>();
        String buildDir = project.getBuild().getDirectory();
        File buildDirectory = new File(buildDir);


        File overridesDir = new File(buildDirectory, "iib-overrides");

        TreeSet<String> possibleEnvironmentNames = new TreeSet<String>();
        for (File file : overridesDir.listFiles())
        {
            if (file.getName().endsWith(".properties"))
            {
                String value = file.getName().substring(0, file.getName().lastIndexOf(".properties"));
                possibleEnvironmentNames.add(value);
            }
        }

        for (String possibleEnvironmentName : possibleEnvironmentNames)
        {
            // / needs to have a correctly names bar file
            String barFilename = project.getArtifactId() + "-" + project.getVersion() + "-" + possibleEnvironmentName + ".bar";
            File barFile = new File(overridesDir, barFilename);
            if (!barFile.exists())
            {
                getLog().warn("The bar file " + barFile.getAbsolutePath() + " could not be found for deployment");
                continue;
            }
            // / needs to have a broker file or config file
            String brokerFilename = possibleEnvironmentName + ".broker";
            File brokerFile = new File(overridesDir, brokerFilename);
            String deployConfigFileName = possibleEnvironmentName + ".deploy-config";
            File deployConfigFile = new File(overridesDir, deployConfigFileName);


            if (!brokerFile.exists() && !deployConfigFile.exists())
            {
                String message = "Missing both .broker and .deploy-config for " + possibleEnvironmentName
                        + ". \n\tThe file " + brokerFile.getAbsolutePath() + " and \n\tthe file " + deployConfigFile.getAbsolutePath() + " were not found.";
                getLog().warn(message);
                getLog().warn("The bar file " + barFile.getAbsolutePath() + " cannot be deployed");
                continue;
            }

            validEnvironmentNames.add(possibleEnvironmentName);
        }


        return validEnvironmentNames;
    }


}
