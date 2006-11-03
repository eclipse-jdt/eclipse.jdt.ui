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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.PotentialProgrammingProblemsFix;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.fix.PotentialProgrammingProblemsCleanUp;

/**
 * Subprocessor for serial version quickfix proposals.
 *
 * @since 3.1
 */
public final class SerialVersionSubProcessor {

	/**
	 * Determines the serial version quickfix proposals.
	 *
	 * @param context
	 *        the invocation context
	 * @param location
	 *        the problem location
	 * @param proposals
	 *        the proposal collection to extend
	 * @throws CoreException 
	 */
	public static final void getSerialVersionProposals(final IInvocationContext context, final IProblemLocation location, final Collection proposals) throws CoreException {

		Assert.isNotNull(context);
		Assert.isNotNull(location);
		Assert.isNotNull(proposals);
		
		IFix[] fixes= PotentialProgrammingProblemsFix.createMissingSerialVersionFixes(context.getASTRoot(), location);
		if (fixes != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
			Map options= new Hashtable();
			options.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID, CleanUpConstants.TRUE);
			options.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT, CleanUpConstants.TRUE);
			FixCorrectionProposal prop1= new SerialVersionDefaultProposal(fixes[0], new PotentialProgrammingProblemsCleanUp(options), 9, image, context);
			proposals.add(prop1);
			options= new Hashtable();
			options.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID, CleanUpConstants.TRUE);
			options.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED, CleanUpConstants.TRUE);
			FixCorrectionProposal prop2= new SerialVersionHashProposal(fixes[1], new PotentialProgrammingProblemsCleanUp(options), 9, image, context);
			proposals.add(prop2);
		}
	}
}
