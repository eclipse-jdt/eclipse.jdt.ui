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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;


public class NewLinesTabPage extends ModifyDialogTabPage {
	
	private final String PREVIEW= 
	createPreviewHeader(FormatterMessages.getString("NewLinesTabPage.preview.header")) + //$NON-NLS-1$
	"public class Empty {}\n" + //$NON-NLS-1$
	"class Example {" + //$NON-NLS-1$
	"  static int [] fArray= {1, 2, 3, 4, 5 };" + //$NON-NLS-1$
	"  Listener fListener= new Listener() {" + //$NON-NLS-1$
	"  };\n" + //$NON-NLS-1$
	"  // the following line contains line breaks\n// which can be preserved:\n" + //$NON-NLS-1$
	"  void\nbar\n()\n {}" + //$NON-NLS-1$
	"  void foo() {" + //$NON-NLS-1$
	"    ;;" + //$NON-NLS-1$
	"    do {} while (false);" + //$NON-NLS-1$
	"    for (;;) {}" + //$NON-NLS-1$
	"  }" + //$NON-NLS-1$
	"}"+ //$NON-NLS-1$
	"\n" + //$NON-NLS-1$
	"enum MyEnum {" + //$NON-NLS-1$
	"    UNDEFINED(0) { }" + //$NON-NLS-1$
	"}" + //$NON-NLS-1$
	"enum EmptyEnum { }";//$NON-NLS-1$
	
	private final String [] NOTINSERT_INSERT= {
	    JavaCore.DO_NOT_INSERT,
	    JavaCore.INSERT
	};
	

	protected CheckboxPreference fThenStatementPref, fSimpleIfPref;

	private CompilationUnitPreview fPreview;

	public NewLinesTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);
	}

	protected void doCreatePreferences(Composite composite, int numColumns) {
		
		final Group newlinesGroup= createGroup(numColumns, composite, FormatterMessages.getString("NewLinesTabPage.newlines_group.title")); //$NON-NLS-1$
		createPref(newlinesGroup, numColumns, "NewLinesTabPage.newlines_group.option.empty_class_body", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION, NOTINSERT_INSERT); //$NON-NLS-1$
		createPref(newlinesGroup, numColumns, "NewLinesTabPage.newlines_group.option.empty_anonymous_class_body", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION, NOTINSERT_INSERT); //$NON-NLS-1$
		createPref(newlinesGroup, numColumns, "NewLinesTabPage.newlines_group.option.empty_method_body", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_METHOD_BODY, NOTINSERT_INSERT); //$NON-NLS-1$
		createPref(newlinesGroup, numColumns, "NewLinesTabPage.newlines_group.option.empty_block", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK, NOTINSERT_INSERT); //$NON-NLS-1$
		createPref(newlinesGroup, numColumns, "NewLinesTabPage.newlines_group.option.empty_enum_declaration", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_DECLARATION, NOTINSERT_INSERT); //$NON-NLS-1$
		createPref(newlinesGroup, numColumns, "NewLinesTabPage.newlines_group.option.empty_enum_constant", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_CONSTANT, NOTINSERT_INSERT); //$NON-NLS-1$

		
		final Group arrayInitializerGroup= createGroup(numColumns, composite, FormatterMessages.getString("NewLinesTabPage.arrayInitializer_group.title")); //$NON-NLS-1$
		createPref(arrayInitializerGroup, numColumns, "NewLinesTabPage.array_group.option.after_opening_brace_of_array_initializer", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER, NOTINSERT_INSERT); //$NON-NLS-1$
		createPref(arrayInitializerGroup, numColumns, "NewLinesTabPage.array_group.option.before_closing_brace_of_array_initializer", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, NOTINSERT_INSERT); //$NON-NLS-1$

		
		final Group emptyStatementsGroup= createGroup(numColumns, composite, FormatterMessages.getString("NewLinesTabPage.empty_statement_group.title")); //$NON-NLS-1$
		createPref(emptyStatementsGroup, numColumns, "NewLinesTabPage.emtpy_statement_group.option.empty_statement_on_new_line", DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, FALSE_TRUE); //$NON-NLS-1$
	}
	
	protected void initializePage() {
	    fPreview.setPreviewText(PREVIEW);
	}
	
    protected JavaPreview doCreateJavaPreview(Composite parent) {
        fPreview= new CompilationUnitPreview(fWorkingValues, parent);
        return fPreview;
    }

    protected void doUpdatePreview() {
        fPreview.update();
    }

	private CheckboxPreference createPref(Composite composite, int numColumns, String messagesKey, String key, String [] values) {
		return createCheckboxPref(composite, numColumns, FormatterMessages.getString(messagesKey), key, values);
	}
}
