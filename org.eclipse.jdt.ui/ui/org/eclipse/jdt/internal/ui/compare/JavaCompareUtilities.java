/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.util.Assert;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.ui.*;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.ui.text.JavaTextTools;


class JavaCompareUtilities {
	
	static int getTabSize() {
		String string= (String) JavaCore.getOptions().get(JavaCore.FORMATTER_TAB_SIZE);
		try {
			int i= Integer.parseInt(string);
			if (i >= 0) {
				return i;
			}
		} catch (NumberFormatException e) {
		}
		return 4;
	}
		
	static String getString(ResourceBundle bundle, String key, String dfltValue) {
		
		if (bundle != null) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException x) {
			}
		}
		return dfltValue;
	}
	
	static String getString(ResourceBundle bundle, String key) {
		return getString(bundle, key, key);
	}
	
	static int getInteger(ResourceBundle bundle, String key, int dfltValue) {
		
		if (bundle != null) {
			try {
				String s= bundle.getString(key);
				if (s != null)
					return Integer.parseInt(s);
			} catch (NumberFormatException x) {
			} catch (MissingResourceException x) {
			}
		}
		return dfltValue;
	}

	static ImageDescriptor getImageDescriptor(int type) {
		switch (type) {			
		case IMember.INITIALIZER:
		case IMember.METHOD:
			return getImageDescriptor("obj16/compare_method.gif"); //$NON-NLS-1$			
		case IMember.FIELD:
			return getImageDescriptor("obj16/compare_field.gif"); //$NON-NLS-1$
		case IMember.PACKAGE_DECLARATION:
			return JavaPluginImages.DESC_OBJS_PACKDECL;
		case IMember.IMPORT_DECLARATION:
			return JavaPluginImages.DESC_OBJS_IMPDECL;
		case IMember.IMPORT_CONTAINER:
			return JavaPluginImages.DESC_OBJS_IMPCONT;
		case IMember.COMPILATION_UNIT:
			return JavaPluginImages.DESC_OBJS_CUNIT;
		}	
		return ImageDescriptor.getMissingImageDescriptor();
	}
	
	static ImageDescriptor getTypeImageDescriptor(boolean isClass) {
		if (isClass)
			return JavaPluginImages.DESC_OBJS_CLASS;
		return JavaPluginImages.DESC_OBJS_INTERFACE;
	}

	static ImageDescriptor getImageDescriptor(IMember element) {
		int t= element.getElementType();
		if (t == IMember.TYPE) {
			IType type= (IType) element;
			try {
				return getTypeImageDescriptor(type.isClass());
			} catch (CoreException e) {
				JavaPlugin.log(e);
				return JavaPluginImages.DESC_OBJS_GHOST;
			}
		}
		return getImageDescriptor(t);
	}
	
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
	
	static Image getImage(IMember member) {
		ImageDescriptor id= getImageDescriptor(member);
		return id.createImage();
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
	 * Reads the contents of the given input stream into a string.
	 * The function assumes that the input stream uses the platform's default encoding
	 * (<code>ResourcesPlugin.getEncoding()</code>).
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
			reader= new BufferedReader(new InputStreamReader(is, ResourcesPlugin.getEncoding()));

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
	 * Returns the contents of the given string as an array of bytes 
	 * in the platform's default encoding.
	 */
	static byte[] getBytes(String s) {
		try {
			return s.getBytes(ResourcesPlugin.getEncoding());
		} catch (UnsupportedEncodingException e) {
			return s.getBytes();
		}
	}
	
	/**
	 * Breaks the contents of the given input stream into an array of strings.
	 * The function assumes that the input stream uses the platform's default encoding
	 * (<code>ResourcesPlugin.getEncoding()</code>).
	 * Returns null if an error occurred.
	 */
	static String[] readLines(InputStream is2) {
		
		BufferedReader reader= null;
		try {
			reader= new BufferedReader(new InputStreamReader(is2, ResourcesPlugin.getEncoding()));
			StringBuffer sb= new StringBuffer();
			List list= new ArrayList();
			while (true) {
				int c= reader.read();
				if (c == -1)
					break;
				sb.append((char)c);
				if (c == '\r') {	// single CR or a CR followed by LF
					c= reader.read();
					if (c == -1)
						break;
					sb.append((char)c);
					if (c == '\n') {
						list.add(sb.toString());
						sb= new StringBuffer();
					}
				} else if (c == '\n') {	// a single LF
					list.add(sb.toString());
					sb= new StringBuffer();
				}
			}
			if (sb.length() > 0)
				list.add(sb.toString());
			return (String[]) list.toArray(new String[list.size()]);

		} catch (IOException ex) {
			return null;

		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
					JavaPlugin.log(ex);
				}
			}
		}
	}
}
