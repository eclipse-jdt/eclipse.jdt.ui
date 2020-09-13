/*******************************************************************************
 * Copyright (c) 2019 Microsoft Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Microsoft Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jface.text.templates.TemplateVariable;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.template.java.CompilationUnitCompletion.Variable;

import org.eclipse.jdt.internal.ui.text.template.contentassist.MultiVariable;

public interface IJavaContext {

	/**
	 * Adds a context type that is also compatible. That means the context can also process templates of that context type.
	 *
	 * @param contextTypeId the context type to accept
	 */
	void addCompatibleContextType(String contextTypeId);

	/**
	 * Returns the compilation unit if one is associated with this context,
	 * <code>null</code> otherwise.
	 *
	 * @return the compilation unit of this context or <code>null</code>
	 */
	ICompilationUnit getCompilationUnit();

	/**
	 * Exception handler when generate the template
	 * @param e Exception
	 */
	void handleException(Exception e);

	/**
	 * Returns the names of arrays available in the current {@link CompilationUnit}'s scope.
	 *
	 * @return the names of local arrays available in the current {@link CompilationUnit}'s scope
	 */
	Variable[] getArrays();

	/**
	 * Returns the names of local variables matching <code>type</code>.
	 *
	 * @param type the type of the variables
	 * @return the names of local variables matching <code>type</code>
	 * @since 3.3
	 */
	Variable[] getLocalVariables(String type);

	/**
	 * Returns the names of fields matching <code>type</code>.
	 *
	 * @param type the type of the fields
	 * @return the names of fields matching <code>type</code>
	 * @since 3.3
	 */
	Variable[] getFields(String type);

	/**
	 * Returns the names of iterables or arrays available in the current {@link CompilationUnit}'s scope.
	 *
	 * @return the names of iterables or arrays available in the current {@link CompilationUnit}'s scope
	 */
	Variable[] getIterables();

	/**
	 * Marks the name as used.
	 * @param name the name to be marked
	 */
	void markAsUsed(String name);

	/**
	 * Return the suggested names matching <code>type</code>
	 * @param type the type of the variable
	 * @return the suggested names matching <code>type</code>
	 * @throws IllegalArgumentException Exception
	 */
	String[] suggestVariableNames(String type) throws IllegalArgumentException;

	/**
	 * Adds an import for type with type name <code>type</code> if possible.
	 * Returns a string which can be used to reference the type.
	 *
	 * @param type the fully qualified name of the type to import
	 * @return returns a type to which the type binding can be assigned to.
	 * 	The returned type contains is unqualified when an import could be added or was already known.
	 * 	It is fully qualified, if an import conflict prevented the import.
	 * @since 3.4
	 */
	String addImport(String type);

	/**
	 * Return the template variable matching <code>name</code>
	 * @param name the variable name
	 * @return the template variable matching <code>name</code>
	 */
	TemplateVariable getTemplateVariable(String name);

	/**
	 * Adds a multi-variable guess dependency.
	 *
	 * @param master the master variable - <code>slave</code> needs to be updated when
	 *        <code>master</code> changes
	 * @param slave the dependent variable
	 * @since 3.3
	 */
	void addDependency(MultiVariable master, MultiVariable slave);
}
