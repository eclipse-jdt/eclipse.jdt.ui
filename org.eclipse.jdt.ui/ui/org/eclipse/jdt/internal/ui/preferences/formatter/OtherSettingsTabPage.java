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

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;


public class OtherSettingsTabPage extends ModifyDialogTabPage {
	
	private final String preview=
		createPreviewHeader("Other Settings") +
		"class Example {" +
		"int theInt= 1;" +
		"String someString= \"Hello\";" +
		"double aDouble= 3.0;" +
		"void foo(int a, int b, int c, int d, int e, int f) {" +
		"}" +
		"}";
		
	
//	private final String [] lineDelimiterNames= {
//				"Unix", "Windows", "Mac"
//	};
//	
//	private final String [] lineDelimiters= {
//				"\n", "\r\n", "\r"
//	};
	
	private final String [] multiAlign= {
										 DefaultCodeFormatterConstants.FORMATTER_NO_ALIGNMENT,
										 DefaultCodeFormatterConstants.FORMATTER_MULTICOLUMN
	};
	
	private final int numColumns= 4;
	
	/**
	 * Create a new GeneralSettingsTabPage.
	 */
	public OtherSettingsTabPage(Map workingValues) {
		super(workingValues);
		fJavaPreview.setPreviewText(preview);
	}
	
	
	protected Composite doCreatePreferences(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		
		final Group generalGroup= createGroup(numColumns, composite, "General settings");
		createNumberPref(generalGroup, numColumns, "Maximum line &width:", DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, 0, Integer.MAX_VALUE);
		createNumberPref(generalGroup, numColumns, "Tab si&ze:", DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, 0, 999);
		createCheckboxPref(generalGroup, numColumns, "U&se tab character", DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, new String [] {JavaCore.SPACE, JavaCore.TAB});
		

		final Group typeMemberGroup= createGroup(numColumns, composite, "Alignment of fields in class declarations");
		createCheckboxPref(typeMemberGroup, numColumns, "Align fields in &columns", DefaultCodeFormatterConstants.FORMATTER_TYPE_MEMBER_ALIGNMENT, multiAlign);
		
		return composite;
	}
}
