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
package import_in;

import import_use.List;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import static java.lang.Math.PI;

public class Provider {
	public File useAsReturn() {
		return null;
	}
	public void useInArgument(File file) {
		file= null;
	}
	public void useInDecl() {
		List list= null;
	}
	public int useInDecl2(){
	  return new ArrayList().size();
	}	
	public Object useInDecl3(){
	  return new java.util.HashMap();
	}	
	public void useInClassLiteral() {
		Class clazz= File.class;
	}
	public void useArray() {
		List[] lists= null;
	}
	public void useInLocalClass() {
		class Local extends File {
			private static final long serialVersionUID = 1L;
			public Local(String s) {
				super(s);
			}
			public void foo(Map map) {
			}
			public void bar(Byte b) {
			}
		}
	}
	public void useStaticImport() {
		double i= PI;
	}
}
