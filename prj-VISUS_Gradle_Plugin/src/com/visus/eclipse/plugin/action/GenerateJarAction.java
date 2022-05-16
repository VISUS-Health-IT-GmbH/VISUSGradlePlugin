/*  GenerateJarAction.java
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
package com.visus.eclipse.plugin.action;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;


/**
 * 	Action to execute the Gradle "jar" task
 * 
 * 	@author hahnen
 */
public class GenerateJarAction extends AbstractHandler {
	/** Overwrite default method */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		DefaultAction.execute(event, "jar", true);
		
		return true;
	}
}
