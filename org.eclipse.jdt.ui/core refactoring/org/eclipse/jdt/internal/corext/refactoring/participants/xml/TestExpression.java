/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class TestExpression extends Expression {

	private String fProperty;
	private Object[] fArgs;
	
	public static final String NAME= "test"; //$NON-NLS-1$
	private static final String ATT_PROPERTY= "property"; //$NON-NLS-1$
	private static final String ATT_ARGS= "args"; //$NON-NLS-1$
	
	private static final Object[] EMPTY_ARGS= new Object[0];
	
	private static class Tokenizer {
		private String fString;
		private int fPosition;
		public Tokenizer(String s) {
			fString= s;
			fPosition= 0;
		}
		public String next() {
			if (fPosition >= fString.length())
				return null;
			int nextComma= getNextCommna();
			String result;
			if (nextComma == -1) {
				result= fString.substring(fPosition, fString.length()).trim();
				fPosition= fString.length();
			} else {
				result= fString.substring(fPosition, nextComma).trim();
				fPosition= nextComma + 1;
			}
			return result;
		}
		private int getNextCommna() {
			boolean quoted= false;
			for (int i= fPosition; i < fString.length(); i++) {
				char ch= fString.charAt(i);
				switch (ch) {
					case '\'':
						quoted= !quoted;
					case ',':
						if (!quoted)
							return i;
							
				}
			}
			return -1;
		}
	}
	
	public TestExpression(IConfigurationElement element) {
		fProperty= element.getAttribute(ATT_PROPERTY);
		fArgs= getArguments(element);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.Expression#evaluate(java.lang.Object)
	 */
	public TestResult evaluate(IVariablePool pool) throws CoreException {
		Object element= pool.getDefaultVariable();
		// hard coded instanceof check to ensure it is evaluated fast.
		if ("instanceof".equals(fProperty)) { //$NON-NLS-1$
			return TestResult.valueOf(isInstanceOf(element, (String)fArgs[0]));
		}
		Method method= TypeExtension.getMethod(element, fProperty);
		if (!method.isLoaded())
			return TestResult.NOT_LOADED;
		Object returnValue= method.invoke(element, fArgs);
		if (!(returnValue instanceof Boolean)) {
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR,
				"Expected result must be of type Boolean", null));
		}
		return TestResult.valueOf((Boolean)returnValue);
	}
	
	//---- Argument parsing --------------------------------------------
	
	public Object[] getArguments(IConfigurationElement element) {
		String args= element.getAttribute(ATT_ARGS);
		if (args == null) {
			String value= element.getAttribute(ATT_VALUE);
			if (value != null) {
				return new Object[] { convertToken(value) };
			} else { 
				// in version two we can support sub elements <string></string> <long></long>
				return EMPTY_ARGS;
			}
		} else {
			List result= new ArrayList();
			Tokenizer tokenizer= new Tokenizer(args);
			String arg;
			while ((arg= tokenizer.next()) != null) {
				result.add(convertToken(arg));
			}
			return result.toArray();
		}
	}
		
	private Object convertToken(String arg) {
		Assert.isTrue(arg.length() > 0);
		if (arg.charAt(0) == '\'' && arg.charAt(arg.length() - 1) == '\'') {
			return arg.substring(1, arg.length() - 1);
		} else if ("true".equals(arg)) { //$NON-NLS-1$
			return Boolean.TRUE;
		} else if ("false".equals(arg)) { //$NON-NLS-1$
			return Boolean.FALSE;
		} else if (arg.indexOf('.') != -1) {
			try {
				return Float.valueOf(arg);
			} catch (NumberFormatException e) {
				return arg;
			}
		} else {
			try {
				return Integer.valueOf(arg);
			} catch (NumberFormatException e) {
				return arg;
			}
		}
	}

	//---- Debugging ---------------------------------------------------
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer args= new StringBuffer();
		for (int i= 0; i < fArgs.length; i++) {
			Object arg= fArgs[i];
			if (arg instanceof String) {
				args.append('\'');
				args.append(arg);
				args.append('\'');
			} else {
				args.append(arg.toString());
			}
			if (i < fArgs.length - 1)
				args.append(", "); //$NON-NLS-1$
		}
		return "<test property=\"" + fProperty + "\" args=\"" + args + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
