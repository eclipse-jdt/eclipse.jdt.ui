/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xml.serialize.Method;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.SerializerFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackage;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerMessages;
import org.eclipse.jface.util.Assert;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @version 	1.0
 * @author
 */
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
	public void writeXML(Map args) throws IOException {
		if (!args.isEmpty()) {
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
			project.setAttribute("name", (String) args.get(JavadocWizard.PROJECT));
			project.setAttribute("default", "javadoc");

			Element javadocTarget= document.createElement("target");
			project.appendChild(javadocTarget);
			javadocTarget.setAttribute("name", "javadoc");

			Element xmlJavadocDesc= document.createElement("javadoc");
			javadocTarget.appendChild(xmlJavadocDesc);

			if (JavadocWizard.WRITECUSTOM == true)
				xmlWriteDoclet(args, document, xmlJavadocDesc);
			else
				xmlWriteJavadocStandardParams(args, xmlJavadocDesc);

			// Write the document to the stream
			OutputFormat format= new OutputFormat();
			format.setIndenting(true);
			SerializerFactory serializerFactory= SerializerFactory.getSerializerFactory(Method.XML);
			Serializer serializer= serializerFactory.makeSerializer(fOutputStream, format);
			serializer.asDOMSerializer().serialize(document);
		}
	}

	private void xmlWriteJavadocStandardParams(Map args, Element xmlJavadocDesc) throws DOMException {

		xmlJavadocDesc.setAttribute(JavadocWizard.STANDARD, (String) args.get(JavadocWizard.STANDARD));
		xmlJavadocDesc.setAttribute(JavadocWizard.VISIBILITY, (String) args.get(JavadocWizard.VISIBILITY));
		xmlJavadocDesc.setAttribute(JavadocWizard.NOTREE, (String) args.get(JavadocWizard.NOTREE));
		xmlJavadocDesc.setAttribute(JavadocWizard.NONAVBAR, (String) args.get(JavadocWizard.NONAVBAR));
		xmlJavadocDesc.setAttribute(JavadocWizard.NOINDEX, (String) args.get(JavadocWizard.NOINDEX));
		xmlJavadocDesc.setAttribute(JavadocWizard.SPLITINDEX, (String) args.get(JavadocWizard.SPLITINDEX));
		xmlJavadocDesc.setAttribute(JavadocWizard.AUTHOR, (String) args.get(JavadocWizard.AUTHOR));
		xmlJavadocDesc.setAttribute(JavadocWizard.VERSION, (String) args.get(JavadocWizard.VERSION));
		xmlJavadocDesc.setAttribute(JavadocWizard.NODEPRECATEDLIST, (String) args.get(JavadocWizard.NODEPRECATEDLIST));
		xmlJavadocDesc.setAttribute(JavadocWizard.NODEPRECATED, (String) args.get(JavadocWizard.NODEPRECATED));
		xmlJavadocDesc.setAttribute(JavadocWizard.PACKAGENAMES, (String) args.get(JavadocWizard.PACKAGENAMES));
		xmlJavadocDesc.setAttribute(JavadocWizard.SOURCEPATH, (String) args.get(JavadocWizard.SOURCEPATH));
		xmlJavadocDesc.setAttribute(JavadocWizard.CLASSPATH, (String) args.get(JavadocWizard.CLASSPATH));
		String str= (String) args.get(JavadocWizard.OVERVIEW);
		if (str != null)
			xmlJavadocDesc.setAttribute(JavadocWizard.OVERVIEW, str);

		str= (String) args.get(JavadocWizard.STYLESHEET);
		if (str != null)
			xmlJavadocDesc.setAttribute(JavadocWizard.STYLESHEET, str);

		str= (String) args.get(JavadocWizard.EXTRAOPTIONS);
		if (str != null)
			xmlJavadocDesc.setAttribute(JavadocWizard.EXTRAOPTIONS, str);

	}

	private void xmlWriteDoclet(Map args, Document document, Element xmlJavadocDesc) throws DOMException {

		xmlJavadocDesc.setAttribute(JavadocWizard.PACKAGENAMES, (String) args.get(JavadocWizard.PACKAGENAMES));
		xmlJavadocDesc.setAttribute(JavadocWizard.SOURCEPATH, (String) args.get(JavadocWizard.SOURCEPATH));
		xmlJavadocDesc.setAttribute(JavadocWizard.CLASSPATH, (String) args.get(JavadocWizard.CLASSPATH));
		xmlJavadocDesc.setAttribute(JavadocWizard.VISIBILITY, (String) args.get(JavadocWizard.VISIBILITY));

		Element doclet= document.createElement("doclet"); //$NON-NLS-1$
		xmlJavadocDesc.appendChild(doclet);
		doclet.setAttribute("name", (String) args.get(JavadocWizard.DOCLET));
		doclet.setAttribute("path", (String) args.get(JavadocWizard.DOCLETPATH));

		String str= (String) args.get(JavadocWizard.OVERVIEW);
		if (str != null)
			xmlJavadocDesc.setAttribute(JavadocWizard.OVERVIEW, str);

		str= (String) args.get(JavadocWizard.EXTRAOPTIONS);
		if (str != null)
			xmlJavadocDesc.setAttribute(JavadocWizard.EXTRAOPTIONS, str);

	}

	/**
	 * Closes this stream.
	 * It is the client's responsibility to close the stream.
	 * 
	 * @exception IOException
	 */
	public void close() throws IOException {
		if (fOutputStream != null) {
			fOutputStream.close();
		}
	}

}