/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;


public class BracesTabPage extends ModifyDialogTabPage {
	
	private final static String PREVIEW=
	createPreviewHeader(FormatterMessages.getString("BracesTabPage.preview.header")) + //$NON-NLS-1$
	"class Empty {}\n" + //$NON-NLS-1$
	"\n" + //$NON-NLS-1$
	"class Example {" + //$NON-NLS-1$
	"  SomeClass fField= new SomeClass() {" + //$NON-NLS-1$
	"  };" + //$NON-NLS-1$
	"  int [] myArray= {1,2,3,4,5,6};" + //$NON-NLS-1$
	"  int [] emptyArray= new int[] {};" + //$NON-NLS-1$
	"  Example() {" + //$NON-NLS-1$
	"  }" + //$NON-NLS-1$
	"  void bar(int p) {" + //$NON-NLS-1$
	"    for (int i= 0; i<10; i++) {" + //$NON-NLS-1$
	"    }" + //$NON-NLS-1$
	"    switch(p) {" + //$NON-NLS-1$
	"      case 0:" + //$NON-NLS-1$
	"        fField.set(0);" + //$NON-NLS-1$
	"        break;" + //$NON-NLS-1$
	"      case 1: {" + //$NON-NLS-1$
	"        break;" + //$NON-NLS-1$
	"        }" + //$NON-NLS-1$
	"      default:" + //$NON-NLS-1$
	"        fField.reset();" + //$NON-NLS-1$
	"    }" + //$NON-NLS-1$
	"  }" + //$NON-NLS-1$
	"  void foo() {}" + //$NON-NLS-1$
	"}"; //$NON-NLS-1$
	
	
	private CompilationUnitPreview fPreview;
	
	
	private final static String [] fBracePositions= {
	    DefaultCodeFormatterConstants.END_OF_LINE,
	    DefaultCodeFormatterConstants.NEXT_LINE,
	    DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED
	};
	
	private final static String [] fExtendedBracePositions= {
		DefaultCodeFormatterConstants.END_OF_LINE,
	    DefaultCodeFormatterConstants.NEXT_LINE,
	    DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED, 
		DefaultCodeFormatterConstants.NEXT_LINE_ON_WRAP
	};
	
	private final static String [] fBracePositionNames= {
	    FormatterMessages.getString("BracesTabPage.position.same_line"), //$NON-NLS-1$
	    FormatterMessages.getString("BracesTabPage.position.next_line"), //$NON-NLS-1$
	    FormatterMessages.getString("BracesTabPage.position.next_line_indented") //$NON-NLS-1$
	};
	
	private final static String [] fExtendedBracePositionNames= {
	    FormatterMessages.getString("BracesTabPage.position.same_line"), //$NON-NLS-1$
	    FormatterMessages.getString("BracesTabPage.position.next_line"), //$NON-NLS-1$
	    FormatterMessages.getString("BracesTabPage.position.next_line_indented"), //$NON-NLS-1$
		FormatterMessages.getString("BracesTabPage.position.next_line_on_wrap") //$NON-NLS-1$
	};

	
	/**
	 * Create a new BracesTabPage.
	 */
	public BracesTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
	}
	
	protected void doCreatePreferences(Composite composite, int numColumns) {
		
		final Group group= createGroup(numColumns, composite, FormatterMessages.getString("BracesTabPage.group.brace_positions.title")); //$NON-NLS-1$
//	    createLabel(numColumns, group, "Brace settings", GridData.FILL_HORIZONTAL);
		createExtendedBracesCombo(group, numColumns, "BracesTabPage.option.class_declaration", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION); //$NON-NLS-1$
		createExtendedBracesCombo(group, numColumns, "BracesTabPage.option.anonymous_class_declaration", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION); //$NON-NLS-1$
		createExtendedBracesCombo(group, numColumns, "BracesTabPage.option.constructor_declaration", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION); //$NON-NLS-1$
		createExtendedBracesCombo(group, numColumns, "BracesTabPage.option.method_declaration", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION); //$NON-NLS-1$
		createExtendedBracesCombo(group, numColumns, "BracesTabPage.option.blocks", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK); //$NON-NLS-1$
		createExtendedBracesCombo(group, numColumns, "BracesTabPage.option.blocks_in_case", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK_IN_CASE); //$NON-NLS-1$
		createBracesCombo(group, numColumns, "BracesTabPage.option.switch_case", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH); //$NON-NLS-1$
		createBracesCombo(group, numColumns, "BracesTabPage.option.array_initializer", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER); //$NON-NLS-1$
		createIndentedCheckboxPref(group, numColumns, "BracesTabPage.option.keep_empty_array_initializer_on_one_line", DefaultCodeFormatterConstants.FORMATTER_KEEP_EMPTY_ARRAY_INITIALIZER_ON_ONE_LINE, FALSE_TRUE); //$NON-NLS-1$

	}
	
	protected void initializePage() {
	    fPreview.setPreviewText(PREVIEW);
	}
	
	protected JavaPreview doCreateJavaPreview(Composite parent) {
	    fPreview= new CompilationUnitPreview(fWorkingValues, parent);
	    return fPreview;
	}
	
	private void createBracesCombo(Composite composite, int numColumns, String messagesKey, String key) {
		createComboPref(composite, numColumns, FormatterMessages.getString(messagesKey), key, fBracePositions, fBracePositionNames);
	}

	private void createExtendedBracesCombo(Composite composite, int numColumns, String messagesKey, String key) {
		createComboPref(composite, numColumns, FormatterMessages.getString(messagesKey), key, fExtendedBracePositions, fExtendedBracePositionNames);
	}
	
	private CheckboxPreference createIndentedCheckboxPref(Composite composite, int numColumns, String messagesKey, String key, String [] values) {
		CheckboxPreference pref= createCheckboxPref(composite, numColumns, FormatterMessages.getString(messagesKey), key, values);
		GridData data= (GridData) pref.getControl().getLayoutData();
		data.horizontalIndent= fPixelConverter.convertWidthInCharsToPixels(1);
		return pref;
	}
	

    protected void doUpdatePreview() {
        fPreview.update();
    }

}
