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

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * @author sib
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ControlStatementsTabPage extends ModifyDialogTabPage {
	
	private final String PREVIEW=
	"/**\n" +
	"* If...else\n" +
	"*/\n\n" +
	"class Example {" +
	"  void bar() {" +
	"    do {} while (true);" +
	"  }" +
	"  void foo2() {" +
	"    if (true) { " +
	"      return;" +
	"    }" +
	"    if (true) {" +
	"      return;" +
	"    } else if (false) {" +
	"      return; " +
	"    } else {" +
	"      return;" +
	"    }" +
	"  }" +
	"  void foo(int state) {" +
	"    if (true) return;" +
	"    if (true) " +
	"      return;" +
	"    else if (false)" +
	"      return;" +
	"    else return;" +
	"  }" +
	"}";
	
	
	
	private final String [] NOTINSERT_INSERT= {
				JavaCore.DO_NOT_INSERT,
						JavaCore.INSERT
	}; 
	

	private final int numColumns= 4; 
	
	
	
	protected CheckboxPreference fThenStatementPref, fSimpleIfPref;
	
	/**
	 * @param workingValues
	 */
	public ControlStatementsTabPage(Map workingValues) {
		super(workingValues);
		fJavaPreview.setPreviewText(PREVIEW);
	}

	protected Composite doCreatePreferences(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		
		final Group generalGroup= createGroup(numColumns, composite, "General");
		createOption(generalGroup, numColumns, "Insert new line in &control statements", DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_CONTROL_STATEMENTS, NOTINSERT_INSERT);
		
		final Group ifElseGroup= createGroup(numColumns, composite, "I&f...else");
		fThenStatementPref= createOption(ifElseGroup, numColumns, "Keep 't&hen' statement on same line", DefaultCodeFormatterConstants.FORMATTER_KEEP_THEN_STATEMENT_ON_SAME_LINE, falseTrue);
		
		Label l= new Label(ifElseGroup, SWT.NONE);
		GridData gd= new GridData();
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(4);
		l.setLayoutData(gd);
		
		fSimpleIfPref= createOption(ifElseGroup, numColumns - 1, "Keep &simple 'if' on one line", DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_IF_ON_ONE_LINE, falseTrue);
		
		fThenStatementPref.addObserver( new Observer() {
			public void update(Observable o, Object arg) {
				fSimpleIfPref.setEnabled(!fThenStatementPref.getChecked());
			}
			
		});
		
		createOption(ifElseGroup, numColumns, "Keep 'else' st&atement on same line", DefaultCodeFormatterConstants.FORMATTER_KEEP_ELSE_STATEMENT_ON_SAME_LINE, falseTrue);
		createCheckboxPref(ifElseGroup, numColumns, "&Keep 'else if' on one line", DefaultCodeFormatterConstants.FORMATTER_COMPACT_ELSE_IF, falseTrue);
		createCheckboxPref(ifElseGroup, numColumns, "Keep guardian cla&use on one line", DefaultCodeFormatterConstants.FORMATTER_FORMAT_GUARDIAN_CLAUSE_ON_ONE_LINE, falseTrue);
		
		return composite;
	}
	
	private CheckboxPreference createOption(Composite composite, int span, String name, String key, String [] values) {
		return createCheckboxPref(composite, span, name, key, values);
	}
}
