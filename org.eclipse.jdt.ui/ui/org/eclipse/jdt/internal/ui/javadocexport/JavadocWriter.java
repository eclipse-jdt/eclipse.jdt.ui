/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.Method;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.SerializerFactory;

import org.eclipse.jface.util.Assert;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

public class JavadocWriter {
	protected OutputStream fOutputStream;

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
	
	public void writeXML(JavadocOptionsManager store) throws IOException {
		
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
			Element project= document.createElement("project");
			document.appendChild(project);
			
			try {
				IJavaProject proj= store.getProject();
				if(proj!=null) {
					project.setAttribute("name", proj.getCorrespondingResource().getName());
				} else project.setAttribute("name", "project_name");
			} catch(DOMException e) {
				project.setAttribute("name", "project_name");
			} catch(JavaModelException e) {
				project.setAttribute("name", "project_name");
			}
			project.setAttribute("default", "javadoc");

			Element javadocTarget= document.createElement("target");
			project.appendChild(javadocTarget);
			javadocTarget.setAttribute("name", "javadoc");

			Element xmlJavadocDesc= document.createElement("javadoc");
			javadocTarget.appendChild(xmlJavadocDesc);

			if (!store.fromStandard())
				xmlWriteDoclet(store, document, xmlJavadocDesc);
			else
				xmlWriteJavadocStandardParams(store, xmlJavadocDesc);

			// Write the document to the stream
			OutputFormat format= new OutputFormat();
			format.setIndenting(true);
			SerializerFactory serializerFactory= SerializerFactory.getSerializerFactory(Method.XML);
			Serializer serializer= serializerFactory.makeSerializer(fOutputStream, format);
			serializer.asDOMSerializer().serialize(document);
		
	}

	private void xmlWriteJavadocStandardParams(JavadocOptionsManager store, Element xmlJavadocDesc) throws DOMException {

		xmlJavadocDesc.setAttribute(store.DESTINATION, store.getDestination());
		xmlJavadocDesc.setAttribute(store.VISIBILITY, store.getAccess());
		xmlJavadocDesc.setAttribute(store.NOTREE, booleanToString(store.getBoolean("notree")));
		xmlJavadocDesc.setAttribute(store.NONAVBAR, booleanToString(store.getBoolean("nonavbar")));
		xmlJavadocDesc.setAttribute(store.NOINDEX, booleanToString(store.getBoolean("noindex")));
		xmlJavadocDesc.setAttribute(store.SPLITINDEX, booleanToString(store.getBoolean("splitindex")));
		xmlJavadocDesc.setAttribute(store.AUTHOR, booleanToString(store.getBoolean("author")));
		xmlJavadocDesc.setAttribute(store.VERSION, booleanToString(store.getBoolean("version")));
		xmlJavadocDesc.setAttribute(store.NODEPRECATEDLIST, booleanToString(store.getBoolean("nodeprecatedlist")));
		xmlJavadocDesc.setAttribute(store.NODEPRECATED, booleanToString(store.getBoolean("nodeprecated")));
		xmlJavadocDesc.setAttribute(store.PACKAGENAMES, toPackageList(store.getPackagenames()));
		xmlJavadocDesc.setAttribute(store.SOURCEPATH, store.getSourcepath());
		xmlJavadocDesc.setAttribute(store.CLASSPATH, store.getClasspath());
		String str= store.getOverview();
		if (!str.equals(""))
			xmlJavadocDesc.setAttribute(store.OVERVIEW, str);

		str= store.getStyleSheet();
		if (!str.equals(""))
			xmlJavadocDesc.setAttribute(store.STYLESHEETFILE, str);

		str= store.getAdditionalParams();
		if (!str.equals(""))
			xmlJavadocDesc.setAttribute(store.EXTRAOPTIONS, str);

	}

	private void xmlWriteDoclet(JavadocOptionsManager store, Document document, Element xmlJavadocDesc) throws DOMException {

		xmlJavadocDesc.setAttribute(store.PACKAGENAMES, toPackageList(store.getPackagenames()));
		xmlJavadocDesc.setAttribute(store.SOURCEPATH, store.getSourcepath());
		xmlJavadocDesc.setAttribute(store.CLASSPATH, store.getClasspath());
		xmlJavadocDesc.setAttribute(store.VISIBILITY, store.getAccess());

		Element doclet= document.createElement("doclet"); //$NON-NLS-1$
		xmlJavadocDesc.appendChild(doclet);
		doclet.setAttribute(store.NAME, store.getDocletName());
		doclet.setAttribute(store.PATH, store.getDocletPath());

		String str= store.getOverview();
		if (!str.equals(""))
			xmlJavadocDesc.setAttribute(store.OVERVIEW, str);

		str= store.getAdditionalParams();
		if (!str.equals(""))
			xmlJavadocDesc.setAttribute(store.EXTRAOPTIONS, str);

	}

	/**
	 * Closes this stream.
	 * It is the client's responsibility to close the stream.
	 * 
	 * @exception IOException
	 */
	private String toPackageList(List packagenames) {
		int i;
		StringBuffer buf= new StringBuffer();
		String[] strs= (String[]) packagenames.toArray(new String[packagenames.size()]);
		for (i = 0; i < strs.length-1; i++) {
			String pack = strs[i];
			buf.append(pack);
			buf.append(",");
		}
		//this should never happen
		if(strs.length > 0)
			buf.append(strs[i]);
		return buf.toString();
	}
	
	private String booleanToString(boolean bool){
		if(bool)
			return "true";
		else return"false";
	}

	public void close() throws IOException {
		if (fOutputStream != null) {
			fOutputStream.close();
		}
	}

}