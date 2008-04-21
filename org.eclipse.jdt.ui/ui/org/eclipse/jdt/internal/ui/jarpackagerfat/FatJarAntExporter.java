/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 213638 [jar exporter] create ANT build file for current settings
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 220257 [jar application] ANT build file does not create Class-Path Entry in Manifest
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackagerfat;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.debug.core.ILaunchConfiguration;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Create an ANT script for a runnable JAR.
 * The script is generated based on the classpath of the 
 * selected launch-configuration. 
 * 
 * @since 3.4
 */
public class FatJarAntExporter {

	private static class SourceInfo {

		public final boolean isJar;
		public final String absPath;
		public final String relJarPath;

		public SourceInfo(boolean isJar, String absPath, String relJarPath) {
			this.isJar= isJar;
			this.absPath= absPath;
			this.relJarPath= relJarPath;
		}
	}

	private ILaunchConfiguration fLaunchConfiguration;
	private IPath fAbsJarfile;
	private IPath fAntScriptLocation;

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
			
			buildANTScript(new FileOutputStream(fAntScriptLocation.toFile()), projectName, fAbsJarfile, mainClass, convert(classpath));
			
		} catch (FileNotFoundException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, 
							Messages.format(FatJarPackagerMessages.FatJarPackageWizard_antScript_error_readingOutputFile, new Object[] {
									BasicElementLabels.getPathLabel(fAntScriptLocation, true), e.getLocalizedMessage() })
							)
					);
		} catch (IOException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, 
							Messages.format(FatJarPackagerMessages.FatJarPackageWizard_antScript_error_writingOutputFile, new Object[] {
									BasicElementLabels.getPathLabel(fAntScriptLocation, true), e.getLocalizedMessage() })
							)
					);
		}
	}

	private static IPath[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
		IRuntimeClasspathEntry[] entries= JavaRuntime.computeUnresolvedRuntimeClasspath(configuration);
		entries= JavaRuntime.resolveRuntimeClasspath(entries, configuration);

		ArrayList userEntries= new ArrayList(entries.length);
		for (int i= 0; i < entries.length; i++) {
			if (entries[i].getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {

				String location= entries[i].getLocation();
				if (location != null) {
					IPath entry= Path.fromOSString(location);
					if (!userEntries.contains(entry)) {
						userEntries.add(entry);
					}
				}
			}
		}
		return (IPath[]) userEntries.toArray(new IPath[userEntries.size()]);
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


	private static SourceInfo[] convert(IPath[] classpath) {
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
	 * Create an ANT script to outputStream.
	 * 
	 * @param outputStream to write ANT script to
	 * @param projectName base project for informational purpose only 
	 * @param absJarfile path to the destination
	 * @param mainClass the optional main-class
	 * @param sourceInfos array of sources
	 * @throws IOException 
	 */
	private void buildANTScript(OutputStream outputStream, String projectName, IPath absJarfile, String mainClass, SourceInfo[] sourceInfos) throws IOException {

		String absJarname= absJarfile.toString();

		DocumentBuilder docBuilder= null;
		DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		try {
			docBuilder= factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new IOException(FatJarPackagerMessages.FatJarPackageAntScript_error_couldNotGetXmlBuilder);
		}
		Document document= docBuilder.newDocument();

		Node comment;

		// Create the document
		Element project= document.createElement("project"); //$NON-NLS-1$
		project.setAttribute("name", "Create Runnable Jar for Project " + projectName); //$NON-NLS-1$ //$NON-NLS-2$
		project.setAttribute("default", "create_run_jar"); //$NON-NLS-1$ //$NON-NLS-2$
		comment= document.createComment("this file was created by Eclipse Runnable JAR Export Wizard"); //$NON-NLS-1$
		project.appendChild(comment);
		comment= document.createComment("ANT 1.7 is required                                        "); //$NON-NLS-1$
		project.appendChild(comment);
		document.appendChild(project);

		Element target= document.createElement("target"); //$NON-NLS-1$
		target.setAttribute("name", "create_run_jar"); //$NON-NLS-1$ //$NON-NLS-2$
		project.appendChild(target);

		Element jar= document.createElement("jar"); //$NON-NLS-1$
		jar.setAttribute("destfile", absJarname); //$NON-NLS-1$s 
		jar.setAttribute("filesetmanifest", "mergewithoutmain"); //$NON-NLS-1$ //$NON-NLS-2$
		target.appendChild(jar);

		Element manifest= document.createElement("manifest"); //$NON-NLS-1$
		jar.appendChild(manifest);

		Element attribute= document.createElement("attribute"); //$NON-NLS-1$
		attribute.setAttribute("name", "Built-By"); //$NON-NLS-1$ //$NON-NLS-2$s 
		attribute.setAttribute("value", "${user.name}"); //$NON-NLS-1$ //$NON-NLS-2$s 
		manifest.appendChild(attribute);

		attribute= document.createElement("attribute"); //$NON-NLS-1$
		attribute.setAttribute("name", "Main-Class"); //$NON-NLS-1$ //$NON-NLS-2$s 
		attribute.setAttribute("value", mainClass); //$NON-NLS-1$ 
		manifest.appendChild(attribute);

		attribute= document.createElement("attribute"); //$NON-NLS-1$
		attribute.setAttribute("name", "Class-Path"); //$NON-NLS-1$ //$NON-NLS-2$s 
		attribute.setAttribute("value", "."); //$NON-NLS-1$ //$NON-NLS-2$ 
		manifest.appendChild(attribute);

		for (int i= 0; i < sourceInfos.length; i++) {
			SourceInfo sourceInfo= sourceInfos[i];
			if (sourceInfo.isJar) {
				Element zipfileset= document.createElement("zipfileset"); //$NON-NLS-1$
				zipfileset.setAttribute("src", sourceInfo.absPath); //$NON-NLS-1$
				zipfileset.setAttribute("excludes", "META-INF/*.SF"); //$NON-NLS-1$ //$NON-NLS-2$
				jar.appendChild(zipfileset);
			} else {
				Element fileset= document.createElement("fileset"); //$NON-NLS-1$
				fileset.setAttribute("dir", sourceInfo.absPath); //$NON-NLS-1$
				jar.appendChild(fileset);
			}
		}

		try {
			// Write the document to the stream
			Transformer transformer= TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); //$NON-NLS-1$ //$NON-NLS-2$
			DOMSource source= new DOMSource(document);
			StreamResult result= new StreamResult(outputStream);
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new IOException(FatJarPackagerMessages.FatJarPackageAntScript_error_couldNotTransformToXML);
		}
	}

}
