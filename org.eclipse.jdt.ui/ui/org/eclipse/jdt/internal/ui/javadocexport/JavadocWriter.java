/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.Method;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.SerializerFactory;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.util.Assert;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

public class JavadocWriter {
	protected OutputStream fOutputStream;
	private IJavaProject[] fProjects;
	/**
	 * Create a JavadocWriter on the given output stream.
	 * It is the client's responsibility to close the output stream.
	 */
	public JavadocWriter(OutputStream outputStream) {
		Assert.isNotNull(outputStream);
		fOutputStream= new BufferedOutputStream(outputStream);
	}
	
	

	/**
	 * Writes a XML representation of the JAR specification
	 * to to the underlying stream.
	 * 
	 * @exception IOException	if writing to the underlying stream fails
	 */
	
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
			
			try {
				fProjects= (IJavaProject[]) store.getJavaProjects().toArray(new IJavaProject[store.getJavaProjects().size()]);
				if(fProjects.length!= 1) {
					project.setAttribute("name", fProjects[0].getCorrespondingResource().getName()); //$NON-NLS-1$
				} else project.setAttribute("name", "project_name"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch(DOMException e) {
				project.setAttribute("name", "project_name"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch(JavaModelException e) {
				project.setAttribute("name", "project_name"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			project.setAttribute("default", "javadoc"); //$NON-NLS-1$ //$NON-NLS-2$

			Element javadocTarget= document.createElement("target"); //$NON-NLS-1$
			project.appendChild(javadocTarget);
			javadocTarget.setAttribute("name", "javadoc"); //$NON-NLS-1$ //$NON-NLS-2$

			Element xmlJavadocDesc= document.createElement("javadoc"); //$NON-NLS-1$
			javadocTarget.appendChild(xmlJavadocDesc);

			if (!store.fromStandard())
				xmlWriteDoclet(store, document, xmlJavadocDesc);
			else
				xmlWriteJavadocStandardParams(store, document,xmlJavadocDesc);

			// Write the document to the stream
			OutputFormat format= new OutputFormat();
			format.setIndenting(true);
			SerializerFactory serializerFactory= SerializerFactory.getSerializerFactory(Method.XML);
			Serializer serializer= serializerFactory.makeSerializer(fOutputStream, format);
			serializer.asDOMSerializer().serialize(document);
		
	}

	//writes ant file, for now only worry about one project
	private void xmlWriteJavadocStandardParams(JavadocOptionsManager store, Document document ,Element xmlJavadocDesc) throws DOMException, CoreException {

		xmlJavadocDesc.setAttribute(store.DESTINATION, store.getDestination());
		xmlJavadocDesc.setAttribute(store.VISIBILITY, store.getAccess());
		xmlJavadocDesc.setAttribute(store.USE, booleanToString(store.getBoolean("use"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.NOTREE, booleanToString(store.getBoolean("notree"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.NONAVBAR, booleanToString(store.getBoolean("nonavbar"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.NOINDEX, booleanToString(store.getBoolean("noindex"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.SPLITINDEX, booleanToString(store.getBoolean("splitindex"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.AUTHOR, booleanToString(store.getBoolean("author"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.VERSION, booleanToString(store.getBoolean("version"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.NODEPRECATEDLIST, booleanToString(store.getBoolean("nodeprecatedlist"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.NODEPRECATED, booleanToString(store.getBoolean("nodeprecated"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(store.PACKAGENAMES, toPackageList(store.getSourceElements()));
		xmlJavadocDesc.setAttribute(store.SOURCEPATH, store.getSourcepath());
		xmlJavadocDesc.setAttribute(store.CLASSPATH, store.getClasspath());
		String str= store.getOverview();
		if (!str.equals("")) //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(store.OVERVIEW, str);

		str= store.getStyleSheet();
		if (!str.equals("")) //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(store.STYLESHEETFILE, str);
			
		str= store.getTitle();	
		if(!str.equals("")) //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(store.TITLE, str);

		str= store.getAdditionalParams();
		if (!str.equals("")) //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(store.EXTRAOPTIONS, str);
			
		if (fProjects.length != 0) { //it should never equal null
			String hrefs = store.getDependencies();
			StringTokenizer tokenizer = new StringTokenizer(hrefs, ";"); //$NON-NLS-1$
			while (tokenizer.hasMoreElements()) {
				String href = (String) tokenizer.nextElement();
				Element links = document.createElement("link"); //$NON-NLS-1$
				xmlJavadocDesc.appendChild(links);
				links.setAttribute(store.HREF, href);
			}
		}
		

	}

	private void xmlWriteDoclet(JavadocOptionsManager store, Document document, Element xmlJavadocDesc) throws DOMException, CoreException {

		xmlJavadocDesc.setAttribute(store.PACKAGENAMES, toPackageList(store.getSourceElements()));
		xmlJavadocDesc.setAttribute(store.SOURCEPATH, store.getSourcepath());
		xmlJavadocDesc.setAttribute(store.CLASSPATH, store.getClasspath());
		xmlJavadocDesc.setAttribute(store.VISIBILITY, store.getAccess());

		Element doclet= document.createElement("doclet"); //$NON-NLS-1$
		xmlJavadocDesc.appendChild(doclet);
		doclet.setAttribute(store.NAME, store.getDocletName());
		doclet.setAttribute(store.PATH, store.getDocletPath());

		String str= store.getOverview();
		if (!str.equals("")) //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(store.OVERVIEW, str);

		str= store.getAdditionalParams();
		if (!str.equals("")) //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(store.EXTRAOPTIONS, str);

	}

	/**
	 * Closes this stream.
	 * It is the client's responsibility to close the stream.
	 * 
	 * @exception IOException
	 */
	private String toPackageList(IJavaElement[] sourceElements) throws JavaModelException {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < sourceElements.length; i++) {
			if (i > 0) {
				buf.append(","); //$NON-NLS-1$
			}
			IJavaElement curr= sourceElements[i];
			if (curr instanceof IPackageFragment) {
				buf.append(curr.getElementName());
			} else {
				buf.append(curr.getUnderlyingResource().getLocation().toOSString());
			}	
		}
		return buf.toString();
	}
	
	private String booleanToString(boolean bool){
		if(bool)
			return "true"; //$NON-NLS-1$
		else return"false"; //$NON-NLS-1$
	}

	public void close() throws IOException {
		if (fOutputStream != null) {
			fOutputStream.close();
		}
	}

}