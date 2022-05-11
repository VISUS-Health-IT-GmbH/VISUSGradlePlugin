package com.visus.eclipse.plugin.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.visus.eclipse.plugin.Activator;


/**
 * 	Executes Gradle commands
 * 
 * 	@author hahnen
 */
public class GradleExecutor {
	/** Run a specific Gradle task */
	public static boolean runGradleTask(final IProject project, final IProgressMonitor monitor, final ILog logger,
										final String task) {
		// 1) Try to retrieve the Gradle root project folder (always containing a "gradlew.bat" / "gradlew" file)
		File gradleRootProject = getGradleRootDir(project.getLocation().toFile());
		if (gradleRootProject == null) {
			logger.log(new Status(
				Status.ERROR, Activator.PLUGIN_ID,
				"Gradle cannot be executed due to no Gradle wrapper found in current or any parent directory!"
			));
			
			return false;
		}
		
		logger.log(new Status(
			Status.INFO, Activator.PLUGIN_ID,
			"Gradle root project directory found at: " + gradleRootProject.getAbsolutePath()
		));
		
		try {
			// 2) Create Gradle command based on OS in use (and if sub or root project)
			ProcessBuilder builder = new ProcessBuilder();
			String[] cmd = new String[] {
				"cmd", "/c", "gradlew.bat :" + project.getName() + ":" + task
			};
			if (System.getProperty("os.name").toLowerCase().contains("win")
				&& project.getLocation().toFile().getAbsolutePath() == gradleRootProject.getAbsolutePath()) {
				cmd = new String[] {
					"cmd", "/c", "gradlew.bat " + task
				};
			} else if (!System.getProperty("os.name").toLowerCase().contains("win")
						&& project.getLocation().toFile().getAbsolutePath() != gradleRootProject.getAbsolutePath())	{
				cmd = new String[] {
					"sh", "-c", "gradlew :" + project.getName() + ":" + task
				};
			} else if (!System.getProperty("os.name").toLowerCase().contains("win")
						&& project.getLocation().toFile().getAbsolutePath() == gradleRootProject.getAbsolutePath()) {
				cmd = new String[] {
					"sh", "-c", "gradlew :" + task
				};
			}
			
			logger.log(new Status(
				Status.INFO, Activator.PLUGIN_ID, "Gradle OS specific command is: " + Arrays.toString(cmd)
			));
			
			// 3) Run command and catch input / error streams and exit code
			Process process = builder.command(cmd)
									 .directory(gradleRootProject)
									 .redirectOutput(ProcessBuilder.Redirect.PIPE)
									 .redirectError(ProcessBuilder.Redirect.PIPE)
									 .start();
			
			boolean finished = process.waitFor(5, TimeUnit.MINUTES);
			String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
								.lines()
								.collect(Collectors.joining("\n"));
			
			if (!finished) {
				logger.log(new Status(
					Status.ERROR, Activator.PLUGIN_ID,
					"Gradle task ran into a timeout of 5 Minutes!\nSee output:\n" + output
				));
				
				return false;
			}
			
			int exitCode = process.exitValue();
			logger.log(new Status(
				Status.INFO, Activator.PLUGIN_ID,
				"Gradle task finished with exit code: " + exitCode + "!\nSee output:\n" + output
			));
			
			// 4) Refresh Eclipse "Project Explorer" / "Navigator"
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			
			return exitCode == 0;
		} catch (IOException | InterruptedException | CoreException err) {
			logger.log(new Status(Status.ERROR, Activator.PLUGIN_ID, err.getMessage(), err));
			
			return false;
		}
	}
	
	
	/**
	 * 	Retrieves the Gradle root project directory
	 * 	
	 * 	@param currentDir the directory of a potential subproject
	 * 	@return root directory if found, false otherwise
	 */
	private static File getGradleRootDir(final File currentDir) {
		File tmpDir = currentDir;
		while (tmpDir != null) {
			if ((new File(tmpDir, "gradlew")).exists() && (new File(tmpDir, "gradlew.bat")).exists()) {
				break;
			}
			
			tmpDir = tmpDir.getParentFile();
		}
		
		return tmpDir;
	}
}
