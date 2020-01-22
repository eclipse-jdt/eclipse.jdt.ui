/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.workingsets;


/**
 * Working set ID constants.
 *
 * @since 3.5
 */
public interface IWorkingSetIDs {

	/** Resource working set ID */
	String RESOURCE= "org.eclipse.ui.resourceWorkingSetPage"; //$NON-NLS-1$

	/** Java working set ID */
	String JAVA= "org.eclipse.jdt.ui.JavaWorkingSetPage"; //$NON-NLS-1$

	/** 'Other Projects' working set ID */
	String OTHERS= "org.eclipse.jdt.internal.ui.OthersWorkingSet"; //$NON-NLS-1$

	/** Dynamic Java sources working set ID */
	String DYNAMIC_SOURCES= "org.eclipse.jdt.internal.ui.DynamicSourcesWorkingSet"; //$NON-NLS-1$
}
