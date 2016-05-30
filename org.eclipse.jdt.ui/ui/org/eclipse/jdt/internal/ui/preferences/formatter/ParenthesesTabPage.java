/*******************************************************************************
 * Copyright (c) 2016 Mateusz Matela and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

public class ParenthesesTabPage extends FormatterTabPage {

	private final String PREVIEW=
	createPreviewHeader(FormatterMessages.ParenthesesTabPage_preview_header) +
	"public class Example {\n" +  //$NON-NLS-1$
	"	enum SomeEnum {\n" +  //$NON-NLS-1$
	"		VALUE1(), VALUE2(\"example\")\n" +  //$NON-NLS-1$
	"	}\n" +  //$NON-NLS-1$
	"	@SomeAnnotation(key1 = \"value1\", key2 = \"value2\")\n" +  //$NON-NLS-1$
	"	void method1() {\n" +  //$NON-NLS-1$
	"		for (int counter = 0; counter < 100; counter++) {\n" +  //$NON-NLS-1$
	"			if (counter % 2 == 0 && counter % 7 == 0 && counter % 13 == 0) {\n" +  //$NON-NLS-1$
	"				try (AutoCloseable resource = null) {\n" +  //$NON-NLS-1$
	"					// read resource\n" +  //$NON-NLS-1$
	"				} catch (Exception e) {\n" +  //$NON-NLS-1$
	"					e.printStackTrace();\n" +  //$NON-NLS-1$
	"				}\n" +  //$NON-NLS-1$
	"			}\n" +  //$NON-NLS-1$
	"		}\n" +  //$NON-NLS-1$
	"	}\n" +  //$NON-NLS-1$
	"	@Deprecated()\n" +  //$NON-NLS-1$
	"	void method2(String argument\n" +  //$NON-NLS-1$
	"	) {\n" +  //$NON-NLS-1$
	"		this.method3(this, this, this, \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\", \"bbbbbbbbbbbbbbbbbbbbbbbbbbbbb\",\n" +  //$NON-NLS-1$
	"				\"ccccccccccccccccccc\");\n" +  //$NON-NLS-1$
	"		do {\n" +  //$NON-NLS-1$
	"			this.method1();\n" +  //$NON-NLS-1$
	"		} while (this.toString()//\n" +  //$NON-NLS-1$
	"				.contains(argument));\n" +  //$NON-NLS-1$
	"	}\n" +  //$NON-NLS-1$
	"	void method3(\n" +  //$NON-NLS-1$
	"			Example argument1, Example argument2, Example argument3, String argument4, String argument5,\n" +  //$NON-NLS-1$
	"			String argument6) {\n" +  //$NON-NLS-1$
	"		method1();\n" +  //$NON-NLS-1$
	"		while (argument1.toString().contains(argument4)\n" +  //$NON-NLS-1$
	"		) {\n" +  //$NON-NLS-1$
	"			argument1.method2(argument5);\n" +  //$NON-NLS-1$
	"		}\n" +  //$NON-NLS-1$
	"	}\n" +  //$NON-NLS-1$
	"	java.util.function.BiConsumer<Integer, Integer> lambda = (Integer a, Integer b) -> {\n" +  //$NON-NLS-1$
	"		switch (a.intValue()) {\n" +  //$NON-NLS-1$
	"			case 0:\n" +  //$NON-NLS-1$
	"				break;\n" +  //$NON-NLS-1$
	"		}\n" +  //$NON-NLS-1$
	"	};\n" +  //$NON-NLS-1$
	"}"; //$NON-NLS-1$

	private CompilationUnitPreview fPreview;


	private final String[] fPositions= {
		DefaultCodeFormatterConstants.COMMON_LINES,
		DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED,
		DefaultCodeFormatterConstants.SEPARATE_LINES,
		DefaultCodeFormatterConstants.PRESERVE_POSITIONS,
	};

	private final String[] fPositionNames= {
		FormatterMessages.ParenthesesTabPage_positions_common_lines,
		FormatterMessages.ParenthesesTabPage_positions_separate_lines_if_wrapped,
		FormatterMessages.ParenthesesTabPage_positions_separate_lines,
		FormatterMessages.ParenthesesTabPage_positions_preserve_positions,
	};

	private final String[] fPositionsWithEmpty= {
		DefaultCodeFormatterConstants.COMMON_LINES,
		DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED,
		DefaultCodeFormatterConstants.SEPARATE_LINES_IF_NOT_EMPTY,
		DefaultCodeFormatterConstants.SEPARATE_LINES,
		DefaultCodeFormatterConstants.PRESERVE_POSITIONS,
	};

	private final String[] fPositionNamesWithEmpty= {
		FormatterMessages.ParenthesesTabPage_positions_common_lines,
		FormatterMessages.ParenthesesTabPage_positions_separate_lines_if_wrapped,
		FormatterMessages.ParenthesesTabPage_positions_separate_lines_if_not_empty,
		FormatterMessages.ParenthesesTabPage_positions_separate_lines,
		FormatterMessages.ParenthesesTabPage_positions_preserve_positions,
	};

	/**
	 * Creates a new ParenthesesTabPage.
	 * 
	 * @param modifyListener the modification listener
	 * @param workingValues the working values
	 */
	public ParenthesesTabPage(IModificationListener modifyListener, Map<String, String> workingValues) {
		super(modifyListener, workingValues);
	}

	@Override
	protected void doCreatePreferences(Composite composite, int numColumns) {
		final Group group= createGroup(numColumns, composite, FormatterMessages.ParenthesesTabPage_group_parentheses_positions);
		createParensCombo(group, numColumns, true, FormatterMessages.ParenthesesTabPage_option_method_declaration, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_METHOD_DECLARATION);
		createParensCombo(group, numColumns, true, FormatterMessages.ParenthesesTabPage_option_method_invocation, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_METHOD_INVOCATION);
		createParensCombo(group, numColumns, true, FormatterMessages.ParenthesesTabPage_option_enum_constant_declaration, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_ENUM_CONSTANT_DECLARATION);
		createParensCombo(group, numColumns, true, FormatterMessages.ParenthesesTabPage_option_annotation, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_ANNOTATION);
		createParensCombo(group, numColumns, true, FormatterMessages.ParenthesesTabPage_option_lambda_declaration, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_LAMBDA_DECLARATION);
		createParensCombo(group, numColumns, false, FormatterMessages.ParenthesesTabPage_option_if_while_statement, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_IF_WHILE_STATEMENT);
		createParensCombo(group, numColumns, false, FormatterMessages.ParenthesesTabPage_option_for_statement, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_FOR_STATEMENT);
		createParensCombo(group, numColumns, false, FormatterMessages.ParenthesesTabPage_option_switch_statement, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_SWITCH_STATEMENT);
		createParensCombo(group, numColumns, false, FormatterMessages.ParenthesesTabPage_option_try_clause, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_TRY_CLAUSE);
		createParensCombo(group, numColumns, false, FormatterMessages.ParenthesesTabPage_option_catch_clause, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_CATCH_CLAUSE);
	}

	@Override
	protected void initializePage() {
		fPreview.setPreviewText(PREVIEW);
	}

	@Override
	protected JavaPreview doCreateJavaPreview(Composite parent) {
		fPreview= new CompilationUnitPreview(fWorkingValues, parent);
		return fPreview;
	}

	private ComboPreference createParensCombo(Composite composite, int numColumns, boolean includeEmpty, String message, String key) {
		return createComboPref(composite, numColumns, message, key, includeEmpty ? fPositionsWithEmpty : fPositions, includeEmpty ? fPositionNamesWithEmpty : fPositionNames);
	}

	@Override
	protected void doUpdatePreview() {
		super.doUpdatePreview();
		fPreview.update();
	}
}
