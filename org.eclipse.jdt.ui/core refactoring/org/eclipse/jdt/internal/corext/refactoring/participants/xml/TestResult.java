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


public class TestResult implements ITestResult {

	private static final int[][] AND= new int[][] {
						// FALSE			//TRUE					//NOT_LOADED			//UNKNOWN
		/* FALSE   */ { ITestResult.FALSE,	ITestResult.FALSE,		ITestResult.FALSE,		ITestResult.FALSE	},
		/* TRUE    */ { ITestResult.FALSE,	ITestResult.TRUE,		ITestResult.NOT_LOADED,	ITestResult.UNKNOWN	},
		/* PNL     */ { ITestResult.FALSE,	ITestResult.NOT_LOADED, ITestResult.NOT_LOADED,	ITestResult.UNKNOWN	},
		/* UNKNOWN */ { ITestResult.FALSE,	ITestResult.UNKNOWN,	ITestResult.UNKNOWN,	ITestResult.UNKNOWN	}
	};

	private static final int[][] OR= new int[][] {
						// FALSE				//TRUE				//NOT_LOADED			//UNKNOWN
		/* FALSE   */ { ITestResult.FALSE,		ITestResult.TRUE,	ITestResult.NOT_LOADED,	ITestResult.UNKNOWN	},
		/* TRUE    */ { ITestResult.TRUE,		ITestResult.TRUE,	ITestResult.TRUE,		ITestResult.TRUE	},
		/* PNL     */ { ITestResult.NOT_LOADED,	ITestResult.TRUE, 	ITestResult.NOT_LOADED,	ITestResult.UNKNOWN	},
		/* UNKNOWN */ { ITestResult.UNKNOWN,	ITestResult.TRUE,	ITestResult.UNKNOWN,	ITestResult.UNKNOWN	}
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
}
