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

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

public class OpenJavaEditorTest extends OpenEditorTest {

	public static final int N_OF_COPIES= 20;
	
	public static final String PATH= "/Eclipse SWT/win32/org/eclipse/swt/graphics/";
	
	public static final String FILE_PREFIX= "TextLayout";
	
	public static final String FILE_SUFFIX= ".java";
	
	private static final String PERSPECTIVE= "org.eclipse.jdt.ui.JavaPerspective";

	public void testOpenJavaEditor1() throws PartInitException {
		// cold run
		measureOpenInEditor(findFiles(OpenEditorTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, N_OF_COPIES));
	}
	
	public void testOpenJavaEditor2() throws PartInitException {
		// warm run
		measureOpenInEditor(findFiles(OpenEditorTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, N_OF_COPIES));
	}

	public static void setUpWorkspace() throws WorkbenchException, IOException {
		IWorkbench workbench= PlatformUI.getWorkbench();
		IWorkbenchWindow activeWindow= workbench.getActiveWorkbenchWindow();
		String workspacePath= ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/";
		
		workbench.showPerspective(PERSPECTIVE, activeWindow);
		
		duplicate(workspacePath + OpenEditorTestSetup.PROJECT + OpenJavaEditorTest.PATH, OpenJavaEditorTest.FILE_PREFIX, OpenJavaEditorTest.FILE_SUFFIX, OpenJavaEditorTest.N_OF_COPIES);
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
