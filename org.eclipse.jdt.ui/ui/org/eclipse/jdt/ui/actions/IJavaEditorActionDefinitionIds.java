/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * Defines the definition IDs for the Java editor actions.
 * 
 * <p>
 * This interface is not intended to be implemented or extended.
 * </p>.
 * 
 * @since 2.0
 */
public interface IJavaEditorActionDefinitionIds extends ITextEditorActionDefinitionIds {

	// edit

	/**
	 * Action definition ID of the edit -> go to matching bracket action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.goto.matching.bracket"</code>).
	 *
	 * @since 2.1
	 */
	public static final String GOTO_MATCHING_BRACKET= "org.eclipse.jdt.ui.edit.text.java.goto.matching.bracket"; //$NON-NLS-1$

	/**
	 * Action definition ID of the edit -> go to next member action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.goto.next.member"</code>).
	 *
	 * @since 2.1
	 */
	public static final String GOTO_NEXT_MEMBER= "org.eclipse.jdt.ui.edit.text.java.goto.next.member"; //$NON-NLS-1$

	/**
	 * Action definition ID of the edit -> go to previous member action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.goto.previous.member"</code>).
	 *
	 * @since 2.1
	 */
	public static final String GOTO_PREVIOUS_MEMBER= "org.eclipse.jdt.ui.edit.text.java.goto.previous.member"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> select enclosing action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.select.enclosing"</code>).
	 */
	public static final String SELECT_ENCLOSING= "org.eclipse.jdt.ui.edit.text.java.select.enclosing"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> select next action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.select.next"</code>).
	 */
	public static final String SELECT_NEXT= "org.eclipse.jdt.ui.edit.text.java.select.next"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> select previous action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.select.previous"</code>).
	 */
	public static final String SELECT_PREVIOUS= "org.eclipse.jdt.ui.edit.text.java.select.previous"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> select restore last action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.select.last"</code>).
	 */
	public static final String SELECT_LAST= "org.eclipse.jdt.ui.edit.text.java.select.last"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> correction assist proposal action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.correction.assist.proposals"</code>).
	 */
	public static final String CORRECTION_ASSIST_PROPOSALS= "org.eclipse.jdt.ui.edit.text.java.correction.assist.proposals"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> content assist proposal action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.content.assist.proposals"</code>).
	 */
	public static final String CONTENT_ASSIST_PROPOSALS= "org.eclipse.jdt.ui.edit.text.java.content.assist.proposals"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> content assist context information action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.content.assist.context.information"</code>).
	 */
	public static final String CONTENT_ASSIST_CONTEXT_INFORMATION= "org.eclipse.jdt.ui.edit.text.java.content.assist.context.information"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> show Javadoc action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.show.javadoc"</code>).
	 */
	public static final String SHOW_JAVADOC= "org.eclipse.jdt.ui.edit.text.java.show.javadoc"; //$NON-NLS-1$

	// source
	
	/**
	 * Action definition ID of the source -> comment action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.comment"</code>).
	 */
	public static final String COMMENT= "org.eclipse.jdt.ui.edit.text.java.comment"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> uncomment action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.uncomment"</code>).
	 */
	public static final String UNCOMMENT= "org.eclipse.jdt.ui.edit.text.java.uncomment"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> format action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.format"</code>).
	 */
	public static final String FORMAT= "org.eclipse.jdt.ui.edit.text.java.format"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> add import action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.add.import"</code>).
	 */
	public static final String ADD_IMPORT= "org.eclipse.jdt.ui.edit.text.java.add.import"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> organize imports action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.organize.imports"</code>).
	 */
	public static final String ORGANIZE_IMPORTS= "org.eclipse.jdt.ui.edit.text.java.organize.imports"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> surround with try/catch action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.surround.with.try.catch"</code>).
	 */
	public static final String SURROUND_WITH_TRY_CATCH= "org.eclipse.jdt.ui.edit.text.java.surround.with.try.catch"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> override methods action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.override.methods"</code>).
	 */
	public static final String OVERRIDE_METHODS= "org.eclipse.jdt.ui.edit.text.java.override.methods"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> add unimplemented constructors action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.add.unimplemented.constructors"</code>).
	 */
	public static final String ADD_UNIMPLEMENTED_CONTRUCTORS= "org.eclipse.jdt.ui.edit.text.java.add.unimplemented.constructors"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> create setter/getter action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.create.getter.setter"</code>).
	 */
	public static final String CREATE_GETTER_SETTER= "org.eclipse.jdt.ui.edit.text.java.create.getter.setter"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> externalize strings action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.externalize.strings"</code>).
	 */
	public static final String EXTERNALIZE_STRINGS= "org.eclipse.jdt.ui.edit.text.java.externalize.strings"; //$NON-NLS-1$
	
	/**
	 * Note: this id is for internal use only.
	 */
	public static final String SHOW_NEXT_PROBLEM= "org.eclipse.jdt.ui.edit.text.java.show.next.problem"; //$NON-NLS-1$
	
	/**
	 * Note: this id is for internal use only.
	 */
	public static final String SHOW_PREVIOUS_PROBLEM= "org.eclipse.jdt.ui.edit.text.java.show.previous.problem"; //$NON-NLS-1$

	// refactor
	
	/**
	 * Action definition ID of the refactor -> pull up action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.pull.up"</code>).
	 */
	public static final String PULL_UP= "org.eclipse.jdt.ui.edit.text.java.pull.up"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> rename element action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.rename.element"</code>).
	 */
	public static final String RENAME_ELEMENT= "org.eclipse.jdt.ui.edit.text.java.rename.element"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> modify method parameters action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.modify.method.parameters"</code>).
	 */
	public static final String MODIFY_METHOD_PARAMETERS= "org.eclipse.jdt.ui.edit.text.java.modify.method.parameters"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> move element action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.move.element"</code>).
	 */
	public static final String MOVE_ELEMENT= "org.eclipse.jdt.ui.edit.text.java.move.element"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> extract local variable action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.extract.local.variable"</code>).
	 */
	public static final String EXTRACT_LOCAL_VARIABLE= "org.eclipse.jdt.ui.edit.text.java.extract.local.variable"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> extract constant action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.extract.constant"</code>).
	 * 
	 * @since 2.1
	 */
	public static final String EXTRACT_CONSTANT= "org.eclipse.jdt.ui.edit.text.java.extract.constant"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> inline local variable action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.inline.local.variable"</code>).
	 */
	public static final String INLINE_LOCAL_VARIABLE= "org.eclipse.jdt.ui.edit.text.java.inline.local.variable"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> self encapsulate field action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.self.encapsulate.field"</code>).
	 */
	public static final String SELF_ENCAPSULATE_FIELD= "org.eclipse.jdt.ui.edit.text.java.self.encapsulate.field"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> extract method action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.extract.method"</code>).
	 */
	public static final String EXTRACT_METHOD= "org.eclipse.jdt.ui.edit.text.java.extract.method"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> inline method action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.inline.method"</code>).
	 * 
	 * @since 2.1
	 */
	public static final String INLINE_METHOD= "org.eclipse.jdt.ui.edit.text.java.inline.method"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> extract interface action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.extract.interface"</code>).
	 * 
	 * @since 2.1
	 */
	public static final String EXTRACT_INTERFACE= "org.eclipse.jdt.ui.edit.text.java.extract.interface"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> move inner type to top level action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.move.inner.to.top.level"</code>).
	 * 
	 * @since 2.1
	 */
	public static final String MOVE_INNER_TO_TOP= "org.eclipse.jdt.ui.edit.text.java.move.inner.to.top.level"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> use supertype action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.use.supertype"</code>).
	 * 
	 * @since 2.1
	 */
	public static final String USE_SUPERTYPE= "org.eclipse.jdt.ui.edit.text.java.use.supertype"; //$NON-NLS-1$

	// navigate
	
	/**
	 * Action definition ID of the navigate -> open action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.open.editor"</code>).
	 */
	public static final String OPEN_EDITOR= "org.eclipse.jdt.ui.edit.text.java.open.editor"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the navigate -> open super implementation action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.open.super.implementation"</code>).
	 */
	public static final String OPEN_SUPER_IMPLEMENTATION= "org.eclipse.jdt.ui.edit.text.java.open.super.implementation"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the navigate -> open external javadoc action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.open.external.javadoc"</code>).
	 */
	public static final String OPEN_EXTERNAL_JAVADOC= "org.eclipse.jdt.ui.edit.text.java.open.external.javadoc"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the navigate -> open type hierarchy action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.org.eclipse.jdt.ui.edit.text.java.open.type.hierarchy"</code>).
	 */
	public static final String OPEN_TYPE_HIERARCHY= "org.eclipse.jdt.ui.edit.text.java.open.type.hierarchy"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the navigate -> show in package explorer action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.show.in.package.view"</code>).
	 */
	public static final String SHOW_IN_PACKAGE_VIEW= "org.eclipse.jdt.ui.edit.text.java.show.in.package.view"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the navigate -> show in navigator action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.show.in.navigator.view"</code>).
	 */
	public static final String SHOW_IN_NAVIGATOR_VIEW= "org.eclipse.jdt.ui.edit.text.java.show.in.navigator.view"; //$NON-NLS-1$

	// search
	
	/**
	 * Action definition ID of the search -> references in workspace action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.references.in.workspace"</code>).
	 */
	public static final String SEARCH_REFERENCES_IN_WORKSPACE= "org.eclipse.jdt.ui.edit.text.java.search.references.in.workspace"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> references in hierarchy action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.references.in.hierarchy"</code>).
	 */
	public static final String SEARCH_REFERENCES_IN_HIERARCHY= "org.eclipse.jdt.ui.edit.text.java.search.references.in.hierarchy"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> references in working set action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.references.in.working.set"</code>).
	 */
	public static final String SEARCH_REFERENCES_IN_WORKING_SET= "org.eclipse.jdt.ui.edit.text.java.search.references.in.working.set"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> read access in workspace action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.read.access.in.workspace"</code>).
	 */
	public static final String SEARCH_READ_ACCESS_IN_WORKSPACE= "org.eclipse.jdt.ui.edit.text.java.search.read.access.in.workspace"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> read access in hierarchy action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.read.access.in.hierarchy"</code>).
	 */
	public static final String SEARCH_READ_ACCESS_IN_HIERARCHY= "org.eclipse.jdt.ui.edit.text.java.search.read.access.in.hierarchy"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> read access in working set action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.read.access.in.working.set"</code>).
	 */
	public static final String SEARCH_READ_ACCESS_IN_WORKING_SET= "org.eclipse.jdt.ui.edit.text.java.search.read.access.in.working.set"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> write access in workspace action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.write.access.in.workspace"</code>).
	 */
	public static final String SEARCH_WRITE_ACCESS_IN_WORKSPACE= "org.eclipse.jdt.ui.edit.text.java.search.write.access.in.workspace"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> write access in hierarchy action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.write.access.in.hierarchy"</code>).
	 */
	public static final String SEARCH_WRITE_ACCESS_IN_HIERARCHY= "org.eclipse.jdt.ui.edit.text.java.search.write.access.in.hierarchy"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> write access in working set action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.write.access.in.working.set"</code>).
	 */
	public static final String SEARCH_WRITE_ACCESS_IN_WORKING_SET= "org.eclipse.jdt.ui.edit.text.java.search.write.access.in.working.set"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> declarations in workspace action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.declarations.in.workspace"</code>).
	 */
	public static final String SEARCH_DECLARATIONS_IN_WORKSPACE= "org.eclipse.jdt.ui.edit.text.java.search.declarations.in.workspace"; //$NON-NLS-1$
	/**
	 * Action definition ID of the search -> declarations in hierarchy action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.declarations.in.hierarchy"</code>).
	 */
	public static final String SEARCH_DECLARATIONS_IN_HIERARCHY= "org.eclipse.jdt.ui.edit.text.java.search.declarations.in.hierarchy"; //$NON-NLS-1$
	/**
	 * Action definition ID of the search -> declarations in working set action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.declarations.in.working.set"</code>).
	 */
	public static final String SEARCH_DECLARATIONS_IN_WORKING_SET= "org.eclipse.jdt.ui.edit.text.java.search.declarations.in.working.set"; //$NON-NLS-1$
	/**
	 * Action definition ID of the search -> implementors in workspace action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.search.implementors.in.workspace"</code>).
	 */
	public static final String SEARCH_IMPLEMENTORS_IN_WORKSPACE= "org.eclipse.jdt.ui.edit.text.java.search.implementors.in.workspace"; //$NON-NLS-1$
	/**
	 * Action definition ID of the search -> implementors in working set action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.seach.implementors.in.working.set"</code>).
	 */
	public static final String SEARCH_IMPLEMENTORS_IN_WORKING_SET= "org.eclipse.jdt.ui.edit.text.java.seach.implementors.in.working.set"; //$NON-NLS-1$

	// miscellaneous
	
	/**
	 * Action definition ID of the toggle presentation toolbar button action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.toggle.presentation"</code>).
	 */
	public static final String TOGGLE_PRESENTATION= "org.eclipse.jdt.ui.edit.text.java.toggle.presentation"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the toggle text hover toolbar button action
	 * (value <code>"org.eclipse.jdt.ui.edit.text.java.toggle.text.hover"</code>).
	 */
	public static final String TOGGLE_TEXT_HOVER= "org.eclipse.jdt.ui.edit.text.java.toggle.text.hover"; //$NON-NLS-1$
}
