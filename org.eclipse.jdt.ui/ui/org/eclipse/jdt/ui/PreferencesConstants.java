package org.eclipse.jdt.ui;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.texteditor.AbstractTextEditor;

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
import org.eclipse.jdt.internal.ui.preferences.JavaBrowsingPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaEditorPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.RefactoringPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;

/**
 * Preference constants used in the JDT-UI preference store.
 * @since 2.0
  */
public class PreferencesConstants {

	private PreferencesConstants() {
		super();
	}
	
	/**
	 * Enable / Disable showing the method return type. Boolean value.
	 */
	public static final String APPEARANCE_METHOD_RETURNTYPE= AppearancePreferencePage.PREF_METHOD_RETURNTYPE;

	/**
	 * Enable / Disable showing the override indicators. Boolean value.
	 */
	public static final String APPEARANCE_OVERRIDE_INDICATOR= AppearancePreferencePage.PREF_OVERRIDE_INDICATOR;

	/**
	 * Package name compression pattern. String value.
	 */	
	public static final String APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW= AppearancePreferencePage.PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW;

	/**
	 * Enable / Disable showing the package name compression. Boolean value.
	 */	
	public static final String APPEARANCE_COMPRESS_PACKAGE_NAMES= AppearancePreferencePage.PREF_COMPRESS_PACKAGE_NAMES;

	/**
	 * Enable / Disable removing prefixes when creating getters. Boolean value.
	 */	
	public static final String CODEGEN_USE_GETTERSETTER_PREFIX= CodeGenerationPreferencePage.PREF_USE_GETTERSETTER_PREFIX;

	/**
	 * List of prefixes. String value; comma separated list
	 */	
	public static final String CODEGEN_GETTERSETTER_PREFIX= CodeGenerationPreferencePage.PREF_GETTERSETTER_PREFIX;

	/**
	 * Enable / Disable removing suffixes when creating getters. Boolean value.
	 */	
	public static final String CODEGEN_USE_GETTERSETTER_SUFFIX= CodeGenerationPreferencePage.PREF_USE_GETTERSETTER_SUFFIX;

	/**
	 * List of suffixes. String value; comma separated list.
	 */	
	public static final String CODEGEN_GETTERSETTER_SUFFIX= CodeGenerationPreferencePage.PREF_USE_GETTERSETTER_SUFFIX;

	/**
	 * Enable / Disable adding JavaDoc stubs to types and methods. Boolean value.
	 */
	public static final String CODEGEN__JAVADOC_STUBS= CodeGenerationPreferencePage.PREF_JAVADOC_STUBS;

	/**
	 * Enable / Disable adding a non-javadoc comment to methods that override.
	 */
	public static final String CODEGEN__NON_JAVADOC_COMMENTS= CodeGenerationPreferencePage.PREF_NON_JAVADOC_COMMENTS;

	/**
	 * Enable / Disable adding a file comment to new created files. Boolean value.
	 */
	public static final String CODEGEN__FILE_COMMENTS= CodeGenerationPreferencePage.PREF_FILE_COMMENTS;
	
	/**
	 * Specifies the import order. Strung value, semicolon separated.
	 */
	public static final String ORGIMPORTS_IMPORTORDER= ImportOrganizePreferencePage.PREF_IMPORTORDER;
	
	/**
	 * Specifies the number of imports added before using an start-import declaration. Int value.
	 */
	public static final String ORGIMPORTS_ONDEMANDTHRESHOLD= ImportOrganizePreferencePage.PREF_ONDEMANDTHRESHOLD;

	/**
	 * Specifies to never import types that start with a lower case character. Boolean value.
	 */
	public static final String ORGIMPORTS_IGNORELOWERCASE= ImportOrganizePreferencePage.PREF_IGNORELOWERCASE;

	/**
	 * Enable / Disable showing cu children in the packages view. Boolean value.
	 */
	public static final String SHOW_CU_CHILDREN=JavaBasePreferencePage.SHOW_CU_CHILDREN;

	/**
	 * Enable / Disable to update the packages view selection when editors are switched. Boolean value.
	 */
	public static final String LINK_PACKAGES_TO_EDITOR= JavaBasePreferencePage.LINK_PACKAGES_TO_EDITOR;

	/**
	 * Enable / Disable to update the hierarchy selection when editors are switched. Boolean value.
	 */
	public static final String LINK_TYPEHIERARCHY_TO_EDITOR= JavaBasePreferencePage.LINK_TYPEHIERARCHY_TO_EDITOR;

	/**
	 * Enable / Disable to use folders for source and output when creating a new Java project. Boolean value.
	 */
	public static final String SRCBIN_FOLDERS_IN_NEWPROJ= JavaBasePreferencePage.SRCBIN_FOLDERS_IN_NEWPROJ;

	/**
	 * Source folder name used when creating a new Java project. String value.
	 */
	public static final String SRCBIN_SRCNAME= JavaBasePreferencePage.SRCBIN_SRCNAME;

	/**
	 * Output folder name used when creating a new Java project. String value.
	 */
	public static final String SRCBIN_BINNAME= JavaBasePreferencePage.SRCBIN_BINNAME;

	/**
	 * Selects the behaviour when opening a type in the hierarchy. String value, OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE or OPEN_TYPE_HIERARCHY_IN_VIEW_PART.
	 */
	public static final String OPEN_TYPE_HIERARCHY= JavaBasePreferencePage.OPEN_TYPE_HIERARCHY;

	/**
	 * String value used by OPEN_TYPE_HIERARCHY.
	 */
	public static final String OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE= JavaBasePreferencePage.OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE;

	/**
	 * String value used by OPEN_TYPE_HIERARCHY.
	 */
	public static final String OPEN_TYPE_HIERARCHY_IN_VIEW_PART= JavaBasePreferencePage.OPEN_TYPE_HIERARCHY_IN_VIEW_PART;
	
	/**
	 * Selects the behaviour when double clicking in the packages view. String value, DOUBLE_CLICK_GOES_INTO or DOUBLE_CLICK_EXPANDS.
	 */
	public static final String DOUBLE_CLICK= JavaBasePreferencePage.DOUBLE_CLICK;

	/**
	 * String value used by DOUBLE_CLICK.
	 */
	public static final String DOUBLE_CLICK_GOES_INTO= JavaBasePreferencePage.DOUBLE_CLICK_GOES_INTO;

	/**
	 * String value used by DOUBLE_CLICK.
	 */
	public static final String DOUBLE_CLICK_EXPANDS= JavaBasePreferencePage.DOUBLE_CLICK_EXPANDS;

	/**
	 * Enable / Disable showing reconciled elements in viewes. Boolean value.
	 */
	public static final String RECONCILE_JAVA_VIEWS= JavaBasePreferencePage.RECONCILE_JAVA_VIEWS;

	/**
	 * Path of the Javadoc command used by the Javadoc creation wizard. String value.
	 */
	public static final String JAVADOC_COMMAND= JavadocPreferencePage.PREF_JAVADOC_COMMAND;


	/**
	 * Enable / Disable the highlighting of matching brackets. Boolean value.
	 */
	public final static String EDITOR_MATCHING_BRACKETS= CompilationUnitEditor.MATCHING_BRACKETS;

	/**
	 * Color used for highlighting matching brackets. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_MATCHING_BRACKETS_COLOR=  CompilationUnitEditor.MATCHING_BRACKETS_COLOR;

	/**
	 * Enable / Disable highlighting the current line. Boolean value.
	 */
	public final static String EDITOR_CURRENT_LINE= CompilationUnitEditor.CURRENT_LINE;

	/**
	 * Color used for highlighting the current line. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_CURRENT_LINE_COLOR= CompilationUnitEditor.CURRENT_LINE_COLOR;

	/**
	 * Enable / Disable the print margin. Boolean value.
	 */
	public final static String EDITOR_PRINT_MARGIN= CompilationUnitEditor.PRINT_MARGIN;
	
	/**
	 * Color used for the print margin. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_PRINT_MARGIN_COLOR= CompilationUnitEditor.PRINT_MARGIN_COLOR;

	/**
	 * Print margin column. Int value.
	 */
	public final static String EDITOR_PRINT_MARGIN_COLUMN= CompilationUnitEditor.PRINT_MARGIN_COLUMN;

	/**
	 * Color used for the find/replace scope. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_FIND_SCOPE_COLOR= AbstractTextEditor.PREFERENCE_COLOR_FIND_SCOPE;

	/**
	 * Enable / Disable using spaces for tabs in the editor. Boolean value.
	 */
	public final static String EDITOR_SPACES_FOR_TABS= CompilationUnitEditor.SPACES_FOR_TABS;

	/**
	 * Number of spaces per tab in the editor. Int value.
	 */
	public final static String EDITOR_TAB_WIDTH= JavaSourceViewerConfiguration.PREFERENCE_TAB_WIDTH;

	/**
	 * Enable / Disable showing problem indicators. Boolean value.
	 */
	public final static String EDITOR_PROBLEM_INDICATION= CompilationUnitEditor.PROBLEM_INDICATION;

	/**
	 * Color used for the problem indicators. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_PROBLEM_INDICATION_COLOR= CompilationUnitEditor.PROBLEM_INDICATION_COLOR;

	/**
	 * Enable / Disable showing the correction indicator. Boolean value.
	 */
	public final static String EDITOR_CORRECTION_INDICATION= WorkInProgressPreferencePage.PREF_SHOW_TEMP_PROBLEMS;

	/**
	 * Enable / Disable evaluating temporary problems. Boolean value.
	 */
	public final static String EDITOR_EVALUTE_TEMPORARY_PROBLEMS= CompilationUnitDocumentProvider.HANDLE_TEMPORARY_PROBLEMS;

	/**
	 * Enable / Disable the overview ruler. Boolean value.
	 */
	public final static String EDITOR_OVERVIEW_RULER= CompilationUnitEditor.OVERVIEW_RULER;

	/**
	 * Enable / Disable the line number ruler. Boolean value.
	 */
	public final static String EDITOR_LINE_NUMBER_RULER= JavaEditor.LINE_NUMBER_RULER;

	/**
	 * Color used for the line number ruler. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_LINE_NUMBER_RULER_COLOR= JavaEditor.LINE_NUMBER_COLOR;

	/**
	 * Color used for the linked positions (templates). RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_LINKED_POSITION_COLOR= CompilationUnitEditor.LINKED_POSITION_COLOR;

	/**
	 * Color used for the text foreground. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_FOREGROUND_COLOR= AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND;

	/**
	 * Default color used for text foreground. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_FOREGROUND_DEFAULT_COLOR= AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT;

	/**
	 * Color used for the text background. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_BACKGROUND_COLOR= AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND;

	/**
	 * Default color used for the text background. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_BACKGROUND_DEFAULT_COLOR= AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT;

	/**
	 * Color used for multi line comments. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_MULTI_LINE_COMMENT_COLOR= IJavaColorConstants.JAVA_MULTI_LINE_COMMENT;

	/**
	 * Enable / Disable the usage of bold style for multi line comments.
	 */
	public final static String EDITOR_MULTI_LINE_COMMENT_BOLD= IJavaColorConstants.JAVA_MULTI_LINE_COMMENT + JavaEditorPreferencePage.BOLD;

	/**
	 * Color used for single line comments. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_SINGLE_LINE_COMMENT_COLOR= IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT;

	/**
	 * Enable / Disable the usage of bold style for single line comments.
	 */
	public final static String EDITOR_SINGLE_LINE_COMMENT_BOLD= IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT + JavaEditorPreferencePage.BOLD;

	/**
	 * Color used for java keywords. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_JAVA_KEYWORD_COLOR= IJavaColorConstants.JAVA_KEYWORD;

	/**
	 * Enable / Disable the usage of bold style for java keywords.
	 */
	public final static String EDITOR_JAVA_KEYWORD_BOLD= IJavaColorConstants.JAVA_KEYWORD + JavaEditorPreferencePage.BOLD;

	/**
	 * Color used for string constants. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_STRING_COLOR= IJavaColorConstants.JAVA_STRING;

	/**
	 * Enable / Disable the usage of bold style for string constants.
	 */
	public final static String EDITOR_STRING_BOLD= IJavaColorConstants.JAVA_STRING + JavaEditorPreferencePage.BOLD;

	/**
	 * Color used for java default text. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_JAVA_DEFAULT_COLOR= IJavaColorConstants.JAVA_DEFAULT;

	/**
	 * Enable / Disable the usage of bold style for java default text.
	 */
	public final static String EDITOR_JAVA_DEFAULT_BOLD= IJavaColorConstants.JAVA_DEFAULT + JavaEditorPreferencePage.BOLD;

	/**
	 * Color used for javadoc keywords. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_JAVADOC_KEYWORD_COLOR= IJavaColorConstants.JAVADOC_KEYWORD;

	/**
	 * Enable / Disable the usage of bold style for javadoc keywords.
	 */
	public final static String EDITOR_JAVADOC_KEYWORD_BOLD= IJavaColorConstants.JAVADOC_KEYWORD + JavaEditorPreferencePage.BOLD;

	/**
	 * Color used for javadoc tags. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_JAVADOC_TAG_COLOR= IJavaColorConstants.JAVADOC_TAG;

	/**
	 * Enable / Disable the usage of bold style for javadoc tags.
	 */
	public final static String EDITOR_JAVADOC_TAG_BOLD= IJavaColorConstants.JAVADOC_TAG + JavaEditorPreferencePage.BOLD;

	/**
	 * Color used for javadoc links. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_JAVADOC_LINKS_COLOR= IJavaColorConstants.JAVADOC_LINK;

	/**
	 * Enable / Disable the usage of bold style for javadoc links.
	 */
	public final static String EDITOR_JAVADOC_LINKS_BOLD= IJavaColorConstants.JAVADOC_LINK + JavaEditorPreferencePage.BOLD;
		
	/**
	 * Color used for javadoc default text. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String EDITOR_JAVADOC_DEFAULT_COLOR= IJavaColorConstants.JAVADOC_DEFAULT;

	/**
	 * Enable / Disable the usage of bold style for javadoc default text.
	 */
	public final static String EDITOR_JAVADOC_DEFAULT_BOLD= IJavaColorConstants.JAVADOC_DEFAULT + JavaEditorPreferencePage.BOLD;

	/**
	 * Enable / Disable the auto activation of the Java code assist. Boolean value.
	 */
	public final static String CODEASSIST_AUTOACTIVATION= ContentAssistPreference.AUTOACTIVATION;

	/**
	 * Specifies the auto activation delay time. Int value.
	 */
	public final static String CODEASSIST_AUTOACTIVATION_DELAY= ContentAssistPreference.AUTOACTIVATION_DELAY;

	/**
	 * Show only visible elements as code assist proposals. Boolean value.
	 */
	public final static String CODEASSIST_SHOW_VISIBLE_PROPOSALS= ContentAssistPreference.SHOW_VISIBLE_PROPOSALS;

	/**
	 * Enable / Disable case sensitivity for filtering code assist proposals. Boolean value.
	 */
	public final static String CODEASSIST_CASE_SENSITIVITY= ContentAssistPreference.CASE_SENSITIVITY;
	
	/**
	 * Enable / Disable ordering of code assist proposals. Boolean value.
	 */
	public final static String CODEASSIST_ORDER_PROPOSALS= ContentAssistPreference.ORDER_PROPOSALS;

	/**
	 * Enable / Disable filling argument names for method completions. Boolean value.
	 */
	public final static String CODEASSIST_FILL_ARGUMENT_NAMES= ContentAssistPreference.FILL_METHOD_ARGUMENTS;

	/**
	 * The characters that select an entry in the code assist selection dialog on Java code. String value. String containing all characters.
	 */
	public final static String CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA= ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVA;

	/**
	 * The characters that select an entry in the code assist selection dialog on Javadoc code. String value. String containing all characters.
	 */
	public final static String CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC= ContentAssistPreference.AUTOACTIVATION_TRIGGERS_JAVADOC;

	/**
	 * Background color used in the code assist selection dialog. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String CODEASSIST_PROPOSALS_BACKGROUND= ContentAssistPreference.PROPOSALS_BACKGROUND;

	/**
	 * Foreground color used in the code assist selection dialog. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String CODEASSIST_PROPOSALS_FOREGROUND= ContentAssistPreference.PROPOSALS_FOREGROUND;
	
	/**
	 * Background color used for parameter hints. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String CODEASSIST_PARAMETERS_BACKGROUND= ContentAssistPreference.PARAMETERS_BACKGROUND;

	/**
	 * Foreground color used for parameter hints. RGB color encoded using PreferenceConverter.
	 * @see org.eclipse.jface.resource.StringConverter
	 * @see org.eclipse.jface.preference.PreferenceConverter
	 */
	public final static String CODEASSIST_PARAMETERS_FOREGROUND= ContentAssistPreference.PARAMETERS_FOREGROUND;

	/**
	 * Sets the behaviour of the refactoring wizard for showing the error page. String value:
	 * REFACTOR_FATAL_SEVERITY, REFACTOR_ERROR_SEVERITY, REFACTOR_WARNING_SEVERITY, REFACTOR_INFO_SEVERITY or REFACTOR_OK_SEVERITY.
	 */
	public static final String REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD= RefactoringPreferencePage.PREF_ERROR_PAGE_SEVERITY_THRESHOLD;

	/**
	 * String value used by REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD.
	 */
	public static final String REFACTOR_FATAL_SEVERITY= RefactoringPreferencePage.FATAL_SEVERITY;
	
	/**
	 * String value used by REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD.
	 */	
	public static final String REFACTOR_ERROR_SEVERITY= RefactoringPreferencePage.ERROR_SEVERITY;

	/**
	 * String value used by REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD.
	 */
	public static final String REFACTOR_WARNING_SEVERITY= RefactoringPreferencePage.WARNING_SEVERITY;

	/**
	 * String value used by REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD.
	 */
	public static final String REFACTOR_INFO_SEVERITY= RefactoringPreferencePage.INFO_SEVERITY;

	/**
	 * String value used by REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD.
	 */
	public static final String REFACTOR_OK_SEVERITY= RefactoringPreferencePage.OK_SEVERITY;

	/**
	 * Automatically save all editor before refactoring.
	 */
	public static final String REFACTOR_SAVE_ALL_EDITORS= RefactoringPreferencePage.PREF_SAVE_ALL_EDITORS;

	/**
	 * Link Java Browsing views to the active editor. Boolean value.
	 */
	public static final String BROWSING_LINK_VIEW_TO_EDITOR= JavaBrowsingPreferencePage.LINK_VIEW_TO_EDITOR;

	/**
	 * Stack Java Browsing views vertically. Boolean value.
	 */
	public static final String BROWSING_STACK_VERTICALLY= JavaBrowsingPreferencePage.STACK_VERTICALLY;
	
	
	/**
	 * Returns the jdt ui preference store.
	 */
	public IPreferenceStore getPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}

}
