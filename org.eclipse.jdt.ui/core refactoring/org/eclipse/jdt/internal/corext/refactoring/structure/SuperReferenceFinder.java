package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.AnonymousLocalTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.LocalTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.refactoring.util.ASTUtil;

class SuperReferenceFinder {

	//no instances
	private SuperReferenceFinder(){
	}
	
	public static ISourceRange[] findSuperReferenceRanges(IMethod method) throws JavaModelException{
		Assert.isNotNull(method);
		AST ast= new AST(method.getCompilationUnit());
		Visitor visitor= new Visitor(method);
		ast.accept(visitor);
		return visitor.getSuperReferenceRanges();
	}
	
	//--- ast visitor ----
	private static class Visitor extends AbstractSyntaxTreeVisitorAdapter{

		private List fFoundRanges;
		private IMethod fMethod;
		private int fMethodSourceStart;
		private int fMethodSourceEnd;
		
		Visitor(IMethod method) throws JavaModelException{
			fMethod= method;
			fFoundRanges= new ArrayList(0);
			fMethodSourceStart= fMethod.getSourceRange().getOffset();
			fMethodSourceEnd= fMethod.getSourceRange().getOffset() + fMethod.getSourceRange().getLength();
		}		
		
		ISourceRange[] getSuperReferenceRanges(){
			return (ISourceRange[]) fFoundRanges.toArray(new ISourceRange[fFoundRanges.size()]);
		}
		
		private boolean withinMethod(AstNode node){
			return (node.sourceStart >= fMethodSourceStart) && (node.sourceStart <= fMethodSourceEnd);
		}
		
		private static ISourceRange getSourceRange(AstNode node){
			int start= ASTUtil.getSourceStart(node);
			int end= ASTUtil.getSourceEnd(node);
			return new SourceRange(start, end - start + 1);
		}
		
		public boolean visit(SuperReference superReference, BlockScope scope) {
			if (! withinMethod(superReference))
				return true;
			
			fFoundRanges.add(getSourceRange(superReference));
			return true;
		}
		
		public boolean visit(LocalTypeDeclaration localTypeDeclaration, BlockScope scope) {
			if (withinMethod(localTypeDeclaration))
				return false;
			return true;
		}
		
		public boolean visit(AnonymousLocalTypeDeclaration anonymousTypeDeclaration, BlockScope scope) {
			if (withinMethod(anonymousTypeDeclaration))
				return false;	
			return true;
		}	
	}
}

