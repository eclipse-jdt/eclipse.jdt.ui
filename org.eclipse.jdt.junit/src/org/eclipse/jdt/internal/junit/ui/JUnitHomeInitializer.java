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
package org.eclipse.jdt.internal.junit.ui;


import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class JUnitHomeInitializer extends ClasspathVariableInitializer {	
	/**
	 * @see ClasspathVariableInitializer#initialize(String)
	 */
	public void initialize(String variable) {
		Plugin plugin= Platform.getPlugin("org.junit"); //$NON-NLS-1$
		
		if (plugin == null) {
			JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_HOME, null);
		}
		URL installLocation= plugin.getDescriptor().getInstallURL();
		URL local= null;
		try {
			try {
				local= Platform.asLocalURL(installLocation);
			} catch (IOException e) {
				JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_HOME, null);
			}
			JavaCore.setClasspathVariable(JUnitPlugin.JUNIT_HOME, new Path(local.getFile()), null);
		} catch (JavaModelException e1) {
			JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_HOME, null);
		}
	}
}
