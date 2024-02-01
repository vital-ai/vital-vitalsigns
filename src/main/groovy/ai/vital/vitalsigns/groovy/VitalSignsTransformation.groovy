package ai.vital.vitalsigns.groovy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.BreakStatement;
import org.codehaus.groovy.ast.stmt.CaseStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.tools.GeneralUtils;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.openjena.atlas.test.Gen;

import ai.vital.vitalsigns.datatype.Truth;


@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
public class VitalSignsTransformation implements ASTTransformation {

    public VitalSignsTransformation() {
        super();
    }

    @Override
    public void visit(ASTNode[] nodes, final SourceUnit sourceUnit) {
        try {
            //local AST 
            if(nodes.length > 1) {
                doVisit((ClassNode) nodes[1], sourceUnit);
            } else {
                //global AST 
                ModuleNode mn = (ModuleNode) nodes[0];
                for(ClassNode n : mn.getClasses()) {
                    doVisit(n, sourceUnit);
                }
            }
        } catch(Exception e) {
            System.err.println(e.getLocalizedMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    private void doVisit(ClassNode classNode , final SourceUnit sourceUnit) {
        
        //order matters!
        ClassCodeExpressionTransformer directAccessTransformer = new ClassCodeExpressionTransformer() {
            
            List<Expression> context = new ArrayList<Expression>();
            
            @Override
            protected SourceUnit getSourceUnit() {
                return sourceUnit;
            }
            
            @Override
            protected void visitStatement(Statement statement) {
//                System.out.println("STATMENT: " + statement);
                super.visitStatement(statement);
            }
            
            @Override
            public Expression transform(Expression exp) {
                
//              System.out.println("EXPRESSION: " + exp);
                
                if(exp instanceof AttributeExpression) {
                   
                    boolean partOfAssignment = false;
                    
                    if(context.size() > 0) {
                        Expression expression = context.get(context.size() - 1);
                        if(expression instanceof BinaryExpression) {
                            BinaryExpression binaryExpression = (BinaryExpression)expression;
                            if(binaryExpression.getLeftExpression() == exp) {
                                String operator = binaryExpression.getOperation().getText();
                                if(operator.equals("=")) {
                                    partOfAssignment = true;
                                }
                            }
                        }
                    }
                    
                    AttributeExpression ae = (AttributeExpression) exp;
                    
//                    System.out.println("Attribute expression: " + ae);
                    
                    return new StaticMethodCallExpression(new ClassNode(VitalSignsDirectFieldAccessHandler.class), "handleDirectFieldAccess", new ArgumentListExpression(ae.getObjectExpression(), ae.getProperty(), GeneralUtils.constX(partOfAssignment, true)));
                            
                } else if(exp instanceof ClosureExpression) {
                    
                    //this visits closure body
                    ClosureExpression ce = (ClosureExpression) exp;
                    
                    Statement code = ce.getCode();
                    if(code instanceof BlockStatement) {
                        visitBlockStatement((BlockStatement) code);
                    }
//                    ce.visit(this);
                    
                    return ce;
                    
                    
                }

                context.add(exp);
                
                Expression r = super.transform(exp);
                
                context.remove(exp);
                
                return r;
                
            }
            
            
        };
        
        ClassCodeExpressionTransformer considerAssignTranformer = new ClassCodeExpressionTransformer() {

            private List<BlockStatement> blockContext = new ArrayList<BlockStatement>();
            
            private ExpressionStatement currentExpressionStmt;
            
            @Override
            protected SourceUnit getSourceUnit() {
                return sourceUnit;
            }
            
            @Override
            public Expression transform(Expression exp) {
                
//                System.out.println("EXPRESSION: " + exp);
                
                if(exp instanceof ClosureExpression) {
                    
                    //this visits closure body
                    ClosureExpression ce = (ClosureExpression) exp;
                    
                    Statement code = ce.getCode();
                    if(code instanceof BlockStatement) {
                        visitBlockStatement((BlockStatement) code);
                    }
//                    ce.visit(this);
                    
                    return ce;
                    
                } else if(exp instanceof MethodCallExpression) {
                    
                    MethodCallExpression mce = (MethodCallExpression)exp;
                    String m = mce.getMethodAsString();
                    
                    if(m.equals("consider")) {
                        
                        BlockStatement yesStatement = null;
                        BlockStatement noStatement = null;
                        BlockStatement muStatement = null;
                        BlockStatement valueStatement = null;
                        BlockStatement unknownStatement = null;
                        
                        Expression condition = null;
                        
                        BlockStatement blockStatement = null;
                        
                        int currentIndex = -1;
                        
                        try {
                            
                            Expression args = mce.getArguments();
                            
                            if(!(args instanceof ArgumentListExpression)) throw new RuntimeException("Expected arg list exception");
                            
                            ArgumentListExpression ale = (ArgumentListExpression) args;
                                
                            if(ale.getExpressions().size() != 2) throw new RuntimeException("Expected 2 args");
                                    
                            condition = ale.getExpression(0);
                            Expression e2 = ale.getExpression(1);
                            
                            if(!(e2 instanceof ClosureExpression)) throw new RuntimeException("Second arg is to a closure");
                            
                            ClosureExpression ce = (ClosureExpression) e2;

                            Statement code = ce.getCode();
                                        
                            if(!(code instanceof BlockStatement)) throw new RuntimeException("Closure body must be a block");
                                         
                            for(Statement stmt : ((BlockStatement) code).getStatements()) {
                                             
                                if(stmt instanceof BlockStatement) {
                                                    
                                    BlockStatement bs = (BlockStatement) stmt;
                                                    
                                    String statementLabel = bs.getStatementLabel();
                                                    
                                    if("YES".equals(statementLabel)) {
                                        if(yesStatement != null) throw new RuntimeException("More than 1 YES block!");
                                        yesStatement = bs;
                                    } else if("NO".equals(statementLabel)) {
                                        if(noStatement != null) throw new RuntimeException("More than 1 NO block!");
                                        noStatement = bs;
                                    } else if("UNKNOWN".equals(statementLabel)) {
                                        if(unknownStatement != null) throw new RuntimeException("More than 1 UNKNOWN block!");
                                        unknownStatement = bs;
                                    } else if("VALUE".equals(statementLabel)) {
                                        if(valueStatement != null) throw new RuntimeException("More than 1 VALUE block!");
                                        valueStatement = bs;
                                    } else if("MU".equals(statementLabel)) {
                                        if(muStatement != null) throw new RuntimeException("More than 1 MU block!");
                                        muStatement = bs;
                                    } else {
                                        throw new RuntimeException("Unlabelled block or unexpected label: " + statementLabel);
                                    }
                                    
                                } else {
                                    
                                    throw new RuntimeException("Only blocks allowed in closure");
                                    
                                }
                                
                            }
                            
                            if(yesStatement == null && noStatement == null && unknownStatement == null && valueStatement == null && muStatement == null) {
                                throw new RuntimeException("Empty consider closure!");
                            }
                            
                            
                            if(currentExpressionStmt == null) throw new RuntimeException("No current method expression");
                            
                            if(currentExpressionStmt.getExpression() != mce ) throw new RuntimeException("non-aligned method expression");
                            
                            if(this.blockContext.size() < 1) throw new RuntimeException("block context is empty!");
                            
                            blockStatement = this.blockContext.get(this.blockContext.size() - 1 );
                            
                            currentIndex = blockStatement.getStatements().indexOf(currentExpressionStmt);
                            
                            if(currentIndex < 0) throw new RuntimeException("current statement index must be >= 0");
                            
                        } catch(RuntimeException e) {
                            
                            Expression val = super.transform(exp);
                            
                            return val;
                        }
                        
                        
                        
                        SwitchStatement ss = new SwitchStatement(condition);
                        
//                        BlockStatement yesStatementBlock = new BlockStatement();
                        
                        if(yesStatement == null) {
                            yesStatement = new BlockStatement();
                        } else {
                            visitBlockStatement(yesStatement);
                        }
                        yesStatement.addStatement(new BreakStatement());
                        
//                        if(yesStatement != null) {
//                            visitBlockStatement(yesStatement);
//                            yesStatementBlock.addStatement(yesStatement);
//                        }
//                        yesStatementBlock.addStatement(new BreakStatement());
                        CaseStatement yesCase = new CaseStatement( GeneralUtils.fieldX(new ClassNode(Truth.class), "YES"), yesStatement );
                        ss.addCase(yesCase);
                        
                        CaseStatement trueCase = new CaseStatement( GeneralUtils.fieldX(new ClassNode(Boolean.class), "TRUE"), yesStatement );
                        ss.addCase(trueCase);
                        
                        
                        
                        if(noStatement == null) {
                            noStatement = new BlockStatement();
                        } else {
                            visitBlockStatement(noStatement);
                        }
                        noStatement.addStatement(new BreakStatement());
                        CaseStatement noCase = new CaseStatement(GeneralUtils.fieldX(new ClassNode(Truth.class), "NO"), noStatement);
                        ss.addCase(noCase);
                        
                        CaseStatement falseCase = new CaseStatement( GeneralUtils.fieldX(new ClassNode(Boolean.class), "FALSE"), noStatement );
                        ss.addCase(falseCase);
                        
                        
                        if(unknownStatement == null) {
                            unknownStatement = new BlockStatement();
                            StaticMethodCallExpression nexp = new StaticMethodCallExpression(new ClassNode(VitalSignsTransformation.class), "defaultUnknown", new ArgumentListExpression());
//                            return nexp;
//                            ConstructorCallExpression ccexp
                            unknownStatement.addStatement(new ExpressionStatement(nexp));
                        } else {
                            visitBlockStatement(unknownStatement);
                        }
                        unknownStatement.addStatement(new BreakStatement());
                        CaseStatement unknownCase = new CaseStatement(GeneralUtils.fieldX(new ClassNode(Truth.class), "UNKNOWN"), unknownStatement);
                        ss.addCase(unknownCase);
                        
                        if(muStatement == null) {
                            muStatement = new BlockStatement();
                            StaticMethodCallExpression nexp = new StaticMethodCallExpression(new ClassNode(VitalSignsTransformation.class), "defaultMu", new ArgumentListExpression());
                            muStatement.addStatement(new ExpressionStatement(nexp));
                        } else {
                            visitBlockStatement(muStatement);
                        }
                        muStatement.addStatement(new BreakStatement());
                        CaseStatement muCase = new CaseStatement(GeneralUtils.fieldX(new ClassNode(Truth.class), "MU"), muStatement);
                        ss.addCase(muCase);

                        
                        CaseStatement nullCase = new CaseStatement(GeneralUtils.fieldX(new ClassNode(VitalSignsTransformation.class), "NULL"), unknownStatement);
                        ss.addCase(nullCase);

                        //if value statement available we need to add if else block 
                        
                        
                        Statement targetStatement = null;
                        
                        if(valueStatement == null) valueStatement = new BlockStatement();
                        
//                        if(valueStatement != null) {
                            
                            BinaryExpression truthOrNull = GeneralUtils.orX(GeneralUtils.isInstanceOfX(condition, new ClassNode(Truth.class)), GeneralUtils.equalsNullX(condition));
                            
                            truthOrNull = GeneralUtils.orX(truthOrNull, GeneralUtils.isInstanceOfX(condition, new ClassNode(Boolean.class)));
//                            new IfStatement(booleanExpression, ifBlock, elseBlock)
                            
                            
                            targetStatement = new IfStatement(new BooleanExpression(truthOrNull), ss, valueStatement);
                            
                            visitBlockStatement(valueStatement);
                            
//                        } else {
//                            
//                            targetStatement = ss;
//                            
//                        }
                        
                        blockStatement.getStatements().set(currentIndex, targetStatement);
                        
                    }
                    
                } else if(exp instanceof BinaryExpression) {
                    
                    BinaryExpression be = (BinaryExpression) exp;
                    
                    try {
                        
                        if( ! be.getOperation().getText().equals("=") ) throw new RuntimeException("Not an assignment");
                        
                        Expression rightExpression = be.getRightExpression();
                        
                        if(!(rightExpression instanceof MethodCallExpression)) throw new RuntimeException("Not a method call!");
                        
                        ArgumentListExpression ale = null;
                        
                        /*
                        if(rightExpression instanceof ConstructorCallExpression) {
                            
                            ConstructorCallExpression cce = (ConstructorCallExpression) rightExpression;
                            
                            Expression args = cce.getArguments();
                            
                            if(!(args instanceof ArgumentListExpression)) throw new RuntimeException("Expected args list");
                            
                            ale = (ArgumentListExpression) args;
                            
                        } else*/ if(rightExpression instanceof MethodCallExpression) {
                            
                            MethodCallExpression mce = (MethodCallExpression) rightExpression;
                            
                            if(!mce.getMethodAsString().equals("assign")) throw new RuntimeException("Not assign method");
                            
                            Expression args = mce.getArguments();
                            
                            if(!(args instanceof ArgumentListExpression)) throw new RuntimeException("Expected args list");
                            
                            ale = (ArgumentListExpression) args;
                            
                        }
                        
                        
                        
                        List<Expression> exps = ale.getExpressions();
                        if(exps.size() < 1  || exps.size() > 3) throw new RuntimeException("Expected  1 - 3 arguments");
                        
                        Expression assignmentExpression = null;
                        ClosureExpression unknownExpression = null;
                        ClosureExpression muExpression = null;
                        
                        assignmentExpression = exps.get(0);
                        
                        assignmentExpression = ale.getExpression(0);
                        
                        if(exps.size() > 1) {
                            
                            Expression un = exps.get(1);
                            if(un instanceof ConstantExpression) {
                                ConstantExpression ce = (ConstantExpression) un;
                            } else if(!(un instanceof ClosureExpression)) throw new RuntimeException("arg#1 must be a closure expression");
                            
                            
                            unknownExpression = (ClosureExpression) un;
                            
                            
                            
                            if(!(unknownExpression.getCode() instanceof BlockStatement)) throw new RuntimeException("closure #1 code must be a block");
                            
                            
                        }
                        
                        if(exps.size() > 2) {
                            
                            Expression mu = exps.get(2);
                            if(!(mu instanceof ClosureExpression)) throw new RuntimeException("arg#2 must be a closure expression");
                            
                            muExpression = (ClosureExpression) mu;
                            
                            if(!(muExpression.getCode() instanceof BlockStatement)) throw new RuntimeException("closure #2 code must be a block");
                            
                        }
                        
                        SwitchStatement ss = new SwitchStatement(assignmentExpression);
                        
                        BlockStatement unknownBlock = new BlockStatement();
                        
                        boolean visitUnknown = false;
                        
                        if(unknownExpression != null) {
                            
                            visitUnknown = true;
                            
                            MethodCallExpression callUnknown = new MethodCallExpression(unknownExpression, "call", new ArgumentListExpression());
                            
                            BinaryExpression newBe = new BinaryExpression(be.getLeftExpression(), GeneralUtils.ASSIGN, callUnknown);
                            
//                            unknownBlock = (BlockStatement) unknownExpression.getCode();
                            unknownBlock.addStatement(new ExpressionStatement(newBe));
                        } else {
//                            unknownBlock = new BlockStatement();
//                            StaticMethodCallExpression nexp = new StaticMethodCallExpression(new ClassNode(VitalSignsTransformation.class), "defaultUnknownAssign", new ArgumentListExpression());
//                            unknownBlock.addStatement(new ExpressionStatement(nexp));
                            BinaryExpression newBe = new BinaryExpression(be.getLeftExpression(), GeneralUtils.ASSIGN, GeneralUtils.constX(null));
                            unknownBlock.addStatement(new ExpressionStatement(newBe));
                        }
                        unknownBlock.addStatement(new BreakStatement());
                        CaseStatement unknownCase = new CaseStatement(GeneralUtils.fieldX(new ClassNode(Truth.class), Truth.UNKNOWN.name()), unknownBlock);
                        ss.addCase(unknownCase);

                        
                        BlockStatement muBlock = new BlockStatement();
                        
                        boolean visitMU = false;
                        
                        if(muExpression != null) {
                            
                            visitMU = true;
                            
                            MethodCallExpression callMu = new MethodCallExpression(muExpression, "call", new ArgumentListExpression());

                            BinaryExpression newBe = new BinaryExpression(be.getLeftExpression(), GeneralUtils.ASSIGN, callMu);
                            
//                            muBlock = (BlockStatement) muExpression.getCode();
                            muBlock.addStatement(new ExpressionStatement(newBe));
                            
                        } else {
//                            muBlock = new BlockStatement();
//                            StaticMethodCallExpression nexp = new StaticMethodCallExpression(new ClassNode(VitalSignsTransformation.class), "defaultMuAssign", new ArgumentListExpression());
//                            muBlock.addStatement(new ExpressionStatement(nexp));
//                            by defe
                            
                            BinaryExpression newBe = new BinaryExpression(be.getLeftExpression(), GeneralUtils.ASSIGN, GeneralUtils.constX(null));
                            muBlock.addStatement(new ExpressionStatement(newBe));
                            
                        }

                        
                        muBlock.addStatement(new BreakStatement());
                        CaseStatement muCase = new CaseStatement(GeneralUtils.fieldX(new ClassNode(Truth.class), Truth.MU.name()), muBlock);
                        ss.addCase(muCase);
                        
                        //default
                        //create new binary expression
                        BinaryExpression newBe = new BinaryExpression(be.getLeftExpression(), GeneralUtils.ASSIGN, assignmentExpression);
                        ss.setDefaultStatement(new ExpressionStatement(newBe));
                        
                        if(currentExpressionStmt == null) throw new RuntimeException("No current method expression");
                        
                        if(currentExpressionStmt.getExpression() != be ) throw new RuntimeException("non-aligned method expression");
                        
                        if(this.blockContext.size() < 1) throw new RuntimeException("block context is empty!");
                        
                        BlockStatement blockStatement = this.blockContext.get(this.blockContext.size() - 1 );
                        
                        int currentIndex = blockStatement.getStatements().indexOf(currentExpressionStmt);
                        
                        if(currentIndex < 0) throw new RuntimeException("current statement index must be >= 0");
                        
                        blockStatement.getStatements().set(currentIndex, ss);
                        
                        if(visitUnknown) {
                            visitBlockStatement((BlockStatement) unknownExpression.getCode());
                        }
                        
                        if(visitMU) {
                            visitBlockStatement((BlockStatement) muExpression.getCode());
                        }
                        
                    } catch(RuntimeException e) {
                        
                        Expression val = super.transform(exp);
                        
                        return val;
                        
                    }
                    
                    

                    
                    
                    //convert that into a switch
                    
                    
                }
                
                Expression val = super.transform(exp);
                
                return val;
            }
            
            @Override
            public void visitBlockStatement(BlockStatement block) {
                
                this.blockContext.add(block);
                
//                System.out.println("BLOCK: " + block);
                
                super.visitBlockStatement(block);
                
                this.blockContext.remove(block);
                
            }

//            @Override
//            public void visitMethod(MethodNode node) {
//                System.out.println("METHOD NODE: " + node);
//                //XXX fix for method body blocks!
//                if( node.getCode() instanceof BlockStatement ) {
//                    visitBlockStatement((BlockStatement) node.getCode());
//                } else {
//                    super.visitMethod(node);
//                }
//                
//            }
            
            @Override
            public void visitExpressionStatement(ExpressionStatement es) {
                
                this.currentExpressionStmt = es;
                
                try {
                    
                    BlockStatement currentBlockStmt = blockContext.size() > 0 ? blockContext.get(blockContext.size() - 1) : null;
                    
                    if(currentBlockStmt == null) throw new RuntimeException("No current block for stmt: " + es);
                    
                    boolean ok = false;
                    
                    for(Statement s : currentBlockStmt.getStatements()) {
                        
                        if(s == es) {
                            ok = true;
                        }
                        
                    }
                    
                    if(!ok) throw new RuntimeException("expression statement block parent not found: " + es);
                    
                } catch(Exception e) {
                    this.currentExpressionStmt = null;
                }
                
                super.visitExpressionStatement(es);
            }


        };
        
        ClassCodeExpressionTransformer trn = new ClassCodeExpressionTransformer() {

            @Override
            protected SourceUnit getSourceUnit() {
                return sourceUnit;
            } 
            
            @Override
            public void visitClosureExpression(ClosureExpression expression) {
                super.visitClosureExpression(expression);
                Statement code = expression.getCode();
                if(code instanceof BlockStatement) {
                    visitBlockStatement((BlockStatement) code);
                }
            }
        
            
            @Override
            public Expression transform(Expression exp) {
                
//                System.out.println("EXPRESSION: " + exp);
                
                if(exp instanceof ClosureExpression) {
                    
                    //this visits closure body
                    ClosureExpression ce = (ClosureExpression) exp;
                    
                    Statement code = ce.getCode();
                    if(code instanceof BlockStatement) {
                        visitBlockStatement((BlockStatement) code);
                    }
//                    ce.visit(this);
                    
                    return ce;
                    
                } else if(exp instanceof BinaryExpression) {
                    
                    BinaryExpression be = (BinaryExpression) exp;
                    
                    Token operation = be.getOperation();
                    
                    String operator = operation.getText();
                    
                    if(false && operator.equals("^=")) {
                        
                        Expression left = be.getLeftExpression();
                        Expression right = be.getRightExpression();
                        
                        if( ( left instanceof VariableExpression || left instanceof PropertyExpression || left instanceof ConstantExpression) && ( right instanceof VariableExpression || right instanceof PropertyExpression || right instanceof ConstantExpression)) {
                            
//                            return new MethodCallExpression(
////                              new VariableExpression("this"),
//                              new ConstantExpression("ai.vital.vitalsigns.groovy.VitalSignsBXOR_ASSIGNHandler.handleBXOR_ASSIGN"),
//                              new ArgumentListExpression(
//                                leftVar,
//                                rightVar
//                              )
//                            );
                            StaticMethodCallExpression nexp = new StaticMethodCallExpression(new ClassNode(VitalSignsBXOR_ASSIGNHandler.class), "handleBXOR_ASSIGN", new ArgumentListExpression(left, right));
                            return nexp;
                            
                        }
                        
                    } else if(operator.equals("&&") || operator.equals("||")) {
                        
                        Expression left = be.getLeftExpression();
                        Expression right = be.getRightExpression();
                        
                        if( ( left instanceof VariableExpression || left instanceof PropertyExpression || left instanceof ConstantExpression) && ( right instanceof VariableExpression || right instanceof PropertyExpression || right instanceof ConstantExpression)) {
                        
                            String suffix = operator.equals("&&") ? "AND" : "OR";
                            
                            StaticMethodCallExpression nexp = new StaticMethodCallExpression(new ClassNode(VitalSignsTruthBooleanOperatorsHandler.class), "handleBoolean" + suffix, new ArgumentListExpression(left, right));
                            return nexp;
                            
                        }
                        
                    } else if(operator.equals("<") || operator.equals(">") || operator.equals(">=") || operator.equals("<=") 
                            || operator.equals("<<") || operator.equals(">>") || operator.equals("<<=") || operator.equals(">>=")
                            
                            || operator.equals("==") || operator.equals("^=")
                            
                            ) {
                        
                        Expression left = be.getLeftExpression();
                        Expression right = be.getRightExpression();
                        
                        StaticMethodCallExpression nexpr = new StaticMethodCallExpression(new ClassNode(VitalSignsComparisonHandler.class), "handleComparison", new ArgumentListExpression(Arrays.asList(left, right, GeneralUtils.constX(operator))));
                        return nexpr;
                        
                    }
                    
                } else if(exp instanceof NotExpression) {
                    
                    NotExpression notExpression = (NotExpression) exp;
                    
                    StaticMethodCallExpression nexpr = new StaticMethodCallExpression(new ClassNode(VitalSignsTruthBooleanOperatorsHandler.class), "handleNegation", new ArgumentListExpression(notExpression.getExpression()));
                    
                    return nexpr;
                    
                }
                
                return super.transform(exp);
            }
            
        };

        classNode.addFieldFirst("YES", ClassNode.ACC_FINAL | ClassNode.ACC_PUBLIC | ClassNode.ACC_STATIC, new ClassNode(Truth.class), GeneralUtils.fieldX(new ClassNode(Truth.class), "YES"));
        classNode.addFieldFirst("NO", ClassNode.ACC_FINAL | ClassNode.ACC_PUBLIC | ClassNode.ACC_STATIC, new ClassNode(Truth.class), GeneralUtils.fieldX(new ClassNode(Truth.class), "NO"));
        classNode.addFieldFirst("UNKNOWN", ClassNode.ACC_FINAL | ClassNode.ACC_PUBLIC | ClassNode.ACC_STATIC, new ClassNode(Truth.class), GeneralUtils.fieldX(new ClassNode(Truth.class), "UNKNOWN"));
        classNode.addFieldFirst("MU", ClassNode.ACC_FINAL | ClassNode.ACC_PUBLIC | ClassNode.ACC_STATIC, new ClassNode(Truth.class), GeneralUtils.fieldX(new ClassNode(Truth.class), "MU"));
        
        
        directAccessTransformer.visitClass(classNode);
        considerAssignTranformer.visitClass(classNode);
        trn.visitClass(classNode);
        
//        trn.visitMethod((MethodNode) nodes[1]);
        
    }

    public static void defaultUnknown() {
        throw new RuntimeException("default consider case UKNOWN handler must be overridden");
    }
    
    public static void defaultMu() {
        throw new RuntimeException("default consider case MU handler must be overridden");
    }
    
    public static void defaultUnknownAssign() {
        throw new RuntimeException("default assign UNKNOWN handler must be overridden");
    }
    
    public static void defaultMuAssign() {
        throw new RuntimeException("default assign UNKNOWN handler must be overridden");
    }
    
    public final static Object NULL = null;
    
}
