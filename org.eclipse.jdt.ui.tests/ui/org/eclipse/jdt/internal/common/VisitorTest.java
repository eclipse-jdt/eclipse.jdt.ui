/*******************************************************************************
 * Copyright (c) 2021, 2022 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;

public class VisitorTest {

	private static CompilationUnit result;
	private static CompilationUnit result2;

	@BeforeAll
	public static void init() {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		String code ="""
			package test;
			import java.util.Collection;
			
			public class E {
				public void hui(Collection<String> arr) {
					Collection coll = null;
					for (String var : arr) {
						 coll.add(var);
						 System.out.println(var);
						 System.err.println(var);
					}
					System.out.println(arr);
				}
			}""";
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setEnvironment(new String[]{}, new String[]{}, null, true);
		parser.setBindingsRecovery(true);
		parser.setResolveBindings(true);
		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_11, options);
		parser.setCompilerOptions(options);
		parser.setUnitName("E");
		parser.setSource(code.toCharArray());
		result = (CompilationUnit) parser.createAST(null);


		String code2="""
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings,List<String> strings2) {
			        Collections.reverse(strings);
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            Iterator it2 = strings2.iterator();
			            while (it2.hasNext()) {
			                String s2 = (String) it2.next();
			                System.out.println(s2);
			            }
			            // OK
			            System.out.println(it.next());
			        }
			        System.out.println();
			    }
			}
			""";
		parser.setEnvironment(new String[]{}, new String[]{}, null, true);
		parser.setBindingsRecovery(true);
		parser.setResolveBindings(true);
		parser.setUnitName("Test");
		parser.setSource(code2.toCharArray());
		result2 = (CompilationUnit) parser.createAST(null);
//		System.out.println(result.toString());
	}

	private void astnodeprocessorend(ASTNode node, @SuppressWarnings("unused") ReferenceHolder<String,NodeFound> holder) {
		String x = "End   "+node.getNodeType() + " :" + node;
		System.out.printf("%-40s %s%n",x,ASTNode.nodeClassForType(node.getNodeType()));
	}

	private Boolean astnodeprocesser(ASTNode node, @SuppressWarnings("unused") ReferenceHolder<String,NodeFound> holder) {
//		NodeFound nodeFound = holder.get(VisitorEnum.fromNodetype(node.getNodeType()));
		String x = "Start "+node.getNodeType() + " :" + node;
		System.out.printf("%-40s %s%n",x,ASTNode.nodeClassForType(node.getNodeType()));
		return true;
	}

	private boolean handleMethodInvocation(MethodInvocation assignment, @SuppressWarnings("unused") ReferenceHolder<String,NodeFound> holder) {
		System.out.println(assignment);
		return true;
	}

	/**
	 * Here the method reference is referring to a method using the right parameter MethodInvocation instead of ASTNode
	 */
	@Test
	public void simpleTest() {
		Set<ASTNode> nodesprocessed = null;
		HelperVisitor<ReferenceHolder<String,NodeFound>,String,NodeFound> hv = new HelperVisitor<>(nodesprocessed, new ReferenceHolder<>());
		hv.addMethodInvocation(this::handleMethodInvocation);
		hv.build(result);
	}

	/**
	 * For methodinvocation there is a method that allows to specify the method name.
	 */
	@Test
	public void simpleTest2() {
		Set<ASTNode> nodesprocessed = null;
		HelperVisitor<ReferenceHolder<String,NodeFound>,String,NodeFound> hv = new HelperVisitor<>(nodesprocessed, new ReferenceHolder<>());
		hv.addMethodInvocation("add", this::handleMethodInvocation);
		hv.build(result);


//		Function<Integer, Integer> multiply = this::extracted;
//		BiFunction<Integer, Integer, Integer> add      = this::extracted2;
//
//		BiFunction<Integer, Integer, Integer> multiplyThenAdd = add.andThen(multiply);
//
//		Integer result2 = multiplyThenAdd.apply(3, 3);
//		System.out.println(result2);
	}

//	private Integer extracted2(Integer value,Integer value2) {
//		return value + value2;
//	}
//
//	private Integer extracted(Integer value) {
//		return value * 2;
//	}

	@Test
	public void simpleTest2b() {
		Set<ASTNode> nodesprocessed = null;
		HelperVisitor<ReferenceHolder<String,NodeFound>,String,NodeFound> hv = new HelperVisitor<>(nodesprocessed, new ReferenceHolder<>());
		BiPredicate<MethodInvocation, ReferenceHolder<String, NodeFound>> bs = this::handleMethodInvocation;
		BiPredicate<MethodInvocation, ReferenceHolder<String, NodeFound>> after = (mi,mi2)->{
			return true;
		};
		BiPredicate<MethodInvocation, ReferenceHolder<String, NodeFound>> bs2= bs.or(after);
		hv.addMethodInvocation("add", bs2);
		hv.build(result);
	}

	@Test
	public void simpleTest3() {
		Set<ASTNode> nodesprocessed = null;
		HelperVisitor<ReferenceHolder<String,NodeFound>,String,NodeFound> hv = new HelperVisitor<>(nodesprocessed, new ReferenceHolder<>());
		VisitorEnum.stream().forEach(ve -> {
			hv.add(ve, this::astnodeprocesser);
		});
		VisitorEnum.stream().forEach(ve -> {
			hv.addEnd(ve, this::astnodeprocessorend);
		});
		hv.build(result);
	}

	/**
	 * Use method reference, one for "visit" and another for "visitend"
	 */
	@Test
	public void simpleTest3b() {
		Set<ASTNode> nodesprocessed = null;
		HelperVisitor<ReferenceHolder<String,NodeFound>,String,NodeFound> hv = new HelperVisitor<>(nodesprocessed, new ReferenceHolder<>());
		VisitorEnum.stream().forEach(ve -> {
			hv.add(ve, this::astnodeprocesser,this::astnodeprocessorend);
		});
		hv.build(result2);
	}

	/**
	 * Use method reference, you can use the method reference returning boolean needed for "visit" for "visitend" too.
	 * That way you need only one method.
	 */
	@Test
	public void simpleTest3c() {
		Set<ASTNode> nodesprocessed = null;
		HelperVisitor<ReferenceHolder<String,NodeFound>,String,NodeFound> hv = new HelperVisitor<>(nodesprocessed, new ReferenceHolder<>());
		VisitorEnum.stream().forEach(ve -> {
			hv.add(ve, this::astnodeprocesser,this::astnodeprocesser);
		});
		hv.build(result2);
	}

	@Test
	public void simpleTest3d() {
		Set<ASTNode> nodesprocessed = null;
		HelperVisitor<ReferenceHolder<String,NodeFound>,String,NodeFound> hv = new HelperVisitor<>(nodesprocessed, new ReferenceHolder<String, NodeFound>());
		VisitorEnum.stream().forEach(ve -> {
			hv.add(ve, (node, holder) -> {
				String x = "Start "+node.getNodeType() + " :" + node;
				System.out.printf("%-40s %s%n",x,ASTNode.nodeClassForType(node.getNodeType()));
				return true;
			});
		});
		VisitorEnum.stream().forEach(ve -> {
			hv.addEnd(ve, (node, holder) -> {
				String x = "End   "+node.getNodeType() + " :" + node;
				System.out.printf("%-40s %s%n",x,ASTNode.nodeClassForType(node.getNodeType()));
			});
		});
		hv.build(result);
	}

	/**
	 * Show how to visit a collection of nodes
	 */
	@Test
	public void simpleTest4() {
		Set<ASTNode> nodesprocessed = null;
		HelperVisitor<ReferenceHolder<String,NodeFound>,String,NodeFound> hv = new HelperVisitor<>(nodesprocessed, new ReferenceHolder<>());
		EnumSet<VisitorEnum> myset = EnumSet.of(
				VisitorEnum.SingleVariableDeclaration,
				VisitorEnum.VariableDeclarationExpression,
				VisitorEnum.VariableDeclarationStatement,
				VisitorEnum.VariableDeclarationFragment);
		myset.forEach(ve -> {
			hv.add(ve, this::astnodeprocesser,this::astnodeprocessorend);
		});
		hv.build(result);
	}

	@Test
	public void simpleTest4b() {
		Set<ASTNode> nodesprocessed = null;
		HelperVisitor<ReferenceHolder<String,NodeFound>,String,NodeFound> hv = new HelperVisitor<>(nodesprocessed, new ReferenceHolder<>());
		EnumSet<VisitorEnum> myset = EnumSet.of(
				VisitorEnum.SingleVariableDeclaration,
				VisitorEnum.VariableDeclarationExpression,
				VisitorEnum.VariableDeclarationStatement,
				VisitorEnum.VariableDeclarationFragment);
		myset.forEach(ve -> {
			addVisitor(hv, ve);
		});
		hv.build(result);
	}

	private void addVisitor(HelperVisitor<ReferenceHolder<String, NodeFound>,String,NodeFound> hv, VisitorEnum ve) {
		hv.add(ve, this::astnodeprocesser,this::astnodeprocessorend);
	}

	@Test
	public void simpleTest4c() {
		EnumSet<VisitorEnum> myset = EnumSet.of(
				VisitorEnum.SingleVariableDeclaration,
				VisitorEnum.VariableDeclarationExpression,
				VisitorEnum.VariableDeclarationStatement,
				VisitorEnum.VariableDeclarationFragment);
		ReferenceHolder<ASTNode, String> dataholder = new ReferenceHolder<>();
		BiPredicate<ASTNode, ReferenceHolder<ASTNode, String>> bs =(node,holder)->{
			System.out.printf("%-40s %s%n","Start "+node.getNodeType() + " :" + node,ASTNode.nodeClassForType(node.getNodeType()));
			return false;
		};
		BiConsumer<ASTNode, ReferenceHolder<ASTNode, String>> bc = (node,holder)->{
			System.out.printf("%-40s %s%n","End   "+node.getNodeType() + " :" + node,ASTNode.nodeClassForType(node.getNodeType()));
		};
		HelperVisitor.callVisitor(result, myset, dataholder,null, bs, bc);
	}



	/**
	 * Show how to use the ReferenceHolder to access data while visiting the AST.
	 * Here: count nodes and list result
	 */
	@Test
	public void simpleTest5() {
		Set<ASTNode> nodesprocessed = null;
		ReferenceHolder<VisitorEnum, Integer> dataholder = new ReferenceHolder<>();
		HelperVisitor<ReferenceHolder<VisitorEnum,Integer>,VisitorEnum,Integer> hv = new HelperVisitor<>(nodesprocessed, dataholder);
		VisitorEnum.stream().forEach(ve -> {
			hv.add(ve, (node, holder) -> {
				holder.merge(VisitorEnum.fromNode(node), 1, Integer::sum);
				return true;
			});
		});
		VisitorEnum.stream().forEach(ve -> {
			hv.addEnd(ve, (node, holder) -> {
				holder.merge(VisitorEnum.fromNode(node), 1, Integer::sum);
			});
		});
		hv.build(result);
		for(VisitorEnum ve: dataholder.keySet()) {
			System.out.println(dataholder.get(ve)+"\t"+ve.name());
		}
	}

	/**
	 * Show how to use the ReferenceHolder to access data while visiting the AST.
	 * Here: count nodes and list result
	 *
	 * Simpler variant compared to the one above only making use of visitend
	 */
	@Test
	public void simpleTest5b() {
		ReferenceHolder<VisitorEnum, Integer> dataholder = new ReferenceHolder<>();
		HelperVisitor.callVisitor(result, EnumSet.allOf(VisitorEnum.class), dataholder,null, this::countVisits);

		/**
		 * Presenting result
		 */
		dataholder.entrySet().stream().forEach(entry->{
			System.out.println(entry.getValue()+"\t"+entry.getKey().name());
		});
	}

	private void countVisits(ASTNode node, ReferenceHolder<VisitorEnum, Integer> holder) {
		holder.merge(VisitorEnum.fromNode(node), 1, Integer::sum);
	}

	@Test
	public void simpleTest5c() {
		ReferenceHolder<ASTNode, Integer> dataholder = new ReferenceHolder<>();
		HelperVisitor.callVisitor(result,EnumSet.of(
				VisitorEnum.SingleVariableDeclaration,
				VisitorEnum.VariableDeclarationExpression,
				VisitorEnum.VariableDeclarationStatement,
				VisitorEnum.VariableDeclarationFragment), dataholder,null, (node,holder)->{
			holder.put(node, node.getStartPosition());
		});

		/**
		 * Presenting result
		 */
		dataholder.entrySet().stream().forEach(entry->{
			System.out.println(entry.getKey()+"\t"+entry.getValue()+"\t"+ASTNode.nodeClassForType(entry.getKey().getNodeType()));
		});
	}

	@Test
	public void simpleTest5d() {
		ReferenceHolder<ASTNode, Map<String,Object>> dataholder = new ReferenceHolder<>();
		HelperVisitor.callVisitor(result,EnumSet.of(
				VisitorEnum.SingleVariableDeclaration,
				VisitorEnum.VariableDeclarationExpression,
				VisitorEnum.VariableDeclarationStatement,
				VisitorEnum.VariableDeclarationFragment), dataholder,null, (node,holder)->{
			Map<String, Object> pernodemap = holder.computeIfAbsent(node, k -> new HashMap<>());
			switch(VisitorEnum.fromNode(node)) {
			case SingleVariableDeclaration:
				SingleVariableDeclaration svd=(SingleVariableDeclaration) node;
				Expression svd_initializer = svd.getInitializer();
				pernodemap.put("init", svd_initializer);
				break;
			case VariableDeclarationExpression:
//				VariableDeclarationExpression vde=(VariableDeclarationExpression) node;
				ASTNodes.getTypedAncestor(node, Statement.class);
				break;
			case VariableDeclarationStatement:
//				VariableDeclarationStatement vds=(VariableDeclarationStatement) node;

				break;
			case VariableDeclarationFragment:
				VariableDeclarationFragment vdf=(VariableDeclarationFragment) node;
				Expression vdf_initializer = vdf.getInitializer();
				pernodemap.put("init", vdf_initializer);
				break;
					//$CASES-OMITTED$
				default:
				break;
			}
		});

		/**
		 * Presenting result
		 */
		dataholder.entrySet().stream().forEach(entry->{
			System.out.println(entry.getKey()+"\t"+ASTNode.nodeClassForType(entry.getKey().getNodeType()));
			System.out.println("===>"+entry.getValue().get("init"));
			System.out.println();
		});
	}

	@Test
	public void simpleTest5e() {
		ReferenceHolder<ASTNode, Map<String,Object>> dataholder = new ReferenceHolder<>();
		HelperVisitor.callVariableDeclarationStatementVisitor(Iterator.class, result2, dataholder,null, (init_iterator,holder_a)->{
			List<String> computeVarName = computeVarName(init_iterator);
			HelperVisitor.callWhileStatementVisitor(init_iterator.getParent(), dataholder,null, (whilestatement,holder)->{
				String name = computeNextVarname(whilestatement);
				if(computeVarName.get(0).equals(name)) {
					HelperVisitor.callMethodInvocationVisitor("next", whilestatement.getBody() ,dataholder,null, (mi,holder2)->{
						Map<String, Object> pernodemap2 = holder2.computeIfAbsent(whilestatement, k -> new HashMap<>());
						Expression element2 = mi.getExpression();
						SimpleName sn= ASTNodes.as(element2, SimpleName.class);
						if (sn !=null) {
							String identifier = sn.getIdentifier();
							if(!name.equals(identifier))
								return true;
							pernodemap2.put("init", init_iterator);
							pernodemap2.put("while", whilestatement);
							pernodemap2.put("next", mi);
							pernodemap2.put("name", identifier);
							return true;
							//											if(holder.containsKey(identifier)) {
							//											if (holder.getHelperVisitor().nodesprocessed.contains(hit.whilestatement)) {
							//												holder.remove(identifier);
							//												return true;
							//											}
						}
						return true;
					});
				}
				return true;
			});
			return true;
		});
		/**
		 * Presenting result
		 */
		System.out.println("#################");
		dataholder.entrySet().stream().forEach(entry->{
			System.out.println("=============");
			System.out.println(entry.getKey());
			System.out.println("init ===>"+entry.getValue().get("init"));
			System.out.println("while ===>"+entry.getValue().get("while"));
			System.out.println("next ===>"+entry.getValue().get("next"));
			System.out.println("name ===>"+entry.getValue().get("name"));
			System.out.println();
		});
	}

	private static String computeNextVarname(WhileStatement whilestatement) {
		String name = null;
		Expression exp = whilestatement.getExpression();
//		Collection<String> usedVarNames= getUsedVariableNames(whilestatement.getBody());
		if (exp instanceof MethodInvocation) {
			MethodInvocation mi = (MethodInvocation) exp;
			Expression expression = mi.getExpression();
			if (mi.getName().getIdentifier().equals("hasNext")) { //$NON-NLS-1$
//				ITypeBinding resolveTypeBinding = expression.resolveTypeBinding();
				SimpleName variable= ASTNodes.as(expression, SimpleName.class);
				if (variable != null) {
					IBinding resolveBinding = variable.resolveBinding();
					name = resolveBinding.getName();
				}
			}
		}
		return name;
	}

	private static List<String> computeVarName(VariableDeclarationStatement node_a) {
		List<String> name = new ArrayList<>();
		VariableDeclarationFragment bli = (VariableDeclarationFragment) node_a.fragments().get(0);
		name.add(bli.getName().getIdentifier());
		Expression exp = bli.getInitializer();
		if (exp instanceof MethodInvocation) {
			MethodInvocation mi = (MethodInvocation) exp;
			Expression element = mi.getExpression();{
				if (element instanceof SimpleName) {
					SimpleName sn = (SimpleName) element;
					if (mi.getName().toString().equals("iterator")) { //$NON-NLS-1$
						name.add(sn.getIdentifier());
					}
				}
			}
		}
		return name;
	}

	/**
	 * Here we use fluent style to express visiting where the subsequent search is starting at the node the preceding search found.
	 *
	 * This sample finds the 3 nodes related to a while loop based on iterator for you.
	 *
	 * 1) VariableDeclarationStatement
	 * 2) "below 1)" all WhileStatement
	 * 3) "below 2)" all MethodInvocation
	 *
	 * "below" is not meant literally as there is a helper lambda expression that allows to navigate on the found node of
	 * the type searched for to another node as a start for the directly following fluent call.
	 *
	 * That means in this case by the lambda expression "s->s.getParent()" in the call to search for VariableDeclarationStatement
	 * the found node is not directly used as start node for the following call to search for WhileStatement. Instead the parent of this node
	 * is used. Otherwise it would not be possible to find related whilestatements.
	 *
	 * A similar trick is used in the next call to find all related MethodInvocations. As we are only interested in ".next()" calls in the
	 * while loop body we use the lambda expression "s -> ((WhileStatement)s).getBody()" to go on searching for ".next()".
	 */
	@Test
	public void simpleTest5f() {
		ReferenceHolder<String, Object> dataholder = new ReferenceHolder<>();
		ASTProcessor<ReferenceHolder<String, Object>, String, Object> astp=new ASTProcessor<>(dataholder, null);
		astp.callVariableDeclarationStatementVisitor(Iterator.class,(node,holder) -> {
			/**
			 * This lambda expression is called for all VariableDeclarationStatement of type Iterator
			 */
			holder.put("init", node);
			List<String> computeVarName = computeVarName((VariableDeclarationStatement)node);
			holder.put("initvarname", computeVarName.get(0));
			return true;
		},s -> s.getParent()).callWhileStatementVisitor((node,holder) -> {
			/**
			 * This lambda expression is called for all WhileStatements below the parent of each VariableDeclarationStatement
			 */
			holder.put("while", node);
			String name = computeNextVarname((WhileStatement)node);
			holder.put("whilevarname", name);
			return true;
		}, s -> ((WhileStatement)s).getBody()).callMethodInvocationVisitor("next",(node,holder) -> {
			/**
			 * This lambda expression is called for all MethodInvocations "next()" in each Body of WhileStatements found above
			 */
			String name=(String) holder.get("initvarname");
			Expression element2 = ((MethodInvocation)node).getExpression();
			SimpleName sn= ASTNodes.as(element2, SimpleName.class);
			if (sn !=null) {
				String identifier = sn.getIdentifier();
				if(!name.equals(identifier))
					return true;
				if(name.equals(holder.get("whilevarname"))) {
					System.out.println("=====================");
					System.out.println("iterator: "+holder.get("init").toString().trim());
					System.out.println("while: "+holder.get("while").toString().trim());
					System.out.println("next: "+node.toString().trim());
				}
			}
			return true;
		}).build(result2);
	}

	@Test
	public void simpleTest5g() {
		ReferenceHolder<String, Object> dataholder = new ReferenceHolder<>();
		ASTProcessor<ReferenceHolder<String, Object>,String,Object> astp=new ASTProcessor<>(dataholder, null);
		astp.callVariableDeclarationStatementVisitor(Iterator.class,(node,holder) -> {
			holder.put("init", node);
			List<String> computeVarName = computeVarName((VariableDeclarationStatement)node);
			holder.put("initvarname", computeVarName.get(0));
			System.out.println("init "+node.getNodeType() + " :" + node);
			return true;
		}).build(result2);
	}

	/**
	 * This one is not really possible in "normal" visitors. Change visitors while visiting.
	 */
	@Test
	public void modifyTest1() {
		Set<ASTNode> nodesprocessed = null;
		HelperVisitor<ReferenceHolder<String,NodeFound>,String,NodeFound> hv = new HelperVisitor<>(nodesprocessed, new ReferenceHolder<>());
		hv.addMethodInvocation("println",(node, holder) -> {
			System.out.println("Start "+node.getNodeType() + " :" + node);
			return true;
		});
		hv.addMethodInvocation((node, holder) -> {
			System.out.println("End "+node.getNodeType() + " :" + node);
			holder.getHelperVisitor().removeVisitor(VisitorEnum.MethodInvocation);
		});
		hv.build(result);
	}

	@Test
	public void modifyTest2() {
		Set<ASTNode> nodesprocessed = null;
		ExpectationTracer dataholder = new ExpectationTracer();
		dataholder.stack.push(null);
		HelperVisitor<ExpectationTracer,ASTNode, SimpleName> hv = new HelperVisitor<>(nodesprocessed, dataholder);
		Set<SimpleName> names = new HashSet<>();
		Set<ASTNode> nodes = new HashSet<>();
		hv.addSingleVariableDeclaration((node, holder) -> {
			names.add(node.getName());
			return true;
		});
		hv.addVariableDeclarationFragment((node, holder) -> {
			names.add(node.getName());
			return true;
		});
		hv.addWhileStatement((node, holder) -> {
			nodes.add(node);
			return true;
		});
		hv.addWhileStatement((node, holder) -> {
			nodes.remove(node);
			Collection<String> usedVarNames= getUsedVariableNames(node.getBody());
			System.out.println(usedVarNames);
		});
		hv.addMethodInvocation("next",(methodinvocationnode, myholder) -> {
			String x = "Start "+methodinvocationnode.getNodeType() + " :" + methodinvocationnode;
			System.out.printf("%-40s %s%n",x,ASTNode.nodeClassForType(methodinvocationnode.getNodeType()));
			return true;
		});
		hv.build(result2);
	}

	Collection<String> getUsedVariableNames(ASTNode node) {
		CompilationUnit root= (CompilationUnit) node.getRoot();
		Collection<String> res= (new ScopeAnalyzer(root)).getUsedVariableNames(node.getStartPosition(), node.getLength());
		return res;
	}
}
