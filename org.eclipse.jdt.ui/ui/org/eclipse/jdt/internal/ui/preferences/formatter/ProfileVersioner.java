/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;


public class ProfileVersioner {
	
	public static final int VERSION_1= 1; // < 20040113 (incl. M6)
	public static final int VERSION_2= 2; // before renaming almost all
	public static final int VERSION_3= 3; // after renaming almost all
	
	/**
	 * Profile version
	 */
	public static final int CURRENT_VERSION= 3;
	

	public static void updateAndComplete(CustomProfile profile) {
		final Map oldSettings= profile.getSettings();
		final Map newSettings= ProfileManager.getDefaultSettings();
		
		switch (profile.getVersion()) {

		/**
		 * Insert updating code here without using breaks, in order to get updates
		 * across several version numbers.
		 */
		case VERSION_1:
			version1to2(oldSettings);
			
		case VERSION_2:
			version2to3(oldSettings);

			
		default:
			for (final Iterator iter= oldSettings.keySet().iterator(); iter.hasNext(); ) {
				final String key= (String)iter.next();
				if (!newSettings.containsKey(key)) 
				    continue;
				
				final String value= (String)oldSettings.get(key);
				if (value != null) {
				    newSettings.put(key, value);
				}
			}
		}
		profile.setVersion(CURRENT_VERSION);
		profile.setSettings(newSettings);
	}
	
	private static void version1to2(final Map oldSettings) {
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_WITHIN_MESSAGE_SEND,	
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_MESSAGE_SEND,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_MESSAGE_SEND);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
		
		checkAndReplace(oldSettings, 
			JavaCore.PLUGIN_ID + ".formatter.inset_space_between_empty_arguments", //$NON-NLS-1$
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARGUMENTS);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_METHOD_DECLARATION_OPEN_PAREN,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CONSTRUCTOR_DECLARATION_OPEN_PAREN);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_WITHIN_MESSAGE_SEND,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_MESSAGE_SEND,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_MESSAGE_SEND);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
	}

	public static int getVersionStatus(CustomProfile profile) {
		final int version= profile.getVersion();
		if (version < CURRENT_VERSION) 
			return -1;
		else if (version > CURRENT_VERSION)
			return 1;
		else 
			return 0;
	}
	
	
	private static void mapOldValueRangeToNew(Map settings, String oldKey, String [] oldValues,
		String newKey, String [] newValues) {

		if (!settings.containsKey(oldKey)) 
			return;
		
		final String value= ((String)settings.get(oldKey));

		if (value == null) 
			return;
		
		for (int i = 0; i < oldValues.length; i++) {
			if (value.equals(oldValues[i])) {
				settings.put(newKey, newValues[i]);
			}
		}

	}
	
	
	private static void checkAndReplace(Map settings, String oldKey, String newKey) {
		checkAndReplace(settings, oldKey, new String [] {newKey});
	}
	
	private static void checkAndReplace(Map settings, String oldKey, String newKey1, String newKey2) {
		checkAndReplace(settings, oldKey, new String [] {newKey1, newKey2});
	}

	private static void checkAndReplace(Map settings, String oldKey, String [] newKeys) {
		if (!settings.containsKey(oldKey)) 
			return;
		
		final String value= (String)settings.get(oldKey);

		if (value == null) 
			return;
		
		for (int i = 0; i < newKeys.length; i++) {
			settings.put(newKeys[i], value);
		}
	}
	
	
	
	private static void version2to3(Map oldSettings) {
		
		/**
		 * This is for the white space settings.
		 */
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_ARRAY_INITIALIZER_CONTINUATION_INDENTATION,
			DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION_FOR_ARRAY_INITIALIZER);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BLOCK_CLOSE_BRACE,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_CATCH_EXPRESSION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CATCH,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CATCH);

		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_MESSAGE_SEND,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_MESSAGE_SEND,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION);

		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_FOR_PARENS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_FOR, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_FOR);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_IF_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_IF, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_IF);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_SWITCH_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SWITCH, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SWITCH);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_SYNCHRONIZED_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SYNCHRONIZED, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SYNCHRONIZED);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_IN_WHILE_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_WHILE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_WHILE);

		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_ARGUMENTS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MESSAGESEND_ARGUMENTS,  
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_ARGUMENTS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS);

		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_ARGUMENTS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS);

		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_THROWS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_THROWS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_THROWS);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_MESSAGE_SEND, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_MESSAGE_SEND, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_ARGUMENTS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_THROWS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_THROWS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS);

		checkAndReplace(oldSettings, 
		    DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS,
		    DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS);	 

		checkAndReplace(oldSettings, 
		    DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICITCONSTRUCTORCALL_ARGUMENTS,
		    DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS);	 

		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MESSAGESEND_ARGUMENTS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_ARGUMENTS,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_FIRST_ARGUMENT, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_MESSAGE_SEND, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ANONYMOUS_TYPE_OPEN_BRACE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BLOCK_OPEN_BRACE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CATCH_EXPRESSION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH);

		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_METHOD_OPEN_BRACE,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CONSTRUCTOR_DECLARATION_OPEN_PAREN, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_FIRST_INITIALIZER, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER);	
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_FOR_PAREN, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_IF_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_MESSAGE_SEND, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CONSTRUCTOR_DECLARATION);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_METHOD_DECLARATION_OPEN_PAREN, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SWITCH_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SWITCH);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SWITCH_OPEN_BRACE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SYNCHRONIZED_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_TYPE_OPEN_BRACE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_WHILE_CONDITION, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_REFERENCE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_REFERENCE,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_REFERENCE);

		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARGUMENTS,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_DECLARATION,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_CONSTRUCTOR_DECLARATION);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARRAY_INITIALIZER, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER);
			 
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_MESSAGESEND_ARGUMENTS, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_FORMAT_GUARDIAN_CLAUSE_ON_ONE_LINE, 
			DefaultCodeFormatterConstants.FORMATTER_KEEP_GUARDIAN_CLAUSE_ON_ONE_LINE);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_REFERENCE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_REFERENCE);
			
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BRACKET_IN_ARRAY_TYPE_REFERENCE, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_TYPE_REFERENCE);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATORS,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR); 

		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATORS,
			DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR); 

		
		/**
		 * This is for the alignment settings.
		 */
		checkAndReplace(oldSettings,
			DefaultCodeFormatterConstants.FORMATTER_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT, 
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_COMPACT_IF_ALIGNMENT, 
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_MESSAGE_SEND_ARGUMENTS_ALIGNMENT ,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_QUALIFIED_ALLOCATION_EXPRESSION_ARGUMENTS_ALIGNMENT ,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_BINARY_EXPRESSION_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_COMPACT_IF_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_CONDITIONAL_EXPRESSION_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_ARRAY_INITIALIZER_EXPRESSIONS_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_METHOD_DECLARATION_ARGUMENTS_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_MESSAGE_SEND_SELECTOR_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_TYPE_DECLARATION_SUPERCLASS_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_TYPE_DECLARATION_SUPERINTERFACES_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_METHOD_THROWS_CLAUSE_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_EXPLICIT_CONSTRUCTOR_ARGUMENTS_ALIGNMENT,
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL);

		mapOldValueRangeToNew(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_TYPE_MEMBER_ALIGNMENT, new String [] {DefaultCodeFormatterConstants.FORMATTER_NO_ALIGNMENT,	DefaultCodeFormatterConstants.FORMATTER_MULTICOLUMN}, 
			DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS, new String [] {DefaultCodeFormatterConstants.FALSE, DefaultCodeFormatterConstants.TRUE});
		
		
			/**
			 * Brace positions.
			 */		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_ANONYMOUS_TYPE_DECLARATION_BRACE_POSITION,
			DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION);
		
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_ARRAY_INITIALIZER_BRACE_POSITION,
			DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER);
		
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_BLOCK_BRACE_POSITION,
			DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK);
		
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_METHOD_DECLARATION_BRACE_POSITION,
			DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION);
		
		checkAndReplace(oldSettings, 
			DefaultCodeFormatterConstants.FORMATTER_TYPE_DECLARATION_BRACE_POSITION,
			DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION);
		
		
		checkAndReplace(oldSettings,
			DefaultCodeFormatterConstants.FORMATTER_SWITCH_BRACE_POSITION,
			DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH);
		
	}
}
