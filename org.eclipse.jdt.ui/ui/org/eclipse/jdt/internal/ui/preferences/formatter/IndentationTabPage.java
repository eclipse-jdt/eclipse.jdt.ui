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
	
	private final String PREVIEW=
	createPreviewHeader("Indentation") +
	"class Example {" +
	"  int [] myArray= {1,2,3,4,5,6};" +
	"  void foo(int parameter) {" +
	"    switch(parameter) {" +
	"    case 0: " +
	"      o.doFoo();" +
	"      break;" +
	"    default:" +
	"      o.doBaz();" +
	"    }" +
	"  }" +
	"  void bar(Vector v) {" +
	"    for (int i= 0; i < 10; i++) {" +
 	"      v.add(new Integer(i));" +
 	"    }" +
	"  }" +
	"}";
	
	private final int numColumns= 4; 
	
	public IndentationTabPage(Map workingValues) {
		super(workingValues);
		fJavaPreview.setPreviewText(PREVIEW);
	}

	protected Composite doCreatePreferences(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));

		final Group generalGroup= createGroup(numColumns, composite, "Indentation levels");
		createNumberPref(generalGroup, numColumns, "&Wrapped lines:", DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, 0, 9999);
		/**
		 * TODO: take it in once defaultcodeformatterconstants is updated.
		 *createNumberPref(generalGroup, numColumns, "&Array intitializers:", DefaultCodeFormatterConstants.FORMATTER_ARRAY_INITIALIZER_CONTINUATION_INDENTATION, 0, Integer.MAX_VALUE);
		 */
		final Group classGroup = createGroup(numColumns, composite, "Class body");
		createCheckboxPref(classGroup, numColumns, "Indent de&clarations within class body", DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER, falseTrue);

		final Group blockGroup= createGroup(numColumns, composite, "Block body");
		createCheckboxPref(blockGroup, numColumns, "Indent &statements within blocks and methods", DefaultCodeFormatterConstants.FORMATTER_INDENT_BLOCK_STATEMENTS, falseTrue);
		
		final Group switchGroup= createGroup(numColumns, composite, "Switch statement");
		createCheckboxPref(switchGroup, numColumns, "Indent statements wit&hin 'switch' body", DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES, falseTrue);
		createCheckboxPref(switchGroup, numColumns, "Indent statements within 'case' bod&y", DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH, falseTrue);
		createCheckboxPref(switchGroup, numColumns, "Indent 'brea&k' statements", DefaultCodeFormatterConstants.FORMATTER_INDENT_BREAKS_COMPARE_TO_CASES, falseTrue);
		
		return composite;
	}
}
