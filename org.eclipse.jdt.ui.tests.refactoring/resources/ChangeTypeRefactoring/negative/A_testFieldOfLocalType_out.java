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
public class A_testFieldOfLocalType_in {
	
	public void foobar() {
		class Listener3 {
			private A_testFieldOfLocalType_in fTest;
			
			private Listener3() {
				fTest= new A_testFieldOfLocalType_in();
			}
			
			public int bar() {
				return foo();
			}
			
			public int foo() {
				return 1;
			}
			
			private String getProperty() {
				return null;
			}
		}
		
		this.addListener(new Listener3() {
			public int bar() {
				return 1;
			}
		});
	}
	
	
	public void addListener(Object o) {
	}

}
