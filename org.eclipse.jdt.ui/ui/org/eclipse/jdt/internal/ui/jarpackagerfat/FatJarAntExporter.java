/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 213638 [jar exporter] create ANT build file for current settings
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 220257 [jar application] ANT build file does not create Class-Path Entry in Manifest
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 219530 [jar application] add Jar-in-Jar ClassLoader option
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 262766 [jar exporter] ANT file for Jar-in-Jar option contains relative path to jar-rsrc-loader.zip
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 269201 [jar exporter] ant file produced by Export runnable jar contains absolut paths instead of relative to workspace
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackagerfat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.debug.core.ILaunchConfiguration;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Create an ANT script for a runnable JAR.
 * The script is generated based on the classpath of the
 * selected launch-configuration.
 *
 * @since 3.4
 */
public abstract class FatJarAntExporter {

	private static final String ANT_PROPERTYNAME_DIR_BUILDFILE= "dir.buildfile"; //$NON-NLS-1$
	private static final String ANT_PROPERTYNAME_DIR_WORKSPACE= "dir.workspace"; //$NON-NLS-1$
	private static final String ANT_PROPERTYNAME_DIR_JARFILE= "dir.jarfile"; //$NON-NLS-1$

	private static final String ANT_PROPERTY_DIR_BUILDFILE= "${"+ANT_PROPERTYNAME_DIR_BUILDFILE+"}"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String ANT_PROPERTY_DIR_WORKSPACE= "${"+ANT_PROPERTYNAME_DIR_WORKSPACE+"}"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String ANT_PROPERTY_DIR_JARFILE= "${"+ANT_PROPERTYNAME_DIR_JARFILE+"}"; //$NON-NLS-1$ //$NON-NLS-2$

	protected static class SourceInfo {

		public final boolean isJar;
		public final String absPath;
		public final String relJarPath;

		public SourceInfo(boolean isJar, String absPath, String relJarPath) {
			this.isJar= isJar;
			this.absPath= absPath;
			this.relJarPath= relJarPath;
		}
	}

	/**
	 * Helper class for path-substitutions.
	 * This class manages a set of path substitutions.
	 * On apply the longest substitution is chosen.
	 */
	private static class PathSubstituter {
		private Map<String, String> pathSubstitutions= new HashMap<>();

		public PathSubstituter addSubstitution(String basePath, String baseReplacement) {
			pathSubstitutions.put(basePath, baseReplacement);
			return this;
		}

		public String substitute(String path) {
			int len= 0;
			String result= path;
			for (Map.Entry<String, String> entry : pathSubstitutions.entrySet()) {
				String basePath = entry.getKey();
				if (basePath.length() < len) {
					continue;
				}
				if (path.equals(basePath)) {
					result= entry.getValue();
					break;
				}
				if (path.startsWith(basePath + File.separator)) {
					len= basePath.length();
					result= entry.getValue() + path.substring(len);
				}
			}
			return result;
		}
	}

	private ILaunchConfiguration fLaunchConfiguration;
	private IPath fAbsJarfile;
	private IPath fAntScriptLocation;

	private String fBuildfileDir;
	private String fWorkspaceDir;
	private String fJarfileDir;

	private PathSubstituter pathSubstituter;

	/**
	 * Create an instance of the ANT exporter. An ANT exporter can generate an ANT script
	 * at the given ant script location for the given launch configuration
	 * @param antScriptLocation the location of the ANT script to generate
	 * @param jarLocation the location of the jar file which the ANT script will generate
	 * @param launchConfiguration the launch configuration to generate a ANT script for
	 */
	public FatJarAntExporter(IPath antScriptLocation, IPath jarLocation, ILaunchConfiguration launchConfiguration) {
		fLaunchConfiguration= launchConfiguration;
		fAbsJarfile= jarLocation;
		fAntScriptLocation= antScriptLocation;
		pathSubstituter= new PathSubstituter();
		try {
			fBuildfileDir= antScriptLocation.toFile().getParentFile().getCanonicalPath();
			pathSubstituter.addSubstitution(fBuildfileDir, ANT_PROPERTY_DIR_BUILDFILE);
		} catch (Exception e) {
			JavaPlugin.log(e);
			fBuildfileDir= "?"; //$NON-NLS-1$
		}
		try {
			fJarfileDir= jarLocation.toFile().getParentFile().getCanonicalPath();
			pathSubstituter.addSubstitution(fJarfileDir, ANT_PROPERTY_DIR_JARFILE);
		} catch (Exception e) {
			JavaPlugin.log(e);
			fJarfileDir= "?"; //$NON-NLS-1$
		}
		try {
			fWorkspaceDir= ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().getCanonicalPath();
			pathSubstituter.addSubstitution(fWorkspaceDir, ANT_PROPERTY_DIR_WORKSPACE);
		} catch (Exception e) {
			JavaPlugin.log(e);
			fWorkspaceDir= "?"; //$NON-NLS-1$
		}
	}

	/**
	 * Create the ANT script based on the information
	 * given in the constructor.
	 *
	 * @param status to report warnings to
	 * @throws CoreException if something went wrong while generating the ant script
	 */
	public void run(MultiStatus status) throws CoreException {
		try {

			IPath[] classpath= getClasspath(fLaunchConfiguration);
			String mainClass= getMainClass(fLaunchConfiguration, status);
			String projectName= fLaunchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$

			buildANTScript(fAntScriptLocation, projectName, fAbsJarfile, mainClass, convert(classpath));

		} catch (FileNotFoundException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, JavaUI.ID_PLUGIN,
							Messages.format(FatJarPackagerMessages.FatJarPackageWizard_antScript_error_readingOutputFile, new Object[] {
									BasicElementLabels.getPathLabel(fAntScriptLocation, true), e.getLocalizedMessage() })
					));
		} catch (IOException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, JavaUI.ID_PLUGIN,
							Messages.format(FatJarPackagerMessages.FatJarPackageWizard_antScript_error_writingOutputFile, new Object[] {
									BasicElementLabels.getPathLabel(fAntScriptLocation, true), e.getLocalizedMessage() })
					));
		}
	}

	private static IPath[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
		IRuntimeClasspathEntry[] entries= JavaRuntime.computeUnresolvedRuntimeClasspath(configuration);
		entries= JavaRuntime.resolveRuntimeClasspath(entries, configuration);

		ArrayList<IPath> userEntries= new ArrayList<>(entries.length);
		boolean isModularConfig= JavaRuntime.isModularConfiguration(configuration);
		for (IRuntimeClasspathEntry cpentry : entries) {
			int classPathProperty= cpentry.getClasspathProperty();
			if ((!isModularConfig && classPathProperty == IRuntimeClasspathEntry.USER_CLASSES)
				|| (isModularConfig && (classPathProperty == IRuntimeClasspathEntry.CLASS_PATH || classPathProperty == IRuntimeClasspathEntry.MODULE_PATH))) {
				String location= cpentry.getLocation();
				if (location != null) {
					IPath entry= Path.fromOSString(location);
					if (!userEntries.contains(entry)) {
						userEntries.add(entry);
					}
				}
			}
		}
		return userEntries.toArray(new IPath[userEntries.size()]);
	}

	private static String getMainClass(ILaunchConfiguration launchConfig, MultiStatus status) {
		String result= null;
		if (launchConfig != null) {
			try {
				result= launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String) null);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		if (result == null) {
			status.add(new Status(IStatus.WARNING, JavaUI.ID_PLUGIN, FatJarPackagerMessages.FatJarPackageWizardPage_LaunchConfigurationWithoutMainType_warning));
			result= ""; //$NON-NLS-1$
		}
		return result;
	}


	protected static SourceInfo[] convert(IPath[] classpath) {
		SourceInfo[] result= new SourceInfo[classpath.length];
		for (int i= 0; i < classpath.length; i++) {
			IPath path= classpath[i];
			if (path != null) {
				if (path.toFile().isDirectory()) {
					result[i]= new SourceInfo(false, path.toString(), ""); //$NON-NLS-1$
				} else if (path.toFile().isFile()) {
					// TODO: check for ".jar" extension?
					result[i]= new SourceInfo(true, path.toString(), ""); //$NON-NLS-1$
				}
			}
		}

		return result;
	}


	/**
	 * Adds dir properties to ANT-Buildfile:
	 * <ul>
	 * <li>&lt;property name="dir.buildfile" value="." /&gt;</li>
	 * <li>&lt;property name="dir.workspace" value="${dir.buildfile}/../.." /&gt;</li>
	 * <li>&lt;property name="dir.jarfile" value="C:/TEMP" /&gt;</li>
	 * </ul>
	 *
	 * @param document the DOM document of the ant build script
	 * @param project the project tag
	 */
	protected void addBaseDirProperties(Document document, Element project) {

		Node comment= document.createComment("define folder properties"); //$NON-NLS-1$
		project.appendChild(comment);

		Element property= document.createElement("property"); //$NON-NLS-1$
		property.setAttribute("name", ANT_PROPERTYNAME_DIR_BUILDFILE); //$NON-NLS-1$
		property.setAttribute("value", "."); //$NON-NLS-1$ //$NON-NLS-2$
		project.appendChild(property);

		property= document.createElement("property"); //$NON-NLS-1$
		property.setAttribute("name", ANT_PROPERTYNAME_DIR_WORKSPACE); //$NON-NLS-1$
		property.setAttribute("value", getWorkspaceRelativeDir()); //$NON-NLS-1$
		project.appendChild(property);

		property= document.createElement("property"); //$NON-NLS-1$
		property.setAttribute("name", ANT_PROPERTYNAME_DIR_JARFILE); //$NON-NLS-1$
		property.setAttribute("value", getJarfileRelativeDir()); //$NON-NLS-1$
		project.appendChild(property);
	}


	/**
	 * Converts the given filename relative to any of the ant-property-dirs:
	 * <ul>
	 * <li>buidfile-dir (where the ant script is)</li>
	 * <li>workspace-dir (eclipse workspace dir)</li>
	 * <li>jarfile-dir (where the jar is written to)</li>
	 * </ul>
	 *
	 * @param absFilename filename whose base dir is substituted
	 * @return either the original filename or a relative path from one of the base-dirs
	 */
	protected String substituteBaseDirs(String absFilename) {
		String canonicleFilename;
		try {
			canonicleFilename= new File(absFilename).getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
			return absFilename;
		}
		String result= pathSubstituter.substitute(canonicleFilename);
		result= result.replace(File.separatorChar, '/');
		return result;
	}

	/**
	 * Returns the workspace-dir path relative to buildfile-dir.
	 *
	 * @return the relative path for the workspace-dir
	 */
	protected String getWorkspaceRelativeDir() {
		String result;
		if (fBuildfileDir.startsWith(fWorkspaceDir + File.separator)) {
			int lastSlash= fWorkspaceDir.length();
			result= "${dir.buildfile}" + File.separator + ".."; //$NON-NLS-1$ //$NON-NLS-2$
			lastSlash= fBuildfileDir.indexOf(File.separator, lastSlash + 1);
			while (lastSlash != -1) {
				result+= File.separator + ".."; //$NON-NLS-1$
				lastSlash= fBuildfileDir.indexOf(File.separator, lastSlash + 1);
			}
		}
		else {
			result= new PathSubstituter()
					.addSubstitution(fBuildfileDir, ANT_PROPERTY_DIR_BUILDFILE)
					.substitute(fWorkspaceDir);
		}
		result= result.replace(File.separatorChar, '/');
		return result;
	}


	/**
	 * Returns jarfile-dir path relative to buildfile-dir or workspace-dir.
	 *
	 * @return the relative path for the jarfile-dir
	 */
	protected String getJarfileRelativeDir() {
		String result= new PathSubstituter()
				.addSubstitution(fBuildfileDir, ANT_PROPERTY_DIR_BUILDFILE)
				.addSubstitution(fWorkspaceDir, ANT_PROPERTY_DIR_WORKSPACE)
				.substitute(fJarfileDir);
		result= result.replace(File.separatorChar, '/');
		return result;
	}


	/**
	 * Create an ANT script at the given location.
	 *
	 * @param antScriptLocation to write ANT script to
	 * @param projectName base project for informational purpose only
	 * @param absJarfile path to the destination
	 * @param mainClass the optional main-class
	 * @param sourceInfos array of sources
	 * @throws IOException if an exception occurred while writing to the stream,
	 */
	protected abstract void buildANTScript(IPath antScriptLocation, String projectName, IPath absJarfile, String mainClass, SourceInfo[] sourceInfos) throws IOException;

}
