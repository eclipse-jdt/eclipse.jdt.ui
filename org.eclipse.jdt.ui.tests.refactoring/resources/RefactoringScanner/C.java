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
class B{
	/**
	 * org.eclipse.TestTestPattern
	 * (org.eclipse.TestPattern)
	 * borg.eclipse.TestPattern
	 */
	void f(){
	}
	
	/*
	 * org.eclipse.TestTestPattern
	 * borg.eclipse.TestTestPattern
	 * rg.eclipse.TestTestPattern
	 * <org.eclipse.TestTestPattern>
	 * <org.eclipse.TestPatternTest>
	 * 
	 * org.eclipse. TestPattern
	 * org.eclipse .TestPattern
	 * 	x.TestPattern
	 */
	void f1(){
		f1();//borg.TestPattern //borg.eclipse.TestPattern
		String g= "ork.TestPattern";//org.eclipse.TestTestPattern
		String g2= "org.eklipse.TestPattern";
		String g3= "org.eclipse.TestPatternMatching";
	}
	
}