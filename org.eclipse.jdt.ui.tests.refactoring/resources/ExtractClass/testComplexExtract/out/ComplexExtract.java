/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package p;

public class ComplexExtract {
	protected ComplexExtractParameter parameterObject = new ComplexExtractParameter(5, 5);

	public void foo(){
		parameterObject.test3++;
		parameterObject.test= 5+7;
		System.out.println(parameterObject.test+" "+parameterObject.test4);
	}
}