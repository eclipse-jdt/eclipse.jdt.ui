/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

/**
 * Class that implements the three operations <code>and</code>,
 * <code>or</code> and </code>not</code> for the group formed
 * by the three values <code>FALSE</code>, <code>TRUE</code>
 * and <code>NOT_LOADED</code>.
 * 
 * TODO some mathematical background.
 */
public class TestResult implements ITestResult {

	private static final int[][] AND= new int[][] {
						// FALSE	//TRUE		//NOT_LOADED
		/* FALSE   */ { FALSE,		FALSE,		FALSE		},
		/* TRUE    */ { FALSE,		TRUE,		NOT_LOADED	},
		/* PNL     */ { FALSE,		NOT_LOADED, NOT_LOADED	},
	};

	private static final int[][] OR= new int[][] {
						// FALSE	//TRUE	//NOT_LOADED
		/* FALSE   */ { FALSE,		TRUE,	NOT_LOADED	},
		/* TRUE    */ { TRUE,		TRUE,	TRUE		},
		/* PNL     */ { NOT_LOADED,	TRUE, 	NOT_LOADED	},
	};

	private static final int[] NOT= new int[] {
		//FALSE		//TRUE	//NOT_LOADED
		TRUE,		FALSE,	NOT_LOADED
	};

	private TestResult() {
		// no instance
	}
	
	public static int and(int left, int right) {
		return AND[left][right];
	}
	
	public static int or(int left, int right) {
		return OR[left][right];
	}
	
	public static int not(int op) {
		return NOT[op];
	}
	
	public static int asTestResult(boolean b) {
		return b ? TRUE : FALSE;
	}
}
