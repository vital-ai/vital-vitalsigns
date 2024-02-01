package ai.vital.vitalsigns.ast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.AssertStatement;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.BreakStatement;
import org.codehaus.groovy.ast.stmt.CaseStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ContinueStatement;
import org.codehaus.groovy.ast.stmt.DoWhileStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.stmt.SynchronizedStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.classgen.BytecodeExpression;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.transform.sc.ListOfExpressionsExpression;
import org.codehaus.groovy.transform.sc.TemporaryVariableExpression;
import org.codehaus.groovy.transform.sc.transformers.CompareIdentityExpression;
import org.codehaus.groovy.transform.sc.transformers.CompareToNullExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroovyBuilderCodeVisitor implements GroovyCodeVisitor {

    private final static Logger log = LoggerFactory.getLogger(GroovyBuilderCodeVisitor.class);
    
    private Set<String> types = new HashSet<String>();
    
    private final static Set<String> blacklist = new HashSet<String>(Arrays.asList(Object.class.getCanonicalName(), "int", "long", "boolean", "float", "double"));
    
    public GroovyBuilderCodeVisitor() {
    }

    private void handleType(ClassNode cn) {
        
        log.debug("Handling classnode name: {}, non-package name: {}, package name: {}, unresolved name: {}",
                new Object[]{cn.getName(), cn.getNameWithoutPackage(), cn.getPackageName(), cn.getUnresolvedName()}
                );
        
        String name = cn.getName();
        
        if(blacklist.contains(name)) return;
        
        types.add(name);
        
    }
    
    public Set<String> getTypes() {
        return types;
    }

    private void handleNode(ASTNode node) {

        if(node == null) return;
        
        if(node instanceof Statement) {
            handleStatement((Statement) node);
        } else if(node instanceof Expression) {
            handleExpression((Expression) node);
        } else if(node instanceof Parameter) {
            Parameter p = (Parameter) node;
            handleType(p.getOriginType());
            handleType(p.getType());
        } else {
            throw new RuntimeException("Unhandled node type: " + node);
        }
        
    }
    
    private void handleStatement(Statement stmt) {
        
        log.debug("Handling statement {}", stmt);
        
        if(stmt == null) return;
        
        if(stmt instanceof AssertStatement) {
            
            AssertStatement as = (AssertStatement) stmt;
            
            visitAssertStatement((AssertStatement) stmt);
            
            handleExpression(as.getBooleanExpression());
            
            handleExpression(as.getMessageExpression());
            
            
        } else if(stmt instanceof BlockStatement) {
            
            BlockStatement bs = (BlockStatement) stmt;
            
            for(Statement st : bs.getStatements()) {
                handleStatement(st);
            }
            
        } else if(stmt instanceof BreakStatement) {
            
//            BreakStatement bs = (BreakStatement) stmt;

        } else if(stmt instanceof BytecodeSequence) {
            
//            BytecodeSequence bseq = (BytecodeSequence) stmt;
            
        } else if(stmt instanceof CaseStatement) {
            
            CaseStatement cs = (CaseStatement) stmt;
            
            handleExpression(cs.getExpression());
            
        } else if(stmt instanceof CatchStatement) {
            
            CatchStatement cs = (CatchStatement) stmt;
            
            handleStatement(cs.getCode());
            
            
        } else if(stmt instanceof ContinueStatement) {
            
//            ContinueStatement cs = (ContinueStatement) stmt;
            
        } else if(stmt instanceof DoWhileStatement) {
            
            DoWhileStatement dws = (DoWhileStatement) stmt;
            
            handleExpression(dws.getBooleanExpression());
            
            handleStatement(dws.getLoopBlock());
            
        } else if(stmt instanceof EmptyStatement) {

//            EmptyStatement es = (EmptyStatement) stmt;
            
        } else if(stmt instanceof ExpressionStatement) {
            
            ExpressionStatement es = (ExpressionStatement) stmt;
            
            handleExpression(es.getExpression());
            
        } else if(stmt instanceof ForStatement) {
            
            ForStatement fs = (ForStatement) stmt;
            
            handleExpression(fs.getCollectionExpression());
            
            handleStatement(fs.getLoopBlock());
            
        } else if(stmt instanceof IfStatement) {
            
            IfStatement is = (IfStatement) stmt;
            
            handleExpression(is.getBooleanExpression());
            
            handleStatement(is.getIfBlock());
            
            handleStatement(is.getElseBlock());
            
        } else if(stmt instanceof ReturnStatement) {
            
            ReturnStatement rs = (ReturnStatement) stmt;
            
            handleExpression(rs.getExpression());
            
        } else if(stmt instanceof SwitchStatement) {
            
            SwitchStatement ss = (SwitchStatement) stmt;
            
            handleExpression(ss.getExpression());
            
            for(Statement s : ss.getCaseStatements()) {
                
                handleStatement(s);
                
            }
            
        } else if(stmt instanceof SynchronizedStatement) {
            
            SynchronizedStatement ss = (SynchronizedStatement) stmt;
            
            handleExpression(ss.getExpression());
            
            handleStatement(ss.getCode());
            
        } else if(stmt instanceof ThrowStatement) {
            
            ThrowStatement ts = (ThrowStatement) stmt;
            
            handleExpression(ts.getExpression());
            
        } else if(stmt instanceof TryCatchStatement) {
            
            TryCatchStatement tcs = (TryCatchStatement) stmt;
            
            handleStatement(tcs.getTryStatement());
            
            for(Statement s : tcs.getCatchStatements()) {
                handleStatement(s);
            }
            
            handleStatement(tcs.getFinallyStatement());
            
        } else if(stmt instanceof WhileStatement) {
            
            WhileStatement ws = (WhileStatement) stmt;
            
            handleExpression(ws.getBooleanExpression());
            
            handleStatement(ws.getLoopBlock());
            
        } else {
            throw new RuntimeException("Unhandled statement type: " + (stmt != null ? stmt.getClass().getCanonicalName() : "(null)"));
        }
        
    }
    
    private void handleExpression(Expression ex) {

        log.debug("Handling expression {}", ex);
        
        if(ex == null) return;

        handleType( ex.getType() );
        
        if(ex instanceof ArrayExpression) {
            
            ArrayExpression ae = (ArrayExpression) ex;

            for(Expression e : ae.getExpressions()) {
                handleExpression(e);
            }
            
        } else if(ex instanceof BinaryExpression) {
            
            BinaryExpression be = (BinaryExpression) ex;
            
            handleExpression(be.getLeftExpression());
            
            handleExpression(be.getRightExpression());

            if(ex instanceof CompareIdentityExpression) {

//                CompareIdentityExpression cie = (CompareIdentityExpression) ex;
                
            } else if(ex instanceof CompareToNullExpression) {
                
//                CompareToNullExpression ctne = (CompareToNullExpression) ex;
                
            } else if(ex instanceof DeclarationExpression) {
                
                DeclarationExpression de = (DeclarationExpression) ex;
                
                handleExpression(de.getVariableExpression());
                
                try {
                    handleExpression(de.getTupleExpression());
                } catch(ClassCastException cce) {}
                
//            } else {
//                throw new RuntimeException("Unhandled binary expression " + ex);
            }
            
        } else if(ex instanceof BitwiseNegationExpression) {
            
            BitwiseNegationExpression bne = (BitwiseNegationExpression) ex;
            
            handleExpression(bne.getExpression());
            
        } else if(ex instanceof BooleanExpression) {
            
            BooleanExpression be = (BooleanExpression) ex;
            
            if(ex instanceof NotExpression) {

//                NotExpression ne = (NotExpression) ex;
                
            }
            
            handleExpression(be.getExpression());
            
        } else if(ex instanceof BytecodeExpression) {
            
//            BytecodeExpression be = ex;
            throw new RuntimeException("Not supported: " + ex);
            
            
        } else if(ex instanceof CastExpression) {
            
            CastExpression ce = (CastExpression) ex;
            
            handleExpression(ce.getExpression());
            
        } else if(ex instanceof ClassExpression) {
            
//            ClassExpression ce = (ClassExpression) ex;
            
        } else if(ex instanceof ClosureExpression) {
            
            ClosureExpression ce = (ClosureExpression) ex;
            
            handleStatement(ce.getCode());
            
            for(Parameter p : ce.getParameters()) {
                handleNode(p);
            }
            
        } else if(ex instanceof ConstantExpression) {
            
//            if(ex instanceof AnnotationConstantExpression) {
//                
//            }
            
//            ConstantExpression ce = (ConstantExpression) ex;
            
            
        } else if(ex instanceof ConstructorCallExpression) {
            
            ConstructorCallExpression cce = (ConstructorCallExpression) ex;
            
            handleExpression(cce.getArguments());
            
            handleNode(cce.getReceiver());
            
        } else if(ex instanceof EmptyExpression) {
            
//            EmptyExpression ee = (EmptyExpression) ex;
            
        } else if(ex instanceof FieldExpression) {
            
            FieldExpression fe = (FieldExpression) ex;
           
            handleNode(fe.getField());
            
            
        } else if(ex instanceof GStringExpression) {
            
//            GStringExpression gse = (GStringExpression) ex;
            
            
            
        } else if(ex instanceof ListExpression) {
            
            ListExpression le = (ListExpression) ex;
            
            for(Expression e : le.getExpressions()) {
                
                handleExpression(e);
                
            }
            
//            if(le instanceof ClosureListExpression) {
//                
//            }
            
        } else if(ex instanceof ListOfExpressionsExpression) {
            
//            ListOfExpressionsExpression loee = (ListOfExpressionsExpression) ex;
            
        } else if(ex instanceof MapEntryExpression) {
            
            MapEntryExpression mee = (MapEntryExpression) ex;
            
            handleExpression(mee.getKeyExpression());
            
            handleExpression(mee.getValueExpression());
            
        } else if(ex instanceof MapExpression) {
        
            MapExpression me = (MapExpression) ex;
            
            if(me instanceof NamedArgumentListExpression) {
                
            }
            
            for(MapEntryExpression mep : me.getMapEntryExpressions()) {
                
                handleExpression(mep);
                
            }
            
            
        } else if(ex instanceof MethodCallExpression) {
            
            MethodCallExpression mce = (MethodCallExpression) ex;
            
            handleExpression(mce.getArguments());
            handleExpression(mce.getMethod());
            handleExpression(mce.getObjectExpression());
            
            handleNode(mce.getMethodTarget());
            
            handleNode(mce.getReceiver());
            
        } else if(ex instanceof MethodPointerExpression) {
            
            MethodPointerExpression mpe = (MethodPointerExpression) ex;
            
            handleExpression(mpe.getExpression());
            handleExpression(mpe.getMethodName());
            
        } else if(ex instanceof PostfixExpression) {
            
            PostfixExpression pe = (PostfixExpression) ex;
            
            handleExpression(pe.getExpression());
            
        } else if(ex instanceof PrefixExpression) {
            
            PrefixExpression pe = (PrefixExpression) ex;
            
            handleExpression(pe.getExpression());
            
        } else if(ex instanceof PropertyExpression) {
            
            PropertyExpression pe = (PropertyExpression) ex;
            
            handleExpression(pe.getObjectExpression());
            
            handleExpression(pe.getProperty());
            
            if(ex instanceof AttributeExpression) {
                
            }
            
        } else if(ex instanceof RangeExpression) {
            
            RangeExpression re = (RangeExpression) ex;
         
            handleExpression(re.getFrom());
            handleExpression(re.getTo());
            
        } else if(ex instanceof SpreadExpression) {
            
            SpreadExpression se = (SpreadExpression) ex;
            
            handleExpression(se.getExpression());
            
        } else if(ex instanceof SpreadMapExpression) {
            
            SpreadMapExpression sme = (SpreadMapExpression) ex;
            
            handleExpression(sme.getExpression());
            
        } else if(ex instanceof StaticMethodCallExpression) {
            
            StaticMethodCallExpression smce = (StaticMethodCallExpression) ex;
            
            handleExpression(smce.getArguments());
            handleNode(smce.getReceiver());
            
        } else if(ex instanceof TemporaryVariableExpression) {
            
//            TemporaryVariableExpression tve = (TemporaryVariableExpression) ex;
            
        } else if(ex instanceof TernaryExpression) {
            
            TernaryExpression te = (TernaryExpression) ex;
         
            handleExpression(te.getBooleanExpression());
            handleExpression(te.getFalseExpression());
            handleExpression(te.getTrueExpression());
            
            if(te instanceof ElvisOperatorExpression) {
                
            }
            
        } else if(ex instanceof TupleExpression) {
            
            TupleExpression te = (TupleExpression) ex;
            
            for(Expression e : te.getExpressions()) {
                handleExpression(e);
            }
            
            if(ex instanceof ArgumentListExpression) {
                
            }
            
        } else if(ex instanceof UnaryMinusExpression) {
            
            UnaryMinusExpression ume = (UnaryMinusExpression) ex;
            
            handleExpression(ume.getExpression());
            
        } else if(ex instanceof UnaryPlusExpression) {

            UnaryPlusExpression upe = (UnaryPlusExpression) ex;
            handleExpression(upe.getExpression());
            
        } else if(ex instanceof VariableExpression) {
            
            VariableExpression ve = (VariableExpression) ex;
            
            handleExpression(ve.getInitialExpression());
            
        } else {
            
            throw new RuntimeException("Unhandled expression type: " + ex);
            
        }
        
    }
    
    @Override
    public void visitArgumentlistExpression(ArgumentListExpression ale) {
        
        log.debug("Visiting argument list expression {}", ale);
        
        handleExpression(ale);
        
    }

    @Override
    public void visitArrayExpression(ArrayExpression ae) {
        
        log.debug("Visiting array expression {}", ae);
        
        handleExpression(ae);
    }

    @Override
    public void visitAssertStatement(AssertStatement arg0) {
        
        log.debug("Visiting assert statement {}", arg0);
        
        handleStatement(arg0);
        
    }

    @Override
    public void visitAttributeExpression(AttributeExpression arg0) {

        log.debug("Visiting attribute expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitBinaryExpression(BinaryExpression arg0) {

        log.debug("Visiting binary expression  {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitBitwiseNegationExpression(BitwiseNegationExpression arg0) {
        
        log.debug("Visiting bitwise negation expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitBlockStatement(BlockStatement arg0) {
        
        log.debug("Visiting block statement {}", arg0);

        handleStatement(arg0);
        
    }

    @Override
    public void visitBooleanExpression(BooleanExpression arg0) {

        log.debug("Visiting boolean expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitBreakStatement(BreakStatement arg0) {
        
        log.debug("Visiting break statement {}", arg0);
        
        handleStatement(arg0);
        
    }

    @Override
    public void visitBytecodeExpression(BytecodeExpression arg0) {
        
        log.debug("Visiting bytecode expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitCaseStatement(CaseStatement arg0) {

        log.debug("Visiting case statement {}", arg0);
        
        handleStatement(arg0);
        
    }

    @Override
    public void visitCastExpression(CastExpression arg0) {

        log.debug("Visiting cast expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitCatchStatement(CatchStatement arg0) {
        
        log.debug("Visiting catch statement {}", arg0);
        
        handleStatement(arg0);
        
    }

    @Override
    public void visitClassExpression(ClassExpression arg0) {
        
        log.debug("Visiting class expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitClosureExpression(ClosureExpression arg0) {
        
        log.debug("Visiting closure expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitLambdaExpression(LambdaExpression lambdaExpression) {

    }

    @Override
    public void visitClosureListExpression(ClosureListExpression arg0) {
        
        log.debug("Visiting closure list expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitConstantExpression(ConstantExpression arg0) {
        
        log.debug("Visiting constant expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitConstructorCallExpression(ConstructorCallExpression arg0) {
        
        log.debug("Visiting constructor call expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitContinueStatement(ContinueStatement arg0) {
        
        log.debug("Visiting continue statement {}", arg0);
        
        handleStatement(arg0);
        
    }
    

    @Override
    public void visitDeclarationExpression(DeclarationExpression arg0) {
        
        log.debug("Visiting declaration expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitDoWhileLoop(DoWhileStatement arg0) {

        log.debug("Visiting do while loop {}", arg0);
        
        handleStatement(arg0);
        
    }

    @Override
    public void visitExpressionStatement(ExpressionStatement arg0) {
        
        log.debug("Visiting expression statement {}", arg0);
     
        handleStatement(arg0);
        
    }



    @Override
    public void visitFieldExpression(FieldExpression arg0) {
        
        log.debug("Visiting field expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitForLoop(ForStatement arg0) {
     
        log.debug("Visiting for loop {}", arg0);
        
        handleStatement(arg0);
        
    }

    @Override
    public void visitGStringExpression(GStringExpression arg0) {
        
        log.debug("Visiting gstring expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitIfElse(IfStatement arg0) {
        
        log.debug("Visiting if else {}", arg0);
        
        handleStatement(arg0);
        
    }

    @Override
    public void visitListExpression(ListExpression arg0) {
        
        log.debug("Visiting list expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitMapEntryExpression(MapEntryExpression arg0) {
        
        log.debug("Visiting map entry expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitMapExpression(MapExpression arg0) {
        
        log.debug("Visiting map expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitMethodCallExpression(MethodCallExpression arg0) {
        
        log.debug("Visiting method call expression {}", arg0);
        
        handleExpression(arg0);        
        
    }

    @Override
    public void visitMethodPointerExpression(MethodPointerExpression arg0) {
        
        log.debug("Visiting method pointer expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitMethodReferenceExpression(MethodReferenceExpression methodReferenceExpression) {

    }

    @Override
    public void visitNotExpression(NotExpression arg0) {
        
        log.debug("Visiting not expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitPostfixExpression(PostfixExpression arg0) {
        
        log.debug("Visiting postfix expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitPrefixExpression(PrefixExpression arg0) {
        
        log.debug("Visiting prefix expression {}", arg0);
        
        handleExpression(arg0);        
        
    }

    @Override
    public void visitPropertyExpression(PropertyExpression arg0) {
        
        log.debug("Visiting property expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitRangeExpression(RangeExpression arg0) {
        
        log.debug("Visiting range expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitReturnStatement(ReturnStatement arg0) {
        
        log.debug("Visiting return statement {}", arg0);
        
        handleStatement(arg0);
        
    }

    @Override
    public void visitShortTernaryExpression(ElvisOperatorExpression arg0) {
        
        log.debug("Visiting short ternary expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitSpreadExpression(SpreadExpression arg0) {
        
        log.debug("Visiting spread expression {}", arg0);
        
        handleExpression(arg0);        
        
    }

    @Override
    public void visitSpreadMapExpression(SpreadMapExpression arg0) {
        
        log.debug("Visiting spread map expression {}", arg0);
        
        handleExpression(arg0);        
        
    }

    @Override
    public void visitStaticMethodCallExpression(StaticMethodCallExpression arg0) {
        
        log.debug("Visiting method call expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitSwitch(SwitchStatement arg0) {
        
        log.debug("Visiting switch {}", arg0);
        
        handleStatement(arg0);
        
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatement arg0) {
        
        log.debug("Visiting synchronized statement {}", arg0);
        
        handleStatement(arg0);
        
    }

    @Override
    public void visitTernaryExpression(TernaryExpression arg0) {
        
        log.debug("Visiting ternary expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitThrowStatement(ThrowStatement arg0) {
        
        log.debug("Visiting throw statement {}", arg0);
        
        handleStatement(arg0);
        
    }

    @Override
    public void visitTryCatchFinally(TryCatchStatement arg0) {
        
        log.debug("Visiting try catch finally {}", arg0);
    }

    @Override
    public void visitTupleExpression(TupleExpression arg0) {
        
        log.debug("Visiting tuple expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitUnaryMinusExpression(UnaryMinusExpression arg0) {
        
        log.debug("Visiting unary expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitUnaryPlusExpression(UnaryPlusExpression arg0) {
        
        log.debug("Visiting unary plus expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitVariableExpression(VariableExpression arg0) {
        
        log.debug("Visiting variable expression {}", arg0);
        
        handleExpression(arg0);
        
    }

    @Override
    public void visitWhileLoop(WhileStatement arg0) {
        
        log.debug("Visiting while loop {}", arg0);
        
        handleStatement(arg0);
        
    }

}
