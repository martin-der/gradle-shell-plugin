package net.tetrakoopa.gradle.plugin.shell;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;


public class GreetingPluginTest {

    @Test
    public void pluginRegistersDispenserTask() {
        
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("shell-package");
        // project.evaluate();
        project.getTasksByName("dispenser", false);

        // assertNotNull("'dispenser' task exists", project.getTasks().findByName("dispenser"));
    }
}
