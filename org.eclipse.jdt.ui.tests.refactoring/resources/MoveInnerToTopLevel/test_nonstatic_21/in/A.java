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
package p;

class A{
	class Inner{
		Inner(){
		}
		Inner(int i){
		}
	}
}
class I2 extends A.Inner{
	I2(){
		new A().super();
	}
	I2(int i){
		new A().super(i);
	}
}