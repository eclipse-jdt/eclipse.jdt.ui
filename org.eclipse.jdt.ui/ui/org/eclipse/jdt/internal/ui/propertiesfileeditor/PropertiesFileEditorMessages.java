/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import java.util.ResourceBundle;

import org.eclipse.osgi.util.NLS;


/**
 * Helper class to get NLSed messages.
 *
 * @since 3.1
 */
final class PropertiesFileEditorMessages extends NLS {

	private static final String BUNDLE_FOR_CONSTRUCTED_KEYS= "org.eclipse.jdt.internal.ui.propertiesfileeditor.ConstructedPropertiesFileEditorMessages";//$NON-NLS-1$
	private static ResourceBundle fgBundleForConstructedKeys= ResourceBundle.getBundle(BUNDLE_FOR_CONSTRUCTED_KEYS);

	/**
	 * Returns the message bundle which contains constructed keys.
	 *
	 * @since 3.1
	 * @return the message bundle
	 */
	public static ResourceBundle getBundleForConstructedKeys() {
		return fgBundleForConstructedKeys;
	}

	private static final String BUNDLE_NAME= PropertiesFileEditorMessages.class.getName();

	private PropertiesFileEditorMessages() {
		// Do not instantiate
	}

	public static String EscapeBackslashCompletionProposal_escapeBackslashes;
	public static String OpenAction_label;
	public static String OpenAction_tooltip;
	public static String OpenAction_error_title;
	public static String OpenAction_error_message;
	public static String OpenAction_error_messageArgs;
	public static String OpenAction_error_messageProblems;
	public static String OpenAction_error_messageErrorSearchingKey;
	public static String OpenAction_error_messageNoResult;
	public static String OpenAction_hyperlinkText;
	public static String OpenAction_SelectionDialog_title;
	public static String OpenAction_SelectionDialog_details;
	public static String OpenAction_SelectionDialog_message;
	public static String OpenAction_SelectionDialog_elementLabel;
	public static String OpenAction_SelectionDialog_elementLabelWithMatchCount;

	public static String PropertiesFileAutoEditStrategy_showQuickAssist;
	public static String PropertiesFileHover_MalformedEncoding;
	public static String EscapeBackslashCompletionProposal_unescapeBackslashes;

	static {
		NLS.initializeMessages(BUNDLE_NAME, PropertiesFileEditorMessages.class);
	}
}