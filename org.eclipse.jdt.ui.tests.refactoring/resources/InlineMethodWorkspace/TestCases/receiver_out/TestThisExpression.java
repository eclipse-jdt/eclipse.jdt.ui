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
package receiver_out;

public class TestThisExpression {
	void m(C c){
		c.m(this);
		class X {
			void foo() {
				foo();
			}
		}
	}
}

class C {
	void m(TestThisExpression t){
	}
}

class Client{
	void f(){
		TestThisExpression t= null;
		C c= null;
		c.m(t);
		class X {
			void foo() {
				foo();
			}
		}
	}
}