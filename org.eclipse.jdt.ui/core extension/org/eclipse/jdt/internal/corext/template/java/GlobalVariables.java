/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import java.text.DateFormat;
import java.util.Calendar;

import org.eclipse.jdt.internal.corext.template.SimpleTemplateVariable;
import org.eclipse.jdt.internal.corext.template.TemplateContext;

/**
 * Global variables which are available in any context.
 */
public class GlobalVariables {

	/**
	 * The cursor variable determines the cursor placement after template edition.
	 */
	public static class Cursor extends SimpleTemplateVariable {
		
		public static final String NAME= "cursor"; //$NON-NLS-1$
		
		public Cursor() {
			super(NAME, JavaTemplateMessages.getString("GlobalVariables.variable.description.cursor")); //$NON-NLS-1$
			setResolved(true);
			setEvaluationString(""); //$NON-NLS-1$
		}
	}

	/**
	 * The word selection variable determines templates that work on a full
	 * lines selection.
	 */
	public static class WordSelection extends SimpleTemplateVariable {
		
		public static final String NAME= "word_selection"; //$NON-NLS-1$
		
		public WordSelection() {
			super(NAME, JavaTemplateMessages.getString("GlobalVariables.variable.description.selectedWord")); //$NON-NLS-1$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			String selection= context.getVariable("selection"); //$NON-NLS-1$
			if (selection == null)
				return ""; //$NON-NLS-1$
			else
				return selection;
		}		
	}

	/**
	 * The line selection variable determines templates that work on selected
	 * lines.
	 */
	public static class LineSelection extends SimpleTemplateVariable {
		
		public static final String NAME= "line_selection"; //$NON-NLS-1$
		
		public LineSelection() {
			super(NAME, JavaTemplateMessages.getString("GlobalVariables.variable.description.selectedLines")); //$NON-NLS-1$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			String selection= context.getVariable("selection"); //$NON-NLS-1$
			if (selection == null)
				return ""; //$NON-NLS-1$
			else
				return selection;
		}		
	}

	/**
	 * The dollar variable inserts an escaped dollar symbol.
	 */
	static class Dollar extends SimpleTemplateVariable {
		public Dollar() {
			super("dollar", JavaTemplateMessages.getString("GlobalVariables.variable.description.dollar")); //$NON-NLS-1$ //$NON-NLS-2$
			setEvaluationString("$"); //$NON-NLS-1$
			setResolved(true);
		}
	}

	/**
	 * The date variable evaluates to the current date.
	 */
	static class Date extends SimpleTemplateVariable {
		public Date() {
			super("date", JavaTemplateMessages.getString("GlobalVariables.variable.description.date")); //$NON-NLS-1$ //$NON-NLS-2$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			return DateFormat.getDateInstance().format(new java.util.Date());
		}
	}		

	/**
	 * The year variable evaluates to the current year.
	 */
	static class Year extends SimpleTemplateVariable {
		public Year() {
			super("year", JavaTemplateMessages.getString("GlobalVariables.variable.description.year")); //$NON-NLS-1$ //$NON-NLS-2$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			return Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
		}
	}
	
	/**
	 * The time variable evaluates to the current time.
	 */
	static class Time extends SimpleTemplateVariable {
		public Time() {
			super("time", JavaTemplateMessages.getString("GlobalVariables.variable.description.time")); //$NON-NLS-1$ //$NON-NLS-2$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			return DateFormat.getTimeInstance().format(new java.util.Date());
		}
	}

	/**
	 * The user variable evaluates to the current user.
	 */
	static class User extends SimpleTemplateVariable {
		public User() {
			super("user", JavaTemplateMessages.getString("GlobalVariables.variable.description.user")); //$NON-NLS-1$ //$NON-NLS-2$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			return System.getProperty("user.name"); //$NON-NLS-1$
		}	
	}
}
