/*  Activator.java
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
package com.visus.eclipse.plugin;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/** The activator class controls the plug-in life cycle */
public class Activator extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "com.visus.eclipse.gradle";	// plug-in ID
	private static Activator plugin;									// shared instance

	
	/** Constructor */
	public Activator() { }

	
	/** Start life cycle */
	@Override
    public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	
	/** Stop life cycle */
	@Override
    public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}


	/** Get the shared instance	*/
	public static Activator getDefault() {
		return plugin;
	}
}
