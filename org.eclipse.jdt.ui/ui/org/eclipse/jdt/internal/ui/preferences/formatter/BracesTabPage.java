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


public class BracesTabPage extends ModifyDialogTabPage {
	
	private final static String fPreview=
	createPreviewHeader(FormatterMessages.getString("BracesTabPage.preview.header")) + //$NON-NLS-1$
	"class Empty {}\n" + //$NON-NLS-1$
	"\n" + //$NON-NLS-1$
	"class Example {" + //$NON-NLS-1$
	"  SomeClass fField= new SomeClass() {" + //$NON-NLS-1$
	"  };" + //$NON-NLS-1$
	"  int [] myArray= {1,2,3,4,5,6};" + //$NON-NLS-1$
	"  void bar(int p) {" + //$NON-NLS-1$
	"    for (int i= 0; i<10; i++) {" + //$NON-NLS-1$
	"    }" + //$NON-NLS-1$
	"    switch(p) {" + //$NON-NLS-1$
	"      case 0:" + //$NON-NLS-1$
	"        fField.set(0);" + //$NON-NLS-1$
	"        break;" + //$NON-NLS-1$
	"      default:" + //$NON-NLS-1$
	"        fField.reset();" + //$NON-NLS-1$
	"    }" + //$NON-NLS-1$
	"  }" + //$NON-NLS-1$
	"  void foo() {}" + //$NON-NLS-1$
	"}"; //$NON-NLS-1$
	
	
	
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

	
	private final static int NUM_COLUMNS= 4;
	
	
	/**
	 * Create a new BracesTabPage.
	 * 
	 * @param workingValues The map wherein the options are stored.
	 */
	public BracesTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
		fJavaPreview.setPreviewText(fPreview);
	}
	
	protected Composite doCreatePreferences(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(NUM_COLUMNS, false));
		
		final Group bracesGroup= createGroup(NUM_COLUMNS, composite, FormatterMessages.getString("BracesTabPage.group.brace_positions.title")); //$NON-NLS-1$
		createExtendedBracesCombo(bracesGroup, "BracesTabPage.option.class_declaration", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION); //$NON-NLS-1$
		createExtendedBracesCombo(bracesGroup, "BracesTabPage.option.anonymous_class_declaration", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION); //$NON-NLS-1$
		createExtendedBracesCombo(bracesGroup, "BracesTabPage.option.method_declaration", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION); //$NON-NLS-1$
		createExtendedBracesCombo(bracesGroup, "BracesTabPage.option.blocks", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK); //$NON-NLS-1$
		createBracesCombo(bracesGroup, "BracesTabPage.option.switch_case", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH); //$NON-NLS-1$
		createBracesCombo(bracesGroup, "BracesTabPage.option.array_initializer", DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER); //$NON-NLS-1$
//		createExtendedBracesCombo(bracesGroup, "BracesTabPage.option.class_declaration", DefaultCodeFormatterConstants.FORMATTER_TYPE_DECLARATION_BRACE_POSITION); //$NON-NLS-1$
//		createExtendedBracesCombo(bracesGroup, "BracesTabPage.option.anonymous_class_declaration", DefaultCodeFormatterConstants.FORMATTER_ANONYMOUS_TYPE_DECLARATION_BRACE_POSITION); //$NON-NLS-1$
//		createExtendedBracesCombo(bracesGroup, "BracesTabPage.option.method_declaration", DefaultCodeFormatterConstants.FORMATTER_METHOD_DECLARATION_BRACE_POSITION); //$NON-NLS-1$
//		createExtendedBracesCombo(bracesGroup, "BracesTabPage.option.blocks", DefaultCodeFormatterConstants.FORMATTER_BLOCK_BRACE_POSITION); //$NON-NLS-1$
//		createBracesCombo(bracesGroup, "BracesTabPage.option.switch_case", DefaultCodeFormatterConstants.FORMATTER_SWITCH_BRACE_POSITION); //$NON-NLS-1$
//		createBracesCombo(bracesGroup, "BracesTabPage.option.array_initializer", DefaultCodeFormatterConstants.FORMATTER_ARRAY_INITIALIZER_BRACE_POSITION); //$NON-NLS-1$
		
		return composite;
	}
	
		private void createBracesCombo(Composite composite, String messagesKey, String key) {
		createComboPref(composite, NUM_COLUMNS, FormatterMessages.getString(messagesKey), key, fBracePositions, fBracePositionNames);
	}

	private void createExtendedBracesCombo(Composite composite, String messagesKey, String key) {
		createComboPref(composite, NUM_COLUMNS, FormatterMessages.getString(messagesKey), key, fExtendedBracePositions, fExtendedBracePositionNames);
	}

}
