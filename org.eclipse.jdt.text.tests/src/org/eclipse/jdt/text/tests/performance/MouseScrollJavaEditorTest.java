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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.ui.PartInitException;

public class MouseScrollJavaEditorTest extends MouseScrollEditorTest {

	private static final Class THIS= MouseScrollJavaEditorTest.class;
	
	private static final String THUMB_SCROLLING_FILE= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";
	
	private static final String AUTO_SCROLLING_FILE= "/org.eclipse.swt/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout.java";
	
	private static final int N_OF_RUNS= 6;

	private static final int N_OF_COLD_RUNS= 3;
	
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}
	
	public void testThumbScrollJavaEditor1() throws PartInitException {
		measureScrolling(N_OF_RUNS, N_OF_COLD_RUNS, new ThumbScrollPoster(), ResourceTestHelper.findFile(THUMB_SCROLLING_FILE));
	}
	
	public void testAutoScrollJavaEditor1() throws PartInitException {
		measureScrolling(N_OF_RUNS, N_OF_COLD_RUNS, new AutoScrollPoster(), ResourceTestHelper.findFile(AUTO_SCROLLING_FILE));
	}
}
