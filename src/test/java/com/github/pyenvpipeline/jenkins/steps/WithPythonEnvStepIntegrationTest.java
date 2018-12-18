/*
 * The MIT License
 *
 * Copyright 2017 Colin Starner.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.github.pyenvpipeline.jenkins.steps;

import com.github.pyenvpipeline.jenkins.VirtualenvManager;
import hudson.Functions;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import jenkins.plugins.shiningpanda.tools.PythonInstallation;
import jenkins.plugins.shiningpanda.tools.PythonInstallationFinder;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import static org.junit.Assert.assertTrue;

public class WithPythonEnvStepIntegrationTest {

    private static final String OS_REPLACE_TARGET ="{{OS_SHELL_COMMAND}}";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public LoggerRule loggingRule = new LoggerRule();

    private static final String shiningPandaTargetInstallation = "jenkins.plugins.shiningpanda.tools.PythonInstallation";

    private<T extends ToolInstallation> T[] unboxToolInstallations(ToolDescriptor<T> descriptor) {
        return descriptor.getInstallations();
    }

    private String formatOSSpecificNodeScipts(String osAgnosticScript) {
        if (Functions.isWindows()) {
            return osAgnosticScript.replaceAll(Pattern.quote(OS_REPLACE_TARGET), "bat");
        } else {
            return osAgnosticScript.replaceAll(Pattern.quote(OS_REPLACE_TARGET), "sh");
        }
    }

    private PythonInstallation findSinglePythonInstallation(ToolDescriptor descriptor) throws Exception {

        PythonInstallation installation = null;

        if (descriptor.getId().equals(shiningPandaTargetInstallation)) {
            ToolInstallation[] installations = unboxToolInstallations(descriptor);

            if (installations.length > 0) {
                installation = (PythonInstallation) installations[0];
            } else {
                List<PythonInstallation> foundInstallations = PythonInstallationFinder.getInstallations();

                if (foundInstallations.size() > 0) {
                    installation = foundInstallations.get(0);
                }
            }
        }

        return installation;
    }

    private PythonInstallation findFirstPythonInstallation() throws Exception{
        PythonInstallation installation = null;

        for (ToolDescriptor<? extends ToolInstallation> desc: ToolInstallation.all()) {
            installation = findSinglePythonInstallation(desc);
            if (installation != null) {
                PythonInstallation.DescriptorImpl cast = (PythonInstallation.DescriptorImpl) desc;
                cast.setInstallations(installation);
                break;
            }
        }

        return installation;
    }

    @Test
    public void ensureDurableTaskCompatibility() throws Exception {
        // We ensure that returnStdout works by examining the lines of output from a Pipeline script that activates
        // a withPythonEnv block, and outputs the version of the Python command that prints the version of the
        // interpreter. If the prefix "python version: " is found in a line of the logs, and that line contains any
        // other text after the prefix, we will know that the returnStdout optional argument works for these commands
        // in a manner compatible with the "sh" and "bat" steps.

        PythonInstallation installation = findFirstPythonInstallation();

        // Python versions prior to 3.4 outputed the results of "python --version" to stderr; 3.4 and after use stdout.
        // Additionally, we can't use print in this instance, due to statement incompatibility between Python 2 and
        // Python 3. This is a semantically equivalent workaround that should work regardless of Python version
        String platformInlinePythonCommand = "python -c \\\"import platform; import sys; sys.stdout.write(platform.python_version()+\\'\\\\n\\')\\\"";

        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "p");
        String script = formatOSSpecificNodeScipts("node { withPythonEnv( pythonInstallation: '" + installation.getName() + "', pythonEnvInstallation: '" + installation.getName() +"') { def version = " +
                OS_REPLACE_TARGET +"(script: '" + platformInlinePythonCommand + "', returnStdout: true).trim()\n" +
                "echo \"python version: ${version}\"" +
                "}  }");
        job.setDefinition(new CpsFlowDefinition(script,true));

        WorkflowRun run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        String logs = JenkinsRule.getLog(run);

        boolean foundOutput = false;
        String prefix = "python version: ";
        for (String line: logs.split("\n")) {
            if (line.startsWith(prefix)) {
                foundOutput = !line.equals(prefix);
                break;
            }
        }

        Assert.assertTrue(foundOutput);
    }

    @Test
    public void shouldWorkWithOneArgument() throws Exception {
        PythonInstallation installation = getPythonInstallation();
        String workflowScript = "node { withPythonEnv('" + installation.getName() + "') {  } }";
        shouldUseShiningPanda(workflowScript, installation);
    }

    @Test
    public void shouldWorkWithTwoArguments() throws Exception {
        PythonInstallation installation = getPythonInstallation();
        String workflowScript = "node { withPythonEnv( pythonInstallation: '" + installation.getName() + "', pythonEnvInstallation: '" + installation.getName() +"') {  } }";
        shouldUseShiningPanda(workflowScript, installation);
    }

    public PythonInstallation getPythonInstallation() throws Exception {
        // Here, we dictate a single PythonInstallation to be used for the ShiningPanda test, so
        // that we can pick the appropriate name, and verify that it is used later down the line
        // Note that this will not work if there are no findable Python Installations on the testing
        // system (i.e. findable via the PythonFinder class provided by the ShiningPanda plugin).
        PythonInstallation installation = findFirstPythonInstallation();
        Assume.assumeTrue(installation != null);

        return installation;

    }

    public void shouldUseShiningPanda(String workflowScript, PythonInstallation installation) throws Exception {
        loggingRule = loggingRule.capture(300);
        loggingRule.record(VirtualenvManager.class, Level.FINE);
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "p");
        job.setDefinition(new CpsFlowDefinition(workflowScript,
                true));
        j.assertBuildStatusSuccess(job.scheduleBuild2(0));
        // Verifies that the presence of ShiningPanda installation is respected
        List<String> messages = loggingRule.getMessages();

        String expectedFoundFlaggedShiningPandaString = "Found Python ToolDescriptor: "
                + shiningPandaTargetInstallation;
        boolean foundExpectedFlaggedShiningPanda = false;
        String expectedMatchedShiningPandaString = "Matched ShiningPanda tool name: " + installation.getName();
        boolean foundExpectedMatchedShiningPanda = false;

        for (String message : messages) {
            if (!foundExpectedFlaggedShiningPanda) {
                foundExpectedFlaggedShiningPanda = message.contains(expectedFoundFlaggedShiningPandaString);
            }

            if (!foundExpectedMatchedShiningPanda) {
                foundExpectedMatchedShiningPanda = message.contains(expectedMatchedShiningPandaString);
            }

            if (foundExpectedFlaggedShiningPanda && foundExpectedMatchedShiningPanda) {
                break;
            }
        }

        assertTrue(foundExpectedFlaggedShiningPanda);
        assertTrue(foundExpectedMatchedShiningPanda);
    }
}
