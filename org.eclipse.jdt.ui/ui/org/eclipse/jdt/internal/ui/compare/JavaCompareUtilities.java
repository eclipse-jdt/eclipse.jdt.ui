/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.ui.text.JavaTextTools;


class JavaCompareUtilities {
	
	/**
	 * Returns a name for the given Java element that uses the same conventions
	 * as the JavaNode name of a corresponding element.
	 */
	static String getJavaElementID(IJavaElement je) {
		
		if (je instanceof IMember && ((IMember)je).isBinary())
			return null;
			
		StringBuffer sb= new StringBuffer();
		
		switch (je.getElementType()) {
		case JavaElement.COMPILATION_UNIT:
			sb.append(JavaElement.JEM_COMPILATIONUNIT);
			break;
		case JavaElement.TYPE:
			sb.append(JavaElement.JEM_TYPE);
			sb.append(je.getElementName());
			break;
		case JavaElement.FIELD:
			sb.append(JavaElement.JEM_FIELD);
			sb.append(je.getElementName());
			break;
		case JavaElement.METHOD:
			sb.append(JavaElement.JEM_METHOD);
			sb.append(JavaElementLabels.getElementLabel(je, JavaElementLabels.M_PARAMETER_TYPES));
			break;
		case JavaElement.INITIALIZER:
			String id= je.getHandleIdentifier();
			int pos= id.lastIndexOf(JavaElement.JEM_INITIALIZER);
			if (pos >= 0)
				sb.append(id.substring(pos));
			break;
		case JavaElement.PACKAGE_DECLARATION:
			sb.append(JavaElement.JEM_PACKAGEDECLARATION);
			break;
		case JavaElement.IMPORT_CONTAINER:
			sb.append('<');
			break;
		case JavaElement.IMPORT_DECLARATION:
			sb.append(JavaElement.JEM_IMPORTDECLARATION);
			sb.append(je.getElementName());			
			break;
		default:
			return null;
		}
		return sb.toString();
	}
	
	/**
	 * Returns a name which identifies the given typed name.
	 * The type is encoded as a single character at the beginning of the string.
	 */
	static String buildID(int type, String name) {
		StringBuffer sb= new StringBuffer();
		switch (type) {
		case JavaNode.CU:
			sb.append(JavaElement.JEM_COMPILATIONUNIT);
			break;
		case JavaNode.CLASS:
		case JavaNode.INTERFACE:
			sb.append(JavaElement.JEM_TYPE);
			sb.append(name);
			break;
		case JavaNode.FIELD:
			sb.append(JavaElement.JEM_FIELD);
			sb.append(name);
			break;
		case JavaNode.CONSTRUCTOR:
		case JavaNode.METHOD:
			sb.append(JavaElement.JEM_METHOD);
			sb.append(name);
			break;
		case JavaNode.INIT:
			sb.append(JavaElement.JEM_INITIALIZER);
			sb.append(name);
			break;
		case JavaNode.PACKAGE:
			sb.append(JavaElement.JEM_PACKAGEDECLARATION);
			break;
		case JavaNode.IMPORT:
			sb.append(JavaElement.JEM_IMPORTDECLARATION);
			sb.append(name);
			break;
		case JavaNode.IMPORT_CONTAINER:
			sb.append('<');
			break;
		default:
			Assert.isTrue(false);
			break;
		}
		return sb.toString();
	}

	static ImageDescriptor getImageDescriptor(String relativePath) {
		
		JavaPlugin plugin= JavaPlugin.getDefault();

		URL installURL= null;
		if (plugin != null)
			installURL= plugin.getDescriptor().getInstallURL();
					
		if (installURL != null) {
			try {
				URL url= new URL(installURL, "icons/full/" + relativePath); //$NON-NLS-1$
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
		return null;
	}
	
	static IDocumentPartitioner createJavaPartitioner() {
		JavaTextTools tools= getJavaTextTools();
		if (tools != null)
			return tools.createDocumentPartitioner();
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
					JavaPlugin.log(ex);
				}
			}
		}
		return null;
	}
	
	/**
	 * Breaks the given string into lines and strips off the line terminator.
	 */
	static String[] readLines(InputStream is) {
		
		try {
			StringBuffer sb= null;
			List list= new ArrayList();
			while (true) {
				int c= is.read();
				if (c == -1)
					break;
				if (c == '\n' || c == '\r') {
					if (sb != null)
						list.add(sb.toString());
					sb= null;
				} else {
					if (sb == null)
						sb= new StringBuffer();
					sb.append((char)c);
				}
			}
			if (sb != null)
				list.add(sb.toString());
			return (String[]) list.toArray(new String[list.size()]);

		} catch (IOException ex) {
			return null;

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ex) {
				}
			}
		}
	}
}
