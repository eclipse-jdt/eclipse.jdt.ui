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
	
	private final String PREVIEW=
	createPreviewHeader("Braces") +
	"class Empty {}\n" +
	"\n" +
	"class Example {" +
	"  SomeClass fField= new SomeClass() {" +
	"  };" +
	"  int [] myArray= {1,2,3,4,5,6};" +
	"  void bar(int p) {" +
	"    for (int i= 0; i<10; i++) {" +
	"    }" +
	"    switch(p) {" +
	"      case 0:" +
	"        fField.set(0);" +
	"        break;" +
	"      default:" +
	"        fField.reset();" +
	"    }" +
	"  }" +
	"  void foo() {}" +
	"}";
	
	
	
	private final String [] BRACE_POSITIONS= {
				DefaultCodeFormatterConstants.END_OF_LINE,
						DefaultCodeFormatterConstants.NEXT_LINE,
				DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED
	};
	
	private final String [] BRACE_POSITION_NAMES= {"Same line", "Next line", "Next line indented"};
	
	private final int numColumns= 4; 

	
	/**
	 * Create a new BracesTabPage.
	 * 
	 * @param workingValues The map wherein the options are stored.
	 */
	public BracesTabPage(Map workingValues) {
		super(workingValues);
		fJavaPreview.setPreviewText(PREVIEW);
	}

	protected Composite doCreatePreferences(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		
		final Group bracesGroup= createGroup(numColumns, composite, "Brace positions");
		createBracesCombo(bracesGroup, "&Class declaration:", DefaultCodeFormatterConstants.FORMATTER_TYPE_DECLARATION_BRACE_POSITION);
		createBracesCombo(bracesGroup, "Anon&ymous class declaration:", DefaultCodeFormatterConstants.FORMATTER_ANONYMOUS_TYPE_DECLARATION_BRACE_POSITION);
		createBracesCombo(bracesGroup, "Met&hod declaration:", DefaultCodeFormatterConstants.FORMATTER_METHOD_DECLARATION_BRACE_POSITION);
		createBracesCombo(bracesGroup, "Bloc&ks:", DefaultCodeFormatterConstants.FORMATTER_BLOCK_BRACE_POSITION);
		createBracesCombo(bracesGroup, "'s&witch...case' statement:", DefaultCodeFormatterConstants.FORMATTER_SWITCH_BRACE_POSITION);
		
		/**
		 * TODO: take it in once defaultcodeformatterconstants is updated.
		 * createBracesCombo(bracesGroup, "&Array initializer:", DefaultCodeFormatterConstants.FORMATTER_ARRAY_INITIALIZER_BRACE_POSITION);
		 */
		
		return composite;
	}
	
	private void createBracesCombo(Composite composite, String name, String key) {
		createComboPref(composite, numColumns, name, key, BRACE_POSITIONS, BRACE_POSITION_NAMES);
	}
}
