/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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

package org.eclipse.jdt.ui;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

/**
 * Gives access to the import rewrite configured with the settings as specified in the user interface.
 * These settings are kept in JDT UI for compatibility reasons.
 *
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 * </p>
 * @since 3.2
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CodeStyleConfiguration {

	private CodeStyleConfiguration() {
		// do not instantiate and subclass
	}


	/**
	 * Returns a {@link ImportRewrite} using {@link ImportRewrite#create(ICompilationUnit, boolean)} and
	 * configures the rewriter with the settings as specified in the JDT UI preferences.
	 *
	 * @param cu the compilation unit to create the rewriter on
	 * @param restoreExistingImports specifies if the existing imports should be kept or removed.
	 * @return the new rewriter configured with the settings as specified in the JDT UI preferences.
	 * @throws JavaModelException thrown when the compilation unit could not be accessed.
	 *
	 * @see ImportRewrite#create(ICompilationUnit, boolean)
	 */
	public static ImportRewrite createImportRewrite(ICompilationUnit cu, boolean restoreExistingImports) throws JavaModelException {
		return org.eclipse.jdt.core.manipulation.CodeStyleConfiguration.createImportRewrite(cu, restoreExistingImports);
	}

	/**
	 * Returns a {@link ImportRewrite} using {@link ImportRewrite#create(CompilationUnit, boolean)} and
	 * configures the rewriter with the settings as specified in the JDT UI preferences.
	 *
	 * @param astRoot the AST root to create the rewriter on
	 * @param restoreExistingImports specifies if the existing imports should be kept or removed.
	 * @return the new rewriter configured with the settings as specified in the JDT UI preferences.
	 *
	 * @see ImportRewrite#create(CompilationUnit, boolean)
	 */
	public static ImportRewrite createImportRewrite(CompilationUnit astRoot, boolean restoreExistingImports) {
		return org.eclipse.jdt.core.manipulation.CodeStyleConfiguration.createImportRewrite(astRoot, restoreExistingImports);
	}
}
