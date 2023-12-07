/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package org.eclipse.jdt.core.manipulation.internal.javadoc;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Javadoc;

public interface IJavadocContentFactory {
	public interface IJavadocAccess {
		public String toHTML();

		public CharSequence getMainDescription();

		public CharSequence getReturnDescription();

		public CharSequence getExceptionDescription(String simpleName);

		public CharSequence getInheritedTypeParamDescription(int typeParamIndex);

		public CharSequence getInheritedParamDescription(int paramIndex) throws JavaModelException;

	}

	public IJavadocAccess createJavadocAccess(IJavaElement element, Javadoc javadoc, String source, JavadocLookup lookup);
}
