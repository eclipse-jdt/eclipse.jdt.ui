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

class PairDance {
    public static void main(String[] args) {
        InvertedPair/*<Integer, Double>*/ ip= new InvertedPair/*<Integer, Double>*/();
        Pair/*<Double, Integer>*/ p= ip;
        p.setA(new Double(1.1));
        Double a= (Double) ip.getA();
        ip.setB(new Integer(2));
        System.out.println(ip);
    }
}
