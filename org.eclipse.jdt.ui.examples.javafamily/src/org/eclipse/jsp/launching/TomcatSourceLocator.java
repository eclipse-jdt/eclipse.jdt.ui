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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.ui.JavaUISourceLocator;

/**
 * A source locator for a Tomcat launch configuration
 */
public class TomcatSourceLocator extends JavaUISourceLocator {
	
	/**
	 * Root web app folder, or <code>null</code> if none
	 */
	private IContainer fWebAppRoot;
	
	public static final String ID_TOMCAT_SOURCE_LOCATOR = "org.eclipse.jsp.TOMCAT_SOURCE_LOCATOR"; //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISourceLocator#getSourceElement(org.eclipse.debug.core.model.IStackFrame)
	 */
	public Object getSourceElement(IStackFrame stackFrame) {
		if (fWebAppRoot != null) {
			IJavaStackFrame frame = (IJavaStackFrame)stackFrame.getAdapter(IJavaStackFrame.class);
			if (frame != null) {
				String sourceName;
				try {
					sourceName = frame.getSourceName();
					if (sourceName != null && sourceName.endsWith("jsp")) { //$NON-NLS-1$
						String sourcePath = frame.getSourcePath("JSP"); //$NON-NLS-1$
						if (sourcePath == null) {
							sourcePath = sourceName;
						}
						IPath path = new Path(sourcePath);
						IFile file = fWebAppRoot.getFile(path);
						if (file.exists()) {
							return file;
						} else {
							return null;
						}
					}
				} catch (DebugException e) {
					e.printStackTrace();
				}
			}
		}
		return super.getSourceElement(stackFrame);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IPersistableSourceLocator#initializeDefaults(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeDefaults(ILaunchConfiguration configuration) throws CoreException {
		super.initializeDefaults(configuration);
		String root = configuration.getAttribute(TomcatLaunchDelegate.ATTR_WEB_APP_ROOT, (String)null);
		if (root != null) {
			IPath path = new Path(root);
			IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (resource != null && resource instanceof IContainer) {
				fWebAppRoot = (IContainer)resource;
			} 
		}
	}
	
	

}
