/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.BufferedOutputStream;import java.io.BufferedWriter;import java.io.IOException;import java.io.ObjectOutputStream;import java.io.OutputStream;import java.io.OutputStreamWriter;import java.util.Iterator;import javax.xml.parsers.DocumentBuilder;import javax.xml.parsers.DocumentBuilderFactory;import javax.xml.parsers.ParserConfigurationException;import org.apache.xml.serialize.Method;import org.apache.xml.serialize.OutputFormat;import org.apache.xml.serialize.Serializer;import org.apache.xml.serialize.SerializerFactory;import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.core.resources.IResource;import org.eclipse.jface.util.Assert;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragment;

/**
 * Writes a JarPackage to an underlying OutputStream
 */
public class JarPackageWriter extends Object {
	
	protected OutputStream fOutputStream;
	
	/**
	 * Create a JarPackageWriter on the given output stream.
	 * It is the clients responsibility to close the output stream.
	 **/
	public JarPackageWriter(OutputStream outputStream) {
		Assert.isNotNull(outputStream);
		fOutputStream= new BufferedOutputStream(outputStream);
	}
	/**
	 * Hook for possible subclasses
	 **/
	protected JarPackageWriter() {
	}
	/**
     * Writes the specified object to the underlying stream.
     * 
     * @exception IOException	if writing to the underlying stream fails
     * @deprecated As of 0.114, replaced by writeXML - will be removed
     */
    public void writeObject(JarPackage jarPackage) throws IOException {
    	Assert.isNotNull(jarPackage);
		new ObjectOutputStream(fOutputStream).writeObject(jarPackage);
	}
	/**
     * Writes a XML representation of the JAR specification
     * to to the underlying stream.
     * @exception IOException	if writing to the underlying stream fails
     */
    public void writeXML(JarPackage jarPackage) throws IOException {
    	Assert.isNotNull(jarPackage);
    	DocumentBuilder docBuilder= null;
    	DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
    	factory.setValidating(true);
 		try {   	
	    	docBuilder= factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new IOException("Could not get XML builder");
 		}
		Document document= docBuilder.newDocument();
		
		// Document and root node
		Element xmlJarDesc= document.createElement("jardesc");
		document.appendChild(xmlJarDesc);

		// JAR location
		Element jar= document.createElement("jar");
		xmlJarDesc.appendChild(jar);
		jar.setAttribute("path", jarPackage.getJarLocation().toString());
		
		// Options
		Element options= document.createElement("options");
		xmlJarDesc.appendChild(options);
		options.setAttribute("overwrite", "" + jarPackage.allowOverwrite());
		options.setAttribute("compress", "" + jarPackage.isCompressed());
		options.setAttribute("exportErrors", "" + jarPackage.exportWarnings());
		options.setAttribute("exportWarnings", "" + jarPackage.exportWarnings());
		options.setAttribute("logErrors", "" + jarPackage.logErrors());
		options.setAttribute("logWarnings", "" + jarPackage.logWarnings());
		options.setAttribute("saveDescription", "" + jarPackage.isDescriptionSaved());
		options.setAttribute("descriptionLocation", jarPackage.getDescriptionLocation().toString());

		// Manifest
		Element manifest= document.createElement("manifest");
		xmlJarDesc.appendChild(manifest);
		manifest.setAttribute("manifestVersion", jarPackage.getManifestVersion());
		manifest.setAttribute("usesManifest", "" + jarPackage.usesManifest());
		manifest.setAttribute("reuseManifest", "" + jarPackage.isManifestReused());
		manifest.setAttribute("saveManifest", "" + jarPackage.isManifestSaved());
		manifest.setAttribute("generateManifest", "" + jarPackage.isManifestGenerated());
		manifest.setAttribute("manifestLocation", jarPackage.getManifestLocation().toString());
		if (jarPackage.getMainClass() != null)
			manifest.setAttribute("mainClassHandleIdentifier", jarPackage.getMainClass().getHandleIdentifier());
		// Sealing
		Element sealing= document.createElement("sealing");
		manifest.appendChild(sealing);
		sealing.setAttribute("sealJar", "" + jarPackage.isJarSealed());
		Element packagesToSeal= document.createElement("packagesToSeal");
		sealing.appendChild(packagesToSeal);
		add(jarPackage.getPackagesToSeal(), packagesToSeal, document);
		Element packagesToUnSeal= document.createElement("packagesToUnSeal");
		sealing.appendChild(packagesToUnSeal);
		add(jarPackage.getPackagesToUnseal(), packagesToUnSeal, document);

		// Selected elements
		Element selectedElements= document.createElement("selectedElements");
		xmlJarDesc.appendChild(selectedElements);
		selectedElements.setAttribute("exportClassFiles", "" + jarPackage.areClassFilesExported());
		selectedElements.setAttribute("exportJavaFiles", "" + jarPackage.areJavaFilesExported());
		Iterator iter= jarPackage.getSelectedElements().iterator();
		while (iter.hasNext()) {
			Object element= iter.next();
			if (element instanceof IResource)
				add((IResource)element, selectedElements, document);
			else if (element instanceof IJavaElement)
				add((IJavaElement)element, selectedElements, document);
			// Note: Other file types are not handled by this writer
		}

		// Write the document to the stream
		OutputFormat format= new OutputFormat();
		format.setIndenting(true);
		SerializerFactory serializerFactory= SerializerFactory.getSerializerFactory(Method.XML);
		Serializer serializer= serializerFactory.makeSerializer(fOutputStream,	format);
		serializer.asDOMSerializer().serialize(document);
    }
	/**
     * Writes a String representation of the JAR specification
     * to to the underlying stream.
     * @exception IOException	Writing to the underlying stream.
     */
    public void writeString(JarPackage jarPackage) throws IOException {
    	Assert.isNotNull(jarPackage);
		OutputStreamWriter streamWriter= new OutputStreamWriter(fOutputStream);
		BufferedWriter writer= new BufferedWriter(streamWriter);
		writer.write("--- JAR package def: ---");
		writer.newLine();
		writer.write("use to init:" + jarPackage.isUsedToInitialize());		
		writer.newLine();
		writer.write("export bin: " + jarPackage.areClassFilesExported());
		writer.newLine();
		writer.write("export java:" + jarPackage.areJavaFilesExported());
		writer.newLine();
		writer.write("JAR file:   " + jarPackage.getJarLocation().toOSString());
		writer.newLine();
		writer.write("compressed: " + jarPackage.isCompressed());
		writer.newLine();
		writer.write("overwrite:  " + jarPackage.allowOverwrite());
		writer.newLine();
		writer.write("save desc:  " + jarPackage.isDescriptionSaved());
		writer.newLine();
		writer.write("desc file:  " + jarPackage.getDescriptionLocation().toString());
		writer.newLine();
		writer.write("--");
		writer.newLine();
		writer.write("generate MF " + jarPackage.isManifestGenerated());
		writer.newLine();
		writer.write("save MF:    " + jarPackage.isManifestSaved());
		writer.newLine();
		writer.write("reuse MF:   " + jarPackage.isManifestReused());
		writer.newLine();
		writer.write("Manifest:   " + jarPackage.getManifestLocation().toString());
		writer.newLine();
		writer.write("JAR sealed: " + jarPackage.isJarSealed());
		writer.newLine();
		writer.write("Main-Class: " + jarPackage.getMainClassName());
		writer.newLine();
		writer.write("Class-Path: " + jarPackage.getDownloadExtensionsPath());
		writer.flush();
    }
	/**
     * Closes this stream.
     * It is the client's responsibility to close the stream.
     * 
	 * @exception IOException
     */
    public void close() throws IOException {
    	if (fOutputStream != null) {
			fOutputStream.flush();
			fOutputStream.close();
    	}
	}

	private void add(IResource resource, Element parent, Document document) {
		Element element= null;
		if (resource.getType() == IResource.PROJECT) {
			element= document.createElement("project");
			parent.appendChild(element);
			element.setAttribute("name", resource.getName());
			return;
		}
		if (resource.getType() == IResource.FILE)
			element= document.createElement("file");
		else if (resource.getType() == IResource.FOLDER)
			element= document.createElement("folder");
		parent.appendChild(element);
		element.setAttribute("path", resource.getFullPath().toString());
	}
	
	private void add(IJavaElement javaElement, Element parent, Document document) {
		Element element= document.createElement("javaElement");
		parent.appendChild(element);
		element.setAttribute("handleIdentifier", javaElement.getHandleIdentifier());
	}

	private void add(IPackageFragment[] packages, Element parent, Document document) {
		for (int i= 0; i < packages.length; i++) {
			Element pkg= document.createElement("package");
			parent.appendChild(pkg);
			pkg.setAttribute("handleIdentifier", packages[i].getHandleIdentifier());
		}
	}
}
