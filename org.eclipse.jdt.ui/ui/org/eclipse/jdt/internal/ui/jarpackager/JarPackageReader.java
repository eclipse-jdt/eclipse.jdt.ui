/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.util.Assert;

import org.eclipse.core.runtime.Path;

/**
 * Reads data from an InputStream and returns a JarPackage
 */
public class JarPackageReader extends Object {
	
	protected InputStream fInputStream;
	
	/**
	 * Reads a Jar Package from the underlying stream.
	 **/
	public JarPackageReader(InputStream inputStream) {
		Assert.isNotNull(inputStream);
		fInputStream= new BufferedInputStream(inputStream);
	}
	/**
	 * Hook for possible subclasses
	 **/
	protected JarPackageReader() {
	}
	/**
     * Reads the JAR specification from the underlying stream.
     * @exception IOException				if writing to the underlying stream fails
     * @exception ClassNotFoundException	if one of the classes in the stream is not found
     */
    public JarPackage readObject() throws IOException, ClassNotFoundException {
		JarPackage jarPackage= null;
		ObjectInputStream objectInput= new ObjectInputStream(fInputStream);
		jarPackage= (JarPackage)objectInput.readObject();
		if (jarPackage != null) {
			org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixme("TO PREVENT NPE FOR NOW - MUST BE HANDLED BY READER");
			jarPackage.setJarLocation(new Path(""));
			jarPackage.setDescriptionLocation(new Path(""));
			jarPackage.setManifestLocation(new Path(""));
			jarPackage.setDownloadExtensionsPath("");
		}
		return jarPackage;
    }
	/**
     * Closes this stream.
	 * @exception IOException
     */
    public void close() throws IOException {
		fInputStream.close();
	}
}
