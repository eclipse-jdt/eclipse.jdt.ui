package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

public class ProblemNodeFinder {

	private ProblemNodeFinder(){}
	
	public static SimpleName[] getProblemNodes(MethodDeclaration methodNode, TextEdit[] edits, TextChange change, String key){
		NameNodeVisitor visitor= new NameNodeVisitor(edits, change, key);
		methodNode.accept(visitor);
		return visitor.getProblemNodes();
	}
	
	private static class NameNodeVisitor extends ASTVisitor {

		private Collection fRanges;
		private Collection fProblemNodes;
		private String fKey;

		public NameNodeVisitor(TextEdit[] edits, TextChange change, String key) {
			Assert.isNotNull(edits);
			Assert.isNotNull(key);
			fRanges= new HashSet(Arrays.asList(RefactoringAnalyzeUtil.getRanges(edits, change)));
			fProblemNodes= new ArrayList(0);
			fKey= key;
		}

		public SimpleName[] getProblemNodes() {
			return (SimpleName[]) fProblemNodes.toArray(new SimpleName[fProblemNodes.size()]);
		}

		private static VariableDeclaration getVariableDeclaration(Name node) {
			IBinding binding= node.resolveBinding();
			if (binding == null && node.getParent() instanceof VariableDeclaration)
				return (VariableDeclaration) node.getParent();

			if (binding != null && binding.getKind() == IBinding.VARIABLE) {
				CompilationUnit cu= (CompilationUnit) ASTNodes.getParent(node, CompilationUnit.class);
				return ASTNodes.findVariableDeclaration(((IVariableBinding) binding), cu);
			}
			return null;
		}

		//----- visit methods 

		public boolean visit(SimpleName node) {
			VariableDeclaration decl= getVariableDeclaration(node);
			if (decl == null)
				return super.visit(node);
			boolean keysEqual= fKey.equals(RefactoringAnalyzeUtil.getFullBindingKey(decl));
			boolean rangeInSet= fRanges.contains(TextRange.createFromStartAndLength(node.getStartPosition(), node.getLength()));

			if (keysEqual && !rangeInSet)
				fProblemNodes.add(node);

			if (!keysEqual && rangeInSet)
				fProblemNodes.add(node);

			return super.visit(node);
		}
	}
}

