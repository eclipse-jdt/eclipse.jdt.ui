/**
 *
 **/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class ConvertLoopFix extends LinkedFix {
	
	private static final String FOR_LOOP_ELEMENT_IDENTIFIER= "element"; //$NON-NLS-1$
	
	private final static class ControlStatementFinder extends GenericVisitor {
		
		private final List/*<IFixRewriteOperation>*/fResult;
		private final Hashtable fUsedNames;
		private final CompilationUnit fCompilationUnit;
		private final boolean fFindForLoopsToConvert;
		private final boolean fConvertIterableForLoops;
		
		public ControlStatementFinder(CompilationUnit compilationUnit, boolean findForLoopsToConvert, boolean convertIterableForLoops, List resultingCollection) {
			
			fFindForLoopsToConvert= findForLoopsToConvert;
			fConvertIterableForLoops= convertIterableForLoops;
			fResult= resultingCollection;
			fUsedNames= new Hashtable();
			fCompilationUnit= compilationUnit;
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
			String identifierName= getFreeVariable(node);
			
			ConvertForLoopOperation convertForLoopOperation= new ConvertForLoopOperation(node, identifierName);
			if (convertForLoopOperation.satisfiesPreconditions()) {
				if (fFindForLoopsToConvert) {
					fUsedNames.put(node, identifierName);
					return convertForLoopOperation;
				}
			} else if (fConvertIterableForLoops) {
				ConvertIterableLoopOperation iterableConverter= new ConvertIterableLoopOperation(fCompilationUnit, node, identifierName);
				if (iterableConverter.isApplicable().isOK()) {
					fUsedNames.put(node, identifierName);
					return iterableConverter;
				}
			}
			
			return null;
		}
		
		private String getFreeVariable(ForStatement node) {
			List usedVaribles= getUsedVariableNames(node);
			usedVaribles.addAll(fUsedNames.values());
			String[] used= (String[])usedVaribles.toArray(new String[usedVaribles.size()]);
			
			String identifierName= FOR_LOOP_ELEMENT_IDENTIFIER;
			int count= 0;
			for (int i= 0; i < used.length; i++) {
				if (used[i].equals(identifierName)) {
					identifierName= FOR_LOOP_ELEMENT_IDENTIFIER + count;
					count++;
					i= 0;
				}
			}
			return identifierName;
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
		
		private List getUsedVariableNames(ASTNode node) {
			CompilationUnit root= (CompilationUnit)node.getRoot();
			IBinding[] varsBefore= (new ScopeAnalyzer(root)).getDeclarationsInScope(node.getStartPosition(), ScopeAnalyzer.VARIABLES);
			IBinding[] varsAfter= (new ScopeAnalyzer(root)).getDeclarationsAfter(node.getStartPosition() + node.getLength(), ScopeAnalyzer.VARIABLES);
			
			List names= new ArrayList();
			for (int i= 0; i < varsBefore.length; i++) {
				names.add(varsBefore[i].getName());
			}
			for (int i= 0; i < varsAfter.length; i++) {
				names.add(varsAfter[i].getName());
			}
			return names;
		}
		
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit, boolean convertForLoops, boolean convertIterableForLoops) {
		if (!JavaModelUtil.is50OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;
		
		if (!convertForLoops && !convertIterableForLoops)
			return null;
		
		List operations= new ArrayList();
		ControlStatementFinder finder= new ControlStatementFinder(compilationUnit, convertForLoops, convertIterableForLoops, operations);
		compilationUnit.accept(finder);
		
		if (operations.isEmpty())
			return null;
		
		IFixRewriteOperation[] ops= (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]);
		return new ConvertLoopFix(FixMessages.ControlStatementsFix_change_name, compilationUnit, ops);
	}
	
	public static IFix createConvertForLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertForLoopOperation convertForLoopOperation= new ConvertForLoopOperation(loop);
		if (!convertForLoopOperation.satisfiesPreconditions())
			return null;
		
		return new ConvertLoopFix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new ILinkedFixRewriteOperation[] { convertForLoopOperation });
	}
	
	public static IFix createConvertIterableLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertIterableLoopOperation loopConverter= new ConvertIterableLoopOperation(compilationUnit, loop, FOR_LOOP_ELEMENT_IDENTIFIER);
		IStatus status= loopConverter.isApplicable();
		if (status.getSeverity() == IStatus.ERROR)
			return null;
		
		ConvertLoopFix result= new ConvertLoopFix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new ILinkedFixRewriteOperation[] { loopConverter });
		result.setStatus(status);
		return result;
	}
	
	protected ConvertLoopFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
	
}
