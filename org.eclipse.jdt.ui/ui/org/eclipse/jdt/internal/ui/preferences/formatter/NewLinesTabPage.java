/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;


public class NewLinesTabPage extends ModifyDialogTabPage {
	
	private final static String fPreview= 
	createPreviewHeader(FormatterMessages.getString("NewLinesTabPage.preview.header")) + //$NON-NLS-1$
	"private class Empty {}\n" + //$NON-NLS-1$
	"class Example {" + //$NON-NLS-1$
	"  static int [] fArray= {1, 2, 3, 4, 5 };" + //$NON-NLS-1$
	"  Listener fListener= new Listener() {" + //$NON-NLS-1$
	"  };" + //$NON-NLS-1$
	"  void bar() {}" + //$NON-NLS-1$
	"  void foo() {" + //$NON-NLS-1$
	"    ;;" + //$NON-NLS-1$
	"    do {} while (false);" + //$NON-NLS-1$
	"    for (;;) {}" + //$NON-NLS-1$
	"  }" + //$NON-NLS-1$
	"}"; //$NON-NLS-1$

	
	private final static String [] NOTINSERT_INSERT= {
	    JavaCore.DO_NOT_INSERT,
	    JavaCore.INSERT
	};
	

	private final static int NUM_COLUMNS= 4; 
	
	protected CheckboxPreference fThenStatementPref, fSimpleIfPref;
	

	public NewLinesTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
		fJavaPreview.setPreviewText(fPreview);
	}

	protected Composite doCreatePreferences(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(NUM_COLUMNS, false));
		
		final Group newlinesGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("NewLinesTabPage.newlines_group.title")); //$NON-NLS-1$
		createPref(newlinesGroup, "NewLinesTabPage.newlines_group.option.empty_class_body", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION, NOTINSERT_INSERT); //$NON-NLS-1$
		createPref(newlinesGroup, "NewLinesTabPage.newlines_group.option.empty_anonymous_class_body", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION, NOTINSERT_INSERT); //$NON-NLS-1$
		createPref(newlinesGroup, "NewLinesTabPage.newlines_group.option.empty_method_body", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_METHOD_BODY, NOTINSERT_INSERT); //$NON-NLS-1$
		createPref(newlinesGroup, "NewLinesTabPage.newlines_group.option.empty_block", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK, NOTINSERT_INSERT); //$NON-NLS-1$
		createPref(newlinesGroup, "NewLinesTabPage.newlines_group.option.after_opening_brace_of_array_initializer", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER, NOTINSERT_INSERT); //$NON-NLS-1$
		createPref(newlinesGroup, "NewLinesTabPage.newlines_group.option.before_closing_brace_of_array_initializer", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, NOTINSERT_INSERT); //$NON-NLS-1$
		
		final Group emptyStatementsGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("NewLinesTabPage.empty_statement_group.title")); //$NON-NLS-1$
		createPref(emptyStatementsGroup, "NewLinesTabPage.emtpy_statement_group.option.empty_statement_on_new_line", DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, FALSE_TRUE); //$NON-NLS-1$
		createPref(emptyStatementsGroup, "NewLinesTabPage.empty_statement_group.option.remove_unnecessary_semicolon", DefaultCodeFormatterConstants.FORMATTER_REMOVE_UNNECESSARY_SEMICOLON, FALSE_TRUE); //$NON-NLS-1$
		
		final Group userGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("NewLinesTabPage.existing_breaks_group.title")); //$NON-NLS-1$
		createPref(userGroup, "NewLinesTabPage.existing_breaks_group.preserve_existing_line_breaks", DefaultCodeFormatterConstants.FORMATTER_PRESERVE_USER_LINEBREAKS, FALSE_TRUE); //$NON-NLS-1$
	
		return composite;
	}
	
	private CheckboxPreference createPref(Composite composite, String messagesKey, String key, String [] values) {
		return createCheckboxPref(composite, NUM_COLUMNS, FormatterMessages.getString(messagesKey), key, values);
	}
}
