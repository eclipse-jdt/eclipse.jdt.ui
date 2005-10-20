package org.eclipse.jdt.internal.ui.fix;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;

import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFix;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Create fixes which can remove unused code
 * @see org.eclipse.jdt.internal.corext.fix.UnusedCodeFix
 *
 */
public class UnusedCodeMultiFix extends AbstractMultiFix {

	private boolean fRemoveUnusedImports;

	public UnusedCodeMultiFix(boolean removeUnusedImports) {
		fRemoveUnusedImports= removeUnusedImports;
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		
		List/*<ImportDeclaration>*/ removeImports= new ArrayList();

		IProblem[] problems= compilationUnit.getProblems();

		for (int i= 0; i < problems.length; i++) {
			if (fRemoveUnusedImports && problems[i].getID() == IProblem.UnusedImport) {
				
				IProblemLocation problem= getProblemLocation(problems[i]);
				ImportDeclaration node= UnusedCodeFix.getImportDeclaration(problem, compilationUnit);
				
				if (node != null) {
					removeImports.add(node);
				}
			}
		}
		
		if (removeImports.size() == 0)
			return null;
		
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		return new UnusedCodeFix(UnusedCodeFix.REMOVE_UNUSED_IMPORT, cu, (ImportDeclaration[])removeImports.toArray(new ImportDeclaration[removeImports.size()]));
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (fRemoveUnusedImports) {
			options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.WARNING);
		}
		return options;
	}

	public Control createConfigurationControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		composite.setLayout(new GridLayout(1, true));
		
		Button addNLSTag= new Button(composite, SWT.CHECK);
		addNLSTag.setText(UnusedCodeFix.REMOVE_UNUSED_IMPORT);
		addNLSTag.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		addNLSTag.setSelection(fRemoveUnusedImports);
		addNLSTag.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				fRemoveUnusedImports= ((Button)e.getSource()).getSelection();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				fRemoveUnusedImports= ((Button)e.getSource()).getSelection();
			}
		});
		
		return composite;
	}

}
