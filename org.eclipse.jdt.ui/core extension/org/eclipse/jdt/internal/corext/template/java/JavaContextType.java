/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Sebastian Davids: sdavids@gmx.de - see bug 25376
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;


/**
 * The context type for templates inside Java code.
 * The same class is used for several context types:
 * <ul>
 * <li>templates for all Java code locations</li>
 * <li>templates for member locations</li>
 * <li>templates for statement locations</li>
 * <li>templates for module-info.java files</li>
 * </ul>
 */
public class JavaContextType extends AbstractJavaContextType {

	/**
	 * The context type id for templates working on all Java code locations
	 */
	public static final String ID_ALL= "java"; //$NON-NLS-1$

	/**
	 * The context type id for templates working on empty Java source files
	 */
	public static final String ID_EMPTY= "java-empty"; //$NON-NLS-1$

	/**
	 * The context type id for templates working on member locations
	 */
	public static final String ID_MEMBERS= "java-members"; //$NON-NLS-1$

	/**
	 * The context type id for templates working on statement locations
	 */
	public static final String ID_STATEMENTS= "java-statements"; //$NON-NLS-1$

	/**
	 * The context type id for templates working on module-info.java files
	 */
	public static final String ID_MODULE= "module"; //$NON-NLS-1$

	@Override
	protected void initializeContext(IJavaContext context) {
		// Separate 'module' context type from 'java' context type
		if (ID_MODULE.equals(getId()) || ID_EMPTY.equals(getId())) {
			return;
		}
		if (!JavaContextType.ID_ALL.equals(getId())) { // a specific context must also allow the templates that work everywhere
			context.addCompatibleContextType(JavaContextType.ID_ALL);
		}
	}

}
