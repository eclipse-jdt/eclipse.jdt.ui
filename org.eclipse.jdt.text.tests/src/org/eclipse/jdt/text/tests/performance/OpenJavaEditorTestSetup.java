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

package org.eclipse.jdt.text.tests.performance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

public class OpenJavaEditorTestSetup extends TestSetup {

	public OpenJavaEditorTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		String workspacePath= ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/";
		duplicate(workspacePath + PerformanceTestSetup.PROJECT + OpenJavaEditorTest.PATH, OpenJavaEditorTest.FILE_PREFIX, OpenJavaEditorTest.FILE_SUFFIX, OpenJavaEditorTest.N_OF_COPIES);
		ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(PerformanceTestSetup.PROJECT + OpenJavaEditorTest.PATH)).refreshLocal(IResource.DEPTH_INFINITE, null);
	}
	
	protected void tearDown() {
		// do nothing, the actual test runs in its own workbench
	}

	private static void duplicate(String origPrefix, String origName, String origPostfix, int n) throws IOException {
		StringBuffer s= FileTool.read(origPrefix + origName + origPostfix);
		
		List positions= identifierPositions(s, origName);
		
		for (int j= 0; j < n; j++) {
			StringBuffer c= new StringBuffer(s.toString());
			replacePositions(c, origName.length(), origName + Integer.toString(j), positions);
			FileTool.write(origPrefix + origName + j + origPostfix, c);
		}
	}

	private static void replacePositions(StringBuffer c, int origLength, String string, List positions) {
		int offset= 0;
		for (Iterator iter= positions.iterator(); iter.hasNext();) {
			int position= ((Integer) iter.next()).intValue();
			c.replace(offset + position, offset + position + origLength, string);
			offset += string.length() - origLength;
		}
	}

	private static List identifierPositions(StringBuffer buffer, String identifier) {
		List positions= new ArrayList();
		int i= -1;
		while (true) {
			i= buffer.indexOf(identifier, i + 1);
			if (i == -1)
				break;
			if (i > 0 && Character.isJavaIdentifierPart(buffer.charAt(i - 1)))
				continue;
			if (i < buffer.length() - 1 && Character.isJavaIdentifierPart(buffer.charAt(i + identifier.length())))
				continue;
			positions.add(new Integer(i));
		}
		return positions;
	}
}
