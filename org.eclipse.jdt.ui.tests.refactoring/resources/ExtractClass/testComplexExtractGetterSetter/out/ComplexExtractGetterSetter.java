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

public class ComplexExtractGetterSetter {
	protected ComplexExtractGetterSetterParameter parameterObject = new ComplexExtractGetterSetterParameter(5, 5);

	public void foo(){
		parameterObject.setTest3(parameterObject.getTest3() + 1);
		parameterObject.setTest(5+7);
		System.out.println(parameterObject.getTest()+" "+parameterObject.getTest4());
	}
}