/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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

import java.util.HashMap;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.manipulation.internal.javadoc.IJavadocContentFactory.IJavadocAccess;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;

public class JavadocLookup {
	public static final IJavadocContentFactory DEFAULT_FACTORY= new IJavadocContentFactory() {
		@Override
		public IJavadocAccess createJavadocAccess(IJavaElement element, Javadoc javadoc, String source, JavadocLookup lookup) {
			if (source.startsWith("///")) { //$NON-NLS-1$
				if (lookup == null)
					return new CoreMarkdownAccessImpl(element, javadoc, source);
				else
					return new CoreMarkdownAccessImpl(element, javadoc, source, lookup);
			}
			if (lookup == null)
				return new CoreJavadocAccessImpl(element, javadoc, source);
			else
				return new CoreJavadocAccessImpl(element, javadoc, source, lookup);
		}
	};

	static final JavadocLookup NONE= new JavadocLookup(null, DEFAULT_FACTORY) {
		@Override
		public CharSequence getInheritedMainDescription(IMethod method) {
			return null;
		}

		@Override
		public CharSequence getInheritedParamDescription(IMethod method, int i) {
			return null;
		}

		@Override
		public CharSequence getInheritedReturnDescription(IMethod method) {
			return null;
		}

		@Override
		public CharSequence getInheritedExceptionDescription(IMethod method, String name) {
			return null;
		}
	};

	private interface DescriptionGetter {
		/**
		 * Returns a Javadoc tag description or <code>null</code>.
		 *
		 * @param contentAccess the content access
		 * @return the description, or <code>null</code> if none
		 * @throws JavaModelException unexpected problem
		 */
		CharSequence getDescription(IJavadocAccess contentAccess) throws JavaModelException;
	}

	private final IType fStartingType;

	private final HashMap<IMethod, IJavadocAccess> fContentAccesses;

	private ITypeHierarchy fTypeHierarchy;

	private MethodOverrideTester fOverrideTester;

	private IJavadocContentFactory fAccessFactory;


	public JavadocLookup(IType startingType, IJavadocContentFactory accessFactory) {
		fStartingType= startingType;
		fContentAccesses= new HashMap<>();
		fAccessFactory= accessFactory != null ? accessFactory : DEFAULT_FACTORY;
	}

	/**
	 * For the given method, returns the main description from an overridden method.
	 *
	 * @param method a method
	 * @return the description that replaces the <code>{&#64;inheritDoc}</code> tag, or
	 *         <code>null</code> if none could be found
	 */
	public CharSequence getInheritedMainDescription(IMethod method) {
		return getInheritedDescription(method, contentAccess -> contentAccess.getMainDescription());
	}

	/**
	 * For the given method, returns the @param tag description for the given type parameter from an
	 * overridden method.
	 *
	 * @param method a method
	 * @param typeParamIndex the index of the type parameter
	 * @return the description that replaces the <code>{&#64;inheritDoc}</code> tag, or
	 *         <code>null</code> if none could be found
	 */
	public CharSequence getInheritedTypeParamDescription(IMethod method, final int typeParamIndex) {
		return getInheritedDescription(method, contentAccess -> contentAccess.getInheritedTypeParamDescription(typeParamIndex));
	}

	/**
	 * For the given method, returns the @param tag description for the given parameter from an
	 * overridden method.
	 *
	 * @param method a method
	 * @param paramIndex the index of the parameter
	 * @return the description that replaces the <code>{&#64;inheritDoc}</code> tag, or
	 *         <code>null</code> if none could be found
	 */
	public CharSequence getInheritedParamDescription(IMethod method, final int paramIndex) {
		return getInheritedDescription(method, contentAccess -> contentAccess.getInheritedParamDescription(paramIndex));
	}

	/**
	 * For the given method, returns the @return tag description from an overridden method.
	 *
	 * @param method a method
	 * @return the description that replaces the <code>{&#64;inheritDoc}</code> tag, or
	 *         <code>null</code> if none could be found
	 */
	public CharSequence getInheritedReturnDescription(IMethod method) {
		return getInheritedDescription(method, contentAccess -> contentAccess.getReturnDescription());
	}

	/**
	 * For the given method, returns the @throws/@exception tag description for the given exception
	 * from an overridden method.
	 *
	 * @param method a method
	 * @param simpleName the simple name of an exception
	 * @return the description that replaces the <code>{&#64;inheritDoc}</code> tag, or
	 *         <code>null</code> if none could be found
	 */
	public CharSequence getInheritedExceptionDescription(IMethod method, final String simpleName) {
		return getInheritedDescription(method, contentAccess -> contentAccess.getExceptionDescription(simpleName));
	}

	private CharSequence getInheritedDescription(final IMethod method, final JavadocLookup.DescriptionGetter descriptionGetter) {
		try {
			return (CharSequence) new InheritDocVisitor() {
				@Override
				public Object visit(IType currType) throws JavaModelException {
					IMethod overridden= getOverrideTester().findOverriddenMethodInType(currType, method);
					if (overridden == null)
						return InheritDocVisitor.CONTINUE;

					IJavadocAccess contentAccess= getJavadocContentAccess(overridden);
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
			JavaManipulationPlugin.log(e);
		}
		return null;
	}

	/**
	 * @param method the method
	 * @return the Javadoc content access for the given method, or <code>null</code> if no Javadoc
	 *         could be found in source
	 * @throws JavaModelException unexpected problem
	 */
	private IJavadocAccess getJavadocContentAccess(IMethod method) throws JavaModelException {
		Object cached= fContentAccesses.get(method);
		if (cached != null)
			return (IJavadocAccess) cached;
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
		Javadoc javadoc= CoreJavadocContentAccessUtility.getJavadocNode(method, rawJavadoc);
		if (javadoc == null) {
			fContentAccesses.put(method, null);
			return null;
		}

		IJavadocAccess contentAccess= this.fAccessFactory.createJavadocAccess(method, javadoc, rawJavadoc, this);
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
