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
package org.eclipse.jdt.internal.corext.javadoc;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;

import org.eclipse.jdt.core.*;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.CorextMessages;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.JavaUIStatus;

public class JavaDocLocations {
	
	public static final String ARCHIVE_PREFIX= "jar:file:/"; //$NON-NLS-1$
	
	private static final QualifiedName QUALIFIED_NAME= new QualifiedName(JavaUI.ID_PLUGIN, "jdoclocation"); //$NON-NLS-1$
	
	private static final String NODE_ROOT= "javadoclocation"; //$NON-NLS-1$
	private static final String NODE_ENTRY= "location_01"; //$NON-NLS-1$
	private static final String NODE_PATH= "path"; //$NON-NLS-1$
	private static final String NODE_URL= "url"; //$NON-NLS-1$
	
	private static final String STORE_FILE= "javadoclocations.xml"; //$NON-NLS-1$
	
	private static final boolean IS_CASE_SENSITIVE = !new File("Temp").equals(new File("temp")); //$NON-NLS-1$ //$NON-NLS-2$
	
	private static Map fgJavadocLocations= null;
	private static JavaDocVMInstallListener fgVMInstallListener= null;
	
	
	private static Map getJavaDocLocations() {
		if (fgJavadocLocations == null) {
			fgJavadocLocations= new HashMap();
			try {
				initJavadocLocations(); //delayed initialization
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		return fgJavadocLocations;	
	}

	private static IPath canonicalizedPath(IPath externalPath) {
		if (externalPath == null || IS_CASE_SENSITIVE)
			return externalPath;

		if (ResourcesPlugin.getWorkspace().getRoot().findMember(externalPath) != null) {
			return externalPath;
		}

		try {
			return new Path(externalPath.toFile().getCanonicalPath());
		} catch (IOException e) {
		}
		return externalPath;
	}

	private static void setJavadocBaseLocation(IPath path, URL url, boolean save) {
		boolean needsSave;
		if (url == null) {
			Object old= getJavaDocLocations().remove(path);
			needsSave= save && (old != null);
		} else {
			URL old= (URL) getJavaDocLocations().put(path, url);
			needsSave= save && (old == null || !url.toExternalForm().equals(old.toExternalForm()));
		}
		if (needsSave) {
			try {
				storeLocations();
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}
	
	/**
	 * Gets the Javadoc location for an archive with the given path.
	 */
	private static URL getJavadocBaseLocation(IPath path) {
		return (URL) getJavaDocLocations().get(path);
	}		
	
	/**
	 * Sets the Javadoc location for an archive with the given path.
	 */
	public static void setLibraryJavadocLocation(IPath archivePath, URL url) {
		setJavadocBaseLocation(canonicalizedPath(archivePath), url, true);
	}

	/**
	 * Sets the Javadocs locations for archives with given paths.
	 */
	public static void setLibraryJavadocLocations(IPath[] archivePaths, URL[] urls) {
		for (int i= urls.length - 1; i >= 0 ; i--) {
			setJavadocBaseLocation(canonicalizedPath(archivePaths[i]), urls[i], i == 0);
		}
	}
	
	/**
	 * Sets the Javadoc location for an archive with the given path.
	 */
	public static void setProjectJavadocLocation(IJavaProject project, URL url) {
		setJavadocBaseLocation(project.getProject().getFullPath(), url, true);
	}
	
	public static URL getProjectJavadocLocation(IJavaProject project) {
		return getJavadocBaseLocation(project.getProject().getFullPath());
	}

	public static URL getLibraryJavadocLocation(IPath archivePath) {
		return getJavadocBaseLocation(canonicalizedPath(archivePath));
	}

	public static URL getJavadocBaseLocation(IJavaElement element) throws JavaModelException {	
		if (element.getElementType() == IJavaElement.JAVA_PROJECT) {
			return getProjectJavadocLocation((IJavaProject) element);
		}
		
		IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
		if (root == null) {
			return null;
		}

		if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
			return getLibraryJavadocLocation(root.getPath());
		} else {
			return getProjectJavadocLocation(root.getJavaProject());
		}	
	}
	
	// loading & storing

	private static File getStoreFile() {
		IPath path= JavaPlugin.getDefault().getStateLocation();
		path= path.append(STORE_FILE);
		return path.toFile();
	}
	
	private static JavaUIException createException(Throwable t, String message) {
		return new JavaUIException(JavaUIStatus.createError(IStatus.ERROR, message, t));
	}	

	private static synchronized void storeLocations() throws CoreException {
		Writer writer= null;
		try {
			writer= new FileWriter(getStoreFile());
			saveToStream(fgJavadocLocations, writer);
		} catch (IOException e) {
			throw createException(e, CorextMessages.getString("JavaDocLocations.error.writeFile")); //$NON-NLS-1$
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				//	error closing reader: ignore
			}
		}
	}
	
	private static boolean loadOldForCompatibility() {
		// in 2.0, the Javadoc locations were store as one big string im the persistent properties
		try {
			IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
			String xmlString= root.getPersistentProperty(QUALIFIED_NAME); 
			if (xmlString != null) { // only set when workspace is old
				Reader reader= new StringReader(xmlString);
				try {
					loadFromStream(reader);
					root.setPersistentProperty(QUALIFIED_NAME, null); // clear property
					return true;
				} finally {
					try {
						reader.close();
					} catch (IOException e) {
						// error closing reader: ignore
					}
				}
			}
		} catch (CoreException e) {
			JavaPlugin.log(e); // log but ignore
		}
		return false;
	}
	
	private static boolean loadFromFile() throws CoreException {
		File file= getStoreFile();
		if (file.exists()) {
			Reader reader= null;
			try {
				reader= new FileReader(getStoreFile());
				loadFromStream(reader);
				return true;
			} catch (IOException e) {
				throw createException(e, CorextMessages.getString("JavaDocLocations.error.readFile")); //$NON-NLS-1$
			} finally {
				try {
					if (reader != null)
					reader.close();
				} catch (IOException e) {}
			}
		}
		return false;
	}	
	
	
	private static void saveToStream(Map locations, Writer writer) throws CoreException {
		try {
			DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			DocumentBuilder builder= factory.newDocumentBuilder();		
			Document document= builder.newDocument();
			
			Element rootElement = document.createElement(NODE_ROOT);
			document.appendChild(rootElement);
	
			Iterator iter= locations.keySet().iterator();
			
			while (iter.hasNext()) {
				IPath path= (IPath) iter.next();
				URL url= getJavadocBaseLocation(path);
			
				Element varElement= document.createElement(NODE_ENTRY);
				varElement.setAttribute(NODE_PATH, path.toString());
				varElement.setAttribute(NODE_URL, url.toExternalForm());
				rootElement.appendChild(varElement);
			}

			Transformer transformer=TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(writer);

			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw createException(e, CorextMessages.getString("JavaDocLocations.error.serializeXML")); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw createException(e, CorextMessages.getString("JavaDocLocations.error.serializeXML")); //$NON-NLS-1$
		}
	}
	
	private static void loadFromStream(Reader reader) throws CoreException {
		Element cpElement;
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			cpElement = parser.parse(new InputSource(reader)).getDocumentElement();
		} catch (SAXException e) {
			throw createException(e, CorextMessages.getString("JavaDocLocations.error.readXML")); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw createException(e, CorextMessages.getString("JavaDocLocations.error.readXML")); //$NON-NLS-1$
		} catch (IOException e) {
			throw createException(e, CorextMessages.getString("JavaDocLocations.error.readXML")); //$NON-NLS-1$
		}
		
		if (cpElement == null) return;
		if (!cpElement.getNodeName().equalsIgnoreCase(NODE_ROOT)) {
			return;
		}
		NodeList list= cpElement.getChildNodes();
		int length= list.getLength();
		for (int i= 0; i < length; ++i) {
			Node node= list.item(i);
			short type= node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				Element element= (Element) node;
				if (element.getNodeName().equalsIgnoreCase(NODE_ENTRY)) {
					String varPath = element.getAttribute(NODE_PATH);
					String varURL = element.getAttribute(NODE_URL);
					try {
						setJavadocBaseLocation(new Path(varPath), new URL(varURL), false);
					} catch (MalformedURLException e) {
						throw createException(e, CorextMessages.getString("JavaDocLocations.error.readXML")); //$NON-NLS-1$
					}
				}
			}
		}
	}	
	
	
	public static void shutdownJavadocLocations() {
		if (fgVMInstallListener == null) {
			return;
		}
		fgVMInstallListener.remove();
		fgVMInstallListener= null;
		fgJavadocLocations= null;			
	}
	
	private static synchronized void initJavadocLocations() throws CoreException {
		try {
			if (loadOldForCompatibility()) {
				storeLocations();
			} else {
				loadFromFile();
			}
		} finally {
			fgVMInstallListener= new JavaDocVMInstallListener();
			fgVMInstallListener.init();	
		}			
	}
	
	public static URL getJavadocLocation(IJavaElement element, boolean includeMemberReference) throws JavaModelException {
		URL baseLocation= getJavadocBaseLocation(element);
		if (baseLocation == null) {
			return null;
		}

		String urlString= baseLocation.toExternalForm();

		StringBuffer pathBuffer= new StringBuffer(urlString);
		if (!urlString.endsWith("/")) { //$NON-NLS-1$
			pathBuffer.append('/');
		}

		switch (element.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT:
				appendPackageSummaryPath((IPackageFragment) element, pathBuffer);
				break;
			case IJavaElement.JAVA_PROJECT:
			case IJavaElement.PACKAGE_FRAGMENT_ROOT :
				appendIndexPath(pathBuffer);
				break;
			case IJavaElement.IMPORT_CONTAINER :
				element= element.getParent();
				// fall through
			case IJavaElement.COMPILATION_UNIT :
				IType mainType= ((ICompilationUnit) element).findPrimaryType();
				if (mainType == null) {
					return null;
				}
				appendTypePath(mainType, pathBuffer);
				break;
			case IJavaElement.CLASS_FILE :
				appendTypePath(((IClassFile) element).getType(), pathBuffer);
				break;
			case IJavaElement.TYPE :
				appendTypePath((IType) element, pathBuffer);
				break;
			case IJavaElement.FIELD :
				IField field= (IField) element;
				appendTypePath(field.getDeclaringType(), pathBuffer);
				if (includeMemberReference) {
					appendFieldReference(field, pathBuffer);
				}
				break;
			case IJavaElement.METHOD :
				IMethod method= (IMethod) element;
				appendTypePath(method.getDeclaringType(), pathBuffer);
				if (includeMemberReference) {
					appendMethodReference(method, pathBuffer);
				}
				break;
			case IJavaElement.INITIALIZER :
				appendTypePath(((IMember) element).getDeclaringType(), pathBuffer);
				break;
			case IJavaElement.IMPORT_DECLARATION :
				IImportDeclaration decl= (IImportDeclaration) element;

				if (decl.isOnDemand()) {
					IJavaElement cont= JavaModelUtil.findTypeContainer(element.getJavaProject(), Signature.getQualifier(decl.getElementName()));
					if (cont instanceof IType) {
						appendTypePath((IType) cont, pathBuffer);
					} else if (cont instanceof IPackageFragment) {
						appendPackageSummaryPath((IPackageFragment) cont, pathBuffer);
					}
				} else {
					IType imp= element.getJavaProject().findType(decl.getElementName());
					appendTypePath(imp, pathBuffer);
				}
				break;
			case IJavaElement.PACKAGE_DECLARATION :
				IJavaElement pack= element.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
				if (pack != null) {
					appendPackageSummaryPath((IPackageFragment) pack, pathBuffer);
				} else {
					return null;
				}
				break;
			default :
				return null;
		}

		try {
			return new URL(pathBuffer.toString());
		} catch (MalformedURLException e) {
			JavaPlugin.log(e);
		}
		return null;
	}	
		
	private static void appendPackageSummaryPath(IPackageFragment pack, StringBuffer buf) {
		String packPath= pack.getElementName().replace('.', '/');
		buf.append(packPath);
		buf.append("/package-summary.html"); //$NON-NLS-1$
	}
	
	private static void appendIndexPath(StringBuffer buf) {
		buf.append("index.html"); //$NON-NLS-1$
	}	
	
	private static void appendTypePath(IType type, StringBuffer buf) {
		IPackageFragment pack= type.getPackageFragment();
		String packPath= pack.getElementName().replace('.', '/');
		String typePath= JavaModelUtil.getTypeQualifiedName(type);
		buf.append(packPath);
		buf.append('/');
		buf.append(typePath);
		buf.append(".html"); //$NON-NLS-1$
	}		
		
	private static void appendFieldReference(IField field, StringBuffer buf) {
		buf.append('#');
		buf.append(field.getElementName());
	}
	
	private static void appendMethodReference(IMethod meth, StringBuffer buf) throws JavaModelException {
		buf.append('#');
		buf.append(meth.getElementName());	
		
		buf.append('(');
		String[] params= meth.getParameterTypes();
		IType declaringType= meth.getDeclaringType();
		for (int i= 0; i < params.length; i++) {
			if (i != 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			String curr= params[i];
			String fullName= JavaModelUtil.getResolvedTypeName(curr, declaringType);
			if (fullName != null) {
				buf.append(fullName);
				int dim= Signature.getArrayCount(curr);
				while (dim > 0) {
					buf.append("[]"); //$NON-NLS-1$
					dim--;
				}
			}
		}
		buf.append(')');
	}


}
