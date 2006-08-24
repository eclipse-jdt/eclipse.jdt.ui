/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;

/**
 * An <code>IFix</code> can calculate a <code>TextChange</code>
 * which applied to a <code>ICompilationUnit</code> will fix
 * one or several problems.
 * 
 * @since 3.2
 */
public interface IFix {
	
	/**
	 * A String describing what the <code>TextChange</code> returned by
	 * <code>createChange</code> will do.
	 * 
	 * @return The description, not null
	 */
	public abstract String getDescription();
	
	/**
	 * A <code>TextChange</code> which applied to <code>getCompilationUnit</code>
	 * will fix a problem.
	 * 
	 * @return The change or null if no fix possible
	 * @throws CoreException
	 */
	public abstract TextChange createChange() throws CoreException;
	
	/**
	 * The <code>ICompilationUnit</code> on which <code>createChange</code> should
	 * be applied to fix a problem.
	 * 
	 * @return The ICompilationUnit, not null
	 */
	public abstract ICompilationUnit getCompilationUnit();
	
	/**
	 * A status to inform about issues with this fix
	 * 
	 * @return The status, not null
	 */
	public abstract IStatus getStatus();
}
