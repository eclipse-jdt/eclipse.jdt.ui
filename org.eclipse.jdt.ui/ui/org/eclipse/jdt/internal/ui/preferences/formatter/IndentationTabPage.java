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

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;


public class IndentationTabPage extends ModifyDialogTabPage {
	
	private final String fPreview=
	createPreviewHeader(FormatterMessages.getString("IndentationTabPage.preview.header")) + //$NON-NLS-1$
	"class Example {" +	//$NON-NLS-1$
	"  int [] myArray= {1,2,3,4,5,6};" + //$NON-NLS-1$
	"  void foo(int parameter) {" + //$NON-NLS-1$
	"    switch(parameter) {" + //$NON-NLS-1$
	"    case 0: " + //$NON-NLS-1$
	"      o.doFoo();" + //$NON-NLS-1$
	"      break;" + //$NON-NLS-1$
	"    default:" + //$NON-NLS-1$
	"      o.doBaz();" + //$NON-NLS-1$
	"    }" + //$NON-NLS-1$
	"  }" + //$NON-NLS-1$
	"  void bar(Vector v) {" + //$NON-NLS-1$
	"    for (int i= 0; i < 10; i++) {" + //$NON-NLS-1$
 	"      v.add(new Integer(i));" + //$NON-NLS-1$
 	"    }" + //$NON-NLS-1$
	"  }" + //$NON-NLS-1$
	"}"; //$NON-NLS-1$
	
	private final int NUM_COLUMNS= 4; 
	
	public IndentationTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
		fJavaPreview.setPreviewText(fPreview);
	}

	protected Composite doCreatePreferences(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(NUM_COLUMNS, false));

		final Group generalGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("IndentationTabPage.general_group.title")); //$NON-NLS-1$
		createNumberPref(generalGroup, NUM_COLUMNS, FormatterMessages.getString("IndentationTabPage.general_group.option.wrapped_lines"), DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, 0, 9999); //$NON-NLS-1$
		createNumberPref(generalGroup, NUM_COLUMNS, FormatterMessages.getString("IndentationTabPage.general_group.option.array_initializers"), DefaultCodeFormatterConstants.FORMATTER_ARRAY_INITIALIZER_CONTINUATION_INDENTATION, 0, Integer.MAX_VALUE); //$NON-NLS-1$

		final Group classGroup = createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("IndentationTabPage.class_group.title")); //$NON-NLS-1$
		createCheckboxPref(classGroup, NUM_COLUMNS, FormatterMessages.getString("IndentationTabPage.class_group.option.indent_declarations_within_class_body"), DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER, FALSE_TRUE); //$NON-NLS-1$

		final Group blockGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("IndentationTabPage.block_group.title")); //$NON-NLS-1$
		createCheckboxPref(blockGroup, NUM_COLUMNS, FormatterMessages.getString("IndentationTabPage.block_group.option.indent_statements_within_blocks_and_methods"), DefaultCodeFormatterConstants.FORMATTER_INDENT_BLOCK_STATEMENTS, FALSE_TRUE); //$NON-NLS-1$
		
		final Group switchGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("IndentationTabPage.switch_group.title")); //$NON-NLS-1$
		createCheckboxPref(switchGroup, NUM_COLUMNS, FormatterMessages.getString("IndentationTabPage.switch_group.option.indent_statements_within_switch_body"), DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES, FALSE_TRUE); //$NON-NLS-1$
		createCheckboxPref(switchGroup, NUM_COLUMNS, FormatterMessages.getString("IndentationTabPage.switch_group.option.indent_statements_within_case_body"), DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH, FALSE_TRUE); //$NON-NLS-1$
		createCheckboxPref(switchGroup, NUM_COLUMNS, FormatterMessages.getString("IndentationTabPage.switch_group.option.indent_break_statements"), DefaultCodeFormatterConstants.FORMATTER_INDENT_BREAKS_COMPARE_TO_CASES, FALSE_TRUE); //$NON-NLS-1$
		
		return composite;
	}
}
