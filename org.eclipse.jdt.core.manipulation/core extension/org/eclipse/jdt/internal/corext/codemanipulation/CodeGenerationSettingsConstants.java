/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
 *     Red Hat Inc. - constants moved from PreferenceConstants here
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

public class CodeGenerationSettingsConstants {

	/**
	 * A named preference that controls if comment stubs will be added
	 * automatically to newly created types and methods.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String CODEGEN_ADD_COMMENTS= "org.eclipse.jdt.ui.javadoc"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the keyword "this" will be added
	 * automatically to field accesses in generated methods.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String CODEGEN_KEYWORD_THIS= "org.eclipse.jdt.ui.keywordthis"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether to add a override annotation for newly created methods
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String CODEGEN_USE_OVERRIDE_ANNOTATION= "org.eclipse.jdt.ui.overrideannotation"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether to use markdown comments (Java 23 and up)
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String CODEGEN_USE_MARKDOWN= "org.eclipse.jdt.ui.usemarkdown"; //$NON-NLS-1$

	/**
	 * A named preferences that controls if types that start with a lower case letters get added by the
	 * "Organize Import" operation.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String ORGIMPORTS_IGNORELOWERCASE= "org.eclipse.jdt.ui.ignorelowercasenames"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether existing on-demand (".*") imports are preserved as
	 * on-demand imports by the "Organize Imports" operation, instead of being expanded into single
	 * imports.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String ORGIMPORTS_KEEP_EXISTING_ONDEMAND= "org.eclipse.jdt.ui.organizeimports.keepexistingondemand"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the "Organize Imports" operation may collapse single
	 * imports into a new on-demand (".*") import once the on-demand threshold is reached. When
	 * disabled, the thresholds are ignored and no new on-demand import is created from single
	 * imports.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String ORGIMPORTS_COLLAPSE_TO_ONDEMAND= "org.eclipse.jdt.ui.organizeimports.collapsetoondemand"; //$NON-NLS-1$

	private CodeGenerationSettingsConstants() {
	}


}
