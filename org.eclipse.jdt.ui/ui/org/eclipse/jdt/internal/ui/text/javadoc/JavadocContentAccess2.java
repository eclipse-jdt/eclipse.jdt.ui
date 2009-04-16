/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.javadoc;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.JavadocContentAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;


/**
 * Helper to get the content of a Javadoc comment as HTML.
 *
 * <p>
 * <strong>This is work in progress. Parts of this will later become
 * API through {@link JavadocContentAccess}</strong>
 * </p>
 *
 * @since 3.4
 */
public class JavadocContentAccess2 {

	private static final String BLOCK_TAG_START= "<dl>"; //$NON-NLS-1$
	private static final String BLOCK_TAG_END= "</dl>"; //$NON-NLS-1$

	private static final String BlOCK_TAG_ENTRY_START= "<dd>"; //$NON-NLS-1$
	private static final String BlOCK_TAG_ENTRY_END= "</dd>"; //$NON-NLS-1$

	private static final String PARAM_NAME_START= "<b>"; //$NON-NLS-1$
	private static final String PARAM_NAME_END= "</b> "; //$NON-NLS-1$

	/**
	 * Implements the "Algorithm for Inheriting Method Comments" as specified for
	 * <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/solaris/javadoc.html#inheritingcomments">1.4.2</a>,
	 * <a href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/windows/javadoc.html#inheritingcomments">1.5</a>, and
	 * <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/javadoc.html#inheritingcomments">1.6</a>.
	 *
	 * <p>
	 * Unfortunately, the implementation is broken in Javadoc implementations since 1.5, see
	 * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6376959">Sun's bug</a>.
	 * </p>
	 *
	 * <p>
	 * We adhere to the spec.
	 * </p>
	 */
	private static abstract class InheritDocVisitor {
		public static final Object STOP_BRANCH= new Object() {
			public String toString() { return "STOP_BRANCH"; } //$NON-NLS-1$
		};
		public static final Object CONTINUE= new Object() {
			public String toString() { return "CONTINUE"; } //$NON-NLS-1$
		};

		/**
		 * Visits a type and decides how the visitor should proceed.
		 *
		 * @param currType the current type
		 * @return <ul>
		 *         <li>{@link #STOP_BRANCH} to indicate that no Javadoc has been found and visiting
		 *         super types should stop here</li>
		 *         <li>{@link #CONTINUE} to indicate that no Javadoc has been found and visiting
		 *         super types should continue</li>
		 *         <li>an {@link Object} or <code>null</code>, to indicate that visiting should be
		 *         cancelled immediately. The returned value is the result of
		 *         {@link #visitInheritDoc(IType, ITypeHierarchy)}</li>
		 *         </ul>
		 * @throws JavaModelException unexpected problem
		 * @see #visitInheritDoc(IType, ITypeHierarchy)
		 */
		public abstract Object visit(IType currType) throws JavaModelException;

		/**
		 * Visits the super types of the given <code>currentType</code>.
		 *
		 * @param currentType the starting type
		 * @param typeHierarchy a super type hierarchy that contains <code>currentType</code>
		 * @return the result from a call to {@link #visit(IType)}, or <code>null</code> if none of
		 *         the calls returned a result
		 * @throws JavaModelException unexpected problem
		 */
		public Object visitInheritDoc(IType currentType, ITypeHierarchy typeHierarchy) throws JavaModelException {
			ArrayList visited= new ArrayList();
			visited.add(currentType);
			Object result= visitInheritDocInterfaces(visited, currentType, typeHierarchy);
			if (result != InheritDocVisitor.CONTINUE)
				return result;

			IType superClass;
			if (currentType.isInterface())
				superClass= currentType.getJavaProject().findType("java.lang.Object"); //$NON-NLS-1$
			else
				superClass= typeHierarchy.getSuperclass(currentType);

			while (superClass != null && ! visited.contains(superClass)) {
				result= visit(superClass);
				if (result == InheritDocVisitor.STOP_BRANCH) {
					return null;
				} else if (result == InheritDocVisitor.CONTINUE) {
					visited.add(superClass);
					result= visitInheritDocInterfaces(visited, superClass, typeHierarchy);
					if (result != InheritDocVisitor.CONTINUE)
						return result;
					else
						superClass= typeHierarchy.getSuperclass(superClass);
				} else {
					return result;
				}
			}

			return null;
		}

		/**
		 * Visits the super interfaces of the given type in the given hierarchy, thereby skipping already visited types.
		 * 
		 * @param visited set of visited types
		 * @param currentType type whose super interfaces should be visited
		 * @param typeHierarchy type hierarchy (must include <code>currentType</code>)
		 * @return the result, or {@link #CONTINUE} if no result has been found
		 * @throws JavaModelException unexpected problem
		 */
		private Object visitInheritDocInterfaces(ArrayList visited, IType currentType, ITypeHierarchy typeHierarchy) throws JavaModelException {
			ArrayList toVisitChildren= new ArrayList();
			IType[] superInterfaces= typeHierarchy.getSuperInterfaces(currentType);
			for (int i= 0; i < superInterfaces.length; i++) {
				IType superInterface= superInterfaces[i];
				if (visited.contains(superInterface))
					continue;
				visited.add(superInterface);
				Object result= visit(superInterface);
				if (result == InheritDocVisitor.STOP_BRANCH) {
					//skip
				} else if (result == InheritDocVisitor.CONTINUE) {
					toVisitChildren.add(superInterface);
				} else {
					return result;
				}
			}
			for (Iterator iter= toVisitChildren.iterator(); iter.hasNext(); ) {
				IType child= (IType) iter.next();
				Object result= visitInheritDocInterfaces(visited, child, typeHierarchy);
				if (result != InheritDocVisitor.CONTINUE)
					return result;
			}
			return InheritDocVisitor.CONTINUE;
		}
	}

	private static class JavadocLookup {
		private static final JavadocLookup NONE= new JavadocLookup(null) {
			public CharSequence getInheritedMainDescription(IMethod method) {
				return null;
			}
			public CharSequence getInheritedParamDescription(IMethod method, int i) {
				return null;
			}
			public CharSequence getInheritedReturnDescription(IMethod method) {
				return null;
			}
			public CharSequence getInheritedExceptionDescription(IMethod method, String name) {
				return null;
			}
		};

		private static interface DescriptionGetter {
			/**
			 * Returns a Javadoc tag description or <code>null</code>.
			 * 
			 * @param contentAccess the content access
			 * @return the description, or <code>null</code> if none
			 * @throws JavaModelException unexpected problem
			 */
			CharSequence getDescription(JavadocContentAccess2 contentAccess) throws JavaModelException;
		}

		private final IType fStartingType;
		private final HashMap fContentAccesses;

		private ITypeHierarchy fTypeHierarchy;
		private MethodOverrideTester fOverrideTester;


		private JavadocLookup(IType startingType) {
			fStartingType= startingType;
			fContentAccesses= new HashMap();
		}

		/**
		 * For the given method, returns the main description from an overridden method.
		 *
		 * @param method a method
		 * @return the description that replaces the <code>{&#64;inheritDoc}</code> tag,
		 * 		or <code>null</code> if none could be found
		 */
		public CharSequence getInheritedMainDescription(IMethod method) {
			return getInheritedDescription(method, new DescriptionGetter() {
				public CharSequence getDescription(JavadocContentAccess2 contentAccess) {
					return contentAccess.getMainDescription();
				}
			});
		}

		/**
		 * For the given method, returns the @param tag description for the given parameter
		 * from an overridden method.
		 *
		 * @param method a method
		 * @param paramIndex the index of the parameter
		 * @return the description that replaces the <code>{&#64;inheritDoc}</code> tag,
		 * 		or <code>null</code> if none could be found
		 */
		public CharSequence getInheritedParamDescription(IMethod method, final int paramIndex) {
			return getInheritedDescription(method, new DescriptionGetter() {
				public CharSequence getDescription(JavadocContentAccess2 contentAccess) throws JavaModelException {
					return contentAccess.getInheritedParamDescription(paramIndex);
				}
			});
		}

		/**
		 * For the given method, returns the @return tag description from an overridden method.
		 *
		 * @param method a method
		 * @return the description that replaces the <code>{&#64;inheritDoc}</code> tag,
		 * 		or <code>null</code> if none could be found
		 */
		public CharSequence getInheritedReturnDescription(IMethod method) {
			return getInheritedDescription(method, new DescriptionGetter() {
				public CharSequence getDescription(JavadocContentAccess2 contentAccess) {
					return contentAccess.getReturnDescription();
				}
			});
		}

		/**
		 * For the given method, returns the @throws/@exception tag description for the given
		 * exception from an overridden method.
		 *
		 * @param method a method
		 * @param simpleName the simple name of an exception
		 * @return the description that replaces the <code>{&#64;inheritDoc}</code> tag,
		 * 		or <code>null</code> if none could be found
		 */
		public CharSequence getInheritedExceptionDescription(IMethod method, final String simpleName) {
			return getInheritedDescription(method, new DescriptionGetter() {
				public CharSequence getDescription(JavadocContentAccess2 contentAccess) {
					return contentAccess.getExceptionDescription(simpleName);
				}
			});
		}

		private CharSequence getInheritedDescription(final IMethod method, final DescriptionGetter descriptionGetter) {
			try {
				return (CharSequence) new InheritDocVisitor() {
					public Object visit(IType currType) throws JavaModelException {
						IMethod overridden= getOverrideTester().findOverriddenMethodInType(currType, method);
						if (overridden == null)
							return InheritDocVisitor.CONTINUE;

						JavadocContentAccess2 contentAccess= getJavadocContentAccess(overridden);
						if (contentAccess == null) {
							if (overridden.getOpenable().getBuffer() == null) {
								// Don't continue this branch when no source is available.
								// We don't extract individual tags from Javadoc attachments,
								// and it would be wrong to copy doc from further up the branch,
								// thereby skipping doc from this overridden method.
								return InheritDocVisitor.STOP_BRANCH;
							} else {
								return InheritDocVisitor.CONTINUE;
							}
						}

						CharSequence overriddenDescription= descriptionGetter.getDescription(contentAccess);
						if (overriddenDescription != null)
							return overriddenDescription;
						else
							return InheritDocVisitor.CONTINUE;
					}
				}.visitInheritDoc(method.getDeclaringType(), getTypeHierarchy());
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			return null;
		}

		/**
		 * @param method the method
		 * @return the Javadoc content access for the given method, or
		 * 		<code>null</code> if no Javadoc could be found in source
		 * @throws JavaModelException unexpected problem
		 */
		private JavadocContentAccess2 getJavadocContentAccess(IMethod method) throws JavaModelException {
			Object cached= fContentAccesses.get(method);
			if (cached != null)
				return (JavadocContentAccess2) cached;
			if (fContentAccesses.containsKey(method))
				return null;

			IBuffer buf= method.getOpenable().getBuffer();
			if (buf == null) { // no source attachment found
				fContentAccesses.put(method, null);
				return null;
			}

			ISourceRange javadocRange= method.getJavadocRange();
			if (javadocRange == null) {
				fContentAccesses.put(method, null);
				return null;
			}

			String rawJavadoc= buf.getText(javadocRange.getOffset(), javadocRange.getLength());
			Javadoc javadoc= getJavadocNode(method, rawJavadoc);
			if (javadoc == null) {
				fContentAccesses.put(method, null);
				return null;
			}

			JavadocContentAccess2 contentAccess= new JavadocContentAccess2(method, javadoc, rawJavadoc, this);
			fContentAccesses.put(method, contentAccess);
			return contentAccess;
		}

		private ITypeHierarchy getTypeHierarchy() throws JavaModelException {
			if (fTypeHierarchy == null)
				fTypeHierarchy= SuperTypeHierarchyCache.getTypeHierarchy(fStartingType);
			return fTypeHierarchy;
		}

		private MethodOverrideTester getOverrideTester() throws JavaModelException {
			if (fOverrideTester == null)
				fOverrideTester= SuperTypeHierarchyCache.getMethodOverrideTester(fStartingType);
			return fOverrideTester;
		}
	}

	private final IMember fMember;
	/**
	 * The method, or <code>null</code> if {@link #fMember} is not a method where {@inheritDoc} could work.
	 */
	private final IMethod fMethod;
	private final Javadoc fJavadoc;
	private final String fSource;
	private final JavadocLookup fJavadocLookup;

	private StringBuffer fBuf;
	private int fLiteralContent;
	private StringBuffer fMainDescription;
	private StringBuffer fReturnDescription;
	private StringBuffer[] fParamDescriptions;
	private HashMap/*<String, StringBuffer>*/ fExceptionDescriptions;

	private JavadocContentAccess2(IMethod method, Javadoc javadoc, String source, JavadocLookup lookup) {
		fMember= method;
		fMethod= method;
		fJavadoc= javadoc;
		fSource= source;
		fJavadocLookup= lookup;
	}

	private JavadocContentAccess2(IMember member, Javadoc javadoc, String source) {
		fMember= member;
		fMethod= null;
		fJavadoc= javadoc;
		fSource= source;
		fJavadocLookup= JavadocLookup.NONE;
	}

	/**
	 * Gets an IMember's Javadoc comment content from the source or Javadoc attachment
	 * and renders the tags and links in HTML.
	 * Returns <code>null</code> if the member does not contain a Javadoc comment or if no source is available.
	 *
	 * @param member				the member to get the Javadoc of
	 * @param allowInherited		for methods with no (Javadoc) comment, the comment of the overridden
	 * 									class is returned if <code>allowInherited</code> is <code>true</code>
	 * @param useAttachedJavadoc	if <code>true</code> Javadoc will be extracted from attached Javadoc
	 * 									if there's no source
	 * @return the Javadoc comment content in HTML or <code>null</code> if the member
	 * 			does not have a Javadoc comment or if no source is available
	 * @throws JavaModelException is thrown when the element's Javadoc can not be accessed
	 * @deprecated use {@link #getHTMLContent(IMember, boolean)}
	 */
	public static String getHTMLContent(IMember member, boolean allowInherited, boolean useAttachedJavadoc) throws JavaModelException {
		return getHTMLContent(member, useAttachedJavadoc);
	}

	/**
	 * Gets an IMember's Javadoc comment content from the source or Javadoc attachment
	 * and renders the tags and links in HTML.
	 * Returns <code>null</code> if the member does not contain a Javadoc comment or if no source is available.
	 *
	 * @param member				the member to get the Javadoc of
	 * @param useAttachedJavadoc	if <code>true</code> Javadoc will be extracted from attached Javadoc
	 * 									if there's no source
	 * @return the Javadoc comment content in HTML or <code>null</code> if the member
	 * 			does not have a Javadoc comment or if no source is available
	 * @throws JavaModelException is thrown when the element's Javadoc can not be accessed
	 */
	public static String getHTMLContent(IMember member, boolean useAttachedJavadoc) throws JavaModelException {
		String sourceJavadoc= getHTMLContentFromSource(member);
		if (sourceJavadoc == null || sourceJavadoc.length() == 0 || sourceJavadoc.trim().equals("{@inheritDoc}")) { //$NON-NLS-1$
			if (useAttachedJavadoc) {
				if (member.getOpenable().getBuffer() == null) { // only if no source available
					return member.getAttachedJavadoc(null);
				}
				if (canInheritJavadoc(member)) {
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

	private static StringBuffer createSuperMethodReferences(final IMethod method) throws JavaModelException {
		IType type= method.getDeclaringType();
		ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(type);
		final MethodOverrideTester tester= SuperTypeHierarchyCache.getMethodOverrideTester(type);

		final ArrayList superInterfaceMethods= new ArrayList();
		final IMethod[] superClassMethod= { null };
		new InheritDocVisitor() {
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

		boolean hasSuperInterfaceMethods= superInterfaceMethods.size() != 0;
		if (!hasSuperInterfaceMethods && superClassMethod[0] == null)
			return null;

		StringBuffer buf= new StringBuffer();
		buf.append("<div>"); //$NON-NLS-1$
		if (hasSuperInterfaceMethods) {
			buf.append("<b>"); //$NON-NLS-1$
			buf.append(JavaDocMessages.JavaDoc2HTMLTextReader_specified_by_section);
			buf.append("</b> "); //$NON-NLS-1$
			for (Iterator iter= superInterfaceMethods.iterator(); iter.hasNext(); ) {
				IMethod overridden= (IMethod) iter.next();
				buf.append(createMethodInTypeLinks(overridden));
				if (iter.hasNext())
					buf.append(JavaElementLabels.COMMA_STRING);
			}
		}
		if (superClassMethod[0] != null) {
			if (hasSuperInterfaceMethods)
				buf.append(JavaElementLabels.COMMA_STRING);
			buf.append("<b>"); //$NON-NLS-1$
			buf.append(JavaDocMessages.JavaDoc2HTMLTextReader_overrides_section);
			buf.append("</b> "); //$NON-NLS-1$
			buf.append(createMethodInTypeLinks(superClassMethod[0]));
		}
		buf.append("</div>"); //$NON-NLS-1$
		return buf;
	}

	private static String createMethodInTypeLinks(IMethod overridden) {
		CharSequence methodLink= createSimpleMemberLink(overridden);
		CharSequence typeLink= createSimpleMemberLink(overridden.getDeclaringType());
		String methodInType= MessageFormat.format(JavaDocMessages.JavaDoc2HTMLTextReader_method_in_type, new Object[] { methodLink, typeLink });
		return methodInType;
	}

	private static CharSequence createSimpleMemberLink(IMember member) {
		StringBuffer buf= new StringBuffer();
		buf.append("<a href='"); //$NON-NLS-1$
		try {
			String uri= JavaElementLinks.createURI(JavaElementLinks.JAVADOC_SCHEME, member);
			buf.append(uri);
		} catch (URISyntaxException e) {
			JavaPlugin.log(e);
		}
		buf.append("'>"); //$NON-NLS-1$
		JavaElementLabels.getElementLabel(member, 0, buf);
		buf.append("</a>"); //$NON-NLS-1$
		return buf;
	}

	private static String getHTMLContentFromSource(IMember member) throws JavaModelException {
		IBuffer buf= member.getOpenable().getBuffer();
		if (buf == null) {
			return null; // no source attachment found
		}

		ISourceRange javadocRange= member.getJavadocRange();
		if (javadocRange == null) {
			if (canInheritJavadoc(member)) {
				// Try to use the inheritDoc algorithm. If it finds nothing (in source), return null.
				String inheritedJavadoc= javadoc2HTML(member, "/***/"); //$NON-NLS-1$
				return inheritedJavadoc != null && inheritedJavadoc.length() > 0 ? inheritedJavadoc : null;
			} else {
				return null;
			}
		}

		String rawJavadoc= buf.getText(javadocRange.getOffset(), javadocRange.getLength());
		return javadoc2HTML(member, rawJavadoc);
	}

	private static Javadoc getJavadocNode(IMember member, String rawJavadoc) {
		//FIXME: take from SharedASTProvider if available
		//Caveat: Javadoc nodes are not available when Javadoc processing has been disabled!
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=212207

		ASTParser parser= ASTParser.newParser(AST.JLS3);

		IJavaProject javaProject= member.getJavaProject();
		parser.setProject(javaProject);
		Map options= javaProject.getOptions(true);
		options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED); // workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=212207
		parser.setCompilerOptions(options);

		String source= rawJavadoc + "class C{}"; //$NON-NLS-1$
		parser.setSource(source.toCharArray());

		CompilationUnit root= (CompilationUnit) parser.createAST(null);
		if (root == null)
			return null;
		List types= root.types();
		if (types.size() != 1)
			return null;
		AbstractTypeDeclaration type= (AbstractTypeDeclaration) types.get(0);
		return type.getJavadoc();
	}

	private static String javadoc2HTML(IMember member, String rawJavadoc) {
		Javadoc javadoc= getJavadocNode(member, rawJavadoc);

		if (javadoc == null) {
			// fall back to JavadocContentAccess:
			try {
				Reader contentReader= JavadocContentAccess.getHTMLContentReader(member, false, false);
				if (contentReader != null)
					return getString(contentReader);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			return null;
		}

		if (canInheritJavadoc(member)) {
			IMethod method= (IMethod) member;
			return new JavadocContentAccess2(method, javadoc, rawJavadoc, new JavadocLookup(method.getDeclaringType())).toHTML();
		}
		return new JavadocContentAccess2(member, javadoc, rawJavadoc).toHTML();
	}

	private static boolean canInheritJavadoc(IMember member) {
		if (member instanceof IMethod && member.getJavaProject().exists()) {
			/*
			 * Exists test catches ExternalJavaProject, in which case no hierarchy can be built.
			 */
			try {
				return ! ((IMethod) member).isConstructor();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return false;
	}

	/**
	 * Gets the reader content as a String
	 *
	 * @param reader the reader
	 * @return the reader content as string
	 */
	private static String getString(Reader reader) {
		StringBuffer buf= new StringBuffer();
		char[] buffer= new char[1024];
		int count;
		try {
			while ((count= reader.read(buffer)) != -1)
				buf.append(buffer, 0, count);
		} catch (IOException e) {
			return null;
		}
		return buf.toString();
	}

	/**
	 * Finds the first available attached Javadoc in the hierarchy of the given method.
	 *
	 * @param method the method
	 * @return the inherited Javadoc from the Javadoc attachment, or <code>null</code> if none
	 * @throws JavaModelException unexpected problem
	 */
	private static String findAttachedDocInHierarchy(final IMethod method) throws JavaModelException {
		IType type= method.getDeclaringType();
		ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(type);
		final MethodOverrideTester tester= SuperTypeHierarchyCache.getMethodOverrideTester(type);

		return (String) new InheritDocVisitor() {
			public Object visit(IType currType) throws JavaModelException {
				IMethod overridden= tester.findOverriddenMethodInType(currType, method);
				if (overridden == null)
					return InheritDocVisitor.CONTINUE;

				if (overridden.getOpenable().getBuffer() == null) { // only if no source available
					//TODO: BaseURL for method can be wrong for attached Javadoc from overridden
					// (e.g. when overridden is from rt.jar). Fix would be to add baseURL here.
					String attachedJavadoc= overridden.getAttachedJavadoc(null);
					if (attachedJavadoc != null)
						return attachedJavadoc;
				}
				return CONTINUE;
			}
		}.visitInheritDoc(type, hierarchy);
	}

	private String toHTML() {
		fBuf= new StringBuffer();
		fLiteralContent= 0;

		// After first loop, non-null entries in the following two lists are missing and need to be inherited:
		List parameterNames= initParameterNames();
		List exceptionNames= initExceptionNames();

		TagElement deprecatedTag= null;
		TagElement start= null;
		List/*<TagElement>*/ parameters= new ArrayList();
		TagElement returnTag= null;
		List/*<TagElement>*/ exceptions= new ArrayList();
		List/*<TagElement>*/ versions= new ArrayList();
		List/*<TagElement>*/ authors= new ArrayList();
		List/*<TagElement>*/ sees= new ArrayList();
		List/*<TagElement>*/ since= new ArrayList();
		List/*<TagElement>*/ rest= new ArrayList();

		List/*<TagElement>*/ tags= fJavadoc.tags();
		for (Iterator iter= tags.iterator(); iter.hasNext(); ) {
			TagElement tag= (TagElement) iter.next();
			String tagName= tag.getTagName();
			if (tagName == null) {
				start= tag;

			} else if (TagElement.TAG_PARAM.equals(tagName)) {
				parameters.add(tag);
				List fragments= tag.fragments();
				if (fragments.size() > 0) {
					Object first= fragments.get(0);
					if (first instanceof SimpleName) {
						String name= ((SimpleName) first).getIdentifier();
						int paramIndex= parameterNames.indexOf(name);
						if (paramIndex != -1) {
							parameterNames.set(paramIndex, null);
						}
					}
				}

			} else if (TagElement.TAG_RETURN.equals(tagName)) {
				if (returnTag == null)
					returnTag= tag; // the Javadoc tool only shows the first return tag

			} else if (TagElement.TAG_EXCEPTION.equals(tagName) || TagElement.TAG_THROWS.equals(tagName)) {
				exceptions.add(tag);
				List fragments= tag.fragments();
				if (fragments.size() > 0) {
					Object first= fragments.get(0);
					if (first instanceof Name) {
						String name= ASTNodes.getSimpleNameIdentifier((Name) first);
						int exceptionIndex= exceptionNames.indexOf(name);
						if (exceptionIndex != -1) {
							exceptionNames.set(exceptionIndex, null);
						}
					}
				}

			} else if (TagElement.TAG_SINCE.equals(tagName)) {
				since.add(tag);
			} else if (TagElement.TAG_VERSION.equals(tagName)) {
				versions.add(tag);
			} else if (TagElement.TAG_AUTHOR.equals(tagName)) {
				authors.add(tag);
			} else if (TagElement.TAG_SEE.equals(tagName)) {
				sees.add(tag);
			} else if (TagElement.TAG_DEPRECATED.equals(tagName)) {
				if (deprecatedTag == null)
					deprecatedTag= tag; // the Javadoc tool only shows the first deprecated tag
			} else {
				rest.add(tag);
			}
		}

		//TODO: @Documented annotations before header
		if (deprecatedTag != null)
			handleDeprecatedTag(deprecatedTag);
		if (start != null)
			handleContentElements(start.fragments());
		else if (fMethod != null) {
			CharSequence inherited= fJavadocLookup.getInheritedMainDescription(fMethod);
			// The Javadoc tool adds "Description copied from class: ..." (only for the main description).
			// We don't bother doing that.
			handleInherited(inherited);
		}

		CharSequence[] parameterDescriptions= new CharSequence[parameterNames.size()];
		boolean hasInheritedParameters= inheritParameterDescriptions(parameterNames, parameterDescriptions);
		boolean hasParameters= parameters.size() > 0 || hasInheritedParameters;

		CharSequence returnDescription= null;
		if (returnTag == null && needsReturnTag())
			returnDescription= fJavadocLookup.getInheritedReturnDescription(fMethod);
		boolean hasReturnTag= returnTag != null || returnDescription != null;

		CharSequence[] exceptionDescriptions= new CharSequence[exceptionNames.size()];
		boolean hasInheritedExceptions= inheritExceptionDescriptions(exceptionNames, exceptionDescriptions);
		boolean hasExceptions= exceptions.size() > 0 || hasInheritedExceptions;

		if (hasParameters || hasReturnTag || hasExceptions
				|| versions.size() > 0 || authors.size() > 0 || since.size() > 0 || sees.size() > 0 || rest.size() > 0
				|| (fBuf.length() > 0 && (parameterDescriptions.length > 0 || exceptionDescriptions.length > 0))
				) {
			handleSuperMethodReferences();
			fBuf.append(BLOCK_TAG_START);
			handleParameterTags(parameters, parameterNames, parameterDescriptions);
			handleReturnTag(returnTag, returnDescription);
			handleExceptionTags(exceptions, exceptionNames, exceptionDescriptions);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_since_section, since);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_version_section, versions);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_author_section, authors);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_see_section, sees);
			handleBlockTags(rest);
			fBuf.append(BLOCK_TAG_END);

		} else if (fBuf.length() > 0) {
			handleSuperMethodReferences();
		}

		String result= fBuf.toString();
		fBuf= null;
		return result;
	}

	private void handleDeprecatedTag(TagElement tag) {
		fBuf.append("<p><b>"); //$NON-NLS-1$
		fBuf.append(JavaDocMessages.JavaDoc2HTMLTextReader_deprecated_section);
		fBuf.append("</b> <i>"); //$NON-NLS-1$
		handleContentElements(tag.fragments());
		fBuf.append("</i><p>"); //$NON-NLS-1$ TODO: Why not </p>? See https://bugs.eclipse.org/bugs/show_bug.cgi?id=243318 .
	}

	private void handleSuperMethodReferences() {
		if (fMethod != null) {
			try {
				StringBuffer superMethodReferences= createSuperMethodReferences(fMethod);
				if (superMethodReferences != null)
					fBuf.append(superMethodReferences);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
	}

	private List initParameterNames() {
		if (fMethod != null) {
			try {
				return new ArrayList(Arrays.asList(fMethod.getParameterNames()));
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return Collections.EMPTY_LIST;
	}

	private List initExceptionNames() {
		if (fMethod != null) {
			try {
				String[] exceptionTypes= fMethod.getExceptionTypes();
				ArrayList exceptionNames= new ArrayList();
				for (int i= 0; i < exceptionTypes.length; i++) {
					exceptionNames.add(Signature.getSimpleName(Signature.toString(exceptionTypes[i])));
				}
				return exceptionNames;
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return Collections.EMPTY_LIST;
	}

	private boolean needsReturnTag() {
		if (fMethod == null)
			return false;
		try {
			return ! Signature.SIG_VOID.equals(fMethod.getReturnType());
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return false;
		}
	}

	private boolean inheritParameterDescriptions(List parameterNames, CharSequence[] parameterDescriptions) {
		boolean hasInheritedParameters= false;
		for (int i= 0; i < parameterNames.size(); i++) {
			String name= (String) parameterNames.get(i);
			if (name != null) {
				parameterDescriptions[i]= fJavadocLookup.getInheritedParamDescription(fMethod, i);
				if (parameterDescriptions[i] != null)
					hasInheritedParameters= true;
			}
		}
		return hasInheritedParameters;
	}

	private boolean inheritExceptionDescriptions(List exceptionNames, CharSequence[] exceptionDescriptions) {
		boolean hasInheritedExceptions= false;
		for (int i= 0; i < exceptionNames.size(); i++) {
			String name= (String) exceptionNames.get(i);
			if (name != null) {
				exceptionDescriptions[i]= fJavadocLookup.getInheritedExceptionDescription(fMethod, name);
				if (exceptionDescriptions[i] != null)
					hasInheritedExceptions= true;
			}
		}
		return hasInheritedExceptions;
	}

	CharSequence getMainDescription() {
		if (fMainDescription == null) {
			fMainDescription= new StringBuffer();
			fBuf= fMainDescription;
			fLiteralContent= 0;

			List tags= fJavadoc.tags();
			for (Iterator iter= tags.iterator(); iter.hasNext(); ) {
				TagElement tag= (TagElement) iter.next();
				String tagName= tag.getTagName();
				if (tagName == null) {
					handleContentElements(tag.fragments());
					break;
				}
			}

			fBuf= null;
		}
		return fMainDescription.length() > 0 ? fMainDescription : null;
	}

	CharSequence getReturnDescription() {
		if (fReturnDescription == null) {
			fReturnDescription= new StringBuffer();
			fBuf= fReturnDescription;
			fLiteralContent= 0;

			List tags= fJavadoc.tags();
			for (Iterator iter= tags.iterator(); iter.hasNext(); ) {
				TagElement tag= (TagElement) iter.next();
				String tagName= tag.getTagName();
				if (TagElement.TAG_RETURN.equals(tagName)) {
					handleContentElements(tag.fragments());
					break;
				}
			}

			fBuf= null;
		}
		return fReturnDescription.length() > 0 ? fReturnDescription : null;
	}

	CharSequence getInheritedParamDescription(int paramIndex) throws JavaModelException {
		if (fMethod != null) {
			String[] parameterNames= fMethod.getParameterNames();
			if (fParamDescriptions == null) {
				fParamDescriptions= new StringBuffer[parameterNames.length];
			} else {
				StringBuffer description= fParamDescriptions[paramIndex];
				if (description != null) {
					return description.length() > 0 ? description : null;
				}
			}

			StringBuffer description= new StringBuffer();
			fParamDescriptions[paramIndex]= description;
			fBuf= description;
			fLiteralContent= 0;

			String paramName= parameterNames[paramIndex];
			List tags= fJavadoc.tags();
			for (Iterator iter= tags.iterator(); iter.hasNext(); ) {
				TagElement tag= (TagElement) iter.next();
				String tagName= tag.getTagName();
				if (TagElement.TAG_PARAM.equals(tagName)) {
					List fragments= tag.fragments();
					if (fragments.size() > 0) {
						Object first= fragments.get(0);
						if (first instanceof SimpleName) {
							String name= ((SimpleName) first).getIdentifier();
							if (name.equals(paramName)) {
								handleContentElements(fragments.subList(1, fragments.size()));
								break;
							}
						}
					}
				}
			}

			fBuf= null;
			return description.length() > 0 ? description : null;
		}
		return null;
	}

	CharSequence getExceptionDescription(String simpleName) {
		if (fMethod != null) {
			if (fExceptionDescriptions == null) {
				fExceptionDescriptions= new HashMap();
			} else {
				StringBuffer description= (StringBuffer) fExceptionDescriptions.get(simpleName);
				if (description != null) {
					return description.length() > 0 ? description : null;
				}
			}

			StringBuffer description= new StringBuffer();
			fExceptionDescriptions.put(simpleName, description);
			fBuf= description;
			fLiteralContent= 0;

			List tags= fJavadoc.tags();
			for (Iterator iter= tags.iterator(); iter.hasNext(); ) {
				TagElement tag= (TagElement) iter.next();
				String tagName= tag.getTagName();
				if (TagElement.TAG_THROWS.equals(tagName) || TagElement.TAG_EXCEPTION.equals(tagName)) {
					List fragments= tag.fragments();
					if (fragments.size() > 0) {
						Object first= fragments.get(0);
						if (first instanceof Name) {
							String name= ASTNodes.getSimpleNameIdentifier((Name) first);
							if (name.equals(simpleName)) {
								if (fragments.size() > 1)
									handleContentElements(fragments.subList(1, fragments.size()));
								break;
							}
						}
					}
				}
			}

			fBuf= null;
			return description.length() > 0 ? description : null;
		}
		return null;
	}

	private void handleContentElements(List nodes) {
		ASTNode previousNode= null;
		for (Iterator iter= nodes.iterator(); iter.hasNext(); ) {
			ASTNode child= (ASTNode) iter.next();
			if (previousNode != null) {
				int previousEnd= previousNode.getStartPosition() + previousNode.getLength();
				int childStart= child.getStartPosition();
				if (previousEnd != childStart) {
					// Need to preserve whitespace before a node that's not
					// directly following the previous node (e.g. on a new line)
					// due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=206518 :
					String textWithStars= fSource.substring(previousEnd, childStart);
					String text= removeDocLineIntros(textWithStars);
					fBuf.append(text);
				}
			}
			previousNode= child;
			if (child instanceof TextElement) {
				handleText(((TextElement) child).getText());
			} else if (child instanceof TagElement) {
				handleInlineTagElement((TagElement) child);
			} else {
				// This is unexpected. Fail gracefully by just copying the source.
				int start= child.getStartPosition();
				String text= fSource.substring(start, start + child.getLength());
				fBuf.append(removeDocLineIntros(text));
			}
		}
	}

	private String removeDocLineIntros(String textWithStars) {
		String lineBreakGroup= "(\\r\\n?|\\n)"; //$NON-NLS-1$
		String noBreakSpace= "[^\r\n&&\\s]"; //$NON-NLS-1$
		return textWithStars.replaceAll(lineBreakGroup + noBreakSpace + "*\\*" /*+ noBreakSpace + '?'*/, "$1"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void handleText(String text) {
		if (fLiteralContent == 0) {
			fBuf.append(text);
		} else {
			appendEscaped(fBuf, text);
		}
	}

	private static void appendEscaped(StringBuffer buf, String text) {
		int nextToCopy= 0;
		int length= text.length();
		for (int i= 0; i < length; i++) {
			char ch= text.charAt(i);
			String rep= null;
			switch (ch) {
				case '&':
					rep= "&amp;"; //$NON-NLS-1$
					break;
				case '"':
					rep= "&quot;"; //$NON-NLS-1$
					break;
				case '<':
					rep= "&lt;"; //$NON-NLS-1$
					break;
				case '>':
					rep= "&gt;"; //$NON-NLS-1$
					break;
			}
			if (rep != null) {
				if (nextToCopy < i)
					buf.append(text.substring(nextToCopy, i));
				buf.append(rep);
				nextToCopy= i + 1;
			}
		}
		if (nextToCopy < length)
			buf.append(text.substring(nextToCopy));
	}

	private void handleInlineTagElement(TagElement node) {
		//TODO: TagElement.TAG_VALUE

		String name= node.getTagName();

		boolean isLink= TagElement.TAG_LINK.equals(name);
		boolean isLinkplain= TagElement.TAG_LINKPLAIN.equals(name);
		boolean isCode= TagElement.TAG_CODE.equals(name);
		boolean isLiteral= TagElement.TAG_LITERAL.equals(name);

		if (isLiteral || isCode)
			fLiteralContent++;
		if (isLink || isCode)
			fBuf.append("<code>"); //$NON-NLS-1$

		if (isLink || isLinkplain)
			handleLink(node.fragments());
		else if (isCode || isLiteral)
			handleContentElements(node.fragments());
		else if (handleInheritDoc(node)) {
			// handled
		} else if (handleDocRoot(node)) {
			// handled
		} else {
			//print uninterpreted source {@tagname ...} for unknown tags
			int start= node.getStartPosition();
			String text= fSource.substring(start, start + node.getLength());
			fBuf.append(removeDocLineIntros(text));
		}

		if (isLink || isCode)
			fBuf.append("</code>"); //$NON-NLS-1$
		if (isLiteral || isCode)
			fLiteralContent--;

	}

	private boolean handleDocRoot(TagElement node) {
		if (!TagElement.TAG_DOCROOT.equals(node.getTagName()))
			return false;

		try {
			String url= null;
			if (fMember.isBinary()) {
				URL javadocBaseLocation= JavaUI.getJavadocBaseLocation(fMember);
				if (javadocBaseLocation != null) {
					url= javadocBaseLocation.toExternalForm();
				}
			} else {
				IPackageFragmentRoot srcRoot= JavaModelUtil.getPackageFragmentRoot(fMember);
				if (srcRoot != null) {
					IResource resource= srcRoot.getResource();
					if (resource != null) {
						/*
						 * Too bad: Browser widget knows nothing about EFS and custom URL handlers,
						 * so IResource#getLocationURI() does not work in all cases.
						 * We only support the local file system for now.
						 * A solution could be https://bugs.eclipse.org/bugs/show_bug.cgi?id=149022 .
						 */
						IPath location= resource.getLocation();
						if (location != null) {
							url= location.toFile().toURI().toASCIIString();
						}
					}

				}
			}
			if (url != null) {
				if (url.endsWith("/")) { //$NON-NLS-1$
					url= url.substring(0, url.length() -1);
				}
				fBuf.append(url);
				return true;
			}
		} catch (JavaModelException e) {
		}
		return false;
	}


	/**
	 * Handle {&#64;inheritDoc}.
	 *
	 * @param node the node
	 * @return <code>true</code> iff the node was an {&#64;inheritDoc} node and has been handled
	 */
	private boolean handleInheritDoc(TagElement node) {
		if (! TagElement.TAG_INHERITDOC.equals(node.getTagName()))
			return false;
		try {
			if (fMethod == null)
				return false;

			TagElement blockTag= (TagElement) node.getParent();
			String blockTagName= blockTag.getTagName();

			if (blockTagName == null) {
				CharSequence inherited= fJavadocLookup.getInheritedMainDescription(fMethod);
				return handleInherited(inherited);

			} else if (TagElement.TAG_PARAM.equals(blockTagName)) {
				List fragments= blockTag.fragments();
				if (fragments.size() > 0) {
					Object first= fragments.get(0);
					if (first instanceof SimpleName) {
						String name= ((SimpleName) first).getIdentifier();
						String[] parameterNames= fMethod.getParameterNames();
						for (int i= 0; i < parameterNames.length; i++) {
							if (name.equals(parameterNames[i])) {
								CharSequence inherited= fJavadocLookup.getInheritedParamDescription(fMethod, i);
								return handleInherited(inherited);
							}
						}
					}
				}

			} else if (TagElement.TAG_RETURN.equals(blockTagName)) {
				CharSequence inherited= fJavadocLookup.getInheritedReturnDescription(fMethod);
				return handleInherited(inherited);

			} else if (TagElement.TAG_THROWS.equals(blockTagName) || TagElement.TAG_EXCEPTION.equals(blockTagName)) {
				List fragments= blockTag.fragments();
				if (fragments.size() > 0) {
					Object first= fragments.get(0);
					if (first instanceof Name) {
						String name= ASTNodes.getSimpleNameIdentifier((Name) first);
						CharSequence inherited= fJavadocLookup.getInheritedExceptionDescription(fMethod, name);
						return handleInherited(inherited);
					}
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return false;
	}

	private boolean handleInherited(CharSequence inherited) {
		if (inherited == null)
			return false;

		fBuf.append(inherited);
		return true;
	}

	private void handleBlockTags(String title, List tags) {
		if (tags.isEmpty())
			return;

		handleBlockTagTitle(title);

		for (Iterator iter= tags.iterator(); iter.hasNext(); ) {
			TagElement tag= (TagElement) iter.next();
			fBuf.append(BlOCK_TAG_ENTRY_START);
			if (TagElement.TAG_SEE.equals(tag.getTagName())) {
				handleSeeTag(tag);
			} else {
				handleContentElements(tag.fragments());
			}
			fBuf.append(BlOCK_TAG_ENTRY_END);
		}
	}

	private void handleReturnTag(TagElement tag, CharSequence returnDescription) {
		if (tag == null && returnDescription == null)
			return;

		handleBlockTagTitle(JavaDocMessages.JavaDoc2HTMLTextReader_returns_section);
		fBuf.append(BlOCK_TAG_ENTRY_START);
		if (tag != null)
			handleContentElements(tag.fragments());
		else
			fBuf.append(returnDescription);
		fBuf.append(BlOCK_TAG_ENTRY_END);
	}

	private void handleBlockTags(List tags) {
		for (Iterator iter= tags.iterator(); iter.hasNext(); ) {
			TagElement tag= (TagElement) iter.next();
			handleBlockTagTitle(tag.getTagName());
			fBuf.append(BlOCK_TAG_ENTRY_START);
			handleContentElements(tag.fragments());
			fBuf.append(BlOCK_TAG_ENTRY_END);
		}
	}

	private void handleBlockTagTitle(String title) {
		fBuf.append("<dt>"); //$NON-NLS-1$
		fBuf.append(title);
		fBuf.append("</dt>"); //$NON-NLS-1$
	}

	private void handleSeeTag(TagElement tag) {
		handleLink(tag.fragments());
	}

	private void handleExceptionTags(List tags, List exceptionNames, CharSequence[] exceptionDescriptions) {
		if (tags.size() == 0 && containsOnlyNull(exceptionNames))
			return;

		handleBlockTagTitle(JavaDocMessages.JavaDoc2HTMLTextReader_throws_section);

		for (Iterator iter= tags.iterator(); iter.hasNext(); ) {
			TagElement tag= (TagElement) iter.next();
			fBuf.append(BlOCK_TAG_ENTRY_START);
			handleThrowsTag(tag);
			fBuf.append(BlOCK_TAG_ENTRY_END);
		}
		for (int i= 0; i < exceptionDescriptions.length; i++) {
			CharSequence description= exceptionDescriptions[i];
			String name= (String) exceptionNames.get(i);
			if (name != null) {
				fBuf.append(BlOCK_TAG_ENTRY_START);
				handleLink(Collections.singletonList(fJavadoc.getAST().newSimpleName(name)));
				if (description != null) {
					fBuf.append(JavaElementLabels.CONCAT_STRING);
					fBuf.append(description);
				}
				fBuf.append(BlOCK_TAG_ENTRY_END);
			}
		}
	}

	private void handleThrowsTag(TagElement tag) {
		List fragments= tag.fragments();
		int size= fragments.size();
		if (size > 0) {
			handleLink(fragments.subList(0, 1));
			if (size > 1) {
				fBuf.append(JavaElementLabels.CONCAT_STRING);
				handleContentElements(fragments.subList(1, size));
			}
		}
	}

	private void handleParameterTags(List tags, List parameterNames, CharSequence[] parameterDescriptions) {
		if (tags.size() == 0 && containsOnlyNull(parameterNames))
			return;

		handleBlockTagTitle(JavaDocMessages.JavaDoc2HTMLTextReader_parameters_section);

		for (Iterator iter= tags.iterator(); iter.hasNext(); ) {
			TagElement tag= (TagElement) iter.next();
			fBuf.append(BlOCK_TAG_ENTRY_START);
			handleParamTag(tag);
			fBuf.append(BlOCK_TAG_ENTRY_END);
		}
		for (int i= 0; i < parameterDescriptions.length; i++) {
			CharSequence description= parameterDescriptions[i];
			String name= (String) parameterNames.get(i);
			if (name != null) {
				fBuf.append(BlOCK_TAG_ENTRY_START);
				fBuf.append(PARAM_NAME_START);
				fBuf.append(name);
				fBuf.append(PARAM_NAME_END);
				if (description != null)
					fBuf.append(description);
				fBuf.append(BlOCK_TAG_ENTRY_END);
			}
		}
	}

	private void handleParamTag(TagElement tag) {
		List fragments= tag.fragments();
		int i= 0;
		int size= fragments.size();
		if (size > 0) {
			Object first= fragments.get(0);
			fBuf.append(PARAM_NAME_START);
			if (first instanceof SimpleName) {
				String name= ((SimpleName) first).getIdentifier();
				fBuf.append(name);
				i++;
			} else if (first instanceof TextElement) {
				String firstText= ((TextElement) first).getText();
				if ("<".equals(firstText)) { //$NON-NLS-1$
					fBuf.append("&lt;"); //$NON-NLS-1$
					i++;
					if (size > 1) {
						Object second= fragments.get(1);
						if (second instanceof SimpleName) {
							String name= ((SimpleName) second).getIdentifier();
							fBuf.append(name);
							i++;
							if (size > 2) {
								Object third= fragments.get(2);
								String thirdText= ((TextElement) third).getText();
								if (">".equals(thirdText)) { //$NON-NLS-1$
									fBuf.append("&gt;"); //$NON-NLS-1$
									i++;
								}
							}
						}
					}
				}
			}
			fBuf.append(PARAM_NAME_END);

			handleContentElements(fragments.subList(i, fragments.size()));
		}
	}

	private void handleLink(List fragments) {
		//TODO: Javadoc shortens type names to minimal length according to context
		int fs= fragments.size();
		if (fs > 0) {
			Object first= fragments.get(0);
			String refTypeName= null;
			String refMemberName= null;
			String[] refMethodParamTypes= null;
			String[] refMethodParamNames= null;
			if (first instanceof Name) {
				Name name = (Name) first;
				refTypeName= name.getFullyQualifiedName();
			} else if (first instanceof MemberRef) {
				MemberRef memberRef= (MemberRef) first;
				Name qualifier= memberRef.getQualifier();
				refTypeName= qualifier == null ? "" : qualifier.getFullyQualifiedName(); //$NON-NLS-1$
				refMemberName= memberRef.getName().getIdentifier();
			} else if (first instanceof MethodRef) {
				MethodRef methodRef= (MethodRef) first;
				Name qualifier= methodRef.getQualifier();
				refTypeName= qualifier == null ? "" : qualifier.getFullyQualifiedName(); //$NON-NLS-1$
				refMemberName= methodRef.getName().getIdentifier();
				List params= methodRef.parameters();
				int ps= params.size();
				refMethodParamTypes= new String[ps];
				refMethodParamNames= new String[ps];
				for (int i= 0; i < ps; i++) {
					MethodRefParameter param= (MethodRefParameter) params.get(i);
					refMethodParamTypes[i]= ASTNodes.asString(param.getType());
					SimpleName paramName= param.getName();
					if (paramName != null)
						refMethodParamNames[i]= paramName.getIdentifier();
				}
			}

			if (refTypeName != null) {
				fBuf.append("<a href='"); //$NON-NLS-1$
				try {
					String scheme= JavaElementLinks.JAVADOC_SCHEME;
					String uri= JavaElementLinks.createURI(scheme, fMember, refTypeName, refMemberName, refMethodParamTypes);
					fBuf.append(uri);
				} catch (URISyntaxException e) {
					JavaPlugin.log(e);
				}
				fBuf.append("'>"); //$NON-NLS-1$
				if (fs > 1) {
					//TODO:
					// - Set fLiteralContent for label? Check spec.
					// - Javadoc of java.util.regex.Pattern has a space in front of link in <pre>
//					if (fs == 2 && fragments.get(1) instanceof TextElement) {
//						String text= removeLeadingWhitespace(((TextElement) fragments.get(1)).getText());
//						if (text.length() != 0)
//							handleText(text);
//						else
//							//throws
//					}
					handleContentElements(fragments.subList(1, fs));
				} else {
					fBuf.append(refTypeName);
					if (refMemberName != null) {
						if (refTypeName.length() > 0) {
							fBuf.append('.');
						}
						fBuf.append(refMemberName);
						if (refMethodParamTypes != null) {
							fBuf.append('(');
							for (int i= 0; i < refMethodParamTypes.length; i++) {
								String pType= refMethodParamTypes[i];
								fBuf.append(pType);
								String pName= refMethodParamNames[i];
								if (pName != null) {
									fBuf.append(' ').append(pName);
								}
								if (i < refMethodParamTypes.length - 1) {
									fBuf.append(", "); //$NON-NLS-1$
								}
							}
							fBuf.append(')');
						}
					}
				}
				fBuf.append("</a>"); //$NON-NLS-1$
			} else {
				handleContentElements(fragments);
			}
		}
	}

	private boolean containsOnlyNull(List parameterNames) {
		for (Iterator iter= parameterNames.iterator(); iter.hasNext(); ) {
			if (iter.next() != null)
				return false;
		}
		return true;
	}

}
