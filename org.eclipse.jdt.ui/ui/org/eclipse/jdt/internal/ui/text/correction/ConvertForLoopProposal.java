/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.internal.corext.fix.IFix;

import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.ui.fix.ICleanUp;

public class ConvertForLoopProposal extends FixCorrectionProposal {

	public ConvertForLoopProposal(IFix fix, ICleanUp cleanUp, int relevance, Image image, IInvocationContext context) {
		super(fix, cleanUp, relevance, image, context);
	}

}
