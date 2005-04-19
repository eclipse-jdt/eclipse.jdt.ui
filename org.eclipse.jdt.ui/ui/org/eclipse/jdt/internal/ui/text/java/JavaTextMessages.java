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
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class JavaTextMessages extends NLS {

	private static final String BUNDLE_NAME= JavaTextMessages.class.getName();

	private JavaTextMessages() {
		// Do not instantiate
	}

	public static String CompletionProcessor_error_accessing_title;
	public static String CompletionProcessor_error_accessing_message;
	public static String CompletionProcessor_error_notOnBuildPath_title;
	public static String CompletionProcessor_error_notOnBuildPath_message;
	public static String ExperimentalProposal_error_msg;
	public static String ParameterGuessingProposal_error_msg;
	public static String ProposalInfo_more_to_come;
	public static String GetterSetterCompletionProposal_getter_label;
	public static String GetterSetterCompletionProposal_setter_label;
	public static String MethodCompletionProposal_constructor_label;
	public static String MethodCompletionProposal_method_label;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavaTextMessages.class);
	}
}