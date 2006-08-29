/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.osgi.util.NLS;

public final class CodeGenerationMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationMessages";//$NON-NLS-1$

	private CodeGenerationMessages() {
		// Do not instantiate
	}

	public static String AddGetterSetterOperation_description;
	public static String AddImportsOperation_description;
	public static String AddImportsOperation_error_notresolved_message;
	public static String AddImportsOperation_error_importclash;
	public static String AddImportsOperation_error_private_class;
	public static String AddUnimplementedMethodsOperation_description;
	public static String AddCustomConstructorOperation_description;
	public static String OrganizeImportsOperation_description;
	public static String AddJavaDocStubOperation_description;
	public static String AddDelegateMethodsOperation_monitor_message;
	public static String ImportsStructure_operation_description;
	public static String GenerateHashCodeEqualsOperation_description;

	static {
		NLS.initializeMessages(BUNDLE_NAME, CodeGenerationMessages.class);
	}

	public static String GenerateHashCodeEqualsOperation_hash_code_comment;
	public static String GenerateHashCodeEqualsOperation_tag_param;
	public static String GenerateHashCodeEqualsOperation_hash_code_argument;
	public static String GenerateHashCodeEqualsOperation_tag_return;
	public static String GenerateHashCodeEqualsOperation_return_comment;
}
