/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.eclipse.jface.util.Assert;

/**
 * Writes a JarPackage to an underlying OutputStream
 */
public class JarPackageWriter extends Object {
	
	protected OutputStream fOutputStream;
	
	/**
	 * Create a JarPackageWriter on the given output stream.
	 **/
	public JarPackageWriter(OutputStream outputStream) {
		Assert.isNotNull(outputStream);
		fOutputStream= outputStream;
	}
	/**
	 * Hook for possible subclasses
	 **/
	protected JarPackageWriter() {
	}
	/**
     * Writes the specified object to the underlying stream.
     * @exception IOException	Writing to the underlying stream.
     */
    public void writeObject(JarPackage jarPackage) throws IOException {
    	Assert.isNotNull(jarPackage);
		new ObjectOutputStream(fOutputStream).writeObject(jarPackage);
	}
	/**
     * Writes a XML representation of the JAR specification
     * to to the underlying stream.
     * @exception IOException	Writing to the underlying stream.
     */
    public void writeXML(JarPackage jarPackage) throws IOException {
    	Assert.isNotNull(jarPackage);
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
	 * @exception IOException
     */
    public void close() throws IOException {
		fOutputStream.flush();
		fOutputStream.close();
	}
}
