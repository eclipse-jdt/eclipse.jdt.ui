/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.compare;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.swt.graphics.Image;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.util.Assert;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.ui.*;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.ui.text.JavaTextTools;


class JavaCompareUtilities {
	
	static int getTabSize() {
		return CodeFormatterUtil.getTabWidth();
	}
		
	static String getString(ResourceBundle bundle, String key, String dfltValue) {
		
		if (bundle != null) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException x) {
				// NeedWork
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
				// NeedWork
			} catch (MissingResourceException x) {
				// NeedWork
			}
		}
		return dfltValue;
	}

	static ImageDescriptor getImageDescriptor(int type) {
		switch (type) {			
		case IJavaElement.INITIALIZER:
		case IJavaElement.METHOD:
			return getImageDescriptor("obj16/compare_method.gif"); //$NON-NLS-1$			
		case IJavaElement.FIELD:
			return getImageDescriptor("obj16/compare_field.gif"); //$NON-NLS-1$
		case IJavaElement.PACKAGE_DECLARATION:
			return JavaPluginImages.DESC_OBJS_PACKDECL;
		case IJavaElement.IMPORT_DECLARATION:
			return JavaPluginImages.DESC_OBJS_IMPDECL;
		case IJavaElement.IMPORT_CONTAINER:
			return JavaPluginImages.DESC_OBJS_IMPCONT;
		case IJavaElement.COMPILATION_UNIT:
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
		if (t == IJavaElement.TYPE) {
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
		case IJavaElement.COMPILATION_UNIT:
			sb.append(JavaElement.JEM_COMPILATIONUNIT);
			break;
		case IJavaElement.TYPE:
			sb.append(JavaElement.JEM_TYPE);
			sb.append(je.getElementName());
			break;
		case IJavaElement.FIELD:
			sb.append(JavaElement.JEM_FIELD);
			sb.append(je.getElementName());
			break;
		case IJavaElement.METHOD:
			sb.append(JavaElement.JEM_METHOD);
			sb.append(JavaElementLabels.getElementLabel(je, JavaElementLabels.M_PARAMETER_TYPES));
			break;
		case IJavaElement.INITIALIZER:
			String id= je.getHandleIdentifier();
			int pos= id.lastIndexOf(JavaElement.JEM_INITIALIZER);
			if (pos >= 0)
				sb.append(id.substring(pos));
			break;
		case IJavaElement.PACKAGE_DECLARATION:
			sb.append(JavaElement.JEM_PACKAGEDECLARATION);
			break;
		case IJavaElement.IMPORT_CONTAINER:
			sb.append('<');
			break;
		case IJavaElement.IMPORT_DECLARATION:
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
	
	static void setupDocument(IDocument document) {
		JavaTextTools tools= getJavaTextTools();
		if (tools != null)
			tools.setupJavaDocumentPartitioner(document);
	}

	/**
	 * Reads the contents of the given input stream into a string.
	 * The function assumes that the input stream uses the platform's default encoding
	 * (<code>ResourcesPlugin.getEncoding()</code>).
	 * Returns null if an error occurred.
	 */
	private static String readString(InputStream is, String encoding) {
		if (is == null)
			return null;
		BufferedReader reader= null;
		try {
			StringBuffer buffer= new StringBuffer();
			char[] part= new char[2048];
			int read= 0;
			reader= new BufferedReader(new InputStreamReader(is, encoding));

			while ((read= reader.read(part)) != -1)
				buffer.append(part, 0, read);
			
			return buffer.toString();
			
		} catch (IOException ex) {
			// NeedWork
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
					// silently ignored
				}
			}
		}
		return null;
	}
	
	public static String readString(IStreamContentAccessor sa) throws CoreException {
		InputStream is= sa.getContents();
		if (is != null) {
			String encoding= null;
			if (sa instanceof IEncodedStreamContentAccessor)
				encoding= ((IEncodedStreamContentAccessor)sa).getCharset();
			if (encoding == null)
				encoding= ResourcesPlugin.getEncoding();
			return readString(is, encoding);
		}
		return null;
	}

	/**
	 * Returns the contents of the given string as an array of bytes 
	 * in the platform's default encoding.
	 */
	static byte[] getBytes(String s, String encoding) {
		try {
			return s.getBytes(encoding);
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
	static String[] readLines(InputStream is2, String encoding) {
		
		BufferedReader reader= null;
		try {
			reader= new BufferedReader(new InputStreamReader(is2, encoding));
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
					// silently ignored
				}
			}
		}
	}
}
