/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import java.text.DateFormat;

import org.eclipse.jdt.internal.corext.template.SimpleTemplateVariable;
import org.eclipse.jdt.internal.corext.template.TemplateContext;

/**
 * Global variables which are available in any context.
 */
public class GlobalVariables {

	/**
	 * The cursor variable determines the cursor placement after template edition.
	 */
	static class Cursor extends SimpleTemplateVariable {
		public Cursor() {
			super(JavaTemplateMessages.getString("GlobalVariables.variable.name.cursor"), JavaTemplateMessages.getString("GlobalVariables.variable.description.cursor")); //$NON-NLS-1$ //$NON-NLS-2$
			setEvaluationString(""); //$NON-NLS-1$
			setResolved(true);
		}
	}

	/**
	 * The dollar variable inserts an escaped dollar symbol.
	 */
	static class Dollar extends SimpleTemplateVariable {
		public Dollar() {
			super(JavaTemplateMessages.getString("GlobalVariables.variable.name.dollar"), JavaTemplateMessages.getString("GlobalVariables.variable.description.dollar")); //$NON-NLS-1$ //$NON-NLS-2$
			setEvaluationString("$"); //$NON-NLS-1$
			setResolved(true);
		}
	}

	/**
	 * The date variable evaluates to the current date.
	 */
	static class Date extends SimpleTemplateVariable {
		public Date() {
			super(JavaTemplateMessages.getString("GlobalVariables.variable.name.date"), JavaTemplateMessages.getString("GlobalVariables.variable.description.date")); //$NON-NLS-1$ //$NON-NLS-2$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			return DateFormat.getDateInstance().format(new java.util.Date());
		}
	}		

	/**
	 * The time variable evaluates to the current time.
	 */
	static class Time extends SimpleTemplateVariable {
		public Time() {
			super(JavaTemplateMessages.getString("GlobalVariables.variable.name.time"), JavaTemplateMessages.getString("GlobalVariables.variable.description.time")); //$NON-NLS-1$ //$NON-NLS-2$
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
			super(JavaTemplateMessages.getString("GlobalVariables.variable.name.user"), JavaTemplateMessages.getString("GlobalVariables.variable.description.user")); //$NON-NLS-1$ //$NON-NLS-2$
			setResolved(true);
		}
		public String evaluate(TemplateContext context) {
			return System.getProperty("user.name"); //$NON-NLS-1$
		}	
	}
}
