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
 * @since 2.0
 */
public interface IJavaEditorActionDefinitionIds extends ITextEditorActionDefinitionIds {

	// edit
	public static final String SELECT_ENCLOSING= "org.eclipse.jdt.ui.edit.text.java.select.enclosing"; //$NON-NLS-1$
	public static final String SELECT_NEXT= "org.eclipse.jdt.ui.edit.text.java.select.next"; //$NON-NLS-1$
	public static final String SELECT_PREVIOUS= "org.eclipse.jdt.ui.edit.text.java.select.previous"; //$NON-NLS-1$
	public static final String SELECT_LAST= "org.eclipse.jdt.ui.edit.text.java.select.last"; //$NON-NLS-1$
	public static final String CORRECTION_ASSIST_PROPOSALS= "org.eclipse.jdt.ui.edit.text.java.correction.assist.proposals"; //$NON-NLS-1$
	public static final String CONTENT_ASSIST_PROPOSALS= "org.eclipse.jdt.ui.edit.text.java.content.assist.proposals"; //$NON-NLS-1$
	public static final String CONTENT_ASSIST_CONTEXT_INFORMATION= "org.eclipse.jdt.ui.edit.text.java.content.assist.context.information"; //$NON-NLS-1$
	public static final String SHOW_JAVADOC= "org.eclipse.jdt.ui.edit.text.java.show.javadoc"; //$NON-NLS-1$

	// source
	public static final String COMMENT= "org.eclipse.jdt.ui.edit.text.java.comment"; //$NON-NLS-1$
	public static final String UNCOMMENT= "org.eclipse.jdt.ui.edit.text.java.uncomment"; //$NON-NLS-1$
	public static final String FORMAT= "org.eclipse.jdt.ui.edit.text.java.format"; //$NON-NLS-1$
	public static final String ADD_IMPORT= "org.eclipse.jdt.ui.edit.text.java.add.import"; //$NON-NLS-1$
	public static final String ORGANIZE_IMPORTS= "org.eclipse.jdt.ui.edit.text.java.organize.imports"; //$NON-NLS-1$
	public static final String SURROUND_WITH_TRY_CATCH= "org.eclipse.jdt.ui.edit.text.java.surround.with.try.catch"; //$NON-NLS-1$
	public static final String OVERRIDE_METHODS= "org.eclipse.jdt.ui.edit.text.java.override.methods"; //$NON-NLS-1$
	public static final String ADD_UNIMPLEMENTED_CONTRUCTORS= "org.eclipse.jdt.ui.edit.text.java.add.unimplemented.constructors"; //$NON-NLS-1$
	public static final String CREATE_GETTER_SETTER= "org.eclipse.jdt.ui.edit.text.java.create.getter.setter"; //$NON-NLS-1$
	public static final String EXTERNALIZE_STRINGS= "org.eclipse.jdt.ui.edit.text.java.externalize.strings"; //$NON-NLS-1$
	public static final String SHOW_NEXT_PROBLEM= "org.eclipse.jdt.ui.edit.text.java.show.next.problem"; //$NON-NLS-1$
	public static final String SHOW_PREVIOUS_PROBLEM= "org.eclipse.jdt.ui.edit.text.java.show.previous.problem"; //$NON-NLS-1$

	// refactor
	public static final String PULL_UP= "org.eclipse.jdt.ui.edit.text.java.pull.up"; //$NON-NLS-1$
	public static final String RENAME_ELEMENT= "org.eclipse.jdt.ui.edit.text.java.rename.element"; //$NON-NLS-1$
	public static final String MODIFY_METHOD_PARAMETERS= "org.eclipse.jdt.ui.edit.text.java.modify.method.parameters"; //$NON-NLS-1$
	public static final String MOVE_ELEMENT= "org.eclipse.jdt.ui.edit.text.java.move.element"; //$NON-NLS-1$
	public static final String EXTRACT_LOCAL_VARIABLE= "org.eclipse.jdt.ui.edit.text.java.extract.local.variable"; //$NON-NLS-1$
	public static final String INLINE_LOCAL_VARIABLE= "org.eclipse.jdt.ui.edit.text.java.inline.local.variable"; //$NON-NLS-1$
	public static final String SELF_ENCAPSULATE_FIELD= "org.eclipse.jdt.ui.edit.text.java.self.encapsulate.field"; //$NON-NLS-1$
	public static final String EXTRACT_METHOD= "org.eclipse.jdt.ui.edit.text.java.extract.method"; //$NON-NLS-1$

	// navigate
	public static final String OPEN_EDITOR= "org.eclipse.jdt.ui.edit.text.java.open.editor"; //$NON-NLS-1$
	public static final String OPEN_SUPER_IMPLEMENTATION= "org.eclipse.jdt.ui.edit.text.java.open.super.implementation"; //$NON-NLS-1$
	public static final String OPEN_EXTERNAL_JAVADOC= "org.eclipse.jdt.ui.edit.text.java.open.external.javadoc"; //$NON-NLS-1$
	public static final String OPEN_TYPE_HIERARCHY= "org.eclipse.jdt.ui.edit.text.java.open.type.hierarchy"; //$NON-NLS-1$
	public static final String SHOW_IN_PACKAGE_VIEW= "org.eclipse.jdt.ui.edit.text.java.show.in.package.view"; //$NON-NLS-1$
	public static final String SHOW_IN_NAVIGATOR_VIEW= "org.eclipse.jdt.ui.edit.text.java.show.in.navigator.view"; //$NON-NLS-1$

	// search
	public static final String SEARCH_REFERENCES_IN_WORKSPACE= "org.eclipse.jdt.ui.edit.text.java.search.references.in.workspace"; //$NON-NLS-1$
	public static final String SEARCH_REFERENCES_IN_HIERARCHY= "org.eclipse.jdt.ui.edit.text.java.search.references.in.hierarchy"; //$NON-NLS-1$
	public static final String SEARCH_REFERENCES_IN_WORKING_SET= "org.eclipse.jdt.ui.edit.text.java.search.references.in.working.set"; //$NON-NLS-1$
	public static final String SEARCH_READ_ACCESS_IN_WORKSPACE= "org.eclipse.jdt.ui.edit.text.java.search.read.access.in.workspace"; //$NON-NLS-1$
	public static final String SEARCH_READ_ACCESS_IN_HIERARCHY= "org.eclipse.jdt.ui.edit.text.java.search.read.access.in.hierarchy"; //$NON-NLS-1$
	public static final String SEARCH_READ_ACCESS_IN_WORKING_SET= "org.eclipse.jdt.ui.edit.text.java.search.read.access.in.working.set"; //$NON-NLS-1$
	public static final String SEARCH_WRITE_ACCESS_IN_WORKSPACE= "org.eclipse.jdt.ui.edit.text.java.search.write.access.in.workspace"; //$NON-NLS-1$
	public static final String SEARCH_WRITE_ACCESS_IN_HIERARCHY= "org.eclipse.jdt.ui.edit.text.java.search.write.access.in.hierarchy"; //$NON-NLS-1$
	public static final String SEARCH_WRITE_ACCESS_IN_WORKING_SET= "org.eclipse.jdt.ui.edit.text.java.search.write.access.in.working.set"; //$NON-NLS-1$
	public static final String SEARCH_DECLARATIONS_IN_WORKSPACE= "org.eclipse.jdt.ui.edit.text.java.search.declarations.in.workspace"; //$NON-NLS-1$
	public static final String SEARCH_DECLARATIONS_IN_HIERARCHY= "org.eclipse.jdt.ui.edit.text.java.search.declarations.in.hierarchy"; //$NON-NLS-1$
	public static final String SEARCH_DECLARATIONS_IN_WORKING_SET= "org.eclipse.jdt.ui.edit.text.java.search.declarations.in.working.set"; //$NON-NLS-1$
	public static final String SEARCH_IMPLEMENTORS_IN_WORKSPACE= "org.eclipse.jdt.ui.edit.text.java.search.implementors.in.workspace"; //$NON-NLS-1$
	public static final String SEARCH_IMPLEMENTORS_IN_WORKING_SET= "org.eclipse.jdt.ui.edit.text.java.seach.implementors.in.working.set"; //$NON-NLS-1$

	// miscellaneous
	public static final String TOGGLE_PRESENTATION= "org.eclipse.jdt.ui.edit.text.java.toggle.presentation"; //$NON-NLS-1$
	public static final String TOGGLE_TEXT_HOVER= "org.eclipse.jdt.ui.edit.text.java.toggle.text.hover"; //$NON-NLS-1$
	
}
