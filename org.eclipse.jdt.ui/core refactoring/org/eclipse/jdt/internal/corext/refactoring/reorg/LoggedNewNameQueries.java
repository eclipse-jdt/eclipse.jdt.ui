/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;

import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.util.JavaElementResourceMapping;

/**
 * Logged implementation of new name queries.
 * 
 * @since 3.3
 */
public final class LoggedNewNameQueries implements INewNameQueries {

	/** Default implementation of a new name query */
	private final class NewNameQuery implements INewNameQuery {

		/** The name */
		private final String fName;

		/** The object */
		private final Object fObject;

		/**
		 * Creates a new new name query.
		 * 
		 * @param object
		 *            the object
		 * @param name
		 *            the initial suggested name
		 */
		public NewNameQuery(final Object object, String name) {
			fObject= object;
			fName= name;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getNewName() {
			if (fObject instanceof IJavaElement) {
				final IJavaElement element= (IJavaElement) fObject;
				final ResourceMapping mapping= JavaElementResourceMapping.create(element);
				return fLog.getNewName(mapping);
			} else if (fObject instanceof JavaElementResourceMapping) {
				final JavaElementResourceMapping mapping= (JavaElementResourceMapping) fObject;
				return fLog.getNewName(mapping);
			} else if (fObject instanceof IResource) {
				final IResource resource= (IResource) fObject;
				return fLog.getNewName(resource);
			}
			return fName;
		}
	}

	/** Null implementation of new name query */
	private static final class NullNewNameQuery implements INewNameQuery {

		/**
		 * {@inheritDoc}
		 */
		public String getNewName() {
			return "null"; //$NON-NLS-1$
		}
	}

	/** The reorg execution log */
	private final ReorgExecutionLog fLog;

	/**
	 * Creates a new logged new name queries.
	 * 
	 * @param log
	 *            the reorg execution log
	 */
	public LoggedNewNameQueries(final ReorgExecutionLog log) {
		fLog= log;
	}

	/**
	 * {@inheritDoc}
	 */
	public INewNameQuery createNewCompilationUnitNameQuery(final ICompilationUnit unit, final String initialSuggestedName) {
		return new NewNameQuery(unit, initialSuggestedName);
	}

	/**
	 * {@inheritDoc}
	 */
	public INewNameQuery createNewPackageFragmentRootNameQuery(final IPackageFragmentRoot root, final String initialSuggestedName) {
		return new NewNameQuery(root, initialSuggestedName);
	}

	/**
	 * {@inheritDoc}
	 */
	public INewNameQuery createNewPackageNameQuery(final IPackageFragment fragment, final String initialSuggestedName) {
		return new NewNameQuery(fragment, initialSuggestedName);
	}

	/**
	 * {@inheritDoc}
	 */
	public INewNameQuery createNewResourceNameQuery(final IResource resource, final String initialSuggestedName) {
		return new NewNameQuery(resource, initialSuggestedName);
	}

	/**
	 * {@inheritDoc}
	 */
	public INewNameQuery createNullQuery() {
		return new NullNewNameQuery();
	}

	/**
	 * {@inheritDoc}
	 */
	public INewNameQuery createStaticQuery(final String name) {
		return new INewNameQuery() {

			public String getNewName() {
				return name;
			}
		};
	}
}