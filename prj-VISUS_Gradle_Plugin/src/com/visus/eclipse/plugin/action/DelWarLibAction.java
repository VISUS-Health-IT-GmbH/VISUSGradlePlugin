package com.visus.eclipse.plugin.action;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;


/**
 * 	Action to execute the Gradle "del_WEB-INF_lib" task
 * 	-> not a standard but deletes the WEB-INF/lib folder created using Gradle "fix_WEB-INF_lib" task
 * 
 * 	@author hahnen
 */
public class DelWarLibAction extends AbstractHandler {
	/** Overwrite default method */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		return DefaultAction.execute(event, "del_WEB-INF_lib");
	}
}
