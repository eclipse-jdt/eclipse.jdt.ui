/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.ExpressionsFix;
import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class ExpressionsCleanUp extends AbstractCleanUp {
	
	/**
	 * Add paranoic parenthesis around conditional expressions.<p>
	 * i.e.:<pre><code>
	 * boolean b= i > 10 && i < 100 || i > 20;
	 * ->
	 * boolean b= ((i > 10) && (i < 100)) || (i > 20);</pre></code>
	 */
	public static final int ADD_PARANOIC_PARENTHESIS= 1;
	
	/**
	 * Remove unnecessary parenthesis around conditional expressions.<p>
	 * i.e.:<pre><code>
	 * boolean b= ((i > 10) && (i < 100)) || (i > 20);
	 * ->
	 * boolean b= i > 10 && i < 100 || i > 20;</pre></code>
	 */
	public static final int REMOVE_UNNECESSARY_PARENTHESIS= 2;

	private static final int DEFAULT_FLAG= 0;
	private static final String SECTION_NAME= "CleanUp_Expressions"; //$NON-NLS-1$

	public ExpressionsCleanUp(int flag) {
		super(flag);
	}

	public ExpressionsCleanUp(IDialogSettings settings) {
		super(getSection(settings, SECTION_NAME), DEFAULT_FLAG);
	}
	
	public ExpressionsCleanUp(Map options) {
		super(options);
	}
	
	public ExpressionsCleanUp() {
		super();
	}
	
	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		return ExpressionsFix.createCleanUp(compilationUnit, 
				isFlag(ADD_PARANOIC_PARENTHESIS),
				isFlag(REMOVE_UNNECESSARY_PARENTHESIS));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		return createFix(compilationUnit);
	}

	public Map getRequiredOptions() {
		return null;
	}

	public void saveSettings(IDialogSettings settings) {
		super.saveSettings(getSection(settings, SECTION_NAME));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getDescriptions() {
		List result= new ArrayList();
		if (isFlag(ADD_PARANOIC_PARENTHESIS)) 
			result.add(MultiFixMessages.ExpressionsCleanUp_addParanoiac_description);
		
		if (isFlag(REMOVE_UNNECESSARY_PARENTHESIS)) 
			result.add(MultiFixMessages.ExpressionsCleanUp_removeUnnecessary_description);
		
		return (String[])result.toArray(new String[result.size()]);
	}
	
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
		if (isFlag(ADD_PARANOIC_PARENTHESIS)) {
			buf.append("boolean b= (((i > 0) && (i < 10)) || (i == 50));\n"); //$NON-NLS-1$
		} else if (isFlag(REMOVE_UNNECESSARY_PARENTHESIS)) {
			buf.append("boolean b= i > 0 && i < 10 || i == 50;\n"); //$NON-NLS-1$
		} else {
			buf.append("boolean b= (i > 0 && i < 10 || i == 50);\n"); //$NON-NLS-1$
		}
		
		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem) throws CoreException {
		if (isFlag(ADD_PARANOIC_PARENTHESIS)) {
			IFix fix= ExpressionsFix.createAddParanoidalParenthesisFix(compilationUnit, new ASTNode[] {problem.getCoveredNode(compilationUnit)});
			if (fix != null)
				return true;
		}
		if (isFlag(REMOVE_UNNECESSARY_PARENTHESIS)) {
			IFix fix= ExpressionsFix.createRemoveUnnecessaryParenthesisFix(compilationUnit, new ASTNode[] {problem.getCoveredNode(compilationUnit)});
			if (fix != null)
				return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public int maximalNumberOfFixes(CompilationUnit compilationUnit) {
		return -1;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getDefaultFlag() {
		return DEFAULT_FLAG;
	}

    protected int createFlag(Map options) {
    	int result= 0;
    	
    	if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES))) {
    		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER))) {
    			result|= REMOVE_UNNECESSARY_PARENTHESIS;
    		}
    		if (CleanUpConstants.TRUE.equals(options.get(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS))) {
    			result|= ADD_PARANOIC_PARENTHESIS;
    		}
    	}
    	
	    return result;
    }

}
