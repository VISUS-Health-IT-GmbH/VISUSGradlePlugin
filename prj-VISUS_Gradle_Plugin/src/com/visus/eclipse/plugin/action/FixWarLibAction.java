package com.visus.eclipse.plugin.action;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;


/**
 * 	Action to execute the Gradle "fix_WEB-INF_lib" task
 * 	-> not a standard but fixes issues with Eclipse Tomcat Plugin and Gradle War generation
 * 
 * 	@author hahnen
 */
public class FixWarLibAction extends AbstractHandler {
	/** Overwrite default method */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		return DefaultAction.execute(event, "fix_WEB-INF_lib");
	}
}
