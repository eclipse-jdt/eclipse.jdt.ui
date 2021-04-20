/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.template.java;



/**
 * The context type for templates inside SWT code.
 * The same class is used for several context types:
 * <dl>
 * <li>templates for all Java code locations</li>
 * <li>templates for member locations</li>
 * <li>templates for statement locations</li>
 * </dl>
 * @since 3.4
 */
public class SWTContextType extends AbstractJavaContextType {

	/**
	 * The context type id for templates working on all Java code locations in SWT projects
	 */
	public static final String ID_ALL= "swt"; //$NON-NLS-1$

	/**
	 * The context type id for templates working on member locations in SWT projects
	 */
	public static final String ID_MEMBERS= "swt-members"; //$NON-NLS-1$

	/**
	 * The context type id for templates working on statement locations in SWT projects
	 */
	public static final String ID_STATEMENTS= "swt-statements"; //$NON-NLS-1$


	@Override
	protected void initializeContext(IJavaContext context) {
		if (!SWTContextType.ID_ALL.equals(getId())) { // a specific context must also allow the templates that work everywhere
			context.addCompatibleContextType(SWTContextType.ID_ALL);
		}
	}
}
