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

public class B {
	private static interface I {
		public void foo();
	}
	private static class CC implements I {

		public void foo() {
		}
	}
	
	public static A createA() {
		return new A();
	}

	public void foo() {
		I i= new I() {
			public void foo() {
			}
			public void bar() {
				foo();
			}
		};
		
		CC c= new CC() {};
		B b;
	}
	
	public void bar() {
		class X {
			public void baz() {
				
			}
		}
		
		class Y extends X {
			public void baz() {
				
			}
		}
	}
}
