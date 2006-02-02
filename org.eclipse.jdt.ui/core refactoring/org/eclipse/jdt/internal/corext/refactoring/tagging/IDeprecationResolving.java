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
package org.eclipse.jdt.internal.corext.refactoring.tagging;

import org.eclipse.ltk.core.refactoring.RefactoringSessionDescriptor;

/**
 * Interface for refactorings which are able to resolve
 * deprecations introduced by other refactorings which
 * created delegates.
 * 
 * @since 3.2
 *
 */
public interface IDeprecationResolving {

	/**
	 * Performs a dynamic check whether this refactoring object is capable of
	 * creating appropriate delegates for the refactored elements. The
	 * return value of this method may change according to the state of the
	 * refactoring.
	 */
	public boolean canEnableDeprecationResolving();

	/**
	 * If <code>canEnableDeprecationResolving</code> returns
	 * <code>true</code>, then this method can be used to provide a refactoring
	 * session descriptor describing the necessary refactorings to resolve this
	 * deprecation.
	 */
	public RefactoringSessionDescriptor createDeprecationResolution();
}