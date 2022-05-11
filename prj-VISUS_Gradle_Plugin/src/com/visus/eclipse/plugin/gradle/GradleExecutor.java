package com.visus.eclipse.plugin.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
			// 2) Create Gradle command based on OS in use
			ProcessBuilder builder = new ProcessBuilder();
			String[] cmd = new String[] {
				"cmd.exe", "/c", "gradlew.bat", ":" + project.getName() + ":jar"
			};
			if (!System.getProperty("os.name").toLowerCase().contains("win")) {
				cmd = new String[] {
					"sh", "-c", "gradlew", ":" + project.getName() + ":jar"
				};
			}
			
			logger.log(new Status(Status.INFO, Activator.PLUGIN_ID, "Gradle OS specific command is: " + cmd));
			
			// 3) Run command and catch input / error streams and exit code
			Process process = builder.command(cmd).directory(gradleRootProject).start();
			BufferedReader is = inputStream(process);
			BufferedReader es = errorStream(process);
			
			int exitCode = -1;
			if (!process.waitFor(5, TimeUnit.MINUTES)) {
				logger.log(new Status(
					Status.ERROR, Activator.PLUGIN_ID, "Gradle task ran into a timeout of 5 Minutes!"
				));
			} else {
				exitCode = process.exitValue();
				logger.log(new Status(
					Status.INFO, Activator.PLUGIN_ID, "Gradle task finished with exit code: " + exitCode
				));
			}
			
			
			// 4) Log possible output for easier debugging
			logger.log(new Status(Status.INFO, Activator.PLUGIN_ID, "Output stream: " + resolveBufferedReader(is)));
			logger.log(new Status(Status.INFO, Activator.PLUGIN_ID, "Error stream: " + resolveBufferedReader(es)));
			
			// 5) Refresh Eclipse "Project Explorer" / "Navigator"
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			
			return exitCode == 0;
		} catch (Exception err) {
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
	
	
	/**
	 * 	Retrieves the input stream of a process
	 * 	
	 * 	@param process to get input stream from
	 * 	@return input stream
	 */
	private static BufferedReader inputStream(Process process) {
		return new BufferedReader(new InputStreamReader(process.getInputStream()));
	}
	
	
	/**
	 * 	Retrieves the error stream of a process
	 * 	
	 * 	@param process to get error stream from
	 * 	@return error stream
	 */
	private static BufferedReader errorStream(Process process) {
		return new BufferedReader(new InputStreamReader(process.getErrorStream()));
	}
	
	
	/**
	 * 	Converts the content of a buffered reader to a string
	 * 
	 * 	@param stream the buffered reader to be evaluated
	 * 	@return content of stream provided
	 * 	@throws IOException when working with stream fails
	 */
	private static String resolveBufferedReader(BufferedReader stream) throws IOException {
		StringBuilder builder = new StringBuilder();
		String text = null;
		
		while ((text = stream.readLine()) != null) {
			builder.append(text);
			builder.append(System.getProperty("line.separator"));
		}
		
		return builder.toString();
	}
}
