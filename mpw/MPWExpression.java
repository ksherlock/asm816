/*
 * Created on Dec 5, 2006
 * Dec 5, 2006 10:23:39 PM
 */
package mpw;

import java.util.ArrayList;

import omf.OMF_Expression;

import expression.*;

import asm816.AsmException;
import asm816.Token;
import asm816.__TokenIterator;


/*
 * MPW has 8 levels of precedence:
 * 
 * lowest to highest:
 * 
 * 1) Logical or, Logical eor
 * 2) Logical and
 * 3) comparison (eq, ne, lt, gt, ge, le)
 * 4) shift left, shift right
 * 5) addition, subtraction
 * 6) multiplication, division, modulo
 * 7) ones complement, logical not, unary negation
 * 8) parenthesis
 */


@SuppressWarnings("unchecked")
public class MPWExpression
{
    private int fPC;
    private ArrayList fData;
 
    private MPWExpression(int pc)
    {
        fPC = pc;
        fData = new ArrayList();
    }    
    
    public static ComplexExpression Parse(__TokenIterator ti, int pc) throws AsmException
    {
        MPWExpression ep = new MPWExpression(pc);
        return ep.ParseExpression(ti);       
    }
    
    private ComplexExpression ParseExpression(__TokenIterator ti) throws AsmException
    {        
        Expression1(ti);
        
        return new ComplexExpression(fData);
    }
    
    private void Expression1(__TokenIterator ti) throws AsmException
    {
        Token t;
        String s;
        int op;
        
        Expression2(ti);
        for(;;)
        {
            t = ti.Peek();
            op = -1;
            switch(t.Type())
            {
            case Token.LOGICAL_OR:
                op = OMF_Expression.EXPR_LOR;
                break;
            case Token.LOGICAL_EOR:
                op = OMF_Expression.EXPR_LEOR;
                break;
            case Token.SYMBOL:
                s = t.toString().toUpperCase();
                if (s.equals("OR"))
                    op = OMF_Expression.EXPR_LOR;
                else if (s.equals("XOR"))
                    op = OMF_Expression.EXPR_LEOR;                
            }
            
            if (op == -1) break;
            
            ti.Next();
            Expression2(ti);
            
            fData.add(new Integer(op));
        }        
        
    }
    
    private void Expression2(__TokenIterator ti) throws AsmException
    {
        Token t;
        String s;
        int op;
        
        Expression3(ti);
        
        for(;;)
        {
            t = ti.Peek();
            op = -1;
            switch(t.Type())
            {
            case Token.LOGICAL_AND:
                op = OMF_Expression.EXPR_LAND;
                    break;
            case Token.SYMBOL:
                s = t.toString().toUpperCase();
                if (s.equals("AND"))
                    op = OMF_Expression.EXPR_LAND;
            }
            
            if (op == -1) break;
            
            ti.Next();
            Expression3(ti);
            
            fData.add(new Integer(op));
        }
    }


    private void Expression3(__TokenIterator ti) throws AsmException
    {
        Token t;
        int op;  
        
        Expression4(ti);
        
        for(;;)
        {
            t = ti.Peek();
            op = -1;
            
            switch(t.Type())
            {
            case Token.EQ:
                op = OMF_Expression.EXPR_EQ;
                break;
            case Token.NE:
                op = OMF_Expression.EXPR_NE;
                break;
            case Token.LT:
                op = OMF_Expression.EXPR_LT;
                break;
            case Token.GT:
                op = OMF_Expression.EXPR_GT;
                break;
            case Token.LE:
                op = OMF_Expression.EXPR_LE;
                break;
            case Token.GE:
                op = OMF_Expression.EXPR_GE;
                break;
            default: op = -1;              
            }
            
            if (op == -1) break;
            
            ti.Next();
            Expression4(ti);

            fData.add(new Integer(op));
        }
    }
    
    private void Expression4(__TokenIterator ti) throws AsmException
    {
        Token t;
        int op;   
     
        Expression5(ti);
        
        for (;;)
        {
            op = -1;
            boolean uminus = false;
            t = ti.Peek();
            switch(t.Type())
            {
            case Token.LEFT_SHIFT:
                op = OMF_Expression.EXPR_SHIFT;
                uminus = false;
                break;
            case Token.RIGHT_SHIFT:
                op =OMF_Expression.EXPR_SHIFT;
                uminus = true;
                break;
            default:
                op = -1;
            }
            if (op == -1) break;
            
            ti.Next();
            Expression5(ti);
            
            if (uminus)
                fData.add(new Integer(OMF_Expression.EXPR_NEG));
            
            fData.add(new Integer(op));                        
        }        
        
    }
    
    private void Expression5(__TokenIterator ti) throws AsmException
    {
        Token t;
        int op;
        
        Expression6(ti);
        
        for (;;)
        {
            op = -1;
            
            t = ti.Peek();
            switch(t.Type())
            {
            case '+':
                op = OMF_Expression.EXPR_ADD;
                break;
            case '-':
                op =OMF_Expression.EXPR_SUB;
                break;
            default:
                op = -1;
            }
            if (op == -1) break;
            
            ti.Next();
            Expression6(ti);
            
            fData.add(new Integer(op));                        
        }
    }
    
    private void Expression6(__TokenIterator ti) throws AsmException
    {
        Token t;
        String s;
        int op;
        
        Expression7(ti);
        
        for(;;)
        {
            op = -1;
            t = ti.Peek();
            switch(t.Type())
            {
            case Token.MOD:
                op = OMF_Expression.EXPR_MOD;
                break;
            case '*':
                op = OMF_Expression.EXPR_MUL;
                break;
            case '/':
                op = OMF_Expression.EXPR_DIV;
                break;
            case Token.SYMBOL:
                s = t.toString().toUpperCase();
                if (s.equals("DIV"))
                    op = OMF_Expression.EXPR_DIV;
                else if (s.equals("MOD"))
                    op = OMF_Expression.EXPR_MOD;
                break;
            default:
                op = -1;
            }
            
            if (op == -1) break;
            
            ti.Next();
            Expression7(ti);
            fData.add(new Integer(op));
            
        }
        
    }
    
    /*
     * this is actually incorrect... - - will not be handled 
     * correctly. (though that's a silly thing to do :)
     */
    private void Expression7(__TokenIterator ti) throws AsmException
    {
        Token t;
        String s;
        int op = -1;
        
        t = ti.Peek();
        switch (t.Type())
        {
        // unary plus - ignored.
        case '+':
            ti.Next();
            break;
        case '-':
            op = OMF_Expression.EXPR_NEG;
            break;
        case '~':
            op = OMF_Expression.EXPR_BNOT;
            break;
        case Token.SYMBOL:
            s = t.toString().toUpperCase();
            if (s.equals("NOT"))
                op = OMF_Expression.EXPR_LNOT;
            break;
        default: 
            op = -1;    
        }
        
        if (op != -1)
             ti.Next();

        Expression8(ti);
        
        if (op != -1)
            fData.add(new Integer(op));           
    }
    
    private void Expression8(__TokenIterator ti) throws AsmException
    {
        Token t;
        String s;

        
        t = ti.Expect('(', '*', Token.SYMBOL, Token.STRING, Token.NUMBER);

        switch(t.Type())
        {
        case '(':
            Expression1(ti);
            ti.Expect((int)')');
            break;
        case '*':
            fData.add(new RelExpression(fPC));
            break;
        case Token.STRING: 
            /*
             * integer constant.. maybe we should check if
             * 1,2, or 4 chars (?)
             */
        case Token.NUMBER:
            fData.add(new ConstExpression(t.Value()));
            break;
        case Token.SYMBOL:
            s = t.toString();
            fData.add(new SymbolExpression(s));
            break; 
        }        
    }
    
}
