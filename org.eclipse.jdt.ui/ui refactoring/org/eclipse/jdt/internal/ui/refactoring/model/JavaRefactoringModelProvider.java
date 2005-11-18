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
package org.eclipse.jdt.internal.ui.refactoring.model;

import org.eclipse.ltk.core.refactoring.model.AbstractRefactoringModelProvider;

/**
 * Java refactoring-aware model provider.
 * 
 * @since 3.2
 */
public final class JavaRefactoringModelProvider extends AbstractRefactoringModelProvider {

	/** The model provider id */
	public static final String JAVA_REFACTORING_MODEL_PROVIDER_ID= "org.eclipse.jdt.ui.modelProvider"; //$NON-NLS-1$

	/**
	 * Creates a new java refactoring model provider.
	 */
	public JavaRefactoringModelProvider() {
		super(JAVA_REFACTORING_MODEL_PROVIDER_ID);
	}
}