/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
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
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 83258 [jar exporter] Deploy java application as executable jar
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 262766 [jar exporter] ANT file for Jar-in-Jar option contains relative path to jar-rsrc-loader.zip
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 262763 [jar exporter] remove Built-By attribute in ANT files from Fat JAR Exporter
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 269201 [jar exporter] ant file produced by Export runnable jar contains absolut paths instead of relative to workspace
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackagerfat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.runtime.IPath;

import org.eclipse.debug.core.ILaunchConfiguration;

import org.eclipse.jdt.internal.ui.util.XmlProcessorFactoryJdtUi;

/**
 * @since 3.5
 */
public class UnpackFatJarAntExporter extends FatJarAntExporter {

	public UnpackFatJarAntExporter(IPath antScriptLocation, IPath jarLocation, ILaunchConfiguration launchConfiguration) {
		super(antScriptLocation, jarLocation, launchConfiguration);
	}

	@Override
	protected void buildANTScript(IPath antScriptLocation, String projectName, IPath absJarfile, String mainClass, SourceInfo[] sourceInfos) throws IOException {
		try (OutputStream outputStream = new FileOutputStream(antScriptLocation.toFile())) {
			String absJarname= absJarfile.toString();

			DocumentBuilder docBuilder= null;
			DocumentBuilderFactory factory= XmlProcessorFactoryJdtUi.createDocumentBuilderFactoryWithErrorOnDOCTYPE();
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
			document.appendChild(project);
			comment= document.createComment("this file was created by Eclipse Runnable JAR Export Wizard"); //$NON-NLS-1$
			project.appendChild(comment);
			comment= document.createComment("ANT 1.7 is required                                        "); //$NON-NLS-1$
			project.appendChild(comment);

			addBaseDirProperties(document, project);

			Element target= document.createElement("target"); //$NON-NLS-1$
			target.setAttribute("name", "create_run_jar"); //$NON-NLS-1$ //$NON-NLS-2$
			project.appendChild(target);

			Element jar= document.createElement("jar"); //$NON-NLS-1$
			jar.setAttribute("destfile", substituteBaseDirs(absJarname)); //$NON-NLS-1$s
			jar.setAttribute("filesetmanifest", "mergewithoutmain"); //$NON-NLS-1$ //$NON-NLS-2$
			target.appendChild(jar);

			Element manifest= document.createElement("manifest"); //$NON-NLS-1$
			jar.appendChild(manifest);

			Element attribute= document.createElement("attribute"); //$NON-NLS-1$
			attribute.setAttribute("name", "Main-Class"); //$NON-NLS-1$ //$NON-NLS-2$s
			attribute.setAttribute("value", mainClass); //$NON-NLS-1$
			manifest.appendChild(attribute);

			attribute= document.createElement("attribute"); //$NON-NLS-1$
			attribute.setAttribute("name", "Class-Path"); //$NON-NLS-1$ //$NON-NLS-2$s
			attribute.setAttribute("value", "."); //$NON-NLS-1$ //$NON-NLS-2$
			manifest.appendChild(attribute);

			for (SourceInfo sourceInfo : sourceInfos) {
				if (sourceInfo.isJar) {
					Element zipfileset= document.createElement("zipfileset"); //$NON-NLS-1$
					zipfileset.setAttribute("src", substituteBaseDirs(sourceInfo.absPath)); //$NON-NLS-1$
					zipfileset.setAttribute("excludes", "META-INF/*.SF"); //$NON-NLS-1$ //$NON-NLS-2$
					jar.appendChild(zipfileset);
				} else {
					Element fileset= document.createElement("fileset"); //$NON-NLS-1$
					fileset.setAttribute("dir", substituteBaseDirs(sourceInfo.absPath)); //$NON-NLS-1$
					jar.appendChild(fileset);
				}
			}

			try {
				// Write the document to the stream
				Transformer transformer= XmlProcessorFactoryJdtUi.createTransformerFactoryWithErrorOnDOCTYPE().newTransformer();
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

}
