/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000,2001
 */
package org.eclipse.jdt.internal.ui.compare;

import java.io.*;
import java.net.*;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;


class JavaCompareUtilities {
	
	public static ImageDescriptor getImageDescriptor(String relativePath) {
		
		JavaPlugin plugin= JavaPlugin.getDefault();

		URL installURL= null;
		if (plugin != null)
			installURL= plugin.getDescriptor().getInstallURL();
					
		if (installURL != null) {
			try {
				URL url= new URL(installURL, "icons/full/" + relativePath);
				return ImageDescriptor.createFromURL(url);
			} catch (MalformedURLException e) {
				Assert.isTrue(false);
			}
		}
		return null;
	}
	
	static JavaTextTools getJavaTextTools() {
		JavaPlugin plugin= JavaPlugin.getDefault();
		if (plugin != null)
			return plugin.getJavaTextTools();
		System.out.println("no Java plugin");
		return null;
	}
	
	static IDocumentPartitioner createJavaPartitioner() {
		JavaTextTools jtt= getJavaTextTools();
		if (jtt != null)
			return jtt.createDocumentPartitioner();
		return null;
	}

	/**
	 * Returns null if an error occurred.
	 */
	static String readString(InputStream is) {
		if (is == null)
			return null;
		BufferedReader reader= null;
		try {
			StringBuffer buffer= new StringBuffer();
			char[] part= new char[2048];
			int read= 0;
			reader= new BufferedReader(new InputStreamReader(is));

			while ((read= reader.read(part)) != -1)
				buffer.append(part, 0, read);
			
			return buffer.toString();
			
		} catch (IOException ex) {
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
				}
			}
		}
		return null;
	}
}