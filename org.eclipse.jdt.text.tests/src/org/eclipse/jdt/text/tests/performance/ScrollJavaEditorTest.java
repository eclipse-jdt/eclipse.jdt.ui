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

package org.eclipse.jdt.text.tests.performance;

import org.eclipse.ui.PartInitException;

public class ScrollJavaEditorTest extends ScrollEditorTest {
	
	private static final String FILE= "org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	private static final int N_OF_RUNS= 10;

	public void testScrollJavaEditor1() throws PartInitException {
		measureScrolling(ResourceTestHelper.findFile(FILE), N_OF_RUNS);
	}

	public void testScrollJavaEditor2() throws PartInitException {
		measureScrolling(ResourceTestHelper.findFile(FILE), N_OF_RUNS);
	}
}
