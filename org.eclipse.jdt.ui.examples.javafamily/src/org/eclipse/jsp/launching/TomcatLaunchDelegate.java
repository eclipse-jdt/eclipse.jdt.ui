/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp.launching;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.variables.LaunchVariableUtil;
import org.eclipse.jdt.internal.launching.JavaLocalApplicationLaunchConfigurationDelegate;
import org.eclipse.jsp.JspUIPlugin;

/**
 * Launch delegate for a local Tomcat server
 */
public class TomcatLaunchDelegate extends JavaLocalApplicationLaunchConfigurationDelegate {

	/**
	 * Identifier for Tomcat launch configurations.
	 */
	public static final String ID_TOMCAT_LAUNCH_CONFIGURATION_TYPE = "org.eclipse.jsp.TomcatConfigurationType"; //$NON-NLS-1$
	
	/**
	 * Identifier for Tomcat classpath provider.
	 */
	public static final String ID_TOMCAT_CLASSPATH_PROVIDER = "org.eclipse.jsp.tomcatClasspathProvider"; //$NON-NLS-1$
		
	/**
	 * Launch configuration attribute - value is path to local installation of Tomcat.
	 * The path may be encoded in a launch variable.
	 */
	public static final String ATTR_CATALINA_HOME = "org.eclipse.jsp.CATALINA_HOME"; //$NON-NLS-1$

	/**
	 * Constructs a new launch delegate
	 */
	public TomcatLaunchDelegate() {
		super();
	}

	/**
	 * Returns the value of the <code>${catalina_home}</code> launch variable.
	 * 
	 * @return the value of the <code>${catalina_home}</code> launch variable
	 * @exception CoreException if the variable or value is undefined
	 */
	public static String getCatalinaHome() throws CoreException {
		MultiStatus status = new MultiStatus(JspUIPlugin.getDefault().getDescriptor().getUniqueIdentifier(), 0, "Unable to resolve ${catalina_home}", null);
		String[] vars = LaunchVariableUtil.expandStrings("${catalina_home}", status, null); //$NON-NLS-1$
		if (status.isOK() && vars != null && vars.length == 1) {
			String home = vars[0];
			IStatus err = null;
			if (home != null && home.length() > 0) {
				File file = new File(home);
				if (file.exists() && file.isDirectory()) {
					return home;
				} else {
					err = new Status(IStatus.ERROR, JspUIPlugin.getDefault().getDescriptor().getUniqueIdentifier(), 0, MessageFormat.format("Specified $catalina_home does not exist: {0}", new String[]{home}), null);
				}
			} else {
				err = new Status(IStatus.ERROR, JspUIPlugin.getDefault().getDescriptor().getUniqueIdentifier(), 0, "Unable to resolve ${catalina_home}", null);
			}
			throw new CoreException(err);
		}
		throw new CoreException(status);
	}
}
