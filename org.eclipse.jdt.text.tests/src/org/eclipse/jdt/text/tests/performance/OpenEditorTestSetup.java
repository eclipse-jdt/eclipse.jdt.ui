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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipFile;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.text.tests.JdtTextTestPlugin;

public class OpenEditorTestSetup extends TestCase {

	private static final String SWT_ZIP= "/testResources/org.eclipse.swt-R3_0.zip";

	private static final String SWT_PROJECT= "org.eclipse.swt";

	private static final int N_OF_COPIES= 20;

	public void testSetup() {
		try {
			IWorkbench workbench= PlatformUI.getWorkbench();
			IWorkbenchWindow activeWindow= workbench.getActiveWorkbenchWindow();
			IWorkbenchPage activePage= activeWindow.getActivePage();
			
			activePage.hideView(activePage.findViewReference("org.eclipse.ui.internal.introview"));
			
			workbench.showPerspective("org.eclipse.jdt.ui.JavaPerspective", activeWindow);
			
			ResourcesPlugin.getWorkspace().getDescription().setAutoBuilding(false);
			
			String workspacePath= ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/";
			FileTool.unzip(new ZipFile(FileTool.getFileInPlugin(JdtTextTestPlugin.getDefault(), new Path(SWT_ZIP))), new File(workspacePath));
			File oldFile= new File(workspacePath + SWT_PROJECT + "/.classpath_win32");
			File newFile= new File(workspacePath + SWT_PROJECT + "/.classpath");
			assertTrue(oldFile.renameTo(newFile));
			String origPrefix= workspacePath + SWT_PROJECT + "/Eclipse SWT/win32/org/eclipse/swt/graphics/";
			String origName= "TextLayout";
			String origPostfix= ".java";
			duplicate(origPrefix, origName, origPostfix, N_OF_COPIES);
			IProject project= createExistingProject(SWT_PROJECT);
			assertTrue(JavaCore.create(project).exists());
			
			ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
			ResourcesPlugin.getWorkspace().getDescription().setAutoBuilding(true);
			
			workbench.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private void duplicate(String origPrefix, String origName, String origPostfix, int n) throws IOException {
		StringBuffer s= read(origPrefix + origName + origPostfix);
		
		List positions= identifierPositions(s, origName);
		
		for (int j= 0; j < n; j++) {
			StringBuffer c= new StringBuffer(s.toString());
			replacePositions(c, origName.length(), origName + Integer.toString(j), positions);
			write(origPrefix + origName + j + origPostfix, c);
		}
	}

	private void write(String fileName, StringBuffer c) throws IOException {
		Writer writer= null;
		try {
			writer= new FileWriter(fileName);
			writer.write(c.toString());
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
			}
		}
	}

	private void replacePositions(StringBuffer c, int origLength, String string, List positions) {
		int offset= 0;
		for (Iterator iter= positions.iterator(); iter.hasNext();) {
			int position= ((Integer) iter.next()).intValue();
			c.replace(offset + position, offset + position + origLength, string);
			offset += string.length() - origLength;
		}
	}

	private List identifierPositions(StringBuffer buffer, String identifier) {
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

	private StringBuffer read(String fileName) throws FileNotFoundException, IOException {
		StringBuffer s= new StringBuffer();
		Reader reader= null;
		try {
			reader= new FileReader(fileName);
			char[] buffer= new char[8196];
			int chars= reader.read(buffer);
			while (chars != -1) {
				s.append(buffer, 0, chars);
				chars= reader.read(buffer);
			}
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e) {
			}
		}
		return s;
	}

	private IProject createExistingProject(String projectName) throws CoreException {

		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IProject project= workspace.getRoot().getProject(projectName);
		IProjectDescription description= workspace.newProjectDescription(projectName);
		description.setLocation(null);

		project.create(description, null);
		project.open(null);
		return project;
	}
}
