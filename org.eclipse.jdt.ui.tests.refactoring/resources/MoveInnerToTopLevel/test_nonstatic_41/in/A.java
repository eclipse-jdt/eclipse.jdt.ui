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
import static java.lang.Math.E;
public class A {
	class Inner {
		static class InnerInner {
			static class InnerInnerInner {}
		}
		public void doit() {
			foo();
			fred++;
			double e= E;
			new Stat();
		}
	}
	static void foo(){};
	static int fred;
	static class Stat{}
}