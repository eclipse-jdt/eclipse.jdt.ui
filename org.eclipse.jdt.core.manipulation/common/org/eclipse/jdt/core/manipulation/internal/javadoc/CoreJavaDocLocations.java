/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation.internal.javadoc;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.URIUtil;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModularClassFile;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.CorextMessages;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;


public class CoreJavaDocLocations {

	private static final String JAR_PROTOCOL= "jar"; //$NON-NLS-1$

	public static final String ARCHIVE_PREFIX= "jar:"; //$NON-NLS-1$

	private static final QualifiedName PROJECT_JAVADOC= new QualifiedName("org.eclipse.jdt.ui", "project_javadoc_location"); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Sets the Javadoc location for an archive with the given path.
	 *
	 * @param project the Java project
	 * @param url the Javadoc location
	 */
	public static void setProjectJavadocLocation(IJavaProject project, URL url) {
		try {
			String location= url != null ? url.toExternalForm() : null;
			setProjectJavadocLocation(project, location);
		} catch (CoreException e) {
			JavaManipulationPlugin.log(e);
		}
	}

	private static void setProjectJavadocLocation(IJavaProject project, String url) throws CoreException {
		project.getProject().setPersistentProperty(PROJECT_JAVADOC, url);
	}

	public static URL getProjectJavadocLocation(IJavaProject project) {
		if (!project.getProject().isAccessible()) {
			return null;
		}
		try {
			String prop= project.getProject().getPersistentProperty(PROJECT_JAVADOC);
			if (prop == null) {
				return null;
			}
			return parseURL(prop);
		} catch (CoreException e) {
			JavaManipulationPlugin.log(e);
		}
		return null;
	}

	public static URL getLibraryJavadocLocation(IClasspathEntry entry) {
		if (entry == null) {
			throw new IllegalArgumentException("Entry must not be null"); //$NON-NLS-1$
		}

		int kind= entry.getEntryKind();
		if (kind != IClasspathEntry.CPE_LIBRARY && kind != IClasspathEntry.CPE_VARIABLE) {
			throw new IllegalArgumentException("Entry must be of kind CPE_LIBRARY or CPE_VARIABLE"); //$NON-NLS-1$
		}

		for (IClasspathAttribute attrib : entry.getExtraAttributes()) {
			if (IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME.equals(attrib.getName())) {
				return parseURL(attrib.getValue());
			}
		}
		return null;
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
			IClasspathEntry entry= root.getResolvedClasspathEntry();
			URL javadocLocation= getLibraryJavadocLocation(entry);
			if (javadocLocation != null) {
				return getLibraryJavadocLocation(entry);
			}
			entry= root.getRawClasspathEntry();
			switch (entry.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY:
				case IClasspathEntry.CPE_VARIABLE:
					return getLibraryJavadocLocation(entry);
				default:
					return null;
			}
		} else {
			return getProjectJavadocLocation(root.getJavaProject());
		}
	}

	public static URL getJavadocLocation(IJavaElement element, boolean includeMemberReference) throws JavaModelException {
		URL baseLocation= getJavadocBaseLocation(element);
		if (baseLocation == null) {
			return null;
		}

		String urlString= baseLocation.toExternalForm();

		StringBuffer urlBuffer= new StringBuffer(urlString);
		if (!urlString.endsWith("/")) { //$NON-NLS-1$
			urlBuffer.append('/');
		}

		StringBuffer pathBuffer= new StringBuffer();
		StringBuffer fragmentBuffer= new StringBuffer();

		switch (element.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT:
				appendPackageSummaryPath((IPackageFragment) element, pathBuffer);
				break;
			case IJavaElement.JAVA_PROJECT:
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				appendIndexPath(pathBuffer);
				break;
			case IJavaElement.IMPORT_CONTAINER:
				element= element.getParent();
				//$FALL-THROUGH$
			case IJavaElement.COMPILATION_UNIT:
				IType mainType= ((ICompilationUnit) element).findPrimaryType();
				if (mainType == null) {
					return null;
				}
				appendTypePath(mainType, pathBuffer);
				break;
			case IJavaElement.CLASS_FILE:
				if (element instanceof IModularClassFile) {
					try {
						appendModuleSummaryPath(((IModularClassFile) element).getModule(), pathBuffer);
					} catch (JavaModelException e) {
						return null;
					}
				} else {
					appendTypePath(((IOrdinaryClassFile) element).getType(), pathBuffer);
				}
				break;
			case IJavaElement.TYPE:
				appendTypePath((IType) element, pathBuffer);
				break;
			case IJavaElement.FIELD:
				IField field= (IField) element;
				appendTypePath(field.getDeclaringType(), pathBuffer);
				if (includeMemberReference) {
					appendFieldReference(field, fragmentBuffer);
				}
				break;
			case IJavaElement.METHOD:
				IMethod method= (IMethod) element;
				appendTypePath(method.getDeclaringType(), pathBuffer);
				if (includeMemberReference) {
					appendMethodReference(method, fragmentBuffer);
				}
				break;
			case IJavaElement.INITIALIZER:
				appendTypePath(((IMember) element).getDeclaringType(), pathBuffer);
				break;
			case IJavaElement.IMPORT_DECLARATION:
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
			case IJavaElement.PACKAGE_DECLARATION:
				IJavaElement pack= element.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
				if (pack != null) {
					appendPackageSummaryPath((IPackageFragment) pack, pathBuffer);
				} else {
					return null;
				}
				break;
			case IJavaElement.JAVA_MODULE:
				IModuleDescription module= (IModuleDescription) element;
				appendModuleSummaryPath(module, pathBuffer);
				break;
			default:
				return null;
		}

		return getURL(urlBuffer, pathBuffer, fragmentBuffer);
	}

	private static URL getURL(StringBuffer urlBuffer, StringBuffer pathBuffer, StringBuffer fragmentBuffer) {
		try {
			String fragment= fragmentBuffer.length() == 0 ? null : fragmentBuffer.toString();
			try {
				URI relativeURI= new URI(null, null, pathBuffer.toString(), fragment);
				urlBuffer.append(relativeURI.toString());
				return new URL(urlBuffer.toString());
			} catch (URISyntaxException e) {
				JavaManipulationPlugin.log(e);
				return new URL(urlBuffer.append(pathBuffer).toString());
			}
		} catch (MalformedURLException e) {
			JavaManipulationPlugin.log(e);
		}
		return null;
	}

	private static void appendPackageSummaryPath(IPackageFragment pack, StringBuffer buf) {
		appendModulePath(pack, buf);
		String packPath= pack.getElementName().replace('.', '/');
		buf.append(packPath);
		buf.append("/package-summary.html"); //$NON-NLS-1$
	}

	private static void appendModuleSummaryPath(IModuleDescription module, StringBuffer buf) {
		String moduleName= module.getElementName();
		buf.append(moduleName);
		buf.append("/module-summary.html"); //$NON-NLS-1$
	}

	private static void appendIndexPath(StringBuffer buf) {
		buf.append("index.html"); //$NON-NLS-1$
	}

	private static void appendTypePath(IType type, StringBuffer buf) {
		IPackageFragment pack= type.getPackageFragment();
		appendModulePath(pack, buf);
		String packPath= pack.getElementName().replace('.', '/');
		String typePath= type.getTypeQualifiedName('.');
		if (packPath.length() > 0) {
			buf.append(packPath);
			buf.append('/');
		}
		buf.append(typePath);
		buf.append(".html"); //$NON-NLS-1$
	}

	private static void appendModulePath(IPackageFragment pack, StringBuffer buf) {
		IModuleDescription moduleDescription= getModuleDescription(pack);
		if (moduleDescription != null) {
			String moduleName= moduleDescription.getElementName();
			if (moduleName != null && moduleName.length() > 0) {
				buf.append(moduleName);
				buf.append('/');
			}
		}
	}

	private static IModuleDescription getModuleDescription(IPackageFragment pack) {
		if (pack == null) {
			return null;
		}
		IModuleDescription moduleDescription= null;
		/*
		 * The Javadoc tool for Java SE 11 uses module name in the created URL.
		 * We can't know what format is required, so we just guess by the project's execution environment or compiler compliance.
		 */
		IJavaProject javaProject= pack.getJavaProject();
		if (javaProject != null && is11OrHigher(javaProject)) {
			if (pack.isReadOnly()) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) pack.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				if (root != null) {
					moduleDescription= root.getModuleDescription();
				}
			} else {
				try {
					moduleDescription= javaProject.getModuleDescription();
				} catch (JavaModelException e) {
					// do nothing
				}
			}
		}
		return moduleDescription;
	}

	/**
	 * This function finds out which jvm/source compliance is being used to see if its version 11 or
	 * higher.
	 *
	 * @param javaProject the java project for which the version number is to be verified.
	 * @return <i>true</i> if the jvm used is version 11 or higher (If no jvm is found then the
	 *         source compliance is used) else returns <i>false</i>
	 */
	private static boolean is11OrHigher(IJavaProject javaProject) {
		boolean is11orHigher= false;
		if (javaProject != null) {
			try {
				IVMInstall install= JavaRuntime.getVMInstall(javaProject);
				if (install instanceof AbstractVMInstall) {
					String vmver= ((AbstractVMInstall) install).getJavaVersion();
					is11orHigher= JavaModelUtil.is11OrHigher(vmver);
				} else {
					is11orHigher= JavaModelUtil.is11OrHigher(javaProject);
				}
			} catch (CoreException e) {
				is11orHigher= JavaModelUtil.is11OrHigher(javaProject);
			}
		}
		return is11orHigher;
	}

	/**
	 * This function finds out which jvm/source compliance is being used to see if its version 10 or
	 * higher.
	 *
	 * @param javaProject the java project for which the version number is to be verified.
	 * @return <i>true</i> if the jvm used is version 10 or higher (If no jvm is found then the
	 *         source compliance is used) else returns <i>false</i>
	 */
	private static boolean is10OrHigher(IJavaProject javaProject) {
		boolean is10orHigher= false;
		if (javaProject != null) {
			try {
				IVMInstall install= JavaRuntime.getVMInstall(javaProject);
				if (install instanceof AbstractVMInstall) {
					String vmver= ((AbstractVMInstall) install).getJavaVersion();
					is10orHigher= JavaModelUtil.is10OrHigher(vmver);
				} else {
					is10orHigher= JavaModelUtil.is10OrHigher(javaProject);
				}
			} catch (CoreException e) {
				is10orHigher= JavaModelUtil.is10OrHigher(javaProject);
			}
		}
		return is10orHigher;
	}

	/**
	 * This function finds out which jvm/source compliance is being used to see if its version 1.8
	 * or 9.
	 *
	 * @param javaProject the java project for which the version number is to be verified.
	 * @return <i>true</i> if the jvm used is 1.8 or 9 (If no jvm is found then the source
	 *         compliance is used) else returns <i>false</i>
	 */
	private static boolean is1d8Or9(IJavaProject javaProject) {
		boolean is1d8Or9= false;
		String compliance= JavaModelUtil.getSourceCompliance(javaProject);
		if (javaProject != null) {
			try {
				IVMInstall install= JavaRuntime.getVMInstall(javaProject);
				if (install instanceof AbstractVMInstall) {
					String vmver= ((AbstractVMInstall) install).getJavaVersion();
					is1d8Or9= JavaModelUtil.is1d8OrHigher(vmver) && !JavaModelUtil.is10OrHigher(vmver);
				} else {
					is1d8Or9= JavaCore.compareJavaVersions(compliance, JavaCore.VERSION_1_8) == 0 || JavaCore.compareJavaVersions(compliance, JavaCore.VERSION_9) == 0;
				}
			} catch (CoreException e) {
				is1d8Or9= JavaCore.compareJavaVersions(compliance, JavaCore.VERSION_1_8) == 0 || JavaCore.compareJavaVersions(compliance, JavaCore.VERSION_9) == 0;
			}
		}
		return is1d8Or9;
	}

	private static void appendFieldReference(IField field, StringBuffer buf) {
		buf.append(field.getElementName());
	}

	private static void appendMethodReference(IMethod meth, StringBuffer buf) throws JavaModelException {
		buf.append(meth.getElementName());

		/*
		 * The Javadoc tool for Java SE 8 changed the anchor syntax and now tries to avoid "strange" characters in URLs.
		 * This breaks all clients that directly create such URLs.
		 * We can't know what format is required, so we just guess by the project's compiler compliance.
		 */
		IJavaProject javaProject= meth.getJavaProject();
		boolean is1d8Or9= is1d8Or9(javaProject);
		boolean is10OrHigher= is10OrHigher(javaProject);
		buf.append(is1d8Or9 ? '-' : '(');
		String[] params= meth.getParameterTypes();
		IType declaringType= meth.getDeclaringType();
		boolean isVararg= Flags.isVarargs(meth.getFlags());
		int lastParam= params.length - 1;
		for (int i= 0; i <= lastParam; i++) {
			if (i != 0) {
				String paramDelim;
				if (is1d8Or9) {
					paramDelim= "-"; //$NON-NLS-1$
				} else if (is10OrHigher) {
					paramDelim= ","; //$NON-NLS-1$
				} else {
					paramDelim= ", "; //$NON-NLS-1$
				}
				buf.append(paramDelim);
			}
			String curr= Signature.getTypeErasure(params[i]);
			String fullName= JavaModelUtil.getResolvedTypeName(curr, declaringType);
			if (fullName == null) { // e.g. a type parameter "QE;"
				fullName= Signature.toString(Signature.getElementType(curr));
			}
			if (fullName != null) {
				buf.append(fullName);
				int dim= Signature.getArrayCount(curr);
				if (i == lastParam && isVararg) {
					dim--;
				}
				while (dim > 0) {
					buf.append(is1d8Or9 ? ":A" : "[]"); //$NON-NLS-1$ //$NON-NLS-2$
					dim--;
				}
				if (i == lastParam && isVararg) {
					buf.append("..."); //$NON-NLS-1$
				}
			}
		}
		buf.append(is1d8Or9 ? '-' : ')');
	}

	public static interface LocationResolver {
		public URL resolve(URL base);
	}

	/**
	 * Returns the location of the Javadoc.
	 *
	 * @param element whose Javadoc location has to be found
	 * @param isBinary <code>true</code> if the Java element is from a binary container
	 * @param callback Allow another chance to resolve the base url
	 * @return the location URL of the Javadoc or <code>null</code> if the location cannot be found
	 * @throws JavaModelException thrown when the Java element cannot be accessed
	 * @since 3.9
	 */
	public static String getBaseURL(IJavaElement element, boolean isBinary, LocationResolver callback) throws JavaModelException {
		if (isBinary) {
			// Source attachment usually does not include Javadoc resources
			// => Always use the Javadoc location as base:
			URL baseURL= CoreJavaDocLocations.getJavadocLocation(element, false);
			if (baseURL != null) {
				if (JAR_PROTOCOL.equals(baseURL.getProtocol())) {
					// It's a JarURLConnection, which is not known to the browser widget.
					// Let's start the help web server:
					URL baseURL2= callback == null ? null : callback.resolve(baseURL);
					if (baseURL2 != null) { // can be null if org.eclipse.help.ui is not available
						baseURL= baseURL2;
					}
				}
				return baseURL.toExternalForm();
			}
		} else {
			IResource resource= element.getResource();
			if (resource != null) {
				/*
				 * Too bad: Browser widget knows nothing about EFS and custom URL handlers,
				 * so IResource#getLocationURI() does not work in all cases.
				 * We only support the local file system for now.
				 * A solution could be https://bugs.eclipse.org/bugs/show_bug.cgi?id=149022 .
				 */
				IPath location= resource.getLocation();
				if (location != null)
					return location.toFile().toURI().toString();
			}
		}
		return null;
	}

	/**
	 * Returns the reason for why the Javadoc of the Java element could not be retrieved.
	 *
	 * @param element whose Javadoc could not be retrieved
	 * @param root the root of the Java element
	 * @return the String message for why the Javadoc could not be retrieved for the Java element or
	 *         <code>null</code> if the Java element is from a source container
	 * @since 3.9
	 */
	public static String getExplanationForMissingJavadoc(IJavaElement element, IPackageFragmentRoot root) {
		String message= null;
		try {
			boolean isBinary= (root.exists() && root.getKind() == IPackageFragmentRoot.K_BINARY);
			if (isBinary) {
				boolean hasAttachedJavadoc= CoreJavaDocLocations.getJavadocBaseLocation(element) != null;
				boolean hasAttachedSource= root.getSourceAttachmentPath() != null;
				IOpenable openable= element.getOpenable();
				boolean hasSource= openable.getBuffer() != null;

				// Provide hint why there's no Java doc
				if (!hasAttachedSource && !hasAttachedJavadoc)
					message= CorextMessages.JavaDocLocations_noAttachments;
				else if (!hasAttachedJavadoc && !hasSource)
					message= CorextMessages.JavaDocLocations_noAttachedJavadoc;
				else if (!hasAttachedSource)
					message= CorextMessages.JavaDocLocations_noAttachedSource;
				else if (!hasSource)
					message= CorextMessages.JavaDocLocations_noInformation;

			}
		} catch (JavaModelException e) {
			message= CorextMessages.JavaDocLocations_error_gettingJavadoc;
			JavaManipulationPlugin.log(e);
		}
		return message;
	}

	/**
	 * Handles the exception thrown from JDT Core when the attached Javadoc cannot be retrieved due
	 * to accessibility issues or location URL issue. This exception is not logged but the
	 * exceptions occurred due to other reasons are logged.
	 *
	 * @param e the exception thrown when retrieving the Javadoc fails
	 * @return the String message for why the Javadoc could not be retrieved
	 * @since 3.9
	 */
	public static String handleFailedJavadocFetch(CoreException e) {
		IStatus status= e.getStatus();
		if (JavaCore.PLUGIN_ID.equals(status.getPlugin())) {
			Throwable cause= e.getCause();
			int code= status.getCode();
			// See bug 120559, bug 400060 and bug 400062
			if (code == IJavaModelStatusConstants.CANNOT_RETRIEVE_ATTACHED_JAVADOC_TIMEOUT
					|| (code == IJavaModelStatusConstants.CANNOT_RETRIEVE_ATTACHED_JAVADOC && (cause instanceof FileNotFoundException || cause instanceof SocketException
							|| cause instanceof UnknownHostException
							|| cause instanceof ProtocolException)))
				return CorextMessages.JavaDocLocations_error_gettingAttachedJavadoc;
		}
		JavaManipulationPlugin.log(e);
		return CorextMessages.JavaDocLocations_error_gettingJavadoc;
	}

	/**
	 * Parse a URL from a String. This method first tries to treat <code>url</code> as a valid,
	 * encoded URL. If that didn't work, it tries to recover from bad URLs, e.g. the unencoded form
	 * we used to use in persistent storage.
	 *
	 * @param url a URL
	 * @return the parsed URL or <code>null</code> if the URL couldn't be parsed
	 * @since 3.9
	 */
	public static URL parseURL(String url) {
		try {
			try {
				return new URI(url).toURL();
			} catch (URISyntaxException e) {
				try {
					// don't log, since we used to store bad (unencoded) URLs
					if (url.startsWith("file:/")) { //$NON-NLS-1$
						// workaround for a bug in the 3-arg URI constructor for paths that contain '[' or ']':
						return new URI("file", null, url.substring(5), null).toURL(); //$NON-NLS-1$
					} else {
						return URIUtil.fromString(url).toURL();
					}
				} catch (URISyntaxException e1) {
					// last try, not expected to happen
					JavaManipulationPlugin.log(e);
					return new URL(url);
				}
			}
		} catch (MalformedURLException e) {
			JavaManipulationPlugin.log(e);
			return null;
		}
	}

	/**
	 * Returns the {@link File} of a <code>file:</code> URL. This method tries to recover from bad
	 * URLs, e.g. the unencoded form we used to use in persistent storage.
	 *
	 * @param url a <code>file:</code> URL
	 * @return the file
	 * @since 3.9
	 */
	public static File toFile(URL url) {
		try {
			return URIUtil.toFile(url.toURI());
		} catch (URISyntaxException e) {
			JavaManipulationPlugin.log(e);
			return new File(url.getFile());
		}
	}

	private CoreJavaDocLocations() {
	}
}
