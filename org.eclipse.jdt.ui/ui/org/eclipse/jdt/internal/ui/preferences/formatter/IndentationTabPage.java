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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;


public class IndentationTabPage extends ModifyDialogTabPage {
	
	private final static String PREVIEW=
	createPreviewHeader(FormatterMessages.getString("IndentationTabPage.preview.header")) + //$NON-NLS-1$
	"class Example {" +	//$NON-NLS-1$
	"  int [] myArray= {1,2,3,4,5,6};" + //$NON-NLS-1$
	"  int theInt= 1;" + //$NON-NLS-1$
	"  String someString= \"Hello\";" + //$NON-NLS-1$
	"  double aDouble= 3.0;" + //$NON-NLS-1$
	"  void foo(int a, int b, int c, int d, int e, int f) {" + //$NON-NLS-1$
	"    switch(a) {" + //$NON-NLS-1$
	"    case 0: " + //$NON-NLS-1$
	"      Other.doFoo();" + //$NON-NLS-1$
	"      break;" + //$NON-NLS-1$
	"    default:" + //$NON-NLS-1$
	"      Other.doBaz();" + //$NON-NLS-1$
	"    }" + //$NON-NLS-1$
	"  }" + //$NON-NLS-1$
	"  void bar(List v) {" + //$NON-NLS-1$
	"    for (int i= 0; i < 10; i++) {" + //$NON-NLS-1$
 	"      v.add(new Integer(i));" + //$NON-NLS-1$
 	"    }" + //$NON-NLS-1$
	"  }" + //$NON-NLS-1$
	"}"; //$NON-NLS-1$
	
	private CompilationUnitPreview fPreview;
	
	public IndentationTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
	}

	protected void doCreatePreferences(Composite composite, int numColumns) {

		final Group generalGroup= createGroup(numColumns, composite, FormatterMessages.getString("IndentationTabPage.general_group.title")); //$NON-NLS-1$
		createNumberPref(generalGroup, numColumns, FormatterMessages.getString("IndentationTabPage.general_group.option.tab_size"), DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, 0, 999); //$NON-NLS-1$
		createCheckboxPref(generalGroup, numColumns, FormatterMessages.getString("IndentationTabPage.general_group.option.use_tab_char"), DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, new String [] {JavaCore.SPACE, JavaCore.TAB}); //$NON-NLS-1$
		
		final Group typeMemberGroup= createGroup(numColumns, composite, FormatterMessages.getString("IndentationTabPage.field_alignment_group.title")); //$NON-NLS-1$
		createCheckboxPref(typeMemberGroup, numColumns, FormatterMessages.getString("IndentationTabPage.field_alignment_group.align_fields_in_columns"), DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS, FALSE_TRUE); //$NON-NLS-1$
		
		final Group classGroup = createGroup(numColumns, composite, "Indentation settings"); //$NON-NLS-1$
		createCheckboxPref(classGroup, numColumns, FormatterMessages.getString("IndentationTabPage.class_group.option.indent_declarations_within_class_body"), DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER, FALSE_TRUE); //$NON-NLS-1$

//		final Group blockGroup= createGroup(numColumns, composite, FormatterMessages.getString("IndentationTabPage.block_group.title")); //$NON-NLS-1$
		createCheckboxPref(classGroup, numColumns, FormatterMessages.getString("IndentationTabPage.block_group.option.indent_statements_within_blocks_and_methods"), DefaultCodeFormatterConstants.FORMATTER_INDENT_BLOCK_STATEMENTS, FALSE_TRUE); //$NON-NLS-1$
		
//		final Group switchGroup= createGroup(numColumns, composite, FormatterMessages.getString("IndentationTabPage.switch_group.title")); //$NON-NLS-1$
		createCheckboxPref(classGroup, numColumns, FormatterMessages.getString("IndentationTabPage.switch_group.option.indent_statements_within_switch_body"), DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH, FALSE_TRUE); //$NON-NLS-1$
		createCheckboxPref(classGroup, numColumns, FormatterMessages.getString("IndentationTabPage.switch_group.option.indent_statements_within_case_body"), DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES, FALSE_TRUE); //$NON-NLS-1$
		createCheckboxPref(classGroup, numColumns, FormatterMessages.getString("IndentationTabPage.switch_group.option.indent_break_statements"), DefaultCodeFormatterConstants.FORMATTER_INDENT_BREAKS_COMPARE_TO_CASES, FALSE_TRUE); //$NON-NLS-1$
	}
	
	public void initializePage() {
	    fPreview.setPreviewText(PREVIEW);
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage#doCreateJavaPreview(org.eclipse.swt.widgets.Composite)
     */
    protected JavaPreview doCreateJavaPreview(Composite parent) {
        fPreview= new CompilationUnitPreview(fWorkingValues, parent);
        return fPreview;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage#doUpdatePreview()
     */
    protected void doUpdatePreview() {
        fPreview.update();
    }
}
