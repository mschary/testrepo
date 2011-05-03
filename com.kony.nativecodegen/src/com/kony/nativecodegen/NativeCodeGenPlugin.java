package com.kony.nativecodegen;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class NativeCodeGenPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.kony.nativecodegen"; //$NON-NLS-1$

	// The shared instance
	private static NativeCodeGenPlugin plugin;

	private String pluginLocation;
	
	/**
	 * The constructor
	 */
	public NativeCodeGenPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		URL fileUrl = FileLocator.toFileURL(FileLocator.find(getBundle(), new Path("/"), null));
		pluginLocation = fileUrl.getFile();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static NativeCodeGenPlugin getDefault() {
		return plugin;
	}
	
	public String getInstallLocation() {
		return pluginLocation;
	}

}
