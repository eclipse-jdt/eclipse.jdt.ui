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

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;


public class NewLinesTabPage extends ModifyDialogTabPage {
	
	private final String PREVIEW= 
	createPreviewHeader("New Lines") +
	"private class Empty {}\n" +
	"class Example {" +
	"  static int [] fArray= {1, 2, 3, 4, 5 };" +
	"  Listener fListener= new Listener() {" +
	"  };" +
	"  void bar() {}" +
	"  void foo() {" +
	"    ;;" +
	"    do {} while (false);" +
	"    for (;;) {}" +
	"  }" +
	"}";

	
	private final String [] NOTINSERT_INSERT= {
				JavaCore.DO_NOT_INSERT,
						JavaCore.INSERT
	};
	

	private final int numColumns= 4; 
	
	protected CheckboxPreference fThenStatementPref, fSimpleIfPref;
	

	public NewLinesTabPage(Map workingValues) {
		super(workingValues);
		fJavaPreview.setPreviewText(PREVIEW);
	}

	protected Composite doCreatePreferences(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		
		final Group newlinesGroup= createGroup(numColumns, composite, "Insert new line");
		createCheckboxPref(newlinesGroup, numColumns, "in empty &class body", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION, NOTINSERT_INSERT);
		createCheckboxPref(newlinesGroup, numColumns, "in empty &anonymous class body", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION, NOTINSERT_INSERT);
		createCheckboxPref(newlinesGroup, numColumns, "in empt&y method body", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_METHOD_BODY, NOTINSERT_INSERT);
		createCheckboxPref(newlinesGroup, numColumns, "in em&pty &block", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK, NOTINSERT_INSERT);
		createCheckboxPref(newlinesGroup, numColumns, "be&fore closing brace of array initializer", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, NOTINSERT_INSERT);
		
		final Group emptyStatementsGroup= createGroup(numColumns, composite, "Empty statements");
		createCheckboxPref(emptyStatementsGroup, numColumns, "Put empty &statement on new line", DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, falseTrue);
		
		final Group userGroup= createGroup(numColumns, composite, "Existing line breaks");
		createCheckboxPref(userGroup, numColumns, "Preserve e&xisting line breaks", DefaultCodeFormatterConstants.FORMATTER_PRESERVE_USER_LINEBREAKS, falseTrue);
		
	
		return composite;
	}
}
