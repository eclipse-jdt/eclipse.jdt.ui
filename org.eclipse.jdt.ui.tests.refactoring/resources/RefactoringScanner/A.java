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
class A{
	/**
	 * TestPattern
	 * 	 TestPattern
	 */
	void f(){
	}
	
	/*
	 * TestPattern
	 * 
	 * 	TestPattern
	 */
	void f1(){
		f1();//TestPattern //org.eclipse.TestPattern
		String g= "TestPattern";
		String g2= "org.eclipse.TestPattern";
	}
	
}