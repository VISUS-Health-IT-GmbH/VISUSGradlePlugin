package com.visus.eclipse.plugin;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.visus.eclipse.plugin.gradle.GradleExecutor;


public class GenerateJarAction extends AbstractHandler {
	/** Execute the action running Gradle "jar" task */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Bundle bundle			= FrameworkUtil.getBundle(Activator.getDefault().getClass());
        final ILog logger 			= Platform.getLog(bundle);
        final ISelection selection	= HandlerUtil.getCurrentSelection(event);

        if (selection instanceof TreeSelection) {
            try {
            	// 1) Get Eclipse project
                final TreeSelection selected	= (TreeSelection) selection;
                final Object firstElement 		= selected.getFirstElement();
                IProject currentProject 		= null;

                if (firstElement instanceof IJavaProject) {
                    currentProject = ((IJavaProject) firstElement).getProject();
                } else if (firstElement instanceof IProject) {
                    currentProject = (IProject) firstElement;
                }
                
                // 2) Check if also a Gradle project
                if (!(new File(currentProject.getLocation().toFile(), "build.gradle")).exists()
            		&& !(new File(currentProject.getLocation().toFile(), "build.gradle.kts")).exists()) {
                	logger.log(new Status(
						Status.INFO, Activator.PLUGIN_ID,
						"This project '" + currentProject.getName()
						+ "' is no Gradle project - no build.gradle / build.gradle.kts found!"
					));
                	
                	return null;
                }
                
                // 3) Run actual Gradle task
                final IProject project 	= currentProject;
                final Job job 			= new Job("Executing Gradle 'jar' task") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						final boolean done = GradleExecutor.runGradleTask(project, monitor, logger, "jar");
						if (done) {
							return Status.OK_STATUS;
						}
						
						logger.log(new Status(
							Status.ERROR, Activator.PLUGIN_ID,
							"Gradle 'jar' task could not be executed on project: " + project.getName()
						));
						
						return Status.CANCEL_STATUS;
					}
                };
                
                job.schedule();
            } catch (Exception exception) {
            	logger.log(new Status(Status.ERROR, Activator.PLUGIN_ID, exception.toString(), exception));
            }
        }
		
		return null;
	}
}
