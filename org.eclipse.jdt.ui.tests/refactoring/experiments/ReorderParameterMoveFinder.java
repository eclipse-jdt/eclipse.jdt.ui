package experiments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.AbstractRefactoringASTAnalyzer;
import org.eclipse.jdt.internal.core.refactoring.DebugUtils;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;

class ReorderParameterMoveFinder extends AbstractSyntaxTreeVisitorAdapter {
	
	private List fRegionsFound;
	private SearchResult[] fSearchResults;
	
	/**
	 * returns List of TextRegion[]
	 */
	List findParameterRegions(SearchResult[] searchResults, ICompilationUnit cu) throws JavaModelException{
		fSearchResults= searchResults;
		fRegionsFound= new ArrayList();
		((CompilationUnit)cu).accept(this);
		return fRegionsFound;
	}
	
	private static TextRegion[] createRegionsArray(MessageSend messageSend){
		TextRegion[] result= new TextRegion[messageSend.arguments.length];
		for (int i= 0; i < result.length; i++){
			int length= messageSend.arguments[i].sourceEnd() - messageSend.arguments[i].sourceStart() + 1;
			result[i]= new TextRegion(messageSend.arguments[i].sourceStart(), length);
		}
		return result;
	}
	
	private static TextRegion[] createRegionsArray(MethodDeclaration methodDeclaration){
		TextRegion[] result= new TextRegion[methodDeclaration.arguments.length];
		for (int i= 0; i < result.length; i++){
			int length= methodDeclaration.arguments[i].sourceEnd() - methodDeclaration.arguments[i].sourceStart() + 1;
			result[i]= new TextRegion(methodDeclaration.arguments[i].sourceStart(), length);
		}
		return result;
	}
	
	private boolean isStartPositionOnList(int start){
		for (int i= 0; i< fSearchResults.length; i++){
			if (fSearchResults[i].getStart() == start)
				return true;
		};
		return false;
	}

	private boolean isOurMethod(MessageSend messageSend){
		return isStartPositionOnList((int) (messageSend.nameSourcePosition >> 32));
	}

	private boolean isOurMethod(MethodDeclaration methodDeclaration){
		return isStartPositionOnList(methodDeclaration.sourceStart());
	}	
	
	//------visit methods
	
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		if (isOurMethod(messageSend))
			fRegionsFound.add(createRegionsArray(messageSend));
		return true;
	}
	
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		if (isOurMethod(methodDeclaration))
			fRegionsFound.add(createRegionsArray(methodDeclaration));
		return true;
	}
}

