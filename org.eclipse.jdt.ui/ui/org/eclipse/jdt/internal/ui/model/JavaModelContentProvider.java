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
package org.eclipse.jdt.internal.ui.model;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

/**
 * Content provider for Java models.
 * 
 * @since 3.2
 */
public final class JavaModelContentProvider extends StandardJavaElementContentProvider {

	/**
	 * Creates a new java model content provider.
	 */
	public JavaModelContentProvider() {
		super(true);
	}
}
