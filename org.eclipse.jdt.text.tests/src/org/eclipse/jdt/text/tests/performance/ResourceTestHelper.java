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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Preferences;

public class ResourceTestHelper {

	public static final int FAIL_IF_EXISTS= 0;
	
	public static final int OVERWRITE_IF_EXISTS= 1;
	
	public static final int SKIP_IF_EXISTS= 2;

	public static void replicate(String src, String destPrefix, String destSuffix, int n, int ifExists) throws CoreException {
		for (int i= 0; i < n; i++)
			copy(src, destPrefix + i + destSuffix, ifExists);
	}

	public static void copy(String src, String dest) throws CoreException {
		copy(src, dest, FAIL_IF_EXISTS);
	}

	public static void copy(String src, String dest, int ifExists) throws CoreException {
		if (handleExisting(dest, ifExists))
			getFile(src).copy(new Path(dest), true, null);
	}

	private static boolean handleExisting(String dest, int ifExists) throws CoreException {
		IFile destFile= getFile(dest);
		switch (ifExists) {
			case FAIL_IF_EXISTS:
				if (destFile.exists())
					throw new IllegalArgumentException("Destination file exists: " + dest);
				return true;
			case OVERWRITE_IF_EXISTS:
				if (destFile.exists())
					destFile.delete(true, null);
				return true;
			case SKIP_IF_EXISTS:
				if (destFile.exists())
					return false;
				return true;
			default:
				throw new IllegalArgumentException();
		}
	}

	private static IFile getFile(String path) {
		return getRoot().getFile(new Path(path));
	}

	public static void delete(String file) throws CoreException {
		getFile(file).delete(true, null);
	}

	public static void delete(String prefix, String suffix, int n) throws CoreException {
		for (int i= 0; i < n; i++)
			delete(prefix + i + suffix);
	}

	public static IFile findFile(String pathStr) {
		IFile file= getFile(pathStr);
		Assert.assertTrue(file != null && file.exists());
		return file;
	}

	public static IFile[] findFiles(String prefix, String suffix, int i, int n) {
		List files= new ArrayList(n);
		for (int j= i; j < i + n; j++) {
			String path= prefix + j + suffix;
			files.add(findFile(path));
		}
		return (IFile[]) files.toArray(new IFile[files.size()]);
	}

	public static StringBuffer read(String src) throws IOException, CoreException {
		return FileTool.read(new InputStreamReader(getFile(src).getContents()));
	}

	public static void write(String dest, final String content) throws IOException, CoreException {
		InputStream stream= new InputStream() {
			private Reader fReader= new StringReader(content);
			public int read() throws IOException {
				return fReader.read();
			}
		};
		getFile(dest).create(stream, true, null);
	}
	

	public static void replicate(String src, String destPrefix, String destSuffix, int n, String srcName, String destNamePrefix, int ifExists) throws IOException, CoreException {
		StringBuffer s= read(src);
		List positions= identifierPositions(s, srcName);
		for (int j= 0; j < n; j++) {
			String dest= destPrefix + j + destSuffix;
			if (handleExisting(dest, ifExists)) {
				StringBuffer c= new StringBuffer(s.toString());
				replacePositions(c, srcName.length(), destNamePrefix + j, positions);
				write(dest, c.toString());
			}
		}
	}

	public static void copy(String src, String dest, String srcName, String destName, int ifExists) throws IOException, CoreException {
		if (handleExisting(dest, ifExists)) {
			StringBuffer buf= read(src);
			List positions= identifierPositions(buf, srcName);
			replacePositions(buf, srcName.length(), destName, positions);
			write(dest, buf.toString());
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

	private static IWorkspaceRoot getRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	public static void incrementalBuild() throws CoreException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
	}

	public static void fullBuild() throws CoreException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
	}

	public static boolean disableAutoBuilding() {
		IWorkspaceDescription description= ResourcesPlugin.getWorkspace().getDescription();
		boolean wasOn= description.isAutoBuilding();
		if (wasOn)
			description.setAutoBuilding(false);
		return wasOn;
	}

	public static boolean enableAutoBuilding() {
		IWorkspaceDescription description= ResourcesPlugin.getWorkspace().getDescription();
		boolean wasOff= !description.isAutoBuilding();
		if (wasOff)
			description.setAutoBuilding(true);
		return wasOff;
	}
	
	public static boolean setAutoBuilding(boolean value) {
		Preferences preferences= ResourcesPlugin.getPlugin().getPluginPreferences();
		boolean oldValue= preferences.getBoolean(ResourcesPlugin.PREF_AUTO_BUILDING);
		if (value != oldValue)
			preferences.setValue(ResourcesPlugin.PREF_AUTO_BUILDING, value);
		return oldValue;
	}

	public static IProject createExistingProject(String projectName) throws CoreException {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IProject project= workspace.getRoot().getProject(projectName);
		IProjectDescription description= workspace.newProjectDescription(projectName);
		description.setLocation(null);
	
		project.create(description, null);
		project.open(null);
		return project;
	}

	public static IProject createProjectFromZip(Plugin installationPlugin, String projectZip, String projectName) throws IOException, ZipException, CoreException {
		String workspacePath= ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/";
		FileTool.unzip(new ZipFile(FileTool.getFileInPlugin(installationPlugin, new Path(projectZip))), new File(workspacePath));
		return createExistingProject(projectName);
	}
}
