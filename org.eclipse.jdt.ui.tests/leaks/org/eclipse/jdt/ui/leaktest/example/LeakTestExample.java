/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.leaktest.example;

import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.ui.leaktest.LeakTestCase;
import org.eclipse.jdt.ui.leaktest.LeakTestSetup;


public class LeakTestExample extends LeakTestCase {

	private static final Class THIS= LeakTestExample.class;
	
	private static class MyClass {
		char[] text= new char[1000];
	}
	

	public static Test suite() {
		return new LeakTestSetup(new TestSuite(THIS));
	}
	
	private Object fGlobalReference;
	

	public LeakTestExample(String name) {
		super(name);
	}
			
	public void testLeakGlobalReference() throws Exception {
		Class cl= MyClass.class;

		// get the count before creating the instance
		int count1= getInstanceCount(cl);
		
		// create the instance
		fGlobalReference= new MyClass();

		// get the count after creating the instance
		int count2= getInstanceCount(cl);
		assertDifferentCount("after creation: ", count1, count2);
		
		// clear all references to the instance
		fGlobalReference= null;
		
		// get the count after clearing the reference of the instance
		int count3= getInstanceCount(cl);
		assertEqualCount("after clear: ", count1, count3);
	}
	
	public void testNoLeakGlobalReference() throws Exception {
		Class cl= MyClass.class;

		// get the count before creating my instance
		int count1= getInstanceCount(cl);
		
		// create the instance
		fGlobalReference= new MyClass();

		// get the count after creating the instance
		int count2= getInstanceCount(cl);
		assertDifferentCount("after creation: ", count1, count2);
		
		// add the instance to a list
		ArrayList list= new ArrayList();
		list.add(fGlobalReference);
		
		// clear the global references of the instance
		fGlobalReference= null;
		
		// get the count after clearing the global reference of the instance
		// instance should still be here, it is referenced in the list
		int count3= getInstanceCount(cl);
		assertEqualCount("after clear: ", count2, count3);
	}
	
	public void testLeakLocalReference() throws Exception {
		Class cl= MyClass.class;

		int count1= getInstanceCount(cl);
		
		MyClass reference= new MyClass();
		
		int count2= getInstanceCount(cl);
		assertDifferentCount("after creation: ", count1, count2);
		
		reference.toString();
		reference= null;
		
		int count3= getInstanceCount(cl);
		assertEqualCount("after clear: ", count1, count3);
	}
	
	public void testNoLeakLocalReference() throws Exception {
		Class cl= MyClass.class;

		int count1= getInstanceCount(cl);
		
		MyClass reference= new MyClass();

		int count2= getInstanceCount(cl);
		assertDifferentCount("after creation: ", count1, count2);
		
		ArrayList list= new ArrayList();
		list.add(reference);
		
		reference= null;
		
		int count3= getInstanceCount(cl);
		assertEqualCount("after clear: ", count2, count3);
	}
	

}
