/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.jdt.internal.ui.fix.CodeFormatCleanUp;
import org.eclipse.jdt.internal.ui.fix.CodeStyleCleanUp;
import org.eclipse.jdt.internal.ui.fix.CommentFormatCleanUp;
import org.eclipse.jdt.internal.ui.fix.ControlStatementsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ConvertLoopCleanUp;
import org.eclipse.jdt.internal.ui.fix.ExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.fix.ImportsCleanUp;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.PotentialProgrammingProblemsCleanUp;
import org.eclipse.jdt.internal.ui.fix.SortMembersCleanUp;
import org.eclipse.jdt.internal.ui.fix.StringCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnimplementedCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.VariableDeclarationCleanUp;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpMessages;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CodeFormatingTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CodeStyleTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.MemberAccessesTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.MissingCodeTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.UnnecessaryCodeTabPage;

/**
 * The clean up registry provides a set of clean ups
 * and there corresponding UI representatives.
 * 
 * @since 3.4
 */
public class CleanUpRegistry {
	
	/**
	 * @return a set of registered clean ups.
	 */
	public ICleanUp[] getCleanUps() {
		return new ICleanUp[] {
				new CodeStyleCleanUp(), 
				new ControlStatementsCleanUp(), 
				new ConvertLoopCleanUp(), 
				new VariableDeclarationCleanUp(), 
				new ExpressionsCleanUp(), 
				new UnusedCodeCleanUp(), 
				new Java50CleanUp(), 
				new PotentialProgrammingProblemsCleanUp(), 
				new UnnecessaryCodeCleanUp(), 
				new StringCleanUp(), 
				new UnimplementedCodeCleanUp(),
				new SortMembersCleanUp(), 
				new ImportsCleanUp(),
				new CommentFormatCleanUp(),
				new CodeFormatCleanUp()};
	}

	/**
	 * @return a set of tab pages which can be used to configure a clean up profile
	 */
	public CleanUpTabPage[] getCleanUpTabPages() {
		CleanUpTabPage[] result= new CleanUpTabPage[5];
		
		result[0]= new CodeStyleTabPage(CleanUpMessages.CleanUpModifyDialog_TabPageName_CodeStyle);
		result[1]= new MemberAccessesTabPage(CleanUpMessages.CleanUpModifyDialog_TabPageName_MemberAccesses);
		result[2]= new UnnecessaryCodeTabPage(CleanUpMessages.CleanUpModifyDialog_TabPageName_UnnecessaryCode);
		result[3]= new MissingCodeTabPage(CleanUpMessages.CleanUpModifyDialog_TabPageName_MissingCode);
		result[4]= new CodeFormatingTabPage(CleanUpMessages.CleanUpModifyDialog_TabPageName_CodeFormating);
		
		return result;
	}

}
