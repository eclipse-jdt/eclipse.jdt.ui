/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.testplugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

public abstract class AbstractCUTestCase extends TestCase {

	public AbstractCUTestCase(String name) {
		super(name);
	}
	
	protected String getFileContents(InputStream in) throws IOException {
		BufferedReader br= new BufferedReader(new InputStreamReader(in));
		
		StringBuffer sb= new StringBuffer();
		try {
			int read= 0;
			while ((read= br.read()) != -1)
				sb.append((char) read);
		} finally {
			br.close();
		}
		return sb.toString();
	}
	
	protected ICompilationUnit createCU(IPackageFragment pack, String name, String contents) throws Exception {
		ICompilationUnit cu= pack.createCompilationUnit(name, contents, true, null);
		cu.save(null, true);
		return cu;
	}
	
	protected ICompilationUnit createCU(IPackageFragment pack, String name, InputStream contents) throws Exception {
		return createCU(pack, name, getFileContents(contents));
	}
}

