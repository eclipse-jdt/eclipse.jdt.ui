/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class NLSSearchMessages extends NLS {

	private static final String BUNDLE_NAME= NLSSearchMessages.class.getName();

	private NLSSearchMessages() {
		// Do not instantiate
	}

	public static String Search_Error_javaElementAccess_title;
	public static String Search_Error_javaElementAccess_message;
	public static String Search_Error_createJavaElement_title;
	public static String Search_Error_createJavaElement_message;
	public static String SearchElementSelectionDialog_title;
	public static String SearchElementSelectionDialog_message;
	public static String SearchOperation_singularLabelPostfix;
	public static String SearchOperation_pluralLabelPatternPostfix;
	public static String NLSSearchResultCollector_unusedKeys;
	public static String NLSSearchResultRequestor_searching;
	public static String NLSSearchResultCollector_duplicateKeys;
	public static String NLSSearchPage_Error_createTypeDialog_message;
	public static String NLSSearchPage_Error_createTypeDialog_title;
	public static String NLSSearchPage_propertyFileGroup_text;
	public static String NLSSearchPage_propertyFileBrowseButton_text;
	public static String NLSSearchPage_propertiesFileSelectionDialog_title;
	public static String NLSSearchPage_propertiesFileSelectionDialog_message;
	public static String NLSSearchPage_wrapperClassGroup_text;
	public static String NLSSearchPage_wrapperClassBrowseButton_text;
	public static String NLSSearchPage_wrapperClassDialog_title;
	public static String NLSSearchPage_wrapperClassDialog_message;
	public static String WorkspaceScope;
	public static String WorkingSetScope;
	public static String SelectionScope;
	public static String EnclosingProjectsScope;
	public static String EnclosingProjectScope;
	public static String NLSSearchQuery_label;
	public static String NLSSearchQuery_wrapperNotExists;
	public static String NLSSearchQuery_propertiesNotExists;

	static {
		NLS.initializeMessages(BUNDLE_NAME, NLSSearchMessages.class);
	}
}