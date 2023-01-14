/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.preferences;

import java.util.HashMap;
import java.util.Map;


/**
 * Keys for preferences store used in BCO
 */
public interface BCOConstants {
	/**
	 * toggle BCO "view content/selection follows editor selection"
	 */
	String LINK_VIEW_TO_EDITOR = "linkViewToEditor"; //$NON-NLS-1$

	/**
	 * toggle reference "view content/selection follows editor selection"
	 */
	String LINK_REF_VIEW_TO_EDITOR = "linkRefViewToEditor"; //$NON-NLS-1$

	/**
	 * show bytecode only for selected element in editor
	 */
	String SHOW_ONLY_SELECTED_ELEMENT = "showOnlySelectedElement"; //$NON-NLS-1$

	/**
	 * show ASMifier java code instead of bytecode
	 */
	String SHOW_ASMIFIER_CODE = "showASMifierCode"; //$NON-NLS-1$

	/**
	 * show ASMifier java code instead of bytecode in compare pane
	 */
	String DIFF_SHOW_ASMIFIER_CODE = "diff_showASMifierCode"; //$NON-NLS-1$

	/**
	 * show raw bytecode (without any additional help like readable class names etc)
	 */
	String SHOW_RAW_BYTECODE = "showRawBytecode"; //$NON-NLS-1$

	/**
	 * show line information (if available)
	 */
	String SHOW_LINE_INFO = "showLineInfo"; //$NON-NLS-1$

	/**
	 * show line information (if available) in compare pane
	 */
	String DIFF_SHOW_LINE_INFO = "diff_showLineInfo"; //$NON-NLS-1$

	/**
	 * show variables information (if available)
	 */
	String SHOW_VARIABLES = "showVariables"; //$NON-NLS-1$

	/**
	 * show variables information (if available) in compare pane
	 */
	String DIFF_SHOW_VARIABLES = "diff_showVariables"; //$NON-NLS-1$

	/**
	 * recalculate stackmap (to see computed frames, works for all classes even before MUSTANG)
	 */
	String SHOW_STACKMAP = "showStackmap"; //$NON-NLS-1$

	/**
	 * recalculate stackmap (to see computed frames, works for all classes even before MUSTANG) in
	 * compare
	 */
	String DIFF_SHOW_STACKMAP = "diff_showStackmap"; //$NON-NLS-1$

	/**
	 * expand stackmap frames
	 */
	String EXPAND_STACKMAP = "expandStackmap"; //$NON-NLS-1$

	/**
	 * expand stackmap frames in compare pane
	 */
	String DIFF_EXPAND_STACKMAP = "diff_expandStackmap"; //$NON-NLS-1$

	/**
	 * recalculate stackmap (to see computed frames, works for all classes even before MUSTANG)
	 */
	String RECALCULATE_STACKMAP = "recalculateStackmap"; //$NON-NLS-1$

	/**
	 * show "analyzer" - LVT and stack tables (for current bytecode selection)
	 */
	String SHOW_ANALYZER = "showAnalyzer"; //$NON-NLS-1$

	/**
	 * Show non decimal values for numeric constants in the bytecode
	 */
	String SHOW_HEX_VALUES = "showHexValues"; //$NON-NLS-1$

	int F_LINK_VIEW_TO_EDITOR = 0;

	int F_SHOW_ONLY_SELECTED_ELEMENT = 1;

	int F_SHOW_ASMIFIER_CODE = 2;

	int F_SHOW_RAW_BYTECODE = 3;

	int F_SHOW_LINE_INFO = 4;

	int F_SHOW_VARIABLES = 5;

	int F_RECALCULATE_STACKMAP = 6;

	int F_EXPAND_STACKMAP = 7;

	int F_SHOW_ANALYZER = 8;

	int F_SHOW_STACKMAP = 9;

	int F_SHOW_HEX_VALUES = 10;

	/**
	 * Value is Integer value from one of F_* constants, key is the String value of one of
	 * corresponding preference keys. It is not intended that the map would be modified by clients.
	 */
	Map<String, Integer> NAME_TO_FLAG_MAP = new ConstantsMap();

	final class ConstantsMap extends HashMap<String, Integer> {
		private static final long serialVersionUID = 1L;

		private ConstantsMap() {
			super();
			put(EXPAND_STACKMAP, Integer.valueOf(F_EXPAND_STACKMAP));
			put(LINK_VIEW_TO_EDITOR, Integer.valueOf(F_LINK_VIEW_TO_EDITOR));
			put(RECALCULATE_STACKMAP, Integer.valueOf(F_RECALCULATE_STACKMAP));
			put(SHOW_ANALYZER, Integer.valueOf(F_SHOW_ANALYZER));
			put(SHOW_ASMIFIER_CODE, Integer.valueOf(F_SHOW_ASMIFIER_CODE));
			put(SHOW_HEX_VALUES, Integer.valueOf(F_SHOW_HEX_VALUES));
			put(SHOW_LINE_INFO, Integer.valueOf(F_SHOW_LINE_INFO));
			put(SHOW_ONLY_SELECTED_ELEMENT, Integer.valueOf(F_SHOW_ONLY_SELECTED_ELEMENT));
			put(SHOW_RAW_BYTECODE, Integer.valueOf(F_SHOW_RAW_BYTECODE));
			put(SHOW_STACKMAP, Integer.valueOf(F_SHOW_STACKMAP));
			put(SHOW_VARIABLES, Integer.valueOf(F_SHOW_VARIABLES));
		}
	}
}
