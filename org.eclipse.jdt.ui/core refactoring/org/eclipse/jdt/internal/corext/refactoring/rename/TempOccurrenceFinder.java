package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.refactoring.Assert;

public class TempOccurrenceFinder {

	//no instances
	private TempOccurrenceFinder(){}
	
	public static Integer[] findTempOccurrenceOffsets(CompilationUnit cu, VariableDeclaration temp, boolean includeReferences, boolean includeDeclaration) {
		TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(temp, includeReferences, includeDeclaration);
		cu.accept(analyzer);
		return analyzer.getOffsets();
	} 
	
	private static class TempOccurrenceAnalyzer extends ASTVisitor {
		private Set fOffsets;
		private boolean fIncludeReferences;
		private boolean fIncludeDeclaration;
		private VariableDeclaration fTempDeclaration;
		private IBinding fTempBinding;
		
		TempOccurrenceAnalyzer(VariableDeclaration tempDeclaration, boolean includeReferences, boolean includeDeclaration){
			Assert.isNotNull(tempDeclaration);
			fOffsets= new HashSet();
			fIncludeDeclaration= includeDeclaration;
			fIncludeReferences= includeReferences;
			fTempDeclaration= tempDeclaration;
			fTempBinding= tempDeclaration.resolveBinding();
		}
		
		Integer[] getOffsets(){
			return (Integer[]) fOffsets.toArray(new Integer[fOffsets.size()]);
		}
		
		private boolean visitNameReference(Name nameReference){
			if (!fIncludeReferences)
				return true;	

			if (fTempBinding != null && fTempBinding == nameReference.resolveBinding())
				fOffsets.add(new Integer(nameReference.getStartPosition()));	
					
			return true;
		}

		private boolean visitVariableDeclaration(VariableDeclaration localDeclaration) {
			if (! fIncludeDeclaration)
				return true;
			
			if (fTempDeclaration.equals(localDeclaration))
				fOffsets.add(new Integer(localDeclaration.getName().getStartPosition()));
			
			return true;
		}
	
		//------- visit ------
		public boolean visit(SingleVariableDeclaration localDeclaration){
			return visitVariableDeclaration(localDeclaration);
		}
		
		public boolean visit(VariableDeclarationFragment localDeclaration){
			return visitVariableDeclaration(localDeclaration);	
		}
			
		public boolean visit(SimpleName singleNameReference){
			return visitNameReference(singleNameReference);
		}
	}
}
