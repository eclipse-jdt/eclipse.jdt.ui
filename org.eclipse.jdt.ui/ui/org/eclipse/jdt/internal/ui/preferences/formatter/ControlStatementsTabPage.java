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
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;


public class ControlStatementsTabPage extends ModifyDialogTabPage {
	
	private final static String fPreview=
	createPreviewHeader(FormatterMessages.getString("ControlStatementsTabPage.preview.header")) + //$NON-NLS-1$
	"class Example {" +	//$NON-NLS-1$	
	"  void bar() {" +	//$NON-NLS-1$
	"    do {} while (true);" +	//$NON-NLS-1$
	"  }" +	//$NON-NLS-1$
	"  void foo2() {" +	//$NON-NLS-1$
	"    if (true) { " + //$NON-NLS-1$
	"      return;" + //$NON-NLS-1$
	"    }" + //$NON-NLS-1$
	"    if (true) {" + //$NON-NLS-1$
	"      return;" + //$NON-NLS-1$
	"    } else if (false) {" +	//$NON-NLS-1$
	"      return; " + //$NON-NLS-1$
	"    } else {" + //$NON-NLS-1$
	"      return;" + //$NON-NLS-1$
	"    }" + //$NON-NLS-1$
	"  }" + //$NON-NLS-1$
	"  void foo(int state) {" + //$NON-NLS-1$
	"    if (true) return;" + //$NON-NLS-1$
	"    if (true) " + //$NON-NLS-1$
	"      return;" + //$NON-NLS-1$
	"    else if (false)" + //$NON-NLS-1$
	"      return;" + //$NON-NLS-1$
	"    else return;" + //$NON-NLS-1$
	"  }" + //$NON-NLS-1$
	"}"; //$NON-NLS-1$
	
	
	
	private final static String [] NOTINSERT_INSERT= {
	    JavaCore.DO_NOT_INSERT,
	    JavaCore.INSERT
	}; 
	

	private final static int NUM_COLUMNS= 4; 
	
	
	
	protected CheckboxPreference fThenStatementPref, fSimpleIfPref;

	
	public ControlStatementsTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
		fJavaPreview.setPreviewText(fPreview);
	}

	protected Composite doCreatePreferences(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(NUM_COLUMNS, false));
		
		final Group generalGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("ControlStatementsTabPage.general_group.title")); //$NON-NLS-1$
		createOption(generalGroup, NUM_COLUMNS, FormatterMessages.getString("ControlStatementsTabPage.general_group.insert_new_line_in_control_statements"), DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_CONTROL_STATEMENTS, NOTINSERT_INSERT); //$NON-NLS-1$
		
		final Group ifElseGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("ControlStatementsTabPage.if_else_group.title")); //$NON-NLS-1$
		fThenStatementPref= createOption(ifElseGroup, NUM_COLUMNS, FormatterMessages.getString("ControlStatementsTabPage.if_else_group.keep_then_on_same_line"), DefaultCodeFormatterConstants.FORMATTER_KEEP_THEN_STATEMENT_ON_SAME_LINE, FALSE_TRUE); //$NON-NLS-1$
		
		Label l= new Label(ifElseGroup, SWT.NONE);
		GridData gd= new GridData();
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(4);
		l.setLayoutData(gd);
		
		fSimpleIfPref= createOption(ifElseGroup, NUM_COLUMNS - 1, FormatterMessages.getString("ControlStatementsTabPage.if_else_group.keep_simple_if_on_one_line"), DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_IF_ON_ONE_LINE, FALSE_TRUE); //$NON-NLS-1$
		
		fThenStatementPref.addObserver( new Observer() {
			public void update(Observable o, Object arg) {
				fSimpleIfPref.setEnabled(!fThenStatementPref.getChecked());
			}
			
		});
		
		fSimpleIfPref.setEnabled(!fThenStatementPref.getChecked());
		
		createOption(ifElseGroup, NUM_COLUMNS, FormatterMessages.getString("ControlStatementsTabPage.if_else_group.keep_else_on_same_line"), DefaultCodeFormatterConstants.FORMATTER_KEEP_ELSE_STATEMENT_ON_SAME_LINE, FALSE_TRUE); //$NON-NLS-1$
		createCheckboxPref(ifElseGroup, NUM_COLUMNS, FormatterMessages.getString("ControlStatementsTabPage.if_else_group.keep_else_if_on_one_line"), DefaultCodeFormatterConstants.FORMATTER_COMPACT_ELSE_IF, FALSE_TRUE); //$NON-NLS-1$
		createCheckboxPref(ifElseGroup, NUM_COLUMNS, FormatterMessages.getString("ControlStatementsTabPage.if_else_group.keep_guardian_clause_on_one_line"), DefaultCodeFormatterConstants.FORMATTER_KEEP_GUARDIAN_CLAUSE_ON_ONE_LINE, FALSE_TRUE); //$NON-NLS-1$
		
		return composite;
	}
	
	private CheckboxPreference createOption(Composite composite, int span, String name, String key, String [] values) {
		return createCheckboxPref(composite, span, name, key, values);
	}
}
