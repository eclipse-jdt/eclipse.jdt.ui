/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.Method;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.SerializerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.util.Assert;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

public class JavadocWriter {
	
	private OutputStream fOutputStream;
	private IJavaProject fJavaProject;
	private IPath fBasePath;

	/**
	 * Create a JavadocWriter on the given output stream.
	 * It is the client's responsibility to close the output stream.
	 * @param basePath The base path to which all path will be made relative (if
	 * possible). If <code>null</code>, paths are not made relative.
	 */
	public JavadocWriter(OutputStream outputStream, IPath basePath, IJavaProject project) {
		Assert.isNotNull(outputStream);
		fOutputStream= new BufferedOutputStream(outputStream);
		fBasePath= basePath;
		fJavaProject= project;
	}

	public void writeXML(JavadocOptionsManager store) throws IOException, CoreException {

		DocumentBuilder docBuilder= null;
		DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		try {
			docBuilder= factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new IOException();
		}
		Document document= docBuilder.newDocument();

		// Create the document
		Element project= document.createElement("project"); //$NON-NLS-1$
		document.appendChild(project);

		project.setAttribute("name", fJavaProject.getElementName()); //$NON-NLS-1$
		project.setAttribute("default", "javadoc"); //$NON-NLS-1$ //$NON-NLS-2$

		Element javadocTarget= document.createElement("target"); //$NON-NLS-1$
		project.appendChild(javadocTarget);
		javadocTarget.setAttribute("name", "javadoc"); //$NON-NLS-1$ //$NON-NLS-2$

		Element xmlJavadocDesc= document.createElement("javadoc"); //$NON-NLS-1$
		javadocTarget.appendChild(xmlJavadocDesc);

		if (!store.fromStandard())
			xmlWriteDoclet(store, document, xmlJavadocDesc);
		else
			xmlWriteJavadocStandardParams(store, document, xmlJavadocDesc);

		// Write the document to the stream
		OutputFormat format= new OutputFormat();
		format.setIndenting(true);
		SerializerFactory serializerFactory= SerializerFactory.getSerializerFactory(Method.XML);
		Serializer serializer= serializerFactory.makeSerializer(fOutputStream, format);
		serializer.asDOMSerializer().serialize(document);

	}

	//writes ant file, for now only worry about one project
	private void xmlWriteJavadocStandardParams(JavadocOptionsManager store, Document document, Element xmlJavadocDesc) throws DOMException, CoreException {

		String destination= getPathString(new Path(store.getDestination()));

		xmlJavadocDesc.setAttribute(store.DESTINATION, destination);
		xmlJavadocDesc.setAttribute(store.VISIBILITY, store.getAccess());
		if (store.isJDK14Mode()) {
			xmlJavadocDesc.setAttribute(store.SOURCE, "1.4"); //$NON-NLS-1$
		}
		xmlJavadocDesc.setAttribute(store.USE, booleanToString(store.getBoolean("use"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.NOTREE, booleanToString(store.getBoolean("notree"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.NONAVBAR, booleanToString(store.getBoolean("nonavbar"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.NOINDEX, booleanToString(store.getBoolean("noindex"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.SPLITINDEX, booleanToString(store.getBoolean("splitindex"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.AUTHOR, booleanToString(store.getBoolean("author"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.VERSION, booleanToString(store.getBoolean("version"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.NODEPRECATEDLIST, booleanToString(store.getBoolean("nodeprecatedlist"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.NODEPRECATED, booleanToString(store.getBoolean("nodeprecated"))); //$NON-NLS-1$


		//set the packages and source files
		List packages= new ArrayList();
		List sourcefiles= new ArrayList();
		sortSourceElement(store.getSourceElements(), sourcefiles, packages);
		if (!packages.isEmpty())
			xmlJavadocDesc.setAttribute(store.PACKAGENAMES, toSeparatedList(packages));

		if (!sourcefiles.isEmpty())
			xmlJavadocDesc.setAttribute(store.SOURCEFILES, toSeparatedList(sourcefiles));

		xmlJavadocDesc.setAttribute(store.SOURCEPATH, getPathString(store.getSourcepath()));
		xmlJavadocDesc.setAttribute(store.CLASSPATH, getPathString(store.getClasspath()));

		String str= store.getOverview();
		if (str.length() > 0) //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(store.OVERVIEW, str);

		str= store.getStyleSheet();
		if (str.length() > 0) //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(store.STYLESHEETFILE, str);

		str= store.getTitle();
		if (str.length() > 0) //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(store.TITLE, str);

		str= store.getAdditionalParams();
		if (str.length() > 0) //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(store.EXTRAOPTIONS, str);

		String hrefs= store.getDependencies();
		StringTokenizer tokenizer= new StringTokenizer(hrefs, ";"); //$NON-NLS-1$
		while (tokenizer.hasMoreElements()) {
			String href= (String) tokenizer.nextElement();
			Element links= document.createElement("link"); //$NON-NLS-1$
			xmlJavadocDesc.appendChild(links);
			links.setAttribute(store.HREF, href);
		}

	}

	private void sortSourceElement(IJavaElement[] iJavaElements, List sourcefiles, List packages) {
		for (int i= 0; i < iJavaElements.length; i++) {
			IJavaElement element= iJavaElements[i];
			IPath p= element.getResource().getLocation();
			if (p == null)
				continue;

			if (element instanceof ICompilationUnit) {
				String relative= getPathString(p);
				sourcefiles.add(relative);
			} else if (element instanceof IPackageFragment) {
				packages.add(element.getElementName());
			}
		}
	}

	private String getPathString(IPath[] paths) {
		StringBuffer buf= new StringBuffer();
		
		for (int i= 0; i < paths.length; i++) {
			if (buf.length() != 0) {
				buf.append(File.pathSeparatorChar);
			}			
			buf.append(getPathString(paths[i]));
		}

		if (buf.length() == 0) {
			buf.append('.');
		}
		return buf.toString();
	}

	private boolean hasSameDevice(IPath p1, IPath p2) {
		String dev= p1.getDevice();
		if (dev == null) {
			return p2.getDevice() == null;
		}
		return dev.equals(p2.getDevice());
	}

	//make the path relative to the base path
	private String getPathString(IPath fullPath) {
		if (fBasePath == null || !hasSameDevice(fullPath, fBasePath)) {
			return fullPath.toOSString();
		}
		int matchingSegments= fBasePath.matchingFirstSegments(fullPath);
		if (fBasePath.segmentCount() == matchingSegments) {
			return getRelativePath(fullPath, matchingSegments);
		}
		IProject proj= fJavaProject.getProject();	
		IPath projLoc= proj.getLocation();
		if (projLoc.segmentCount() <= matchingSegments && projLoc.isPrefixOf(fullPath)) {
			return getRelativePath(fullPath, matchingSegments);
		}
		IPath workspaceLoc= proj.getWorkspace().getRoot().getLocation();
		if (workspaceLoc.segmentCount() <= matchingSegments && workspaceLoc.isPrefixOf(fullPath)) {
			return getRelativePath(fullPath, matchingSegments);
		}		
		return fullPath.toOSString();
	}

	private String getRelativePath(IPath fullPath, int matchingSegments) {
		StringBuffer res= new StringBuffer();
		int backSegments= fBasePath.segmentCount() - matchingSegments;
		while (backSegments > 0) {
			res.append(".."); //$NON-NLS-1$
			res.append(File.separatorChar);
			backSegments--;
		}
		int segCount= fullPath.segmentCount();
		for (int i= matchingSegments; i < segCount; i++) {
			if (i > matchingSegments) {
				res.append(File.separatorChar);
			}
			res.append(fullPath.segment(i));
		}
		return res.toString();
	}

	private void xmlWriteDoclet(JavadocOptionsManager store, Document document, Element xmlJavadocDesc) throws DOMException, CoreException {

		//set the packages and source files
		List packages= new ArrayList();
		List sourcefiles= new ArrayList();
		sortSourceElement(store.getSourceElements(), sourcefiles, packages);
		if (!packages.isEmpty())
			xmlJavadocDesc.setAttribute(store.PACKAGENAMES, toSeparatedList(packages));

		if (!sourcefiles.isEmpty())
			xmlJavadocDesc.setAttribute(store.SOURCEFILES, toSeparatedList(sourcefiles));

		xmlJavadocDesc.setAttribute(store.SOURCEPATH, getPathString(store.getSourcepath()));
		xmlJavadocDesc.setAttribute(store.CLASSPATH, getPathString(store.getClasspath()));
		xmlJavadocDesc.setAttribute(store.VISIBILITY, store.getAccess());

		Element doclet= document.createElement("doclet"); //$NON-NLS-1$
		xmlJavadocDesc.appendChild(doclet);
		doclet.setAttribute(store.NAME, store.getDocletName());
		doclet.setAttribute(store.PATH, store.getDocletPath());

		String str= store.getOverview();
		if (str.length() > 0) //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(store.OVERVIEW, str);

		str= store.getAdditionalParams();
		if (str.length() > 0) //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(store.EXTRAOPTIONS, str);

	}

	private String toSeparatedList(List packages) throws JavaModelException {
		StringBuffer buf= new StringBuffer();
		Iterator iter= packages.iterator();
		int nAdded= 0;
		while (iter.hasNext()) {
			if (nAdded > 0) {
				buf.append(","); //$NON-NLS-1$
			}
			nAdded++;
			String curr= (String) iter.next();
			buf.append(curr);
		}
		return buf.toString();
	}

	private String booleanToString(boolean bool) {
		if (bool)
			return "true"; //$NON-NLS-1$
		else
			return "false"; //$NON-NLS-1$
	}

	public void close() throws IOException {
		if (fOutputStream != null) {
			fOutputStream.close();
		}
	}

}