/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.ltk.core.refactoring.Refactoring;

/**
 * Interface for description providers used in conjunction with
 * {@link ExpandableSettingSection}
 * 
 * @since 3.2
 */
public interface IDescriptionProvider {

	/**
	 * Returns a description of the settings used for the specified refactoring.
	 * 
	 * @param refactoring
	 *            the refactoring
	 * @return A non-empty human-readable description, or <code>null</code>
	 */
	public String getDescription(Refactoring refactoring);
}