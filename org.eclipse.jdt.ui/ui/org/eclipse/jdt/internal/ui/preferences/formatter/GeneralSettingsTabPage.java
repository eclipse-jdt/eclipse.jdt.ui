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

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * @author sib
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class GeneralSettingsTabPage extends ModifyDialogTabPage {
	
	private final String preview=
		"class Example {" +
		"int myInt= 1;" +
		"String myString= \"Hello\";" +
		"double myDouble= 3.0;" +
		"}";
		
	
	private final String [] lineDelimiterNames= {
				"Unix", "Windows", "Mac"
	};
	
	private final String [] lineDelimiters= {
				"\n", "\r\n", "\r"
	};
	
	private final String [] multiAlign= {
										 DefaultCodeFormatterConstants.FORMATTER_NO_ALIGNMENT,
										 DefaultCodeFormatterConstants.FORMATTER_MULTICOLUMN
	};
	
	private final int numColumns= 4;
	
	/**
	 * Create a new GeneralSettingsTabPage.
	 */
	public GeneralSettingsTabPage(Map workingValues) {
		super(workingValues);
		fJavaPreview.setPreviewText(preview);
	}
	
	
	protected Composite doCreatePreferences(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		
		final Group generalGroup= createGroup(numColumns, composite, "General settings");
		createNumberPref(generalGroup, numColumns, "Maximum line &width:", DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, 0, Integer.MAX_VALUE);
		createComboPref(generalGroup, numColumns, "Line delimiter for so&urce files:", DefaultCodeFormatterConstants.FORMATTER_LINE_SEPARATOR, lineDelimiters, lineDelimiterNames);

		final Group typeMemberGroup= createGroup(numColumns, composite, "Alignment of fields in class declarations");
		createCheckboxPref(typeMemberGroup, numColumns, "Align fields in columns:", DefaultCodeFormatterConstants.FORMATTER_TYPE_MEMBER_ALIGNMENT, multiAlign);
		
		return composite;
	}
}
