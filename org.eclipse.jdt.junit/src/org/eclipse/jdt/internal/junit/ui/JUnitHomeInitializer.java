/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;


import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;

public class JUnitHomeInitializer extends ClasspathVariableInitializer {	
	/**
	 * @see ClasspathVariableInitializer#initialize(String)
	 */
	public void initialize(String variable) {
		Bundle bundle= Platform.getBundle("org.junit"); //$NON-NLS-1$
		if (bundle == null) {
			JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_HOME, null);
			return;
		}
		URL installLocation= bundle.getEntry("/"); //$NON-NLS-1$
		URL local= null;
		try {
			local= Platform.asLocalURL(installLocation);
		} catch (IOException e) {
			JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_HOME, null);
			return;
		}
		try {
			String fullPath= new File(local.getPath()).getAbsolutePath();
			JavaCore.setClasspathVariable(JUnitPlugin.JUNIT_HOME, new Path(fullPath), null);
		} catch (JavaModelException e1) {
			JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_HOME, null);
		}
	}
}
