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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;

import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;

import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;

/**
 * Descriptor object of a java copy refactoring.
 * 
 * @since 3.3
 */
public final class JavaCopyRefactoringDescriptor extends JavaRefactoringDescriptor {

	/** The reorg execution log */
	private final ReorgExecutionLog fLog;

	/**
	 * Creates a new java copy refactoring descriptor.
	 * 
	 * @param log
	 *            the reorg execution log
	 * @param id
	 *            the unique id of the refactoring
	 * @param project
	 *            the project name, or <code>null</code>
	 * @param description
	 *            the description
	 * @param comment
	 *            the comment, or <code>null</code>
	 * @param arguments
	 *            the argument map
	 * @param flags
	 *            the flags
	 */
	public JavaCopyRefactoringDescriptor(final ReorgExecutionLog log, final String id, final String project, final String description, final String comment, final Map arguments, final int flags) {
		super(null, id, project, description, comment, arguments, flags);
		Assert.isNotNull(log);
		fLog= log;
	}

	/**
	 * {@inheritDoc}
	 */
	public Map getArguments() {
		final Map arguments= new HashMap(super.getArguments());
		ReorgPolicyFactory.storeReorgExecutionLog(getProject(), arguments, fLog);
		return arguments;
	}
}