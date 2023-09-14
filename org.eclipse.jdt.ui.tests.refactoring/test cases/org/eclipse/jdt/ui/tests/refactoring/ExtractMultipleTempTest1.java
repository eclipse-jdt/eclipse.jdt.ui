/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Taiming Wang <3120205503@bit.edu.cn> - [extract local] Extract Similar Expression in All Methods If End-Users Want. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/785
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

public class ExtractMultipleTempTest1 {
	/*
	 * this is a test java file for pull request 680: Refactoring history based name recommendation #680
	 */
	public static void main(String [] args) {
		System.out.println("test4PR680");
	}
}
class A extends RelOptCost{
	public A(int a, int b) {
		super(a, b);
	}

	@Override
	public RelOptCost computeSelfCost(int a1, int b1) {
		return super.computeSelfCost(a1, b1).multiplyBy(1);
	}

}

class A1 extends RelOptCost{
	public A1(int a, int b) {
		super(a, b);
	}

	@Override
	public RelOptCost computeSelfCost(int a1, int b1) {
		return super.computeSelfCost(a1, b1).multiplyBy(9);
	}
}
class A2 extends RelOptCost{
	public A2(int a, int b) {
		super(a, b);
	}

	@Override
	public RelOptCost computeSelfCost(int a1, int b1) {
		return super.computeSelfCost(a1, b1).multiplyBy(2);
	}
}
class A3 extends RelOptCost{
	public A3(int a, int b) {
		super(a, b);
	}

	@Override
	public RelOptCost computeSelfCost(int a1, int b1) {
		System.out.println(super.computeSelfCost(a1, b1));
		return super.computeSelfCost(a1, b1).multiplyBy(5);
	}
}

class RelOptCost {
	int a=0;
	int b=0;
	public RelOptCost(int a, int b) {
		this.a=a;
		this.b=b;
	}
	public RelOptCost computeSelfCost(int a1, int b1) {
		return new RelOptCost(a1,b1);
	}
	public RelOptCost multiplyBy(int d) {
		return new RelOptCost(this.a*d,this.b*d);
	}
}