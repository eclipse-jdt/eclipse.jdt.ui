/**
 *
 **/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class ConvertLoopFix extends LinkedFix {
	
	private final static class ControlStatementFinder extends GenericVisitor {
		
		private final List/*<IFixRewriteOperation>*/fResult;
		private final Hashtable fUsedNames;
		private final boolean fFindForLoopsToConvert;
		private final boolean fConvertIterableForLoops;
		private final boolean fMakeFinal;
		
		public ControlStatementFinder(boolean findForLoopsToConvert, boolean convertIterableForLoops, boolean makeFinal, List resultingCollection) {
			fFindForLoopsToConvert= findForLoopsToConvert;
			fConvertIterableForLoops= convertIterableForLoops;
			fMakeFinal= makeFinal;
			fResult= resultingCollection;
			fUsedNames= new Hashtable();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.ForStatement)
		 */
		public boolean visit(ForStatement node) {
			if (fFindForLoopsToConvert || fConvertIterableForLoops) {
				ForStatement current= node;
				ConvertLoopOperation operation= getConvertOperation(current);
				ConvertLoopOperation oldOperation= null;
				while (operation != null) {
					if (oldOperation == null) {
						fResult.add(operation);
					} else {
						oldOperation.setBodyConverter(operation);
					}
					
					if (current.getBody() instanceof ForStatement) {
						current= (ForStatement)current.getBody();
						oldOperation= operation;
						operation= getConvertOperation(current);
					} else {
						operation= null;
					}
				}
				current.getBody().accept(this);
				return false;
			}
			
			return super.visit(node);
		}
		
		private ConvertLoopOperation getConvertOperation(ForStatement node) {
			
			Collection usedNamesCollection= fUsedNames.values();
			String[] usedNames= (String[])usedNamesCollection.toArray(new String[usedNamesCollection.size()]);
			ConvertLoopOperation convertForLoopOperation= new ConvertForLoopOperation(node, usedNames, fMakeFinal);
			if (convertForLoopOperation.satisfiesPreconditions().isOK()) {
				if (fFindForLoopsToConvert) {
					fUsedNames.put(node, convertForLoopOperation.getIntroducedVariableName());
					return convertForLoopOperation;
				}
			} else if (fConvertIterableForLoops) {
				ConvertLoopOperation iterableConverter= new ConvertIterableLoopOperation(node, usedNames, fMakeFinal);
				if (iterableConverter.satisfiesPreconditions().isOK()) {
					fUsedNames.put(node, iterableConverter.getIntroducedVariableName());
					return iterableConverter;
				}
			}
			
			return null;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#endVisit(org.eclipse.jdt.core.dom.ForStatement)
		 */
		public void endVisit(ForStatement node) {
			if (fFindForLoopsToConvert || fConvertIterableForLoops) {
				fUsedNames.remove(node);
			}
			super.endVisit(node);
		}
		
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit, boolean convertForLoops, boolean convertIterableForLoops, boolean makeFinal) {
		if (!JavaModelUtil.is50OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;
		
		if (!convertForLoops && !convertIterableForLoops)
			return null;
		
		List operations= new ArrayList();
		ControlStatementFinder finder= new ControlStatementFinder(convertForLoops, convertIterableForLoops, makeFinal, operations);
		compilationUnit.accept(finder);
		
		if (operations.isEmpty())
			return null;
		
		IFixRewriteOperation[] ops= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new ConvertLoopFix(FixMessages.ControlStatementsFix_change_name, compilationUnit, ops);
	}
	
	public static IFix createConvertForLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertLoopOperation convertForLoopOperation= new ConvertForLoopOperation(loop);
		if (!convertForLoopOperation.satisfiesPreconditions().isOK())
			return null;
		
		return new ConvertLoopFix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new ILinkedFixRewriteOperation[] {convertForLoopOperation});
	}
	
	public static IFix createConvertIterableLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertIterableLoopOperation loopConverter= new ConvertIterableLoopOperation(loop);
		IStatus status= loopConverter.satisfiesPreconditions();
		if (status.getSeverity() == IStatus.ERROR)
			return null;
		
		ConvertLoopFix result= new ConvertLoopFix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new ILinkedFixRewriteOperation[] {loopConverter});
		result.setStatus(status);
		return result;
	}
	
	protected ConvertLoopFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
	
}
