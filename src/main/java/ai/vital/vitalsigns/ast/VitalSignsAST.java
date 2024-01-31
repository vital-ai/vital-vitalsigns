package ai.vital.vitalsigns.ast;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.constant.ConsiderValueConstant;
import ai.vital.vitalsigns.constant.HasValueConstant;
import ai.vital.vitalsigns.constant.TruthConstant;
import ai.vital.vitalsigns.datatype.Truth;
import groovy.transform.Trait;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.syntax.Types;

import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static groovyjarjarasm.asm.Opcodes.ACC_STATIC;

import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.util.List;


@GroovyASTTransformation(phase=CompilePhase.CANONICALIZATION)
public class VitalSignsAST implements ASTTransformation {

    public VitalSignsAST() {
        super();
    }
    @Override
    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {

        // System.out.println("Applying AST Transformation to: " + sourceUnit.getName());

        for (ClassNode classNode : sourceUnit.getAST().getClasses()) {

            addConsiderMethod(classNode);

            addConsiderValueMethod(classNode);

            for (MethodNode method : classNode.getMethods()) {

                if(method.getCode() == null) { continue; }

                method.getCode().visit( new CodeVisitorSupport() {
                    @Override
                    public void visitBinaryExpression(BinaryExpression expression) {

                        if (expression.getOperation().getType() == Types.COMPARE_EQUAL) {

                            Expression leftExpr = expression.getLeftExpression();

                            Expression rightExpr = expression.getRightExpression();

                            MethodCallExpression customEqualityCall = new MethodCallExpression(
                                    new ClassExpression(new ClassNode(VitalSignsAST.class)),
                                    "customEqualityCheck",
                                    new ArgumentListExpression(new Expression[]{leftExpr, rightExpr})
                            );

                            customEqualityCall.setMethodTarget(
                                    new ClassNode(VitalSignsAST.class).getMethod("customEqualityCheck", new Parameter[]{
                                            new Parameter(ClassHelper.OBJECT_TYPE, "a"),
                                            new Parameter(ClassHelper.OBJECT_TYPE, "b")
                                    })
                            );

                            expression.setLeftExpression(customEqualityCall);

                            expression.setRightExpression(new ConstantExpression(true));
                        }
                        super.visitBinaryExpression(expression);
                    }
                });
            }
        }
    }


    private void addConsiderMethod(ClassNode classNode) {

        if(classNode.isInterface()) { return; }

        if (classNode.getAnnotations(new ClassNode(Trait.class)).isEmpty()) {

            List<MethodNode> methodList = classNode.getMethods("consider");


            // remove method inserted by implementing ConsiderInterface
            for(MethodNode considerMethod : methodList) {

                classNode.removeMethod(considerMethod);

            }


            BlockStatement body = new BlockStatement();

            Parameter[] params = new Parameter[] {new Parameter(ClassHelper.OBJECT_TYPE, "arg")};


            body.addStatement(new ExpressionStatement(
                    new StaticMethodCallExpression(
                            new ClassNode(VitalSignsAST.class),
                            "considerImplementation",
                            new ArgumentListExpression(new Expression[] {new VariableExpression("arg")})
                    )
            ));

            ClassNode truthClassNode = new ClassNode(Truth.class);

            AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(VitalSignsAnnotation.class));

            MethodNode methodNode = new MethodNode(
                    "consider",
                    //ACC_PUBLIC | ACC_STATIC,
                    ACC_PUBLIC,
                    truthClassNode,
                    params,
                    ClassNode.EMPTY_ARRAY,
                    body
            );

            methodNode.addAnnotation(annotationNode);

            // switched to non-static
            // static method
            classNode.addMethod( methodNode );

            return;
        }
        else {

            BlockStatement body = new BlockStatement();

            Parameter[] params = new Parameter[] {new Parameter(ClassHelper.OBJECT_TYPE, "arg")};

            body.addStatement(new ExpressionStatement(
                    new StaticMethodCallExpression(
                            new ClassNode(VitalSignsAST.class),
                            "considerImplementation",
                            new ArgumentListExpression(new Expression[] {new VariableExpression("arg")})
                    )
            ));

            // non-static method

            ClassNode truthClassNode = new ClassNode(Truth.class);

            AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(VitalSignsAnnotation.class));

            MethodNode methodNode = new MethodNode(
                    "consider",
                    ACC_PUBLIC,
                    truthClassNode,
                    params,
                    ClassNode.EMPTY_ARRAY,
                    body
            );

            methodNode.addAnnotation(annotationNode);

            classNode.addMethod( methodNode );

            return;
        }

    }


    private void addConsiderValueMethod(ClassNode classNode) {

        if (classNode.getAnnotations(new ClassNode(Trait.class)).isEmpty()) {

            BlockStatement body = new BlockStatement();

            Parameter[] params = new Parameter[] {new Parameter(ClassHelper.OBJECT_TYPE, "arg")};

            body.addStatement(new ExpressionStatement(
                    new StaticMethodCallExpression(
                            new ClassNode(VitalSignsAST.class),
                            "considerValueImplementation",
                            new ArgumentListExpression(new Expression[] {new VariableExpression("arg")})
                    )
            ));


            ClassNode considerClassNode = new ClassNode(ConsiderValueConstant.class);


            AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(VitalSignsAnnotation.class));

            MethodNode methodNode = new MethodNode(
                    "considerValue",
                    ACC_PUBLIC | ACC_STATIC,
                    considerClassNode,
                    params,
                    ClassNode.EMPTY_ARRAY,
                    body
            );

            methodNode.addAnnotation(annotationNode);

            // static method
            classNode.addMethod( methodNode );

            return;
        }
        else {

            BlockStatement body = new BlockStatement();

            Parameter[] params = new Parameter[] {new Parameter(ClassHelper.OBJECT_TYPE, "arg")};

            body.addStatement(new ExpressionStatement(
                    new StaticMethodCallExpression(
                            new ClassNode(VitalSignsAST.class),
                            "considerValueImplementation",
                            new ArgumentListExpression(new Expression[] {new VariableExpression("arg")})
                    )
            ));

            // non-static method

            ClassNode considerClassNode = new ClassNode(ConsiderValueConstant.class);

            AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(VitalSignsAnnotation.class));

            MethodNode methodNode = new MethodNode("considerValue",
                    ACC_PUBLIC,
                    considerClassNode,
                    params,
                    ClassNode.EMPTY_ARRAY,
                    body
            );

            methodNode.addAnnotation(annotationNode);

            classNode.addMethod( methodNode );

            return;
        }

    }


    public static Truth considerImplementation(Object obj) {

        System.out.println("ConsiderImplementation logic executed.");

        if(obj == null) {

            System.out.println("ConsiderImplementation Object Parameter Class: " + null );

            return Truth.UNKNOWN;


        }
        else {

            Class c = obj.getClass();

            System.out.println("ConsiderImplementation Object Parameter Class: " + c.getName());

            return Truth.YES;


        }

        // return ConsiderValue.UNKNOWN;
    }

    public static ConsiderValueConstant considerValueImplementation(Object obj) {

        System.out.println("ConsiderValueImplementation logic executed.");

        if(obj == null) {

            System.out.println("ConsiderValueImplementation Object Parameter Class: " + null );

            return TruthConstant.UNKNOWN;


        }
        else {

            Class c = obj.getClass();

            System.out.println("ConsiderValueImplementation Object Parameter Class: " + c.getName());

            return HasValueConstant.HasValue;


        }

        // return ConsiderValue.UNKNOWN;
    }



    public static boolean customEqualityCheck(Object a, Object b) {

        System.out.println("Comparing " + a + " with " + b);

        // defer back to groovy side
        return VitalSigns.equality(a,b);

        // return a.equals(b);
    }
}
