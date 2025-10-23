package net.tetrakoopa.gradle.plugin.shell;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;


public class GreetingPluginTest {
    @Test
    public void dummy() {
    }
    public void pluginRegistersATask() {
        // Create a test project and apply the plugin
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("shell");

        // Verify the result
        assertNotNull(project.getTasks().findByName("dispenser"));
    }
}
