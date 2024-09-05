/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.infra;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;


public class RefactoringTestPlugin extends Plugin {

	private static RefactoringTestPlugin fgDefault;

	public RefactoringTestPlugin() {
		fgDefault= this;
	}

	public static RefactoringTestPlugin getDefault() {
		return fgDefault;
	}

	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public InputStream getTestResourceStream(String fileName) throws IOException {
		IPath path= new Path("resources").append(fileName);
		URL url= getBundle().getEntry("/"+ path.toString());
		return url.openStream();
	}

}
