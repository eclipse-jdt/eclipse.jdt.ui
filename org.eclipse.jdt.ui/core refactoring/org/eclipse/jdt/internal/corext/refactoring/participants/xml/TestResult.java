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

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Class that implements the three operations <code>and</code>,
 * <code>or</code> and </code>not</code> for the group formed
 * by the three values <code>FALSE</code>, <code>TRUE</code>
 * and <code>NOT_LOADED</code>.
 * 
 * TODO some mathematical background.
 */
public class TestResult {
	
	private int fValue;
	
	private static final int FALSE_VALUE= 0;
	private static final int TRUE_VALUE= 1;
	private static final int NOT_LOADED_VALUE= 2;
	
	public static final TestResult FALSE= new TestResult(FALSE_VALUE);
	public static final TestResult TRUE= new TestResult(TRUE_VALUE);
	public static final TestResult NOT_LOADED= new TestResult(NOT_LOADED_VALUE);

	private static final TestResult[][] AND= new TestResult[][] {
						// FALSE	//TRUE		//NOT_LOADED
		/* FALSE   */ { FALSE,		FALSE,		FALSE		},
		/* TRUE    */ { FALSE,		TRUE,		NOT_LOADED	},
		/* PNL     */ { FALSE,		NOT_LOADED, NOT_LOADED	},
	};

	private static final TestResult[][] OR= new TestResult[][] {
						// FALSE	//TRUE	//NOT_LOADED
		/* FALSE   */ { FALSE,		TRUE,	NOT_LOADED	},
		/* TRUE    */ { TRUE,		TRUE,	TRUE		},
		/* PNL     */ { NOT_LOADED,	TRUE, 	NOT_LOADED	},
	};

	private static final TestResult[] NOT= new TestResult[] {
		//FALSE		//TRUE	//NOT_LOADED
		TRUE,		FALSE,	NOT_LOADED
	};

	/*
	 * No instances outside of <code>TestResult</code>
	 */
	private TestResult(int value) {
		fValue= value;
	}
	
	public TestResult and(TestResult op) {
		return AND[fValue][op.fValue];
	}
	
	public TestResult or(TestResult op) {
		return OR[fValue][op.fValue];
	}
	
	public TestResult not() {
		return NOT[fValue];
	}
	
	public static TestResult valueOf(boolean b) {
		return b ? TRUE : FALSE;
	}
	
	public static TestResult valueOf(Boolean b) {
		return b.booleanValue() ? TRUE : FALSE;
	}
	
	public String toString() {
		switch (fValue) {
			case 0:
				return "false"; //$NON-NLS-1$
			case 1:
				return "true"; //$NON-NLS-1$
			case 2:
				return "not_loaded"; //$NON-NLS-1$
		}
		Assert.isTrue(false);
		return null;
	}
}
