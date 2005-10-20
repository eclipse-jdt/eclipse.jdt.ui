package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;

import org.eclipse.jdt.internal.ui.fix.IMultiFix;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

public class CleanUpRefactoring extends Refactoring {
	
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
	
	private List/*<ICompilationUnit>*/ fCompilationUnits;
	private List/*<IMultiFix>*/ fProblemSolutions;
	
	public CleanUpRefactoring() {
		fProblemSolutions= new ArrayList();
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
	
	public void addProblemSolution(IMultiFix fix) {
		fProblemSolutions.add(fix);
	}
	
	public void clearProblemSolutions() {
		fProblemSolutions.clear();
	}
	
	public boolean hasSolutions() {
		return !fProblemSolutions.isEmpty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	public String getName() {
		return "Clean up refactoring";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		
		if (pm == null)
			pm= new NullProgressMonitor();
		
		if (fCompilationUnits.size() == 0 || fProblemSolutions.size() == 0) {
			pm.done();
			return null;
		}
		
		pm.beginTask("", fCompilationUnits.size()); //$NON-NLS-1$
		
		Map options= JavaCore.getOptions();
		
		for (Iterator iter= fProblemSolutions.iterator(); iter.hasNext();) {
			IMultiFix fix= (IMultiFix)iter.next();
			Map fixOptions= fix.getRequiredOptions();
			if (fixOptions != null)
				options.putAll(fixOptions);
		}
		
		final List solutions= new ArrayList();
		
		int length= 50;
		int start= 0;
		int end;

		
		do {
			end= start + 1;
			while (
					end - start < length && 
					end < fCompilationUnits.size() && 
					((ICompilationUnit)fCompilationUnits.get(end - 1)).getJavaProject().equals(((ICompilationUnit)fCompilationUnits.get(end)).getJavaProject())) {
				end++;
			}
			
			List workingSet= fCompilationUnits.subList(start, end);
			
			final SubProgressMonitor sub= new SubProgressMonitor(pm, workingSet.size());
			ICompilationUnit[] compilationUnits= (ICompilationUnit[])workingSet.toArray(new ICompilationUnit[workingSet.size()]);
		
			ASTParser parser= ASTParser.newParser(ASTProvider.AST_LEVEL);
			parser.setResolveBindings(true);
			parser.setProject(compilationUnits[0].getJavaProject());
			parser.setCompilerOptions(options);
			
			try {
				parser.createASTs(compilationUnits, new String[0], new ASTRequestor() {
		
					public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
						
						calculateSolution(solutions, ast);
						
						sub.subTask(source.getElementName());
					}
					
				}, sub);
			} catch(FixCalculationException e) {
				throw e.getException();
			}
			
			start= end;
		
		} while (end < fCompilationUnits.size());
		
		CompositeChange result= new CompositeChange(getName());
		for (Iterator iter= solutions.iterator(); iter.hasNext();) {
			Change element= (Change)iter.next();
			result.add(element);
		}
		
		pm.done();
		
		return result;
	}
	
	public ICompilationUnit[] getCompilationUnits() {
		return (ICompilationUnit[])fCompilationUnits.toArray(new ICompilationUnit[fCompilationUnits.size()]);
	}

	private void calculateSolution(final List solutions, CompilationUnit ast) {
		TextChange solution= null;
		for (Iterator iterator= fProblemSolutions.iterator(); iterator.hasNext();) {
			IMultiFix problemSolution= (IMultiFix)iterator.next();
			try {
				IFix fix= problemSolution.createFix(ast);
				if (fix != null) {
					TextChange current= fix.createChange();
					if (solution == null) {
						solution= current;
					} else {
						TextChangeCompatibility.addTextEdit(solution, current.getName(), current.getEdit());
					}
				}
			} catch (CoreException e) {
				throw new FixCalculationException(e);
			}
		}
		if (solution != null)
			solutions.add(solution);
	}

}
