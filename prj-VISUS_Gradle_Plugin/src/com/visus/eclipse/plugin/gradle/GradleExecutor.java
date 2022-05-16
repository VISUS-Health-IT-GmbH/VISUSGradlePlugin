/*  GradleExecutor.java
 *
 *  Copyright (C) 2022, VISUS Health IT GmbH
 *  This software and supporting documentation were developed by
 *    VISUS Health IT GmbH
 *    Gesundheitscampus-Sued 15-17
 *    D-44801 Bochum, Germany
 *    http://www.visus.com
 *    mailto:info@visus.com
 *
 *  -> see LICENCE at root of repository
 */
package com.visus.eclipse.plugin.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
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
										final String task, boolean refreshIDE) {
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
				&& project.getLocation().toFile().toPath().equals(gradleRootProject.toPath())) {
				cmd = new String[] {
					"cmd", "/c", "gradlew.bat " + task
				};
				
				logger.log(new Status(
					Status.INFO, Activator.PLUGIN_ID, project.getName() + " is the root project!"
				));
			} else if (!System.getProperty("os.name").toLowerCase().contains("win")
						&& !project.getLocation().toFile().toPath().equals(gradleRootProject.toPath()))	{
				cmd = new String[] {
					"sh", "-c", "gradlew :" + project.getName() + ":" + task
				};
			} else if (!System.getProperty("os.name").toLowerCase().contains("win")
						&& project.getLocation().toFile().toPath().equals(gradleRootProject.toPath())) {
				cmd = new String[] {
					"sh", "-c", "gradlew :" + task
				};
				
				logger.log(new Status(
					Status.INFO, Activator.PLUGIN_ID, project.getName() + " is the root project!"
				));
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
			
			long timeout = calculateTimeout(project, gradleRootProject);
			boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
			String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
								.lines()
								.collect(Collectors.joining("\n"));
			
			// 4) Refresh Eclipse "Project Explorer" / "Package Explorer"
			if (refreshIDE) {
				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			}
			
			// 5) Check for result or timeout
			if (!finished && output.contains("BUILD SUCCESSFUL in")) {
				logger.log(new Status(
					Status.WARNING, Activator.PLUGIN_ID,
					"Gradle task finished with timout but was successful!\nSee output:\n" + output
				));
			} else if (!finished) {
				logger.log(new Status(
					Status.ERROR, Activator.PLUGIN_ID,
					"Gradle task failed with timeout of " + timeout + " Seconds!\nSee output:\n" + output
				));
				
				return false;
			} else {
				logger.log(new Status(
					Status.INFO, Activator.PLUGIN_ID,
					"Gradle task finished with exit code: " + process.exitValue() + "!\nSee output:\n" + output
				));
			}
			
			return true;
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
	
	
	/**
	 * 	Issue: Some task executions end in a timeout - even though the Gradle task was successful
	 * 	-> calculated timeout better than standard 5 minutes ...
	 * 	
	 * 	@param project the project invoking an action with a Gradle task
	 * 	@param gradleRootProject the Gradle root project found earlier
	 * 	@return timeout in seconds based on Gradle project structure
	 * 	@throws IOException when reading file(s) fails in any way
	 */
	private static long calculateTimeout(final IProject project, File gradleRootProject) throws IOException {
		if (project.getLocation().toFile().toPath().equals(gradleRootProject.toPath())) {
			// 1) Root project takes even longer (check for sub projects)
			File settings = null;
			if (new File(gradleRootProject, "settings.gradle").exists()) {
				settings = new File(gradleRootProject, "settings.gradle");
			} else if (new File(gradleRootProject, "settings.gradle.kts").exists()) {
				settings = new File(gradleRootProject, "settings.gradle.kts");
			} else {
				// I have no idea what happened - just assume 60 seconds!
				return 60;
			}
			
			// For every subproject found add 10 seconds (but at least 30 seconds)!
			String output = Files.readString(settings.toPath());
			int numberOfSubprojects = (output.split(Pattern.quote("include \":")).length -1)
									+ (output.split(Pattern.quote("include(\":")).length -1)
									+ (output.split(Pattern.quote("include \':")).length -1)
									+ (output.split(Pattern.quote("include(\':")).length -1);
			return Math.max(numberOfSubprojects, 3) * 10;
		}
		
		// 2) Sub project (check for project dependencies)
		File build = null;
		if (new File(project.getLocation().toFile(), "build.gradle").exists()) {
			build = new File(project.getLocation().toFile(), "build.gradle");
		} else if (new File(project.getLocation().toFile(), "build.gradle.kts").exists()) {
			build = new File(project.getLocation().toFile(), "build.gradle.kts");
		} else {
			// I have no idea what happened - just assume 60 seconds!
			return 60;
		}
		
		// For every project dependency found add 10 seconds (but at least 30 seconds)!
		String output = Files.readString(build.toPath());
		int numberOfProjectDependencies = (output.split(Pattern.quote("project \":")).length -1)
										+ (output.split(Pattern.quote("project(\":")).length -1)
										+ (output.split(Pattern.quote("project \':")).length -1)
										+ (output.split(Pattern.quote("project(\':")).length -1);
		return Math.max(numberOfProjectDependencies, 3) * 10;
	}
}
