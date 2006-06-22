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

/**
 * Creates a new monitoring create target queries.
 * 
 * @since 3.3
 */
public final class MonitoringCreateTargetQueries implements ICreateTargetQueries {

	private final CreateTargetExecutionLog fCreationLog;

	private final ICreateTargetQueries fDelegate;

	/**
	 * Creates a new monitoring create target queries.
	 * 
	 * @param delegate
	 *            the delegate
	 * @param log
	 *            the creation log
	 */
	public MonitoringCreateTargetQueries(ICreateTargetQueries delegate, CreateTargetExecutionLog log) {
		fDelegate= delegate;
		fCreationLog= log;
	}

	/**
	 * {@inheritDoc}
	 */
	public ICreateTargetQuery createNewPackageQuery() {
		return new ICreateTargetQuery() {

			public Object getCreatedTarget(Object selection) {
				final Object target= fDelegate.createNewPackageQuery().getCreatedTarget(selection);
				fCreationLog.markAsCreated(selection, target);
				return target;
			}

			public String getNewButtonLabel() {
				return fDelegate.createNewPackageQuery().getNewButtonLabel();
			}
		};
	}
}
