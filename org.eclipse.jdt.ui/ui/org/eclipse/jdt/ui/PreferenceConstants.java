/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.WorkbenchChainedTextFontFieldEditor;

import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.jdt.ui.text.IJavaColorConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.MembersOrderPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.NewJavaProjectPreferencePage;

/**
 * Preference constants used in the JDT-UI preference store. Clients should only read the
 * JDT-UI preference store using these values. Clients are not allowed to modify the 
 * preference store programmatically.
 * 
 * @since 2.0
  */
public class PreferenceConstants {

	private PreferenceConstants() {
	}
	
	/**
	 * A named preference that controls return type rendering of methods in the UI.
	 * <p>
	 * Value is of type <code>Boolean</code>: if <code>true</code> return types
	 * are rendered
	 * </p>
	 */
	public static final String APPEARANCE_METHOD_RETURNTYPE= "org.eclipse.jdt.ui.methodreturntype";//$NON-NLS-1$

	/**
	 * A named preference that controls if override indicators are rendered in the UI.
	 * <p>
	 * Value is of type <code>Boolean</code>: if <code>true</code> override 
	 * indicators are rendered
	 * </p>
	 */
	public static final String APPEARANCE_OVERRIDE_INDICATOR= "org.eclipse.jdt.ui.overrideindicator";//$NON-NLS-1$

	/**
	 * A named preference that defines the pattern used for package name compression.
	 * <p>
	 * Value is of type <code>String</code>. For example foe the given package name 'org.eclipse.jdt' pattern
	 * '.' will compress it to '..jdt', '1~' to 'o~.e~.jdt'.
	 * </p>
	 */	
	public static final String APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW= "PackagesView.pkgNamePatternForPackagesView";//$NON-NLS-1$

	/**
	 * A named preference that controls if package name compression is turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * 
	 * @see #APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW
	 */	
	public static final String APPEARANCE_COMPRESS_PACKAGE_NAMES= "org.eclipse.jdt.ui.compresspackagenames";//$NON-NLS-1$

	/**
	 * A named preference that controls if empty inner packages are folded in
	 * the hierarchical mode of the package explorer.
	 * <p>
	 * Value is of type <code>Boolean</code>: if <code>true</code> empty
	 * inner packages are folded.
	 * </p>
	 * @since 2.1
	 */
	public static final String APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER= "org.eclipse.jdt.ui.flatPackagesInPackageExplorer";//$NON-NLS-1$

	/**
	 * A named preference that controls if prefix removal during setter/getter generation is turned on or off. 
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public static final String CODEGEN_USE_GETTERSETTER_PREFIX= "org.eclipse.jdt.ui.gettersetter.prefix.enable";//$NON-NLS-1$

	/**
	 * A named preference that holds a list of prefixes to be removed from a local variable to compute setter 
	 * and gettter names.
	 * <p>
	 * Value is of type <code>String</code>: comma separated list of prefixed
	 * </p>
	 * 
	 * @see #CODEGEN_USE_GETTERSETTER_PREFIX
	 */	
	public static final String CODEGEN_GETTERSETTER_PREFIX= "org.eclipse.jdt.ui.gettersetter.prefix.list";//$NON-NLS-1$

	/**
	 * A named preference that controls if suffix removal during setter/getter generation is turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public static final String CODEGEN_USE_GETTERSETTER_SUFFIX= "org.eclipse.jdt.ui.gettersetter.suffix.enable";//$NON-NLS-1$

	/**
	 * A named preference that holds a list of suffixes to be removed from a local variable to compute setter 
	 * and getter names.
	 * <p>
	 * Value is of type <code>String</code>: comma separated list of suffixes
	 * </p>
	 * 
	 * @see #CODEGEN_USE_GETTERSETTER_SUFFIX
	 */	
	public static final String CODEGEN_GETTERSETTER_SUFFIX= "org.eclipse.jdt.ui.gettersetter.suffix.list"; //$NON-NLS-1$

	/**
	 * A name preference that controls if a JavaDoc stub gets added to newly created types and methods.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String CODEGEN__JAVADOC_STUBS= "org.eclipse.jdt.ui.javadoc"; //$NON-NLS-1$

	/**
	 * A named preference that controls if a non-javadoc comment gets added to methods generated via the 
	 * "Override Methods" operation.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String CODEGEN__NON_JAVADOC_COMMENTS= "org.eclipse.jdt.ui.seecomments"; //$NON-NLS-1$

	/**
	 * A named preference that controls if a file comment gets added to newly created files.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String CODEGEN__FILE_COMMENTS= "org.eclipse.jdt.ui.filecomments"; //$NON-NLS-1$
	
	/**
	 * A named preference that holds a list of comma separated package names. The list specifies the import order used by
	 * the "Organize Imports" opeation.
	 * <p>
	 * Value is of type <code>String</code>: comma separated list of package names
	 * </p>
	 */
	public static final String ORGIMPORTS_IMPORTORDER= "org.eclipse.jdt.ui.importorder"; //$NON-NLS-1$
	
	/**
	 * A named preference that specifies the number of imports added before a star-import declaration is used.
	 * <p>
	 * Value is of type <code>Int</code>: positive value specifing the number of non star-import is used
	 * </p>
	 */
	public static final String ORGIMPORTS_ONDEMANDTHRESHOLD= "org.eclipse.jdt.ui.ondemandthreshold"; //$NON-NLS-1$

	/**
	 * A named preferences that controls if types that start with a lower case letters get added by the
	 * "Organize Import" operation.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String ORGIMPORTS_IGNORELOWERCASE= "org.eclipse.jdt.ui.ignorelowercasenames"; //$NON-NLS-1$

	/**
	 * A named preference that speficies whether children of a compilation unit are shown in the package explorer.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String SHOW_CU_CHILDREN= "org.eclipse.jdt.ui.packages.cuchildren"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the package explorer's selection is linked to the active editor.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String LINK_PACKAGES_TO_EDITOR= "org.eclipse.jdt.ui.packages.linktoeditor"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the hierarchy view's selection is linked to the active editor.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String LINK_TYPEHIERARCHY_TO_EDITOR= "org.eclipse.jdt.ui.packages.linktypehierarchytoeditor"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the browsing view's selection is
	 * linked to the active editor.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public static final String LINK_BROWSING_VIEW_TO_EDITOR= "org.eclipse.jdt.ui.browsing.linktoeditor"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether new projects are generated using source and output folder.
	 * <p>
	 * Value is of type <code>Boolean</code>. if <code>true</code> new projects are created with a source and
	 * output folder. If <code>false</code> source and output folder equals to the project.
	 * </p>
	 */
	public static final String SRCBIN_FOLDERS_IN_NEWPROJ= "org.eclipse.jdt.ui.wizards.srcBinFoldersInNewProjects"; //$NON-NLS-1$

	/**
	 * A named preference that specifies the source folder name used when creating a new Java project. Value is inactive
	 * if <code>SRCBIN_FOLDERS_IN_NEWPROJ</code> is set to <code>false</code>.
	 * <p>
	 * Value is of type <code>String</code>. 
	 * </p>
	 * 
	 * @see #SRCBIN_FOLDERS_IN_NEWPROJ
	 */
	public static final String SRCBIN_SRCNAME= "org.eclipse.jdt.ui.wizards.srcBinFoldersSrcName"; //$NON-NLS-1$

	/**
	 * A named preference that specifies the output folder name used when creating a new Java project. Value is inactive
	 * if <code>SRCBIN_FOLDERS_IN_NEWPROJ</code> is set to <code>false</code>.
	 * <p>
	 * Value is of type <code>String</code>. 
	 * </p>
	 * 
	 * @see #SRCBIN_FOLDERS_IN_NEWPROJ
	 */
	public static final String SRCBIN_BINNAME= "org.eclipse.jdt.ui.wizards.srcBinFoldersBinName"; //$NON-NLS-1$

	/**
	 * A named preference that holds a list of possible JRE libraries used by the New Java Project wizard. An library 
	 * consists of a description and an arbitrary number of <code>IClasspathEntry</code>s, that will represent the 
	 * JRE on the new project's classpath. 
	 * <p>
	 * Value is of type <code>String</code>: a semicolon separated list of encoded JRE libraries. 
	 * <code>NEWPROJECT_JRELIBRARY_INDEX</code> defines the currently used library. Clients
	 * should use the method <code>encodeJRELibrary</code> to encode a JRE library into a string
	 * and the methods <code>decodeJRELibraryDescription(String)</code> and <code>
	 * decodeJRELibraryClasspathEntries(String)</code> to decode the description and the array
	 * of classpath entries from an encoded string.
	 * </p>
	 * 
	 * @see #NEWPROJECT_JRELIBRARY_INDEX
	 * @see #encodeJRELibrary(String, IClasspathEntry[])
	 * @see #decodeJRELibraryDescription(String)
	 * @see #decodeJRELibraryClasspathEntries(String)
	 */
	public static final String NEWPROJECT_JRELIBRARY_LIST= "org.eclipse.jdt.ui.wizards.jre.list"; //$NON-NLS-1$

	/**
	 * A named preferences that specifies the current active JRE library.
	 * <p>
	 * Value is of type <code>Int</code>: an index into the list of possible JRE libraries.
	 * </p>
	 * 
	 * @see #NEWPROJECT_JRELIBRARY_LIST
	 */
	public static final String NEWPROJECT_JRELIBRARY_INDEX= "org.eclipse.jdt.ui.wizards.jre.index"; //$NON-NLS-1$

	/**
	 * A named preference that controls if a new type hierarchy gets opened in a 
	 * new type hierarchy perspective or inside the type hierarchy view part.
	 * <p>
	 * Value is of type <code>String</code>: possible values are <code>
	 * OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE</code> or <code>
	 * OPEN_TYPE_HIERARCHY_IN_VIEW_PART</code>.
	 * </p>
	 * 
	 * @see #OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE
	 * @see #OPEN_TYPE_HIERARCHY_IN_VIEW_PART
	 */
	public static final String OPEN_TYPE_HIERARCHY= "org.eclipse.jdt.ui.openTypeHierarchy"; //$NON-NLS-1$

	/**
	 * A string value used by the named preference <code>OPEN_TYPE_HIERARCHY</code>.
	 * 
	 * @see #OPEN_TYPE_HIERARCHY
	 */
	public static final String OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE= "perspective"; //$NON-NLS-1$

	/**
	 * A string value used by the named preference <code>OPEN_TYPE_HIERARCHY</code>.
	 * 
	 * @see #OPEN_TYPE_HIERARCHY
	 */
	public static final String OPEN_TYPE_HIERARCHY_IN_VIEW_PART= "viewPart"; //$NON-NLS-1$
	
	/**
	 * A named preference that controls the behaviour when double clicking on a container in the packages view. 
	 * <p>
	 * Value is of type <code>String</code>: possible values are <code>
	 * DOUBLE_CLICK_GOES_INTO</code> or <code>
	 * DOUBLE_CLICK_EXPANDS</code>.
	 * </p>
	 * 
	 * @see #DOUBLE_CLICK_EXPANDS
	 * @see #DOUBLE_CLICK_GOES_INTO
	 */
	public static final String DOUBLE_CLICK= "packageview.doubleclick"; //$NON-NLS-1$

	/**
	 * A string value used by the named preference <code>DOUBLE_CLICK</code>.
	 * 
	 * @see #DOUBLE_CLICK
	 */
	public static final String DOUBLE_CLICK_GOES_INTO= "packageview.gointo"; //$NON-NLS-1$

	/**
	 * A string value used by the named preference <code>DOUBLE_CLICK</code>.
	 * 
	 * @see #DOUBLE_CLICK
	 */
	public static final String DOUBLE_CLICK_EXPANDS= "packageview.doubleclick.expands"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether Java views update their presentation while editing or when saving the
	 * content of an editor. 
	 * <p>
	 * Value is of type <code>String</code>: possible values are <code>
	 * UPDATE_ON_SAVE</code> or <code>
	 * UPDATE_WHILE_EDITING</code>.
	 * </p>
	 * 
	 * @see #UPDATE_ON_SAVE
	 * @see #UPDATE_WHILE_EDITING
	 */
	public static final String UPDATE_JAVA_VIEWS= "JavaUI.update"; //$NON-NLS-1$

	/**
	 * A string value used by the named preference <code>UPDATE_JAVA_VIEWS</code>
	 * 
	 * @see #UPDATE_JAVA_VIEWS
	 */
	public static final String UPDATE_ON_SAVE= "JavaUI.update.onSave"; //$NON-NLS-1$

	/**
	 * A string value used by the named preference <code>UPDATE_JAVA_VIEWS</code>
	 * 
	 * @see #UPDATE_JAVA_VIEWS
	 */
	public static final String UPDATE_WHILE_EDITING= "JavaUI.update.whileEditing"; //$NON-NLS-1$

	/**
	 * A named preference that holds the path of the Javadoc command used by the Javadoc creation wizard.
	 * <p>
	 * Value is of type <code>String</code>.
	 * </p>
	 */
	public static final String JAVADOC_COMMAND= "command"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether bracket matching highlighting is turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_MATCHING_BRACKETS= "matchingBrackets"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used to highlight matching brackets.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string 
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_MATCHING_BRACKETS_COLOR=  "matchingBracketsColor"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the current line highlighting is turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_CURRENT_LINE= "currentLine"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used to highlight the current line.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_CURRENT_LINE_COLOR= "currentLineColor"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the print margin is turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_PRINT_MARGIN= "printMargin"; //$NON-NLS-1$
	
	/**
	 * A named preference that holds the color used to render the print margin.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_PRINT_MARGIN_COLOR= "printMarginColor"; //$NON-NLS-1$

	/**
	 * Print margin column. Int value.
	 */
	public final static String EDITOR_PRINT_MARGIN_COLUMN= "printMarginColumn"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used for the find/replace scope.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_FIND_SCOPE_COLOR= AbstractTextEditor.PREFERENCE_COLOR_FIND_SCOPE;

	/**
	 * A named preference that specifies if the editor uses spaces for tabs.
	 * <p>
	 * Value is of type <code>Boolean</code>. If <code>true</code>spaces instead of tabs are used
	 * in the editor. If <code>false</code> the editor inserts a tab character when pressing the tab
	 * key.
	 * </p>
	 */
	public final static String EDITOR_SPACES_FOR_TABS= "spacesForTabs"; //$NON-NLS-1$

	/**
	 * A named preference that holds the number of spaces used per tab in the editor.
	 * <p>
	 * Value is of type <code>Int</code>: positive int value specifying the number of
	 * spaces per tab.
	 * </p>
	 */
	public final static String EDITOR_TAB_WIDTH= "org.eclipse.jdt.ui.editor.tab.width"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the outline view selection
	 * should stay in sync with with the element at the current cursor position.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE= "JavaEditor.SyncOutlineOnCursorMove"; //$NON-NLS-1$

	/**
	 * A named preference that controls if correction indicators are shown in the UI.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_CORRECTION_INDICATION= "JavaEditor.ShowTemporaryProblem"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the editor shows problem indicators in text (squiggly lines). 
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_PROBLEM_INDICATION= "problemIndication"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used to render problem indicators.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see #EDITOR_PROBLEM_INDICATION
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_PROBLEM_INDICATION_COLOR= "problemIndicationColor"; //$NON-NLS-1$

	/**PreferenceConstants.EDITOR_PROBLEM_INDICATION_COLOR;
	 * A named preference that controls whether the editor shows warning indicators in text (squiggly lines). 
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_WARNING_INDICATION= "warningIndication"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used to render warning indicators.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see #EDITOR_WARNING_INDICATION
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_WARNING_INDICATION_COLOR= "warningIndicationColor"; //$NON-NLS-1$
	
	/**
	 * A named preference that controls whether the editor shows task indicators in text (squiggly lines). 
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_TASK_INDICATION= "taskIndication"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used to render task indicators.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see #EDITOR_TASK_INDICATION
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_TASK_INDICATION_COLOR= "taskIndicationColor"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the editor shows bookmark
	 * indicators in text (squiggly lines).
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_BOOKMARK_INDICATION= "bookmarkIndication"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used to render bookmark indicators.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 *
	 * @see #EDITOR_BOOKMARK_INDICATION
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 * @since 2.1
	 */
	public final static String EDITOR_BOOKMARK_INDICATION_COLOR= "bookmarkIndicationColor"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the editor shows search
	 * indicators in text (squiggly lines).
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_SEARCH_RESULT_INDICATION= "searchResultIndication"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used to render search indicators.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 *
	 * @see #EDITOR_SEARCH_RESULT_INDICATION
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 * @since 2.1
	 */
	public final static String EDITOR_SEARCH_RESULT_INDICATION_COLOR= "searchResultIndicationColor"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the editor shows unknown
	 * indicators in text (squiggly lines).
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_UNKNOWN_INDICATION= "othersIndication"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used to render unknown
	 * indicators.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 *
	 * @see #EDITOR_UNKNOWN_INDICATION
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 * @since 2.1
	 */
	public final static String EDITOR_UNKNOWN_INDICATION_COLOR= "othersIndicationColor"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the overview ruler shows error
	 * indicators.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_ERROR_INDICATION_IN_OVERVIEW_RULER= "errorIndicationInOverviewRuler"; //$NON-NLS-1$
	
	/**
	 * A named preference that controls whether the overview ruler shows warning
	 * indicators.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_WARNING_INDICATION_IN_OVERVIEW_RULER= "warningIndicationInOverviewRuler"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the overview ruler shows task
	 * indicators.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_TASK_INDICATION_IN_OVERVIEW_RULER= "taskIndicationInOverviewRuler"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the overview ruler shows
	 * bookmark indicators.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_BOOKMARK_INDICATION_IN_OVERVIEW_RULER= "bookmarkIndicationInOverviewRuler"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the overview ruler shows
	 * search result indicators.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER= "searchResultIndicationInOverviewRuler"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the overview ruler shows
	 * unknown indicators.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_UNKNOWN_INDICATION_IN_OVERVIEW_RULER= "othersIndicationInOverviewRuler"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the 'close strings' feature
	 *  is   enabled.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_CLOSE_STRINGS= "closeStrings"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the 'wrap strings' feature is
	 * enabled.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_WRAP_STRINGS= "wrapStrings"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the 'close brackets' feature is
	 * enabled.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_CLOSE_BRACKETS= "closeBrackets"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the 'close java docs' feature is
	 * enabled.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_CLOSE_JAVADOCS= "closeJavaDocs"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the 'add JavaDoc tags' feature
	 * is enabled.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_ADD_JAVADOC_TAGS= "addJavaDocTags"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the 'format Javadoc tags'
	 * feature is enabled.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_FORMAT_JAVADOCS= "formatJavaDocs"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the 'smart paste' feature is
	 * enabled.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_SMART_PASTE= "smartPaste"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the 'smart home-end' feature is
	 * enabled.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String EDITOR_SMART_HOME_END= AbstractTextEditor.PREFERENCE_NAVIGATION_SMART_HOME_END;

	/**
	 * A named preference that controls if temporary problems are evaluated and shown in the UI.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_EVALUTE_TEMPORARY_PROBLEMS= "handleTemporaryProblems"; //$NON-NLS-1$

	/**
	 * A named preference that controls if the overview ruler is shown in the UI.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_OVERVIEW_RULER= "overviewRuler"; //$NON-NLS-1$

	/**
	 * A named preference that controls if the line number ruler is shown in the UI.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_LINE_NUMBER_RULER= "lineNumberRuler"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used to render line numbers inside the line number ruler.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 * @see #EDITOR_LINE_NUMBER_RULER
	 */
	public final static String EDITOR_LINE_NUMBER_RULER_COLOR= "lineNumberColor"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used to render linked positions inside code templates.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_LINKED_POSITION_COLOR= "linkedPositionColor"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used as the text foreground.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_FOREGROUND_COLOR= AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND;

	/**
	 * A named preference that describes if the system default foreground color
	 * is used as the text foreground.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_FOREGROUND_DEFAULT_COLOR= AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT;

	/**
	 * A named preference that holds the color used as the text background.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_BACKGROUND_COLOR= AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND;

	/**
	 * A named preference that describes if the system default background color
	 * is used as the text foreground.
	 * <p>
	 * Value is of type <code>Boolean</code>. 
	 * </p>
	 */
	public final static String EDITOR_BACKGROUND_DEFAULT_COLOR= AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT;

	/**
	 * Preference key suffix for bold text style preference keys.
	 */
	public static final String EDITOR_BOLD_SUFFIX= "_bold"; //$NON-NLS-1$

	/**
	 * A named preference that holds the color used to render multi line comments.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_MULTI_LINE_COMMENT_COLOR= IJavaColorConstants.JAVA_MULTI_LINE_COMMENT;

	/**
	 * A named preference that controls whether multi line comments are rendered in bold.
	 * <p>
	 * Value is of type <code>Boolean</code>. If <code>true</code> multi line comments are rendered
	 * in bold. If <code>false</code> the are rendered using no font style attribute.
	 * </p>
	 */
	public final static String EDITOR_MULTI_LINE_COMMENT_BOLD= IJavaColorConstants.JAVA_MULTI_LINE_COMMENT + EDITOR_BOLD_SUFFIX; 

	/**
	 * A named preference that holds the color used to render single line comments.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_SINGLE_LINE_COMMENT_COLOR= IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT;

	/**
	 * A named preference that controls whether sinle line comments are rendered in bold.
	 * <p>
	 * Value is of type <code>Boolean</code>. If <code>true</code> single line comments are rendered
	 * in bold. If <code>false</code> the are rendered using no font style attribute.
	 * </p>
	 */
	public final static String EDITOR_SINGLE_LINE_COMMENT_BOLD= IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT + EDITOR_BOLD_SUFFIX; 

	/**
	 * A named preference that holds the color used to render java keywords.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_JAVA_KEYWORD_COLOR= IJavaColorConstants.JAVA_KEYWORD;

	/**
	 * A named preference that controls whether keywords are rendered in bold.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_JAVA_KEYWORD_BOLD= IJavaColorConstants.JAVA_KEYWORD + EDITOR_BOLD_SUFFIX;

	/**
	 * A named preference that holds the color used to render string constants.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_STRING_COLOR= IJavaColorConstants.JAVA_STRING;

	/**
	 * A named preference that controls whether string constants are rendered in bold.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_STRING_BOLD= IJavaColorConstants.JAVA_STRING + EDITOR_BOLD_SUFFIX;

	/**
	 * A named preference that holds the color used to render java default text.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_JAVA_DEFAULT_COLOR= IJavaColorConstants.JAVA_DEFAULT;

	/**
	 * A named preference that controls whether Java default text is rendered in bold.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_JAVA_DEFAULT_BOLD= IJavaColorConstants.JAVA_DEFAULT + EDITOR_BOLD_SUFFIX;

	/**
	 * A named preference that holds the color used to render javadoc keywords.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_JAVADOC_KEYWORD_COLOR= IJavaColorConstants.JAVADOC_KEYWORD;

	/**
	 * A named preference that controls whether javadoc keywords are rendered in bold.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_JAVADOC_KEYWORD_BOLD= IJavaColorConstants.JAVADOC_KEYWORD + EDITOR_BOLD_SUFFIX;

	/**
	 * A named preference that holds the color used to render javadoc tags.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_JAVADOC_TAG_COLOR= IJavaColorConstants.JAVADOC_TAG;

	/**
	 * A named preference that controls whether javadoc tags are rendered in bold.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_JAVADOC_TAG_BOLD= IJavaColorConstants.JAVADOC_TAG + EDITOR_BOLD_SUFFIX;

	/**
	 * A named preference that holds the color used to render javadoc links.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_JAVADOC_LINKS_COLOR= IJavaColorConstants.JAVADOC_LINK;

	/**
	 * A named preference that controls whether javadoc links are rendered in bold.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_JAVADOC_LINKS_BOLD= IJavaColorConstants.JAVADOC_LINK + EDITOR_BOLD_SUFFIX;
		
	/**
	 * A named preference that holds the color used to render javadoc default text.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_JAVADOC_DEFAULT_COLOR= IJavaColorConstants.JAVADOC_DEFAULT;

	/**
	 * A named preference that controls whether javadoc default text is rendered in bold.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_JAVADOC_DEFAULT_BOLD= IJavaColorConstants.JAVADOC_DEFAULT + EDITOR_BOLD_SUFFIX;

	/**
	 * A named preference that holds the color used for 'linked-mode' underline.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 *
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 * @since 2.1
	 */
	public final static String EDITOR_LINK_COLOR= "linkColor"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether hover tooltips in the editor are turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String EDITOR_SHOW_HOVER= "org.eclipse.jdt.ui.editor.showHover"; //$NON-NLS-1$

	/**
	 * A named preference that defines the hover shown when no control key is
	 * pressed.
	 * <p>Value is of type <code>String</code>: possible values are <code>
	 * EDITOR_NO_HOVER_CONFIGURED_ID</code> or
	 * <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the hover id of a hover
	 * contributed as <code>javaEditorTextHovers</code>.
	 * </p>
	 * @see #EDITOR_NO_HOVER_CONFIGURED_ID
	 * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
	 * @see JavaUI
	 * @since 2.1
	 */
	public static final String EDITOR_NONE_HOVER= "noneHover"; //$NON-NLS-1$

	/**
	 * A named preference that defines the hover shown when the
	 * <code>CTRL</code> modifier key is pressed.
	 * <p>Value is of type <code>String</code>: possible values are <code>
	 * EDITOR_NO_HOVER_CONFIGURED_ID</code> or
	 * <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the hover id of a
	 * hover contributed as <code>javaEditorTextHovers</code>.
	 * </p>
	 * @see #EDITOR_NO_HOVER_CONFIGURED_ID
	 * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
	 * @see JavaUI
	 * @since 2.1
	 */
	public static final String EDITOR_CTRL_HOVER= "ctrlHover"; //$NON-NLS-1$
	
	/**
	 * A named preference that defines the hover shown when the
	 * <code>SHIFT</code> modifier key is pressed.
	 * <p>Value is of type <code>String</code>: possible values are <code>
	 * EDITOR_NO_HOVER_CONFIGURED_ID</code> or
	 * <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the hover id of a
	 * hover contributed as <code>javaEditorTextHovers</code>.
	 * </p>
	 * @see #EDITOR_NO_HOVER_CONFIGURED_ID
	 * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
	 * @see JavaUI ID_*_HOVER
	 * @since 2.1
	 */
	public static final String EDITOR_SHIFT_HOVER= "shiftHover"; //$NON-NLS-1$

	/**
	 * A named preference that defines the hover shown when the
	 * <code>CTRL + ALT</code> modifier keys is pressed.
	 * <p>Value is of type <code>String</code>: possible values are <code>
	 * EDITOR_NO_HOVER_CONFIGURED_ID</code> or
	 * <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the hover id of a
	 * hover contributed as <code>javaEditorTextHovers</code>.
	 * </p>
	 * @see #EDITOR_NO_HOVER_CONFIGURED_ID
	 * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
	 * @see JavaUI ID_*_HOVER
	 * @since 2.1
	 */
	public static final String EDITOR_CTRL_ALT_HOVER= "ctrlAltHover"; //$NON-NLS-1$

	/**
	 * A named preference that defines the hover shown when the
	 * <code>CTRL + ALT + SHIFT</code> modifier keys is pressed.
	 * <p>Value is of type <code>String</code>: possible values are <code>
	 * EDITOR_NO_HOVER_CONFIGURED_ID</code> or
	 * <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the hover id of a
	 * hover contributed as <code>javaEditorTextHovers</code>.
	 * </p>
	 * @see #EDITOR_NO_HOVER_CONFIGURED_ID
	 * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
	 * @see JavaUI ID_*_HOVER
	 * @since 2.1
	 */
	public static final String EDITOR_CTRL_ALT_SHIFT_HOVER= "ctrlAltShiftHover"; //$NON-NLS-1$

	/**
	 * A named preference that defines the hover shown when the
	 * <code>CTRL + SHIFT</code> modifier keys is pressed.
	 * <p>Value is of type <code>String</code>: possible values are <code>
	 * EDITOR_NO_HOVER_CONFIGURED_ID</code> or
	 * <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the hover id of a
	 * hover contributed as <code>javaEditorTextHovers</code>.
	 * </p>
	 * @see #EDITOR_NO_HOVER_CONFIGURED_ID
	 * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
	 * @see JavaUI ID_*_HOVER
	 * @since 2.1
	 */
	public static final String EDITOR_CTRL_SHIFT_HOVER= "ctrlShiftHover"; //$NON-NLS-1$

	/**
	 * A named preference that defines the hover shown when the
	 * <code>ALT</code> modifier key is pressed.
	 * <p>Value is of type <code>String</code>: possible values are <code>
	 * EDITOR_NO_HOVER_CONFIGURED_ID</code>,
	 * <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code>  or the hover id of a
	 * hover contributed as <code>javaEditorTextHovers</code>.
	 * </p>
	 * @see #EDITOR_NO_HOVER_CONFIGURED_ID
	 * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
	 * @see JavaUI ID_*_HOVER
	 * @since 2.1
	 */
	public static final String EDITOR_ALT_SHIFT_HOVER= "altShiftHover"; //$NON-NLS-1$

	/**
	 * A string value used by the named preferences for hover configuration to
	 * descibe that no hover should be shown for the given key modifiers.
	 * @since 2.1
	 */
	public static final String EDITOR_NO_HOVER_CONFIGURED_ID= "noHoverConfiguredId"; //$NON-NLS-1$
	
	/**
	 * A string value used by the named preferences for hover configuration to
	 * descibe that the default hover should be shown for the given key
	 * modifiers. The default hover is described by the
	 * <code>EDITOR_DEFAULT_HOVER</code> property.
	 * @since 2.1
	 */
	public static final String EDITOR_DEFAULT_HOVER_CONFIGURED_ID= "defaultHoverConfiguredId"; //$NON-NLS-1$

	/**
	 * A named preference that defines the hover named the 'default hover'.
	 * Value is of type <code>String</code>: possible values are <code>
	 * EDITOR_NO_HOVER_CONFIGURED_ID</code> or <code> the hover id of a hover
	 * contributed as <code>javaEditorTextHovers</code>.
	 * </p>
	 *@since 2.1
	 */
	public static final String EDITOR_DEFAULT_HOVER= "defaultHover"; //$NON-NLS-1$

	/**
	 * A named preference that controls if segmented view (show selected element only) is turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String EDITOR_SHOW_SEGMENTS= "org.eclipse.jdt.ui.editor.showSegments"; //$NON-NLS-1$

	/**
	 * A named preference that controls if the Java code assist gets auto activated.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String CODEASSIST_AUTOACTIVATION= "content_assist_autoactivation"; //$NON-NLS-1$

	/**
	 * A name preference that holds the auto activation delay time in milli seconds.
	 * <p>
	 * Value is of type <code>Int</code>.
	 * </p>
	 */
	public final static String CODEASSIST_AUTOACTIVATION_DELAY= "content_assist_autoactivation_delay"; //$NON-NLS-1$

	/**
	 * A named preference that controls if code assist contains only visible proposals.
	 * <p>
	 * Value is of type <code>Boolean</code>. if <code>true<code> code assist only contains visible members. If 
	 * <code>false</code> all members are included.
	 * </p>
	 */
	public final static String CODEASSIST_SHOW_VISIBLE_PROPOSALS= "content_assist_show_visible_proposals"; //$NON-NLS-1$

	/**
	 * A named preference that controls if the Java code assist inserts a
	 * proposal automatically if only one proposal is available.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String CODEASSIST_AUTOINSERT= "content_assist_autoinsert"; //$NON-NLS-1$

	/**
	 * A named preference that controls if the Java code assist adds import
	 * statements.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String CODEASSIST_ADDIMPORT= "content_assist_add_import"; //$NON-NLS-1$
	
	/**
	 * A named preference that controls if the Java code assist only inserts
	 * completions. If set to false the proposals can also _replace_ code.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String CODEASSIST_INSERT_COMPLETION= "content_assist_insert_completion"; //$NON-NLS-1$	

	/**
	 * A named preference that controls whether code assist proposals filtering is case sensitive or not.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String CODEASSIST_CASE_SENSITIVITY= "content_assist_case_sensitivity"; //$NON-NLS-1$
	
	/**
	 * A named preference that defines if code assist proposals are sorted in alphabetical order.
	 * <p>
	 * Value is of type <code>Boolean</code>. If <code>true</code> that are sorted in alphabetical 
	 * order. If <code>false</code> that are unsorted.
	 * </p>
	 */
	public final static String CODEASSIST_ORDER_PROPOSALS= "content_assist_order_proposals"; //$NON-NLS-1$

	/**
	 * A named preference that controls if argument names are filled in when a method is selected from as list
	 * of code assist proposal.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String CODEASSIST_FILL_ARGUMENT_NAMES= "content_assist_fill_method_arguments"; //$NON-NLS-1$

	/**
	 * A named preference that controls if method arguments are guessed when a
	 * method is selected from as list of code assist proposal.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String CODEASSIST_GUESS_METHOD_ARGUMENTS= "content_assist_guess_method_arguments"; //$NON-NLS-1$

	/**
	 * A named preference that holds the characters that auto activate code assist in Java code.
	 * <p>
	 * Value is of type <code>Sring</code>. All characters that trigger auto code assist in Java code.
	 * </p>
	 */
	public final static String CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA= "content_assist_autoactivation_triggers_java"; //$NON-NLS-1$

	/**
	 * A named preference that holds the characters that auto activate code assist in Javadoc.
	 * <p>
	 * Value is of type <code>Sring</code>. All characters that trigger auto code assist in Javadoc.
	 * </p>
	 */
	public final static String CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC= "content_assist_autoactivation_triggers_javadoc"; //$NON-NLS-1$

	/**
	 * A named preference that holds the background color used in the code assist selection dialog.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String CODEASSIST_PROPOSALS_BACKGROUND= "content_assist_proposals_background"; //$NON-NLS-1$

	/**
	 * A named preference that holds the foreground color used in the code assist selection dialog.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String CODEASSIST_PROPOSALS_FOREGROUND= "content_assist_proposals_foreground"; //$NON-NLS-1$
	
	/**
	 * A named preference that holds the background color used for parameter hints.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String CODEASSIST_PARAMETERS_BACKGROUND= "content_assist_parameters_background"; //$NON-NLS-1$

	/**
	 * A named preference that holds the foreground color used in the code assist selection dialog
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String CODEASSIST_PARAMETERS_FOREGROUND= "content_assist_parameters_foreground"; //$NON-NLS-1$

	/**
	 * A named preference that holds the background color used in the code
	 * assist selection dialog to mark replaced code.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 *
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 * @since 2.1
	 */
	public final static String CODEASSIST_REPLACEMENT_BACKGROUND= "content_assist_completion_replacement_background"; //$NON-NLS-1$

	/**
	 * A named preference that holds the foreground color used in the code
	 * assist selection dialog to mark replaced code.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 *
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 * @since 2.1
	 */
	public final static String CODEASSIST_REPLACEMENT_FOREGROUND= "content_assist_completion_replacement_foreground"; //$NON-NLS-1$


	/**
	 * A named preference that controls the behaviour of the refactoring wizard for showing the error page. 
	 * <p>
	 * Value is of type <code>String</code>. Valid values are: 
	 * <code>REFACTOR_FATAL_SEVERITY</code>,
	 * <code>REFACTOR_ERROR_SEVERITY</code>,
	 * <code>REFACTOR_WARNING_SEVERITY</code>
	 * <code>REFACTOR_INFO_SEVERITY</code>,
	 * <code>REFACTOR_OK_SEVERITY</code>.
	 * </p>
	 * 
	 * @see #REFACTOR_FATAL_SEVERITY
	 * @see #REFACTOR_ERROR_SEVERITY
	 * @see #REFACTOR_WARNING_SEVERITY
	 * @see #REFACTOR_INFO_SEVERITY
	 * @see #REFACTOR_OK_SEVERITY
	 */
	public static final String REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD= "Refactoring.ErrorPage.severityThreshold"; //$NON-NLS-1$

	/**
	 * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>.
	 * 
	 * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
	 */
	public static final String REFACTOR_FATAL_SEVERITY= "4"; //$NON-NLS-1$
	
	/**
	 * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>.
	 * 
	 * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
	 */	
	public static final String REFACTOR_ERROR_SEVERITY= "3"; //$NON-NLS-1$

	/**
	 * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>.
	 * 
	 * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
	 */
	public static final String REFACTOR_WARNING_SEVERITY= "2"; //$NON-NLS-1$

	/**
	 * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>.
	 * 
	 * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
	 */
	public static final String REFACTOR_INFO_SEVERITY= "1"; //$NON-NLS-1$

	/**
	 * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>.
	 * 
	 * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
	 */
	public static final String REFACTOR_OK_SEVERITY= "0"; //$NON-NLS-1$

	/**
	 * A named preference thet controls whether all dirty editors are automatically saved before a refactoring is
	 * executed.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String REFACTOR_SAVE_ALL_EDITORS= "Refactoring.savealleditors"; //$NON-NLS-1$

	/**
	 * A named preference that controls if the Java Browsing views are linked to the active editor.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * 
	 * @see #LINK_PACKAGES_TO_EDITOR
	 */
	public static final String BROWSING_LINK_VIEW_TO_EDITOR= "org.eclipse.jdt.ui.browsing.linktoeditor"; //$NON-NLS-1$

	/**
	 * A named preference that controls the layout of the Java Browsing views vertically. Boolean value.
	 * <p>
	 * Value is of type <code>Boolean</code>. If <code>true<code> the views are stacked vertical.
	 * If <code>false</code> they are stacked horizontal.
	 * </p>
	 */
	public static final String BROWSING_STACK_VERTICALLY= "org.eclipse.jdt.ui.browsing.stackVertically"; //$NON-NLS-1$
	
	
	/**
	 * A named preference that controls if templates are formatted when applied.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 *
	 * @since 2.1
	 */	
	public static final String TEMPLATES_USE_CODEFORMATTER= "org.eclipse.jdt.ui.template.format"; //$NON-NLS-1$

	
	
	public static void initializeDefaultValues(IPreferenceStore store) {
		store.setDefault(PreferenceConstants.EDITOR_SHOW_HOVER, true);
		store.setDefault(PreferenceConstants.EDITOR_SHOW_SEGMENTS, false);

		// JavaBasePreferencePage
		store.setDefault(PreferenceConstants.LINK_PACKAGES_TO_EDITOR, true);
		store.setDefault(PreferenceConstants.LINK_TYPEHIERARCHY_TO_EDITOR, false);
		store.setDefault(PreferenceConstants.LINK_BROWSING_VIEW_TO_EDITOR, true);
		store.setDefault(PreferenceConstants.OPEN_TYPE_HIERARCHY, PreferenceConstants.OPEN_TYPE_HIERARCHY_IN_VIEW_PART);
		store.setDefault(PreferenceConstants.DOUBLE_CLICK, PreferenceConstants.DOUBLE_CLICK_EXPANDS);
		store.setDefault(PreferenceConstants.UPDATE_JAVA_VIEWS, PreferenceConstants.UPDATE_WHILE_EDITING);	
		
		// AppearancePreferencePage
		store.setDefault(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, false);
		store.setDefault(PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE, false);
		store.setDefault(PreferenceConstants.APPEARANCE_OVERRIDE_INDICATOR, true);
		store.setDefault(PreferenceConstants.SHOW_CU_CHILDREN, true);
		store.setDefault(PreferenceConstants.BROWSING_STACK_VERTICALLY, false);
		store.setDefault(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW, ""); //$NON-NLS-1$
		store.setDefault(PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER, true);

		// ImportOrganizePreferencePage
		store.setDefault(PreferenceConstants.ORGIMPORTS_IMPORTORDER, "java;javax;org;com"); //$NON-NLS-1$
		store.setDefault(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD, 99);
		store.setDefault(PreferenceConstants.ORGIMPORTS_IGNORELOWERCASE, true);

		// ClasspathVariablesPreferencePage
		// CodeFormatterPreferencePage
		// CompilerPreferencePage
		// no initialization needed
		
		// RefactoringPreferencePage
		store.setDefault(PreferenceConstants.REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD, PreferenceConstants.REFACTOR_ERROR_SEVERITY);
		store.setDefault(PreferenceConstants.REFACTOR_SAVE_ALL_EDITORS, false);		

		// TemplatePreferencePage
		store.setDefault(PreferenceConstants.TEMPLATES_USE_CODEFORMATTER, true);
		
		// CodeGenerationPreferencePage
		store.setDefault(PreferenceConstants.CODEGEN_USE_GETTERSETTER_PREFIX, false);
		store.setDefault(PreferenceConstants.CODEGEN_USE_GETTERSETTER_SUFFIX, false);
		store.setDefault(PreferenceConstants.CODEGEN_GETTERSETTER_PREFIX, "fg, f, _$, _, m_"); //$NON-NLS-1$
		store.setDefault(PreferenceConstants.CODEGEN_GETTERSETTER_SUFFIX, "_"); //$NON-NLS-1$
		store.setDefault(PreferenceConstants.CODEGEN__JAVADOC_STUBS, true);
		store.setDefault(PreferenceConstants.CODEGEN__NON_JAVADOC_COMMENTS, false);
		store.setDefault(PreferenceConstants.CODEGEN__FILE_COMMENTS, false);		

		// JavaEditorPreferencePage
		/*
		 * Ensure that the display is accessed only in the UI thread.
		 * Ensure that there are no side effects of switching the thread.
		 */
		final RGB[] rgbs= new RGB[3];
		final Display display= Display.getDefault();
		display.syncExec(new Runnable() {
			public void run() {
				Color c= display.getSystemColor(SWT.COLOR_GRAY);
				rgbs[0]= c.getRGB();
				c= display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
				rgbs[1]= c.getRGB();
				c= display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
				rgbs[2]= c.getRGB();
			}
		});

		store.setDefault(PreferenceConstants.EDITOR_MATCHING_BRACKETS, true);
		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR,  rgbs[0]);

		store.setDefault(PreferenceConstants.EDITOR_CURRENT_LINE, true);
		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_CURRENT_LINE_COLOR, new RGB(225, 235, 224));

		store.setDefault(PreferenceConstants.EDITOR_PRINT_MARGIN, false);
		store.setDefault(PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN, 80);
		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_PRINT_MARGIN_COLOR, new RGB(176, 180 , 185));

		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_FIND_SCOPE_COLOR, new RGB(185, 176 , 180));

		store.setDefault(PreferenceConstants.EDITOR_PROBLEM_INDICATION, true);
		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_PROBLEM_INDICATION_COLOR, new RGB(255, 0 , 128));
		store.setDefault(PreferenceConstants.EDITOR_ERROR_INDICATION_IN_OVERVIEW_RULER, true);

		store.setDefault(PreferenceConstants.EDITOR_WARNING_INDICATION, true);
		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_WARNING_INDICATION_COLOR, new RGB(244, 200 , 45));
		store.setDefault(PreferenceConstants.EDITOR_WARNING_INDICATION_IN_OVERVIEW_RULER, true);

		store.setDefault(PreferenceConstants.EDITOR_TASK_INDICATION, false);
		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_TASK_INDICATION_COLOR, new RGB(0, 128, 255));
		store.setDefault(PreferenceConstants.EDITOR_TASK_INDICATION_IN_OVERVIEW_RULER, false);

		store.setDefault(PreferenceConstants.EDITOR_BOOKMARK_INDICATION, false);
		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_BOOKMARK_INDICATION_COLOR, new RGB(34, 164, 99));
		store.setDefault(PreferenceConstants.EDITOR_BOOKMARK_INDICATION_IN_OVERVIEW_RULER, false);

		store.setDefault(PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION, false);
		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_COLOR, new RGB(192, 192, 192));
		store.setDefault(PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER, false);

		store.setDefault(PreferenceConstants.EDITOR_UNKNOWN_INDICATION, false);
		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_UNKNOWN_INDICATION_COLOR, new RGB(0, 0, 0));
		store.setDefault(PreferenceConstants.EDITOR_UNKNOWN_INDICATION_IN_OVERVIEW_RULER, false);

		store.setDefault(PreferenceConstants.EDITOR_CORRECTION_INDICATION, true);
		store.setDefault(PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE, false);

		store.setDefault(PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS, true);

		store.setDefault(PreferenceConstants.EDITOR_OVERVIEW_RULER, true);

		store.setDefault(PreferenceConstants.EDITOR_LINE_NUMBER_RULER, false);
		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR, new RGB(0, 0, 0));

		WorkbenchChainedTextFontFieldEditor.startPropagate(store, JFaceResources.TEXT_FONT);

		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_LINKED_POSITION_COLOR, new RGB(0, 200 , 100));
		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_LINK_COLOR, new RGB(0, 0, 255));

		PreferenceConverter.setDefault(store,  PreferenceConstants.EDITOR_FOREGROUND_COLOR, rgbs[1]);
		store.setDefault(PreferenceConstants.EDITOR_FOREGROUND_DEFAULT_COLOR, true);

		PreferenceConverter.setDefault(store,  PreferenceConstants.EDITOR_BACKGROUND_COLOR, rgbs[2]);
		store.setDefault(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR, true);

		store.setDefault(PreferenceConstants.EDITOR_TAB_WIDTH, 4);
		store.setDefault(PreferenceConstants.EDITOR_SPACES_FOR_TABS, false);

		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_COLOR, new RGB(63, 127, 95));
		store.setDefault(PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_BOLD, false); //$NON-NLS-1$

		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_COLOR, new RGB(63, 127, 95));
		store.setDefault(PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_BOLD, false); //$NON-NLS-1$

		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_JAVA_KEYWORD_COLOR, new RGB(127, 0, 85));
		store.setDefault(PreferenceConstants.EDITOR_JAVA_KEYWORD_BOLD, true); //$NON-NLS-1$

		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_STRING_COLOR, new RGB(42, 0, 255));
		store.setDefault(PreferenceConstants.EDITOR_STRING_BOLD, false); //$NON-NLS-1$

		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_JAVA_DEFAULT_COLOR, new RGB(0, 0, 0));
		store.setDefault(PreferenceConstants.EDITOR_JAVA_DEFAULT_BOLD, false); //$NON-NLS-1$

		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_JAVADOC_KEYWORD_COLOR, new RGB(127, 159, 191));
		store.setDefault(PreferenceConstants.EDITOR_JAVADOC_KEYWORD_BOLD, true); //$NON-NLS-1$

		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_JAVADOC_TAG_BOLD, new RGB(127, 127, 159));
		store.setDefault(PreferenceConstants.EDITOR_JAVADOC_TAG_BOLD, false); //$NON-NLS-1$

		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_JAVADOC_LINKS_COLOR, new RGB(63, 63, 191));
		store.setDefault(PreferenceConstants.EDITOR_JAVADOC_LINKS_BOLD, false); //$NON-NLS-1$

		PreferenceConverter.setDefault(store, PreferenceConstants.EDITOR_JAVADOC_DEFAULT_COLOR, new RGB(63, 95, 191));
		store.setDefault(PreferenceConstants.EDITOR_JAVADOC_DEFAULT_BOLD, false);		 //$NON-NLS-1$

		store.setDefault(PreferenceConstants.CODEASSIST_AUTOACTIVATION, true);
		store.setDefault(PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY, 500);

		store.setDefault(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
		PreferenceConverter.setDefault(store, PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND, new RGB(254, 241, 233));
		PreferenceConverter.setDefault(store, PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND, new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND, new RGB(254, 241, 233));
		PreferenceConverter.setDefault(store, PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND, new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND, new RGB(255, 255, 0));
		PreferenceConverter.setDefault(store, PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND, new RGB(255, 0, 0));
		store.setDefault(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA, "."); //$NON-NLS-1$
		store.setDefault(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC, "@"); //$NON-NLS-1$
		store.setDefault(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS, true);
		store.setDefault(PreferenceConstants.CODEASSIST_CASE_SENSITIVITY, false);
		store.setDefault(PreferenceConstants.CODEASSIST_ORDER_PROPOSALS, false);
		store.setDefault(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		store.setDefault(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, true);
		store.setDefault(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, false);
		store.setDefault(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, true);

		store.setDefault(PreferenceConstants.EDITOR_SMART_PASTE, true);
		store.setDefault(PreferenceConstants.EDITOR_CLOSE_STRINGS, true);
		store.setDefault(PreferenceConstants.EDITOR_CLOSE_BRACKETS, true);
		store.setDefault(PreferenceConstants.EDITOR_CLOSE_JAVADOCS, true);
		store.setDefault(PreferenceConstants.EDITOR_WRAP_STRINGS, true);
		store.setDefault(PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS, true);
		store.setDefault(PreferenceConstants.EDITOR_FORMAT_JAVADOCS, true);

		store.setDefault(PreferenceConstants.EDITOR_SMART_HOME_END, true);

		store.setDefault(PreferenceConstants.EDITOR_DEFAULT_HOVER, JavaPlugin.ID_BESTMATCH_HOVER);
		store.setDefault(PreferenceConstants.EDITOR_NONE_HOVER, PreferenceConstants.EDITOR_DEFAULT_HOVER_CONFIGURED_ID);
		store.setDefault(PreferenceConstants.EDITOR_CTRL_HOVER, JavaPlugin.ID_SOURCE_HOVER);
		store.setDefault(PreferenceConstants.EDITOR_SHIFT_HOVER, PreferenceConstants.EDITOR_DEFAULT_HOVER_CONFIGURED_ID);
		store.setDefault(PreferenceConstants.EDITOR_CTRL_SHIFT_HOVER, PreferenceConstants.EDITOR_DEFAULT_HOVER_CONFIGURED_ID);
		store.setDefault(PreferenceConstants.EDITOR_CTRL_ALT_HOVER, PreferenceConstants.EDITOR_DEFAULT_HOVER_CONFIGURED_ID);
		store.setDefault(PreferenceConstants.EDITOR_ALT_SHIFT_HOVER, PreferenceConstants.EDITOR_DEFAULT_HOVER_CONFIGURED_ID);
		store.setDefault(PreferenceConstants.EDITOR_CTRL_ALT_SHIFT_HOVER, PreferenceConstants.EDITOR_DEFAULT_HOVER_CONFIGURED_ID);
		
		JavadocPreferencePage.initDefaults(store);
		NewJavaProjectPreferencePage.initDefaults(store);
		MembersOrderPreferencePage.initDefaults(store);		
	}
	
	/**
	 * Returns the JDT-UI preference store.
	 * 
	 * @return the JDT-UI preference store
	 */
	public IPreferenceStore getPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}
	
	/**
	 * Encodes a JRE library to be used in the named preference <code>NEWPROJECT_JRELIBRARY_LIST</code>. 
	 * 
	 * @param description a string value describing the JRE library. The description is used
	 * to indentify the JDR library in the UI
	 * @param entries an array of classpath entries to be encoded
	 * 
	 * @return the encoded string.
	*/
	public static String encodeJRELibrary(String description, IClasspathEntry[] entries) {
		return NewJavaProjectPreferencePage.encodeJRELibrary(description, entries);
	}
	
	/**
	 * Decodes an encoded JRE library and returns its description string.
	 * 
	 * @return the description of an encoded JRE library
	 * 
	 * @see #encodeJRELibrary(String, IClasspathEntry[])
	 */
	public static String decodeJRELibraryDescription(String encodedLibrary) {
		return NewJavaProjectPreferencePage.decodeJRELibraryDescription(encodedLibrary);
	}
	
	/**
	 * Decodes an encoded JRE library and returns its classpath entries.
	 * 
	 * @return the array of classpath entries of an encoded JRE library.
	 * 
	 * @see #encodeJRELibrary(String, IClasspathEntry[])
	 */
	public static IClasspathEntry[] decodeJRELibraryClasspathEntries(String encodedLibrary) {
		return NewJavaProjectPreferencePage.decodeJRELibraryClasspathEntries(encodedLibrary);
	}
	
	/**
	 * Returns the current configuration for the JRE to be used as default in new Java projects.
	 * This is a convenience method to access the named preference <code>NEWPROJECT_JRELIBRARY_LIST
	 * </code> with the index defined by <code> NEWPROJECT_JRELIBRARY_INDEX</code>.
	 *
	 * @return the current default set of classpath entries
	 *  
	 * @see #NEWPROJECT_JRELIBRARY_LIST
	 * @see #NEWPROJECT_JRELIBRARY_INDEX
	 */
	public static IClasspathEntry[] getDefaultJRELibrary() {
		return NewJavaProjectPreferencePage.getDefaultJRELibrary();
	}		
}
