/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.text.edits.TextEditVisitor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringTickProvider;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.fix.IMultiFix;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

public class CleanUpRefactoring extends Refactoring {

	private static final int BATCH_SIZE= 40;

	private class FixCalculationException extends RuntimeException {

		private static final long serialVersionUID= 3807273310144726165L;
		
		private final CoreException fException;

		public FixCalculationException(CoreException exception) {
			fException= exception;
		}

		public CoreException getException() {
			return fException;
		}
		
	}

	private static final RefactoringTickProvider CLEAN_UP_REFACTORING_TICK_PROVIDER= new RefactoringTickProvider(0, 0, 1, 0);
	
	private List/*<ICompilationUnit>*/ fCompilationUnits;
	private List/*<IMultiFix>*/ fMultiFixes;
	
	public CleanUpRefactoring() {
		fMultiFixes= new ArrayList();
		fCompilationUnits= new ArrayList();
	}
	
	public void addCompilationUnit(ICompilationUnit unit) {
		fCompilationUnits.add(unit);
	}
	
	public void clearCompilationUnits() {
		fCompilationUnits.clear();
	}
	
	public boolean hasCompilationUnits() {
		return !fCompilationUnits.isEmpty();
	}
	
	public void addMultiFix(IMultiFix fix) {
		fMultiFixes.add(fix);
	}
	
	public void clearMultiFixes() {
		fMultiFixes.clear();
	}
	
	public boolean hasMultiFix() {
		return !fMultiFixes.isEmpty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	public String getName() {
		return FixMessages.CleanUpRefactoring_Refactoring_name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (pm != null) {
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.worked(1);
			pm.done();
		}
		return new RefactoringStatus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (pm != null) {
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.worked(1);
			pm.done();
		}
		return new RefactoringStatus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		
		if (pm == null)
			pm= new NullProgressMonitor();
		
		if (fCompilationUnits.size() == 0 || fMultiFixes.size() == 0) {
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.worked(1);
			pm.done();
			return new NullChange();
		}
		
		pm.beginTask("", fCompilationUnits.size()); //$NON-NLS-1$
		
		Map fixOptions= getMultiFixOptions();
		
		final List resultingFixes= new ArrayList();
		
		int start= 0;
		int end;

		Iterator compilationUnitsIterator= fCompilationUnits.iterator();
		ICompilationUnit beforeEndCU= (ICompilationUnit)compilationUnitsIterator.next();
		ICompilationUnit endCU= null;
		do {
			end= start + 1;
			while (
					end - start < BATCH_SIZE && 
					end < fCompilationUnits.size() &&
					compilationUnitsIterator.hasNext() && 
					beforeEndCU.getJavaProject().equals((endCU= (ICompilationUnit)compilationUnitsIterator.next()).getJavaProject())) {
				end++;
				beforeEndCU= endCU;
			}
			if (compilationUnitsIterator.hasNext()) {
				beforeEndCU= endCU;
			}
			
			final List workingSet= fCompilationUnits.subList(start, end);
			ICompilationUnit[] compilationUnits= (ICompilationUnit[])workingSet.toArray(new ICompilationUnit[workingSet.size()]);
			IJavaProject javaProject= compilationUnits[0].getJavaProject();

			ASTParser parser= createParser(fixOptions, javaProject);
			parse(resultingFixes, start, compilationUnits, new SubProgressMonitor(pm, workingSet.size()), parser);
			
			start= end;
		
		} while (end < fCompilationUnits.size());
		
		CompositeChange result= new CompositeChange(getName());
		for (Iterator iter= resultingFixes.iterator(); iter.hasNext();) {
			Change element= (Change)iter.next();
			result.add(element);
		}
		
		pm.done();
		
		return result;
	}

	/**
	 * @return Options for all multi-fixes in <code>fMultiFixes</code>
	 */
	private Map getMultiFixOptions() {
		Map fixOptions= new Hashtable();
		for (Iterator iter= fMultiFixes.iterator(); iter.hasNext();) {
			IMultiFix fix= (IMultiFix)iter.next();
			Map curFixOptions= fix.getRequiredOptions();
			if (curFixOptions != null)
				fixOptions.putAll(curFixOptions);
		}
		return fixOptions;
	}

	private void parse(final List solutions, final int start, final ICompilationUnit[] compilationUnits, final SubProgressMonitor sub, ASTParser parser) throws CoreException {
		final int[] index= new int[] {start};
		final Integer size= new Integer(fCompilationUnits.size());
		sub.subTask(Messages.format(FixMessages.CleanUpRefactoring_ProcessingCompilationUnit_message, new Object[] {getTypeName(compilationUnits[index[0]- start]), new Integer(index[0] + 1), size}));
		
		try {
			parser.createASTs(compilationUnits, new String[0], new ASTRequestor() {

				public void acceptAST(ICompilationUnit source, CompilationUnit ast) {

					calculateSolution(solutions, ast);
					index[0]++;
					if (index[0] - start < compilationUnits.length) {
						sub.subTask(Messages.format(FixMessages.CleanUpRefactoring_ProcessingCompilationUnit_message, new Object[] {getTypeName(compilationUnits[index[0] - start]), new Integer(index[0] + 1), size}));
					}
				}
				
			}, sub);
		} catch(FixCalculationException e) {
			throw e.getException();
		}
	}

	private ASTParser createParser(Map fixOptions, IJavaProject javaProject) {
		ASTParser parser= ASTParser.newParser(ASTProvider.AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setProject(javaProject);
				
		Map options= RefactoringASTParser.getCompilerOptions(javaProject);
		options.putAll(fixOptions);
		parser.setCompilerOptions(options);
		return parser;
	}

	private String getTypeName(final ICompilationUnit unit) {
		String elementName= unit.getElementName();
		if (elementName.length() > 5) {
			return elementName.substring(0, elementName.indexOf('.'));
		}
		return elementName;
	}
	
	public ICompilationUnit[] getCompilationUnits() {
		return (ICompilationUnit[])fCompilationUnits.toArray(new ICompilationUnit[fCompilationUnits.size()]);
	}

	private void calculateSolution(final List solutions, CompilationUnit ast) {
		TextChange solution= null;
		for (Iterator iterator= fMultiFixes.iterator(); iterator.hasNext();) {
			IMultiFix problemSolution= (IMultiFix)iterator.next();
			try {
				IFix fix= problemSolution.createFix(ast);
				if (fix != null) {
					TextChange current= fix.createChange();
					if (solution != null)
						mergeTextChanges(current, solution);
					solution= current;
				}
			} catch (CoreException e) {
				throw new FixCalculationException(e);
			}
		}
		if (solution != null)
			solutions.add(solution);
	}

	public static void mergeTextChanges(TextChange target, TextChange source) {
		final List edits= new ArrayList();
		source.getEdit().accept(new TextEditVisitor() {
			public boolean visitNode(TextEdit edit) {
				if (!(edit instanceof MultiTextEdit)) {
					edits.add(edit);
				}
				return super.visitNode(edit);
			}
			
		});
		if (edits.isEmpty())
			return;
		
		final List removedEdits= new ArrayList();
		target.getEdit().accept(new TextEditVisitor() {
			public boolean visit(DeleteEdit deleteEdit) {
				int start= deleteEdit.getOffset();
				int end= start + deleteEdit.getLength();
				
				List toRemove= new ArrayList();
				for (Iterator iter= edits.iterator(); iter.hasNext();) {
					TextEdit edit= (TextEdit)iter.next();
					int offset= edit.getOffset();
					if (offset >= start && offset <= end) {
						toRemove.add(edit);
					}
				}
				
				if (!toRemove.isEmpty()) {
					edits.removeAll(toRemove);
					removedEdits.addAll(toRemove);
				}
				
				return super.visit(deleteEdit);
			}
		});
		for (Iterator iter= edits.iterator(); iter.hasNext();) {
			TextEdit edit= (TextEdit)iter.next();
			edit.getParent().removeChild(edit);
			TextChangeCompatibility.insert(target.getEdit(), edit);
		}
		TextEditBasedChangeGroup[] changeGroups= source.getChangeGroups();
		for (int i= 0; i < changeGroups.length; i++) {
			TextEditGroup textEditGroup= changeGroups[i].getTextEditGroup();
			TextEditGroup newGroup= new TextEditGroup(textEditGroup.getName());
			TextEdit[] textEdits= textEditGroup.getTextEdits();
			for (int j= 0; j < textEdits.length; j++) {
				if (!removedEdits.contains(textEdits[j]))
					newGroup.addTextEdit(textEdits[j]);
			}
			target.addTextEditGroup(newGroup);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getRefactoringTickProvider()
	 */
	protected RefactoringTickProvider doGetRefactoringTickProvider() {
		return CLEAN_UP_REFACTORING_TICK_PROVIDER;
	}
}