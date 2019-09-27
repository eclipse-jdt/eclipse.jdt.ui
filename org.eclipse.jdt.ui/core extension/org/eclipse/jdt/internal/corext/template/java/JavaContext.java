/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Lars Vogel  <lars.vogel@gmail.com> - [templates][content assist] Ctrl+Space without any starting letter shows to no templates - https://bugs.eclipse.org/406463
 *     Lukas Hanke <hanke@yatta.de> - [templates][content assist] Content assist for 'for' loop should suggest member variables - https://bugs.eclipse.org/117215
 *     Nicolaj Hoess <nicohoess@gmail.com> - Make some internal methods accessible to help Postfix Code Completion plug-in - https://bugs.eclipse.org/433500
 *     Microsoft Corporation - moved template related code to jdt.core.manipulation - https://bugs.eclipse.org/549989
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitCompletion.Variable;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.template.contentassist.MultiVariable;
import org.eclipse.jdt.internal.ui.text.template.contentassist.MultiVariableGuess;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;


/**
 * A context for Java source.
 */
public class JavaContext extends CompilationUnitContext implements IJavaContext {

	private JavaContextCore fJavaContextCore;

	/**
	 * Creates a java template context.
	 *
	 * @param type   the context type.
	 * @param document the document.
	 * @param completionOffset the completion offset within the document.
	 * @param completionLength the completion length.
	 * @param compilationUnit the compilation unit (may be <code>null</code>).
	 */
	public JavaContext(TemplateContextType type, IDocument document, int completionOffset, int completionLength, ICompilationUnit compilationUnit) {
		super(type, document, completionOffset, completionLength, compilationUnit);
		this.fJavaContextCore = new JavaContextCore(this, type, document, completionOffset, completionLength, compilationUnit);
	}

	/**
	 * Creates a java template context.
	 *
	 * @param type   the context type.
	 * @param document the document.
	 * @param completionPosition the position defining the completion offset and length
	 * @param compilationUnit the compilation unit (may be <code>null</code>).
	 * @since 3.2
	 */
	public JavaContext(TemplateContextType type, IDocument document, Position completionPosition, ICompilationUnit compilationUnit) {
		super(type, document, completionPosition, compilationUnit);
		this.fJavaContextCore = new JavaContextCore(this, type, document, completionPosition, compilationUnit);
	}

	/**
	 * Adds a context type that is also compatible. That means the context can also process templates of that context type.
	 *
	 * @param contextTypeId the context type to accept
	 */
	@Override
	public void addCompatibleContextType(String contextTypeId) {
		this.fJavaContextCore.addCompatibleContextType(contextTypeId);
	}


	/**
	 * Returns the indentation level at the position of code completion.
	 *
	 * @return the indentation level at the position of the code completion
	 */
	private int getIndentation() {
		int start= getStart();
		IDocument document= getDocument();
		try {
			IRegion region= document.getLineInformationOfOffset(start);
			String lineContent= document.get(region.getOffset(), region.getLength());
			IJavaProject project= getJavaProject();
			return Strings.computeIndentUnits(lineContent, project);
		} catch (BadLocationException e) {
			return 0;
		}
	}

	/*
	 * @see TemplateContext#evaluate(Template template)
	 */
	@Override
	public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {
		TemplateBuffer buffer= this.fJavaContextCore.evaluate(template);

		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		boolean useCodeFormatter= prefs.getBoolean(PreferenceConstants.TEMPLATES_USE_CODEFORMATTER);

		IJavaProject project= getJavaProject();
		JavaFormatter formatter= new JavaFormatter(TextUtilities.getDefaultLineDelimiter(getDocument()), getIndentation(), useCodeFormatter, project);
		formatter.format(buffer, this);

		return buffer;
	}

	/*
	 * @see TemplateContext#canEvaluate(Template templates)
	 */
	@Override
	public boolean canEvaluate(Template template) {
		return this.fJavaContextCore.canEvaluate(template);
	}

	/*
	 * @see DocumentTemplateContext#getCompletionPosition();
	 */
	@Override
	public int getStart() {
		return this.fJavaContextCore.getStart();
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.DocumentTemplateContext#getEnd()
	 */
	@Override
	public int getEnd() {
		return this.fJavaContextCore.getEnd();
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.DocumentTemplateContext#getKey()
	 */
	@Override
	public String getKey() {
		return this.fJavaContextCore.getKey();
	}

	/**
	 * Returns the character before the start position of the completion.
	 *
	 * @return the character before the start position of the completion
	 */
	public char getCharacterBeforeStart() {
		return this.fJavaContextCore.getCharacterBeforeStart();
	}

	@Override
	public void handleException(Exception e) {
		String title= JavaTemplateMessages.JavaContext_error_title;
		if (e instanceof CoreException)
			ExceptionHandler.handle((CoreException)e, null, title, null);
		else if (e instanceof InvocationTargetException)
			ExceptionHandler.handle((InvocationTargetException)e, null, title, null);
		else {
			JavaPlugin.log(e);
			String message= e.getMessage();
			if (message == null) {
				message= JavaTemplateMessages.JavaContext_unexpected_error_message;
			}
			MessageDialog.openError(null, title, message);
		}
	}

	/**
	 * Returns the names of arrays available in the current {@link CompilationUnit}'s scope.
	 *
	 * @return the names of local arrays available in the current {@link CompilationUnit}'s scope
	 */
	@Override
	public Variable[] getArrays() {
		return this.fJavaContextCore.getArrays();
	}

	/**
	 * Returns the names of local variables matching <code>type</code>.
	 *
	 * @param type the type of the variables
	 * @return the names of local variables matching <code>type</code>
	 * @since 3.3
	 */
	@Override
	public Variable[] getLocalVariables(String type) {
		return this.fJavaContextCore.getLocalVariables(type);
	}

	/**
	 * Returns the names of fields matching <code>type</code>.
	 *
	 * @param type the type of the fields
	 * @return the names of fields matching <code>type</code>
	 * @since 3.3
	 */
	@Override
	public Variable[] getFields(String type) {
		return this.fJavaContextCore.getFields(type);
	}

	/**
	 * Returns the names of iterables or arrays available in the current {@link CompilationUnit}'s scope.
	 *
	 * @return the names of iterables or arrays available in the current {@link CompilationUnit}'s scope
	 */
	@Override
	public Variable[] getIterables() {
		return this.fJavaContextCore.getIterables();
	}

	@Override
	public void markAsUsed(String name) {
		this.fJavaContextCore.markAsUsed(name);
	}

	@Override
	public String[] suggestVariableNames(String type) throws IllegalArgumentException {
		return this.fJavaContextCore.suggestVariableNames(type);
	}

	String[] computeExcludes() {
		return this.fJavaContextCore.computeExcludes();
	}

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
	@Override
	public String addImport(String type) {
		return this.fJavaContextCore.addImport(type);
	}

	/**
	 * Adds a static import for the member with name <code>qualifiedMemberName</code>. The member is
	 * either a static field or a static method or a '*' to import all static members of a type.
	 *
	 * @param qualifiedMemberName the fully qualified name of the member to import or a qualified type
	 * 			name plus a '.*' suffix.
	 * @return returns either the simple member name if the import was successful or else the qualified name.
	 * @since 3.4
	 */
	public String addStaticImport(String qualifiedMemberName) {
		return this.fJavaContextCore.addStaticImport(qualifiedMemberName);
	}

	@Override
	public TemplateVariable getTemplateVariable(String name) {
		return this.fJavaContextCore.getTemplateVariable(name);
	}

	/**
	 * Adds a multi-variable guess dependency.
	 *
	 * @param master the master variable - <code>slave</code> needs to be updated when
	 *        <code>master</code> changes
	 * @param slave the dependent variable
	 * @since 3.3
	 */
	@Override
	public void addDependency(MultiVariable master, MultiVariable slave) {
		MultiVariableGuess guess= getMultiVariableGuess();
		if (guess == null) {
			guess= new MultiVariableGuess();
			setMultiVariableGuess(guess);
		}

		guess.addDependency(master, slave);
	}

}
