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

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.jdt.ui.text.IJavaColorConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.AppearancePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.CodeGenerationPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaEditorPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.NewJavaProjectPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.RefactoringPreferencePage;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;

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
	public static final String APPEARANCE_METHOD_RETURNTYPE= AppearancePreferencePage.PREF_METHOD_RETURNTYPE;

	/**
	 * A named preference that controls if override indicators are rendered in the UI.
	 * <p>
	 * Value is of type <code>Boolean</code>: if <code>true</code> override 
	 * indicators are rendered
	 * </p>
	 */
	public static final String APPEARANCE_OVERRIDE_INDICATOR= AppearancePreferencePage.PREF_OVERRIDE_INDICATOR;

	/**
	 * A named preference that defines the pattern used for package name compression.
	 * <p>
	 * Value is of type <code>String</code>. For example foe the given package name 'org.eclipse.jdt' pattern
	 * '.' will compress it to '..jdt', '1~' to 'o~.e~.jdt'.
	 * </p>
	 */	
	public static final String APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW= AppearancePreferencePage.PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW;

	/**
	 * A named preference that controls if package name compression is turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * 
	 * @see #APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW
	 */	
	public static final String APPEARANCE_COMPRESS_PACKAGE_NAMES= AppearancePreferencePage.PREF_COMPRESS_PACKAGE_NAMES;

	/**
	 * A named preference that controls if prefix removal during setter/getter generation is turned on or off. 
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public static final String CODEGEN_USE_GETTERSETTER_PREFIX= CodeGenerationPreferencePage.PREF_USE_GETTERSETTER_PREFIX;

	/**
	 * A named preference that holds a list of prefixes to be removed from a local variable to compute setter 
	 * and gettter names.
	 * <p>
	 * Value is of type <code>String</code>: comma separated list of prefixed
	 * </p>
	 * 
	 * @see #CODEGEN_USE_GETTERSETTER_PREFIX
	 */	
	public static final String CODEGEN_GETTERSETTER_PREFIX= CodeGenerationPreferencePage.PREF_GETTERSETTER_PREFIX;

	/**
	 * A named preference that controls if suffix removal during setter/getter generation is turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public static final String CODEGEN_USE_GETTERSETTER_SUFFIX= CodeGenerationPreferencePage.PREF_USE_GETTERSETTER_SUFFIX;

	/**
	 * A named preference that holds a list of suffixes to be removed from a local variable to compute setter 
	 * and getter names.
	 * <p>
	 * Value is of type <code>String</code>: comma separated list of suffixes
	 * </p>
	 * 
	 * @see #CODEGEN_USE_GETTERSETTER_SUFFIX
	 */	
	public static final String CODEGEN_GETTERSETTER_SUFFIX= CodeGenerationPreferencePage.PREF_GETTERSETTER_SUFFIX;

	/**
	 * A name preference that controls if a JavaDoc stub gets added to newly created types and methods.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String CODEGEN__JAVADOC_STUBS= CodeGenerationPreferencePage.PREF_JAVADOC_STUBS;

	/**
	 * A named preference that controls if a non-javadoc comment gets added to methods generated via the 
	 * "Override Methods" operation.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String CODEGEN__NON_JAVADOC_COMMENTS= CodeGenerationPreferencePage.PREF_NON_JAVADOC_COMMENTS;

	/**
	 * A named preference that controls if a file comment gets added to newly created files.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String CODEGEN__FILE_COMMENTS= CodeGenerationPreferencePage.PREF_FILE_COMMENTS;
	
	/**
	 * A named preference that holds a list of comma separated package names. The list specifies the import order used by
	 * the "Organize Imports" opeation.
	 * <p>
	 * Value is of type <code>String</code>: comma separated list of package names
	 * </p>
	 */
	public static final String ORGIMPORTS_IMPORTORDER= ImportOrganizePreferencePage.PREF_IMPORTORDER;
	
	/**
	 * A named preference that specifies the number of imports added before a star-import declaration is used.
	 * <p>
	 * Value is of type <code>Int</code>: positive value specifing the number of non star-import is used
	 * </p>
	 */
	public static final String ORGIMPORTS_ONDEMANDTHRESHOLD= ImportOrganizePreferencePage.PREF_ONDEMANDTHRESHOLD;

	/**
	 * A named preferences that controls if types that start with a lower case letters get added by the
	 * "Organize Import" operation.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String ORGIMPORTS_IGNORELOWERCASE= ImportOrganizePreferencePage.PREF_IGNORELOWERCASE;

	/**
	 * A named preference that speficies whether children of a compilation unit are shown in the package explorer.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String SHOW_CU_CHILDREN=AppearancePreferencePage.SHOW_CU_CHILDREN;

	/**
	 * A named preference that controls whether the package explorer's selection is linked to the active editor.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String LINK_PACKAGES_TO_EDITOR= JavaBasePreferencePage.LINK_PACKAGES_TO_EDITOR;

	/**
	 * A named preference that controls whether the hierarchy view's selection is linked to the active editor.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String LINK_TYPEHIERARCHY_TO_EDITOR= JavaBasePreferencePage.LINK_TYPEHIERARCHY_TO_EDITOR;

	/**
	 * A named preference that controls whether new projects are generated using source and output folder.
	 * <p>
	 * Value is of type <code>Boolean</code>. if <code>true</code> new projects are created with a source and
	 * output folder. If <code>false</code> source and output folder equals to the project.
	 * </p>
	 */
	public static final String SRCBIN_FOLDERS_IN_NEWPROJ= NewJavaProjectPreferencePage.SRCBIN_FOLDERS_IN_NEWPROJ;

	/**
	 * A named preference that specifies the source folder name used when creating a new Java project. Value is inactive
	 * if <code>SRCBIN_FOLDERS_IN_NEWPROJ</code> is set to <code>false</code>.
	 * <p>
	 * Value is of type <code>String</code>. 
	 * </p>
	 * 
	 * @see #SRCBIN_FOLDERS_IN_NEWPROJ
	 */
	public static final String SRCBIN_SRCNAME= NewJavaProjectPreferencePage.SRCBIN_SRCNAME;

	/**
	 * A named preference that specifies the output folder name used when creating a new Java project. Value is inactive
	 * if <code>SRCBIN_FOLDERS_IN_NEWPROJ</code> is set to <code>false</code>.
	 * <p>
	 * Value is of type <code>String</code>. 
	 * </p>
	 * 
	 * @see #SRCBIN_FOLDERS_IN_NEWPROJ
	 */
	public static final String SRCBIN_BINNAME= NewJavaProjectPreferencePage.SRCBIN_BINNAME;

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
	public static final String NEWPROJECT_JRELIBRARY_LIST= NewJavaProjectPreferencePage.CLASSPATH_JRELIBRARY_LIST;

	/**
	 * A named preferences that specifies the current active JRE library.
	 * <p>
	 * Value is of type <code>Int</code>: an index into the list of possible JRE libraries.
	 * </p>
	 * 
	 * @see #NEWPROJECT_JRELIBRARY_LIST
	 */
	public static final String NEWPROJECT_JRELIBRARY_INDEX= NewJavaProjectPreferencePage.CLASSPATH_JRELIBRARY_INDEX;

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
	public static final String OPEN_TYPE_HIERARCHY= JavaBasePreferencePage.OPEN_TYPE_HIERARCHY;

	/**
	 * A string value used by the named preference <code>OPEN_TYPE_HIERARCHY</code>.
	 * 
	 * @see #OPEN_TYPE_HIERARCHY
	 */
	public static final String OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE= JavaBasePreferencePage.OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE;

	/**
	 * A string value used by the named preference <code>OPEN_TYPE_HIERARCHY</code>.
	 * 
	 * @see #OPEN_TYPE_HIERARCHY
	 */
	public static final String OPEN_TYPE_HIERARCHY_IN_VIEW_PART= JavaBasePreferencePage.OPEN_TYPE_HIERARCHY_IN_VIEW_PART;
	
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
	public static final String DOUBLE_CLICK= JavaBasePreferencePage.DOUBLE_CLICK;

	/**
	 * A string value used by the named preference <code>DOUBLE_CLICK</code>.
	 * 
	 * @see #DOUBLE_CLICK
	 */
	public static final String DOUBLE_CLICK_GOES_INTO= JavaBasePreferencePage.DOUBLE_CLICK_GOES_INTO;

	/**
	 * A string value used by the named preference <code>DOUBLE_CLICK</code>.
	 * 
	 * @see #DOUBLE_CLICK
	 */
	public static final String DOUBLE_CLICK_EXPANDS= JavaBasePreferencePage.DOUBLE_CLICK_EXPANDS;

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
	public static final String UPDATE_JAVA_VIEWS= JavaBasePreferencePage.UPDATE_JAVA_VIEWS;

	/**
	 * A string value used by the named preference <code>UPDATE_JAVA_VIEWS</code>
	 * 
	 * @see #UPDATE_JAVA_VIEWS
	 */
	public static final String UPDATE_ON_SAVE= JavaBasePreferencePage.UPDATE_ON_SAVE;

	/**
	 * A string value used by the named preference <code>UPDATE_JAVA_VIEWS</code>
	 * 
	 * @see #UPDATE_JAVA_VIEWS
	 */
	public static final String UPDATE_WHILE_EDITING= JavaBasePreferencePage.UPDATE_WHILE_EDITING;

	/**
	 * A named preference that holds the path of the Javadoc command used by the Javadoc creation wizard.
	 * <p>
	 * Value is of type <code>String</code>.
	 * </p>
	 */
	public static final String JAVADOC_COMMAND= JavadocPreferencePage.PREF_JAVADOC_COMMAND;

	/**
	 * A named preference that controls whether bracket matching highlighting is turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_MATCHING_BRACKETS= CompilationUnitEditor.MATCHING_BRACKETS;

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
	public final static String EDITOR_MATCHING_BRACKETS_COLOR=  CompilationUnitEditor.MATCHING_BRACKETS_COLOR;

	/**
	 * A named preference that controls whether the current line highlighting is turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_CURRENT_LINE= CompilationUnitEditor.CURRENT_LINE;

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
	public final static String EDITOR_CURRENT_LINE_COLOR= CompilationUnitEditor.CURRENT_LINE_COLOR;

	/**
	 * A named preference that controls whether the print margin is turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_PRINT_MARGIN= CompilationUnitEditor.PRINT_MARGIN;
	
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
	public final static String EDITOR_PRINT_MARGIN_COLOR= CompilationUnitEditor.PRINT_MARGIN_COLOR;

	/**
	 * Print margin column. Int value.
	 */
	public final static String EDITOR_PRINT_MARGIN_COLUMN= CompilationUnitEditor.PRINT_MARGIN_COLUMN;

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
	public final static String EDITOR_SPACES_FOR_TABS= CompilationUnitEditor.SPACES_FOR_TABS;

	/**
	 * A named preference that holds the number of spaces used per tab in the editor.
	 * <p>
	 * Value is of type <code>Int</code>: positive int value specifying the number of
	 * spaces per tab.
	 * </p>
	 */
	public final static String EDITOR_TAB_WIDTH= JavaSourceViewerConfiguration.PREFERENCE_TAB_WIDTH;

	/**
	 * A named preference that controls whether the editor shows problem indicators in text (squiggly lines). 
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_PROBLEM_INDICATION= CompilationUnitEditor.ERROR_INDICATION;

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
	public final static String EDITOR_PROBLEM_INDICATION_COLOR= CompilationUnitEditor.ERROR_INDICATION_COLOR;

	/**
	 * A named preference that controls whether the editor shows warning indicators in text (squiggly lines). 
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_WARNING_INDICATION= CompilationUnitEditor.WARNING_INDICATION;

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
	public final static String EDITOR_WARNING_INDICATION_COLOR= CompilationUnitEditor.WARNING_INDICATION_COLOR;
	
	/**
	 * A named preference that controls whether the editor shows task indicators in text (squiggly lines). 
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_TASK_INDICATION= CompilationUnitEditor.MARKER_INDICATION;

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
	public final static String EDITOR_TASK_INDICATION_COLOR= CompilationUnitEditor.MARKER_INDICATION_COLOR;

	/**
	 * A named preference that controls if correction indicators are shown in the UI.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_CORRECTION_INDICATION= JavaEditorPreferencePage.PREF_SHOW_TEMP_PROBLEMS;

	/**
	 * A named preference that controls if temporary problems are evaluated and shown in the UI.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_EVALUTE_TEMPORARY_PROBLEMS= CompilationUnitDocumentProvider.HANDLE_TEMPORARY_PROBLEMS;

	/**
	 * A named preference that controls if the overview ruler is shown in the UI.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_OVERVIEW_RULER= CompilationUnitEditor.OVERVIEW_RULER;

	/**
	 * A named preference that controls if the line number ruler is shown in the UI.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String EDITOR_LINE_NUMBER_RULER= JavaEditor.LINE_NUMBER_RULER;

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
	public final static String EDITOR_LINE_NUMBER_RULER_COLOR= JavaEditor.LINE_NUMBER_COLOR;

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
	public final static String EDITOR_LINKED_POSITION_COLOR= CompilationUnitEditor.LINKED_POSITION_COLOR;

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
	 * A named preference that holds the default color used as text foreground.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
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
	 * A named preference that holds the default color used as the text background.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_BACKGROUND_DEFAULT_COLOR= AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT;

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
	public final static String EDITOR_MULTI_LINE_COMMENT_BOLD= IJavaColorConstants.JAVA_MULTI_LINE_COMMENT + JavaEditorPreferencePage.BOLD;

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
	public final static String EDITOR_SINGLE_LINE_COMMENT_BOLD= IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT + JavaEditorPreferencePage.BOLD;

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
	public final static String EDITOR_JAVA_KEYWORD_BOLD= IJavaColorConstants.JAVA_KEYWORD + JavaEditorPreferencePage.BOLD;

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
	public final static String EDITOR_STRING_BOLD= IJavaColorConstants.JAVA_STRING + JavaEditorPreferencePage.BOLD;

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
	public final static String EDITOR_JAVA_DEFAULT_BOLD= IJavaColorConstants.JAVA_DEFAULT + JavaEditorPreferencePage.BOLD;

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
	public final static String EDITOR_JAVADOC_KEYWORD_BOLD= IJavaColorConstants.JAVADOC_KEYWORD + JavaEditorPreferencePage.BOLD;

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
	public final static String EDITOR_JAVADOC_TAG_BOLD= IJavaColorConstants.JAVADOC_TAG + JavaEditorPreferencePage.BOLD;

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
	public final static String EDITOR_JAVADOC_LINKS_BOLD= IJavaColorConstants.JAVADOC_LINK + JavaEditorPreferencePage.BOLD;
		
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
	public final static String EDITOR_JAVADOC_DEFAULT_BOLD= IJavaColorConstants.JAVADOC_DEFAULT + JavaEditorPreferencePage.BOLD;

	/**
	 * A named preference that controls whether hover tooltips in the editor are turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String EDITOR_SHOW_HOVER= "org.eclipse.jdt.ui.editor.showHover"; //$NON-NLS-1$

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
	public final static String CODEASSIST_AUTOACTIVATION= ContentAssistPreference.AUTOACTIVATION;

	/**
	 * A name preference that holds the auto activation delay time in milli seconds.
	 * <p>
	 * Value is of type <code>Int</code>.
	 * </p>
	 */
	public final static String CODEASSIST_AUTOACTIVATION_DELAY= ContentAssistPreference.AUTOACTIVATION_DELAY;

	/**
	 * A named preference that controls if code assist contains only visible proposals.
	 * <p>
	 * Value is of type <code>Boolean</code>. if <code>true<code> code assist only contains visible members. If 
	 * <code>false</code> all members are included.
	 * </p>
	 */
	public final static String CODEASSIST_SHOW_VISIBLE_PROPOSALS= ContentAssistPreference.SHOW_VISIBLE_PROPOSALS;

	/**
	 * A named preference that controls whether code assist proposals filtering is case sensitive or not.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String CODEASSIST_CASE_SENSITIVITY= ContentAssistPreference.CASE_SENSITIVITY;
	
	/**
	 * A named preference that defines if code assist proposals are sorted in alphabetical order.
	 * <p>
	 * Value is of type <code>Boolean</code>. If <code>true</code> that are sorted in alphabetical 
	 * order. If <code>false</code> that are unsorted.
	 * </p>
	 */
	public final static String CODEASSIST_ORDER_PROPOSALS= ContentAssistPreference.ORDER_PROPOSALS;

	/**
	 * A named preference that controls if argument names are filled in when a method is selected from as list
	 * of code assist proposal.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String CODEASSIST_FILL_ARGUMENT_NAMES= ContentAssistPreference.FILL_METHOD_ARGUMENTS;

	/**
	 * A named preference that holds the characters that auto activate code assist in Java code.
	 * <p>
	 * Value is of type <code>Sring</code>. All characters that trigger auto code assist in Java code.
	 * </p>
	 */
	public final static String CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA= ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVA;

	/**
	 * A named preference that holds the characters that auto activate code assist in Javadoc.
	 * <p>
	 * Value is of type <code>Sring</code>. All characters that trigger auto code assist in Javadoc.
	 * </p>
	 */
	public final static String CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC= ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVADOC;

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
	public final static String CODEASSIST_PROPOSALS_BACKGROUND= ContentAssistPreference.PROPOSALS_BACKGROUND;

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
	public final static String CODEASSIST_PROPOSALS_FOREGROUND= ContentAssistPreference.PROPOSALS_FOREGROUND;
	
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
	public final static String CODEASSIST_PARAMETERS_BACKGROUND= ContentAssistPreference.PARAMETERS_BACKGROUND;

	/**
	 * A named preference that holds the foreground color used for parameter hints.
	 * <p>
	 * Value is of type <code>String</code>. A RGB color value encoded as a string
	 * using class <code>PreferenceConverter</code>
	 * </p>
	 * 
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String CODEASSIST_PARAMETERS_FOREGROUND= ContentAssistPreference.PARAMETERS_FOREGROUND;

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
	public static final String REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD= RefactoringPreferencePage.PREF_ERROR_PAGE_SEVERITY_THRESHOLD;

	/**
	 * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>.
	 * 
	 * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
	 */
	public static final String REFACTOR_FATAL_SEVERITY= RefactoringPreferencePage.FATAL_SEVERITY;
	
	/**
	 * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>.
	 * 
	 * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
	 */	
	public static final String REFACTOR_ERROR_SEVERITY= RefactoringPreferencePage.ERROR_SEVERITY;

	/**
	 * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>.
	 * 
	 * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
	 */
	public static final String REFACTOR_WARNING_SEVERITY= RefactoringPreferencePage.WARNING_SEVERITY;

	/**
	 * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>.
	 * 
	 * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
	 */
	public static final String REFACTOR_INFO_SEVERITY= RefactoringPreferencePage.INFO_SEVERITY;

	/**
	 * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>.
	 * 
	 * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
	 */
	public static final String REFACTOR_OK_SEVERITY= RefactoringPreferencePage.OK_SEVERITY;

	/**
	 * A named preference thet controls whether all dirty editors are automatically saved before a refactoring is
	 * executed.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String REFACTOR_SAVE_ALL_EDITORS= RefactoringPreferencePage.PREF_SAVE_ALL_EDITORS;

	/**
	 * A named preference that controls if the Java Browsing views are linked to the active editor.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * 
	 * @see #LINK_PACKAGES_TO_EDITOR
	 */
	public static final String BROWSING_LINK_VIEW_TO_EDITOR= JavaBasePreferencePage.LINK_BROWSING_VIEW_TO_EDITOR;

	/**
	 * A named preference that controls the layout of the Java Browsing views vertically. Boolean value.
	 * <p>
	 * Value is of type <code>Boolean</code>. If <code>true<code> the views are stacked vertical.
	 * If <code>false</code> they are stacked horizontal.
	 * </p>
	 */
	public static final String BROWSING_STACK_VERTICALLY= AppearancePreferencePage.STACK_BROWSING_VIEWS_VERTICALLY;
	
	
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
