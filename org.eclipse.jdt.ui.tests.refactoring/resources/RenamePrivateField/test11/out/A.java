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
import java.util.List;

class Test {
	static class Element{
	}
	
	static class A {
		private final List<Element> fElements;
		
		public A(List<Element> list) {
			fElements= list;
		}
		public List<Element> getElements() {
			return fElements;
		}
		public void setElements(List<Element> newElements) {
			fList= newElements;
		}
	}
	
	{ 
		A a= new A(new List<Element>());
		a.setElements(a.getElements());
	}
}
