/*******************************************************************************
 * Copyright (c) 2008, 2022 IBM Corporation and others.
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
 *     Stephan Herrmann - Contribution for Bug 403917 - [1.8] Render TYPE_USE annotations in Javadoc hover/view
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;

/**
 * Links inside Javadoc hovers and Javadoc view.
 *
 * @since 1.21
 */
public class CoreJavaElementLinks {

	public static final String OPEN_LINK_SCHEME= "eclipse-open"; //$NON-NLS-1$
	public static final String JAVADOC_SCHEME= "eclipse-javadoc"; //$NON-NLS-1$
	public static final String JAVADOC_VIEW_SCHEME= "eclipse-javadoc-view"; //$NON-NLS-1$
	private static final char LINK_BRACKET_REPLACEMENT= '\u2603';

	/**
	 * The link is composed of a number of segments, separated by LINK_SEPARATOR:
	 * <p>
	 * segments[0]: ""<br>
	 * segments[1]: baseElementHandle<br>
	 * segments[2]: typeName<br>
	 * segments[3]: memberName<br>
	 * segments[4...]: parameterTypeName (optional)
	 */
	private static final char LINK_SEPARATOR= '\u2602';

	private CoreJavaElementLinks() {
		// static only
	}

	/**
	 * Creates an {@link URI} with the given scheme for the given element.
	 *
	 * @param scheme the scheme
	 * @param element the element
	 * @return an {@link URI}, encoded as {@link URI#toASCIIString() ASCII} string, ready to be used
	 *         as <code>href</code> attribute in an <code>&lt;a&gt;</code> tag
	 * @throws URISyntaxException if the arguments were invalid
	 */
	public static String createURI(String scheme, IJavaElement element) throws URISyntaxException {
		return createURI(scheme, element, null, null, null);
	}

	/**
	 * Creates an {@link URI} with the given scheme based on the given element.
	 * The additional arguments specify a member referenced from the given element.
	 *
	 * @param scheme a scheme
	 * @param element the declaring element
	 * @param refTypeName a (possibly qualified) type or package name, can be <code>null</code>
	 * @param refMemberName a member name, can be <code>null</code>
	 * @param refParameterTypes a (possibly empty) array of (possibly qualified) parameter type
	 *            names, can be <code>null</code>
	 * @return an {@link URI}, encoded as {@link URI#toASCIIString() ASCII} string, ready to be used
	 *         as <code>href</code> attribute in an <code>&lt;a&gt;</code> tag
	 * @throws URISyntaxException if the arguments were invalid
	 */
	public static String createURI(String scheme, IJavaElement element, String refTypeName, String refMemberName, String[] refParameterTypes) throws URISyntaxException {
		return createURIAsUri(scheme, element, refTypeName, refMemberName, refParameterTypes).toASCIIString();
	}

	public static URI createURIAsUri(String scheme, IJavaElement element, String refTypeName, String refMemberName, String[] refParameterTypes) throws URISyntaxException {
		/*
		 * We use an opaque URI, not ssp and fragments (to work around Safari bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=212527 (wrongly encodes #)).
		 */

		StringBuilder ssp= new StringBuilder(60);
		ssp.append(LINK_SEPARATOR); // make sure first character is not a / (would be hierarchical URI)

		// replace '[' manually, since URI confuses it for an IPv6 address as per RFC 2732:
		ssp.append(element.getHandleIdentifier().replace('[', LINK_BRACKET_REPLACEMENT)); // segments[1]

		if (refTypeName != null) {
			ssp.append(LINK_SEPARATOR);
			ssp.append(refTypeName); // segments[2]

			if (refMemberName != null) {
				ssp.append(LINK_SEPARATOR);
				ssp.append(refMemberName); // segments[3]

				if (refParameterTypes != null) {
					ssp.append(LINK_SEPARATOR);
					for (int i= 0; i < refParameterTypes.length; i++) {
						ssp.append(refParameterTypes[i]); // segments[4|5|..]
						if (i != refParameterTypes.length - 1) {
							ssp.append(LINK_SEPARATOR);
						}
					}
				}
			}
		}
		return new URI(scheme, ssp.toString(), null);
	}

	public static IJavaElement parseURI(URI uri) {
		String ssp= uri.getSchemeSpecificPart();
		String[] segments= ssp.split(String.valueOf(LINK_SEPARATOR), -1);
		String refModuleName= null;
		// replace '[' manually, since URI confuses it for an IPv6 address as per RFC 2732:
		IJavaElement element= JavaCore.create(segments[1].replace(LINK_BRACKET_REPLACEMENT, '['));
		boolean canReferModuleName= canReferModuleName(element);
		if (segments.length > 2) {
			String refTypeName= segments[2];
			int index= refTypeName.indexOf('/');
			if (index != -1 && canReferModuleName) {
				refModuleName= refTypeName.substring(0, index);
				refTypeName= refTypeName.substring(index+1);
			}
			if ((refTypeName== null || refTypeName.isEmpty()) &&
					(refModuleName != null && !refModuleName.isEmpty())) {
				return getModule(element, refModuleName);
			}
			if (refTypeName.indexOf('/') == -1) {
				if (refTypeName.indexOf('.') == -1) {
					try {
						ITypeParameter resolvedTypeVariable= resolveTypeVariable(element, refTypeName);
						if (resolvedTypeVariable != null)
							return resolvedTypeVariable;
					} catch (JavaModelException e) {
						JavaManipulationPlugin.log(e);
					}
				}
			}
			if (element instanceof IAnnotation) {
				element= element.getParent();
			}
			if (element instanceof IModuleDescription) {
				element = element.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
				// continue below in branch IPackageFragment...
			}
			if (element instanceof ILocalVariable) {
				element= ((ILocalVariable) element).getDeclaringMember();
			} else if (element instanceof ITypeParameter) {
				element= ((ITypeParameter) element).getDeclaringMember();
			}
			if (element instanceof IMember && !(element instanceof IType)) {
				element= ((IMember) element).getDeclaringType();
			}

			if (element instanceof IPackageFragment) {
				try {
					IPackageFragment root= (IPackageFragment) element;
					element= resolvePackageInfoType(root, refTypeName);
					if (element == null) {
						// find it as package
						IJavaProject javaProject= root.getJavaProject();
						return JavaModelUtil.findTypeContainer(javaProject, refTypeName);
					}
				} catch (JavaModelException e) {
					JavaManipulationPlugin.log(e);
				}
			}

			if (element instanceof IType) {
				try {
					IType type= (IType) element;
					if (refTypeName.length() > 0) {
						type= resolveType(type, refTypeName);
						if (type == null) {
							IPackageFragment pack= JavaModelUtil.getPackageFragmentRoot(element).getPackageFragment(refTypeName);
							if (pack.exists())
								return pack;
						}
					}
					if (type != null) {
						element= type;
						if (segments.length > 3) {
							String refMemberName= segments[3];
							if (segments.length > 4) {
								String[] paramSignatures= new String[segments[4].length() == 0 ? 0 : segments.length - 4];
								for (int i= 0; i < paramSignatures.length; i++) {
									paramSignatures[i]= Signature.createTypeSignature(segments[i + 4], false);
								}
								IMethod method= type.getMethod(refMemberName, paramSignatures);
								IMethod[] methods= type.findMethods(method);
								if (methods != null) {
									return methods[0];
								} else {
									//TODO: methods whose signature contains type parameters can not be found
									// easily, since the Javadoc references are erasures

									//Shortcut: only check name and parameter count:
									for (IMethod method1 : type.getMethods()) {
										method= method1;
										if (method.getElementName().equals(refMemberName) && method.getNumberOfParameters() == paramSignatures.length)
											return method;
									}

									// reference can also point to method from supertype:
									ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(type);
									method= JavaModelUtil.findMethodInHierarchy(hierarchy, type, refMemberName, paramSignatures, false);
									if (method != null)
										return method;
								}
							} else {
								IField field= type.getField(refMemberName);
								if (field.exists()) {
									return field;
								} else {
									for (IMethod method : type.getMethods()) {
										if (method.getElementName().equals(refMemberName))
											return method;
									}
								}
							}

						}
					} else {
						// FIXME: either remove or show dialog
//						JavaManipulationPlugin.logErrorMessage("JavaElementLinks could not resolve " + uri); //$NON-NLS-1$
					}
					if (type == null &&
							refModuleName == null &&
							canReferModuleName &&
							segments.length <= 3) {
						return getModule(element, refTypeName);
					}
					return type;
				} catch (JavaModelException e) {
					JavaManipulationPlugin.log(e);
				}
			}
		}
		return element;
	}


	private static IJavaElement getModule(IJavaElement element, String moduleName) {
		if (element == null || moduleName == null) {
			return null;
		}
		IJavaProject javaProject= element.getJavaProject();
		try {
			return javaProject.findModule(moduleName, null);
		} catch (JavaModelException e1) {
			// do nothing
		}
		return null;
	}

	private static boolean canReferModuleName(IJavaElement element) {
		boolean canRefer= false;
		if (element != null) {
			IJavaProject javaProject= element.getJavaProject();
			if (javaProject != null) {
				canRefer = JavaModelUtil.is15OrHigher(javaProject);
			}
		}
		return canRefer;
	}

	private static IType resolvePackageInfoType(IPackageFragment pack, String refTypeName) throws JavaModelException {
		// Note: The scoping rules of JLS7 6.3 are broken for package-info.java, see https://bugs.eclipse.org/216451#c4
		// We follow the javadoc tool's implementation and only support fully-qualified type references:
		IJavaProject javaProject= pack.getJavaProject();
		return javaProject.findType(refTypeName, (IProgressMonitor) null);

		// This implementation would make sense, but the javadoc tool doesn't support it:
//		IClassFile classFile= pack.getClassFile(JavaModelUtil.PACKAGE_INFO_CLASS);
//		if (classFile.exists()) {
//			return resolveType(classFile.getType(), refTypeName);
//		}
//
//		// check if refTypeName is a qualified name
//		int firstDot= refTypeName.indexOf('.');
//		if (firstDot != -1) {
//			String typeNameRest= refTypeName.substring(firstDot + 1);
//			String simpleTypeName= refTypeName.substring(0, firstDot);
//			IType simpleType= resolvePackageInfoType(pack, simpleTypeName);
//			if (simpleType != null) {
//				// a type-qualified name
//				return resolveType(simpleType, typeNameRest);
//			} else {
//				// a fully-qualified name
//				return javaProject.findType(refTypeName, (IProgressMonitor) null);
//			}
//		}
//
//		ICompilationUnit cu= pack.getCompilationUnit(JavaModelUtil.PACKAGE_INFO_JAVA);
//		if (! cu.exists()) {
//			// refTypeName is a simple name in the package-info.java from the source attachment. Sorry, we give up here...
//			return null;
//		}
//
//		// refTypeName is a simple name in a CU. Let's play the shadowing rules of JLS7 6.4.1:
//		// 1) single-type import
//		// 2) enclosing package
//		// 3) java.lang.* (JLS7 7.3)
//		// 4) on-demand import
//		IImportDeclaration[] imports= cu.getImports();
//		for (int i= 0; i < imports.length; i++) {
//			IImportDeclaration importDecl= imports[i];
//			String name= importDecl.getElementName();
//			if (Flags.isStatic(importDecl.getFlags())) {
//				imports[i]= null;
//			} else 	if (! importDecl.isOnDemand()) {
//				if (name.endsWith('.' + refTypeName)) {
//					// 1) single-type import
//					IType type= javaProject.findType(name, (IProgressMonitor) null);
//					if (type != null)
//						return type;
//				}
//				imports[i]= null;
//			}
//		}
//
//		// 2) enclosing package
//		IType type= javaProject.findType(pack.getElementName() + '.' + refTypeName, (IProgressMonitor) null);
//		if (type != null)
//			return type;
//
//		// 3) java.lang.* (JLS7 7.3)
//		type= javaProject.findType("java.lang." + refTypeName, (IProgressMonitor) null); //$NON-NLS-1$
//		if (type != null)
//			return type;
//
//		// 4) on-demand import
//		for (int i= 0; i < imports.length; i++) {
//			IImportDeclaration importDecl= imports[i];
//			if (importDecl != null) {
//				String name= importDecl.getElementName();
//				name= name.substring(0, name.length() - 1); //remove the *
//				type= javaProject.findType(name + refTypeName, (IProgressMonitor) null);
//				if (type != null)
//					return type;
//			}
//		}
//		return null;
	}

	private static ITypeParameter resolveTypeVariable(IJavaElement baseElement, String typeVariableName) throws JavaModelException {
		while (baseElement != null) {
			switch (baseElement.getElementType()) {
				case IJavaElement.METHOD:
					for (ITypeParameter typeParameter : ((IMethod)baseElement).getTypeParameters()) {
						if (typeParameter.getElementName().equals(typeVariableName)) {
							return typeParameter;
						}
					}
					break;


				case IJavaElement.TYPE:
					for (ITypeParameter typeParameter : ((IType)baseElement).getTypeParameters()) {
						if (typeParameter.getElementName().equals(typeVariableName)) {
							return typeParameter;
						}
					}
					break;


				case IJavaElement.JAVA_MODEL:
				case IJavaElement.JAVA_PROJECT:
				case IJavaElement.PACKAGE_FRAGMENT:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:

				case IJavaElement.CLASS_FILE:
				case IJavaElement.COMPILATION_UNIT:

				case IJavaElement.PACKAGE_DECLARATION:
				case IJavaElement.IMPORT_CONTAINER:
				case IJavaElement.IMPORT_DECLARATION:
					return null;

				default:
					break;
			}
			// look for type parameters in enclosing members:
			baseElement= baseElement.getParent();
		}
		return null;
	}

	private static IType resolveType(IType baseType, String refTypeName) throws JavaModelException {
		if (refTypeName.length() == 0)
			return baseType;

		String[][] resolvedNames= baseType.resolveType(refTypeName);
		if (resolvedNames != null && resolvedNames.length > 0) {
			return baseType.getJavaProject().findType(resolvedNames[0][0], resolvedNames[0][1].replace('$', '.'), (IProgressMonitor) null);

		} else if (baseType.isBinary()) {
			// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=206597
			IType type= baseType.getJavaProject().findType(refTypeName, (IProgressMonitor) null);
			if (type == null) {
				// could be unqualified reference:
				type= baseType.getJavaProject().findType(baseType.getPackageFragment().getElementName() + '.' + refTypeName, (IProgressMonitor) null);
			}
			return type;

		} else {
			return null;
		}
	}

	/**
	 * Creates a link with the given URI and label text.
	 *
	 * @param uri the URI
	 * @param label the label
	 * @return the HTML link
	 * @since 1.21
	 */
	public static String createLink(String uri, String label) {
		return "<a href='" + uri + "'>" + label + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Creates a header link with the given URI and label text.
	 *
	 * @param uri the URI
	 * @param label the label
	 * @return the HTML link
	 * @since 1.21
	 */
	public static String createHeaderLink(String uri, String label) {
		return createHeaderLink(uri, label, ""); //$NON-NLS-1$
	}

	/**
	 * Creates a link with the given URI, label and title text.
	 *
	 * @param uri the URI
	 * @param label the label
	 * @param title the title to be displayed while hovering over the link (can be empty)
	 * @return the HTML link
	 * @since 1.21
	 */
	public static String createHeaderLink(String uri, String label, String title) {
		if (title.length() > 0) {
			title= " title='" + title + "'";  //$NON-NLS-1$ //$NON-NLS-2$
		}
		return "<a class='header' href='" + uri + "'" + title + ">" + label + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}
