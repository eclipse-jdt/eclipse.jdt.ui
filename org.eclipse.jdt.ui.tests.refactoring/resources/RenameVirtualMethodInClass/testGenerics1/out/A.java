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
class Test {
    public static void main(String[] args) {
        new A<Number>().k(new Double(1));
        new A<Integer>().k(new Integer(2));

        new Impl().m(new Integer(3));
        new Impl().m(new Float(4));
        
        A<Number> a= new Impl();
        a.k(new Integer(6));
        a.k(new Double(7));
    }
}


class A<G> {
	void k(G g) { System.out.println("A#m(G): " + g); }
}

class Impl extends A<Number> {
	void m(Integer g) { System.out.println("nonripple Impl#m(Integer): " + g);}
	void k(Number g) { System.out.println("Impl#m(Number): " + g); }
}
