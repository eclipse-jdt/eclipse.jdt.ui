/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//rename X to XYZ
package p;
public class A{
	class XYZ{
		XYZ(XYZ X){new XYZ(null);}
	}
	A(){}
	A(A A){}
	A m(){
		new XYZ(null);
		return (A)new A();
	}
};
class B{
	A.XYZ ax= new A().new XYZ(null);
}