/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
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
 *     Tom Hofmann, Google <eclipse@tom.eicher.name> - [hovering] NPE when hovering over @value reference within a type's javadoc - https://bugs.eclipse.org/bugs/show_bug.cgi?id=320084
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation.internal.javadoc;

import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Javadoc;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;


/**
 * Helper to get the content of a Javadoc comment as HTML.
 *
 * This class has been heavily copied and modified from JavadocContentAccess2 in jdt.ui to make it
 * an instance class instead of static methods only. This will allow bleeding-edge consumers with no
 * access to UI to subclass and extend at their own risk with no promises of API compatibility.
 */
public class CoreJavadocAccess {

	protected static final String BASE_URL_COMMENT_INTRO= "<!-- baseURL=\""; //$NON-NLS-1$

	protected IJavadocContentFactory fFactory;

	public CoreJavadocAccess() {
		this(JavadocLookup.DEFAULT_FACTORY);
	}

	public CoreJavadocAccess(IJavadocContentFactory factory) {
		this.fFactory= factory;
	}

	/**
	 * @param content HTML content produced by <code>getHTMLContent(...)</code>
	 * @return the baseURL to use for the given content, or <code>null</code> if none
	 */
	public String extractBaseURL(String content) {
		int introStart= content.indexOf(BASE_URL_COMMENT_INTRO);
		if (introStart != -1) {
			int introLength= BASE_URL_COMMENT_INTRO.length();
			int endIndex= content.indexOf('"', introStart + introLength);
			if (endIndex != -1) {
				return content.substring(introStart + introLength, endIndex);
			}
		}
		return null;
	}

	/**
	 * Returns the Javadoc for a PackageDeclaration.
	 *
	 * @param packageDeclaration the Java element whose Javadoc has to be retrieved
	 * @return the package documentation in HTML format or <code>null</code> if there is no
	 *         associated Javadoc
	 * @throws CoreException if the Java element does not exists or an exception occurs while
	 *             accessing the file containing the package Javadoc
	 */
	public String getHTMLContent(IPackageDeclaration packageDeclaration) throws CoreException {
		IJavaElement element= packageDeclaration.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
		if (element instanceof IPackageFragment) {
			return getHTMLContent((IPackageFragment) element);
		}
		return null;
	}


	/**
	 * Returns the Javadoc for a package which could be present in package.html, package-info.java
	 * or from an attached Javadoc.
	 *
	 * @param packageFragment the package which is requesting for the document
	 * @return the document content in HTML format or <code>null</code> if there is no associated
	 *         Javadoc
	 * @throws CoreException if the Java element does not exists or an exception occurs while
	 *             accessing the file containing the package Javadoc
	 */
	public String getHTMLContent(IPackageFragment packageFragment) throws CoreException {
		return readHTMLContent(packageFragment);
	}

	protected String readHTMLContent(IPackageFragment packageFragment) throws CoreException {
		IPackageFragmentRoot root= (IPackageFragmentRoot) packageFragment.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);

		//1==> Handle the case when the documentation is present in package-info.java or package-info.class file
		ITypeRoot packageInfo;
		boolean isBinary= root.getKind() == IPackageFragmentRoot.K_BINARY;
		if (isBinary) {
			packageInfo= packageFragment.getClassFile(JavaModelUtil.PACKAGE_INFO_CLASS);
		} else {
			packageInfo= packageFragment.getCompilationUnit(JavaModelUtil.PACKAGE_INFO_JAVA);
		}
		if (packageInfo != null && packageInfo.exists()) {
			String cuSource= packageInfo.getSource();
			//the source can be null for some of the class files
			if (cuSource != null) {
				Javadoc packageJavadocNode= CoreJavadocContentAccessUtility.getPackageJavadocNode(packageFragment, cuSource);
				if (packageJavadocNode != null) {
					IJavaElement element;
					if (isBinary) {
						element= ((IOrdinaryClassFile) packageInfo).getType();
					} else {
						element= packageInfo.getParent(); // parent is the IPackageFragment
					}
					return this.fFactory.createJavadocAccess(element, packageJavadocNode, cuSource, null).toHTML();
				}
			}
		}

		// 2==> Handle the case when the documentation is done in package.html file. The file can be either in normal source folder or coming from a jar file
		else {
			Object[] nonJavaResources= packageFragment.getNonJavaResources();
			// 2.1 ==>If the package.html file is present in the source or directly in the binary jar
			for (Object nonJavaResource : nonJavaResources) {
				if (nonJavaResource instanceof IFile) {
					IFile iFile= (IFile) nonJavaResource;
					if (iFile.exists() && JavaModelUtil.PACKAGE_HTML.equals(iFile.getName())) {
						return CoreJavadocContentAccessUtility.getIFileContent(iFile);
					}
				}
			}

			// 2.2==>The file is present in a binary container
			if (isBinary) {
				for (Object nonJavaResource : nonJavaResources) {
					// The content is from an external binary class folder
					if (nonJavaResource instanceof IJarEntryResource) {
						IJarEntryResource jarEntryResource= (IJarEntryResource) nonJavaResource;
						String encoding= CoreJavadocContentAccessUtility.getSourceAttachmentEncoding(root);
						if (JavaModelUtil.PACKAGE_HTML.equals(jarEntryResource.getName()) && jarEntryResource.isFile()) {
							return CoreJavadocContentAccessUtility.getHTMLContent(jarEntryResource, encoding);
						}
					}
				}
				//2.3 ==>The file is present in the source attachment path.
				String contents= getHTMLContentFromAttachedSource(root, packageFragment);
				if (contents != null)
					return contents;
			}
		}

		//3==> Handle the case when the documentation is coming from the attached Javadoc
		if ((root.isArchive() || root.isExternal())) {
			return packageFragment.getAttachedJavadoc(null);

		}
		return ""; //$NON-NLS-1$
	}


	protected String getHTMLContentFromAttachedSource(IPackageFragmentRoot root, IPackageFragment packageFragment) throws CoreException {
		String filePath= packageFragment.getElementName().replace('.', '/') + '/' + JavaModelUtil.PACKAGE_INFO_JAVA;
		String contents= CoreJavadocContentAccessUtility.getFileContentFromAttachedSource(root, filePath);
		if (contents != null) {
			Javadoc packageJavadocNode= CoreJavadocContentAccessUtility.getPackageJavadocNode(packageFragment, contents);
			if (packageJavadocNode != null)
				return this.fFactory.createJavadocAccess(packageFragment, packageJavadocNode, contents, null).toHTML();

		}
		filePath= packageFragment.getElementName().replace('.', '/') + '/' + JavaModelUtil.PACKAGE_HTML;
		return CoreJavadocContentAccessUtility.getFileContentFromAttachedSource(root, filePath);
	}


	/**
	 * Gets an IJavaElement's Javadoc comment content from the source or Javadoc attachment and
	 * renders the tags and links in HTML. Returns <code>null</code> if the element does not have a
	 * Javadoc comment or if no source is available.
	 *
	 * @param element the element to get the Javadoc of
	 * @param useAttachedJavadoc if <code>true</code> Javadoc will be extracted from attached
	 *            Javadoc if there's no source
	 * @return the Javadoc comment content in HTML or <code>null</code> if the element does not have
	 *         a Javadoc comment or if no source is available
	 * @throws CoreException is thrown when the element's Javadoc cannot be accessed
	 */
	public String getHTMLContent(IJavaElement element, boolean useAttachedJavadoc) throws CoreException {
		if (element instanceof IPackageFragment) {
			return getHTMLContent((IPackageFragment) element);
		}
		if (element instanceof IPackageDeclaration) {
			return getHTMLContent((IPackageDeclaration) element);
		}
		if (!(element instanceof IMember)
				&& !(element instanceof ITypeParameter)
				&& (!(element instanceof ILocalVariable) || !(((ILocalVariable) element).isParameter()))) {
			return null;
		}
		String sourceJavadoc= getHTMLContentFromSource(element);
		if (sourceJavadoc == null || sourceJavadoc.length() == 0 || "{@inheritDoc}".equals(sourceJavadoc.trim())) { //$NON-NLS-1$
			if (useAttachedJavadoc) {
				if (element.getOpenable().getBuffer() == null) { // only if no source available
					try {
						return element.getAttachedJavadoc(null);
					} catch (Exception e) {
						JavaManipulationPlugin.log(e);
						return null;
					}
				}
				IMember member= null;
				if (element instanceof ILocalVariable) {
					member= ((ILocalVariable) element).getDeclaringMember();
				} else if (element instanceof ITypeParameter) {
					member= ((ITypeParameter) element).getDeclaringMember();
				} else if (element instanceof IMember) {
					member= (IMember) element;
				}
				if (CoreJavadocContentAccessUtility.canInheritJavadoc(member)) {
					IMethod method= (IMethod) member;
					String attachedDocInHierarchy= findAttachedDocInHierarchy(method);

					// Prepend "Overrides:" / "Specified by:" reference headers to make clear
					// that description has been copied from super method.
					if (attachedDocInHierarchy == null)
						return sourceJavadoc;
					StringBuffer superMethodReferences= createSuperMethodReferences(method);
					if (superMethodReferences == null)
						return attachedDocInHierarchy;
					superMethodReferences.append(attachedDocInHierarchy);
					return superMethodReferences.toString();
				}
			}
		}
		return sourceJavadoc;
	}

	record SuperclassReferenceMethodData(ArrayList<IMethod> superInterfaceMethods, IMethod superClassMethod) {
	}

	static SuperclassReferenceMethodData getSuperclassReferenceMethodData(final IMethod method) throws JavaModelException {
		IType type= method.getDeclaringType();
		ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(type);
		final MethodOverrideTester tester= SuperTypeHierarchyCache.getMethodOverrideTester(type);

		final ArrayList<IMethod> superInterfaceMethods= new ArrayList<>();
		final IMethod[] superClassMethod= { null };
		new InheritDocVisitor() {
			@Override
			public Object visit(IType currType) throws JavaModelException {
				IMethod overridden= tester.findOverriddenMethodInType(currType, method);
				if (overridden == null)
					return InheritDocVisitor.CONTINUE;

				if (currType.isInterface())
					superInterfaceMethods.add(overridden);
				else
					superClassMethod[0]= overridden;

				return STOP_BRANCH;
			}
		}.visitInheritDoc(type, hierarchy);

		boolean hasSuperInterfaceMethods= !superInterfaceMethods.isEmpty();
		if (!hasSuperInterfaceMethods && superClassMethod[0] == null)
			return null;
		return new SuperclassReferenceMethodData(superInterfaceMethods, superClassMethod[0]);
	}

	protected StringBuffer createSuperMethodReferences(final IMethod method) throws JavaModelException {
		SuperclassReferenceMethodData data= getSuperclassReferenceMethodData(method);
		if (data == null)
			return null;
		return createSuperMethodReferencesHTML(data.superInterfaceMethods(), data.superClassMethod());
	}

	protected StringBuffer createSuperMethodReferencesHTML(ArrayList<IMethod> superInterfaceMethods, IMethod superClassMethod) {
		return createSuperMethodReferencesHTMLStaticImpl(superInterfaceMethods, superClassMethod);
	}

	static StringBuffer createSuperMethodReferencesHTMLStaticImpl(ArrayList<IMethod> superInterfaceMethods, IMethod superClassMethod){
		boolean hasSuperInterfaceMethods= !superInterfaceMethods.isEmpty();
		StringBuffer buf= new StringBuffer();
		buf.append("<div>"); //$NON-NLS-1$
		if (hasSuperInterfaceMethods) {
			buf.append("<b>"); //$NON-NLS-1$
			buf.append(JavaDocMessages.JavaDoc2HTMLTextReader_specified_by_section);
			buf.append("</b> "); //$NON-NLS-1$
			for (Iterator<IMethod> iter= superInterfaceMethods.iterator(); iter.hasNext();) {
				IMethod overridden= iter.next();
				buf.append(CoreJavadocContentAccessUtility.createMethodInTypeLinks(overridden));
				if (iter.hasNext())
					buf.append(JavaElementLabelsCore.COMMA_STRING);
			}
		}
		if (superClassMethod != null) {
			if (hasSuperInterfaceMethods)
				buf.append(JavaElementLabelsCore.COMMA_STRING);
			buf.append("<b>"); //$NON-NLS-1$
			buf.append(JavaDocMessages.JavaDoc2HTMLTextReader_overrides_section);
			buf.append("</b> "); //$NON-NLS-1$
			buf.append(CoreJavadocContentAccessUtility.createMethodInTypeLinks(superClassMethod));
		}
		buf.append("</div>"); //$NON-NLS-1$
		return buf;
	}


	protected String getHTMLContentFromSource(IJavaElement element) throws JavaModelException {
		IMember member;
		if (element instanceof ILocalVariable) {
			member= ((ILocalVariable) element).getDeclaringMember();
		} else if (element instanceof ITypeParameter) {
			member= ((ITypeParameter) element).getDeclaringMember();
		} else if (element instanceof IMember) {
			member= (IMember) element;
		} else {
			return null;
		}

		IBuffer buf= member.getOpenable().getBuffer();
		if (buf == null) {
			return null; // no source attachment found
		}

		ISourceRange javadocRange= member.getJavadocRange();
		if (javadocRange == null) {
			if (CoreJavadocContentAccessUtility.canInheritJavadoc(member)) {
				// Try to use the inheritDoc algorithm.
				String inheritedJavadoc= javadoc2HTML(member, element, "/***/"); //$NON-NLS-1$
				if (inheritedJavadoc != null && inheritedJavadoc.length() > 0) {
					return inheritedJavadoc;
				}
			}
			return getJavaFxPropertyDoc(member);
		}

		String rawJavadoc= buf.getText(javadocRange.getOffset(), javadocRange.getLength());
		return javadoc2HTML(member, element, rawJavadoc);
	}

	protected String formatJavaFxGetterSetterPropertyDoc(String content, String propertyName, boolean isSetter) {
		if (content != null) {
			if (isSetter) {
				content= MessageFormat.format(JavaDocMessages.JavadocContentAccess2_setproperty_message, new Object[] { propertyName, content });
			} else {
				content= MessageFormat.format(JavaDocMessages.JavadocContentAccess2_getproperty_message, new Object[] { propertyName, content });
			}
		}
		return content;
	}

	protected String getJavaFxPropertyDoc(IMember member) throws JavaModelException {
		// XXX: should not do this by default (but we don't have settings for Javadoc, see https://bugs.eclipse.org/424283 )
		if (member instanceof IMethod) {
			String name= member.getElementName();
			boolean isGetter= name.startsWith("get") && name.length() > 3; //$NON-NLS-1$
			boolean isBooleanGetter= name.startsWith("is") && name.length() > 2; //$NON-NLS-1$
			boolean isSetter= name.startsWith("set") && name.length() > 3; //$NON-NLS-1$

			if (isGetter || isBooleanGetter || isSetter) {
				String propertyName= firstToLower(name.substring(isBooleanGetter ? 2 : 3));
				IType type= member.getDeclaringType();
				IMethod method= type.getMethod(propertyName + "Property", new String[0]); //$NON-NLS-1$

				if (method.exists()) {
					String content= getHTMLContentFromSource(method);
					return formatJavaFxGetterSetterPropertyDoc(content, propertyName, isSetter);
				}
			} else if (name.endsWith("Property")) { //$NON-NLS-1$
				String propertyName= name.substring(0, name.length() - 8);

				IType type= member.getDeclaringType();
				IField field= type.getField(propertyName);
				if (field.exists()) {
					return getHTMLContentFromSource(field);
				}
			}
		}
		return null;
	}

	protected String firstToLower(String propertyName) {
		char[] c= propertyName.toCharArray();
		c[0]= Character.toLowerCase(c[0]);
		return String.valueOf(c);
	}

	protected String javadoc2HTML(IMember member, IJavaElement element, String rawJavadoc) {
		Javadoc javadoc= CoreJavadocContentAccessUtility.getJavadocNode(member, rawJavadoc);

		if (javadoc == null) {
			try (Reader contentReader= CoreJavadocContentAccessUtility.getHTMLContentReader(member, false, false)) {
				if (contentReader != null)
					return CoreJavadocContentAccessUtility.getString(contentReader);
			} catch (JavaModelException | IOException e1) {
				JavaManipulationPlugin.log(e1);
			}
			return null;
		}

		if (CoreJavadocContentAccessUtility.canInheritJavadoc(member)) {
			IMethod method= (IMethod) member;
			return this.fFactory.createJavadocAccess(element, javadoc, rawJavadoc, new JavadocLookup(method.getDeclaringType(), this.fFactory)).toHTML();
		}
		return this.fFactory.createJavadocAccess(element, javadoc, rawJavadoc, null).toHTML();
	}

	/**
	 * Finds the first available attached Javadoc in the hierarchy of the given method.
	 *
	 * @param method the method
	 * @return the inherited Javadoc from the Javadoc attachment, or <code>null</code> if none
	 * @throws JavaModelException unexpected problem
	 */
	protected String findAttachedDocInHierarchy(final IMethod method) throws JavaModelException {
		IType type= method.getDeclaringType();
		ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(type);
		final MethodOverrideTester tester= SuperTypeHierarchyCache.getMethodOverrideTester(type);

		return (String) new InheritDocVisitor() {
			@Override
			public Object visit(IType currType) throws JavaModelException {
				IMethod overridden= tester.findOverriddenMethodInType(currType, method);
				if (overridden == null)
					return InheritDocVisitor.CONTINUE;

				if (overridden.getOpenable().getBuffer() == null) { // only if no source available
					String attachedJavadoc= overridden.getAttachedJavadoc(null);
					if (attachedJavadoc != null) {
						// BaseURL for the original method can be wrong for attached Javadoc from overridden
						// (e.g. when overridden is from rt.jar).
						// Fix is to store the baseURL inside the doc content and later fetch it with #extractBaseURL(String).
						String baseURL= CoreJavaDocLocations.getBaseURL(overridden, overridden.isBinary(), null);
						if (baseURL != null) {
							attachedJavadoc= BASE_URL_COMMENT_INTRO + baseURL + "\"--> " + attachedJavadoc; //$NON-NLS-1$
						}
						return attachedJavadoc;
					}
				}
				return CONTINUE;
			}
		}.visitInheritDoc(type, hierarchy);
	}

}
