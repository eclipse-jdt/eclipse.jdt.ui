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

import org.eclipse.jdt.internal.corext.fix.CodeStyleFix;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.CodeStyleFix.TupleForNonStaticAccess;
import org.eclipse.jdt.internal.corext.fix.CodeStyleFix.TupleForUnqualifiedAccess;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Creates fixes which can resolve code style issuse 
 * @see org.eclipse.jdt.internal.corext.fix.CodeStyleFix
 */
public class CodeStyleMultiFix extends AbstractMultiFix {

	private boolean fAddThisQualifier;
	private boolean fChangeNonStaticAccessToStatic;
	
	public CodeStyleMultiFix(boolean addThisQualifier, boolean changeNonStaticAccessToStatic) {
		fAddThisQualifier= addThisQualifier;
		fChangeNonStaticAccessToStatic= changeNonStaticAccessToStatic;
	}

	public IFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (compilationUnit == null)
			return null;
		
		List/*<TupleForUnqualifiedAccess>*/ bindingTuples= new ArrayList();
		List/*<TupleForNonStaticAccess>*/ nonStaticTuples= new ArrayList(); 
		
		IProblem[] problems= compilationUnit.getProblems();
		for (int i= 0; i < problems.length; i++) {
			IProblemLocation problem= getProblemLocation(problems[i]);
			TupleForNonStaticAccess tupleDirect= null;
			if (fChangeNonStaticAccessToStatic && CodeStyleFix.isNonStaticAccess(problem)) {
				tupleDirect= CodeStyleFix.getTupleForNonStaticAccess(compilationUnit, problem);
			}
			if (tupleDirect != null) {
				nonStaticTuples.add(tupleDirect);
			} else if (fAddThisQualifier && problems[i].getID() == IProblem.UnqualifiedFieldAccess) {
				TupleForUnqualifiedAccess tuple= CodeStyleFix.getBindingTuple(compilationUnit, problem);
				if (tuple != null) {
					bindingTuples.add(tuple);
				}
			}
			
		}
		if (bindingTuples.size() == 0 && nonStaticTuples.size() == 0)
			return null;
		
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		TupleForUnqualifiedAccess[] nonQualifiedAccesses= (TupleForUnqualifiedAccess[])bindingTuples.toArray(new TupleForUnqualifiedAccess[bindingTuples.size()]);
		TupleForNonStaticAccess[] nonStaticAccesses= (TupleForNonStaticAccess[])nonStaticTuples.toArray(new TupleForNonStaticAccess[nonStaticTuples.size()]);
		return new CodeStyleFix(CodeStyleFix.ADD_THIS_QUALIFIER, cu, nonQualifiedAccesses, nonStaticAccesses);
	}

	public Map getRequiredOptions() {
		Map options= new Hashtable();
		if (fAddThisQualifier)
			options.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.WARNING);
		if (fChangeNonStaticAccessToStatic)
			options.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.WARNING);
		return options;
	}

	public Control createConfigurationControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		composite.setLayout(new GridLayout(1, true));
		
		Button addThisQualifier= new Button(composite, SWT.CHECK);
		addThisQualifier.setText(CodeStyleFix.ADD_THIS_QUALIFIER);
		addThisQualifier.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		addThisQualifier.setSelection(fAddThisQualifier);
		addThisQualifier.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				fAddThisQualifier= ((Button)e.getSource()).getSelection();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				fAddThisQualifier= ((Button)e.getSource()).getSelection();
			}
		});
		
		Button removeNonStaticAccess= new Button(composite, SWT.CHECK);
		removeNonStaticAccess.setText("Change access to static using declaring type");
		removeNonStaticAccess.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		removeNonStaticAccess.setSelection(fChangeNonStaticAccessToStatic);
		removeNonStaticAccess.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				fChangeNonStaticAccessToStatic= ((Button)e.getSource()).getSelection();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				fChangeNonStaticAccessToStatic= ((Button)e.getSource()).getSelection();
			}
		});
		
		return composite;
	}

}
