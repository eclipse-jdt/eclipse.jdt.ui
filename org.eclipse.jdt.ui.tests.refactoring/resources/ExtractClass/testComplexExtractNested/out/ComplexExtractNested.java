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
package p;

public class ComplexExtractNested {
	public static class ComplexExtractNestedParameter {
		public int test;
		public int test2;
		public int test3;
		public int test4;
		public ComplexExtractNestedParameter(int test2, int test4) {
			this.test2 = test2;
			this.test4 = test4;
		}
	}

	protected ComplexExtractNestedParameter parameterObject = new ComplexExtractNestedParameter(5, 5);

	public void foo(){
		parameterObject.test3++;
		parameterObject.test= 5+7;
		System.out.println(parameterObject.test+" "+parameterObject.test4);
	}
}