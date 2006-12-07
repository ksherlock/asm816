package orca;

import java.util.ArrayList;

import omf.OMF_Expression;

import expression.*;

import asm816.AsmException;
import asm816.Token;
import asm816.__TokenIterator;

/*
 * Orca has 5 levels of precedence:
 *
 * 1) comparison
 * 2) + - logical OR, logical EOR
 * 3) * / logical AND, shift
 * 4) logical NOT, unary plus, unary minus
 * 5) ()
 * 
 */


@SuppressWarnings("unchecked")
public class OrcaExpression
{
    private int fPC;
    private ArrayList fData;
 
    private OrcaExpression(int pc)
    {
        fPC = pc;
        fData = new ArrayList();
    }    
    
    public static ComplexExpression Parse(__TokenIterator ti, int pc) throws AsmException
    {
        OrcaExpression ep = new OrcaExpression(pc);
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
        int op;  
        
        Expression2(ti);
        
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
            case Token.GE:
                op = OMF_Expression.EXPR_GE;
                break;
            case Token.LE:
                op = OMF_Expression.EXPR_LE;
                break;
            default: 
                op = -1;              
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
        int op;
        
        Expression3(ti);
        
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
                op = OMF_Expression.EXPR_SUB;
                break;
            case Token.LOGICAL_OR:
                op = OMF_Expression.EXPR_LOR;
                break;
            case Token.LOGICAL_EOR:
                op = OMF_Expression.EXPR_LEOR;
                break;
            default:
                op = -1;
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
            op = -1;
            t = ti.Peek();
            switch(t.Type())
            {
            case '*':
                op = OMF_Expression.EXPR_MUL;
                break;
            case '/':
                op = OMF_Expression.EXPR_DIV;
                break;
            case '|':
                op = OMF_Expression.EXPR_SHIFT;
                break;
            case Token.LOGICAL_AND:
                op = OMF_Expression.EXPR_LAND;
                break;
                
            default:
                op = -1;
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
            ti.Next();
            op = OMF_Expression.EXPR_NEG;
            break;
        case Token.LOGICAL_NOT:
            ti.Next();
            op = OMF_Expression.EXPR_LNOT;  
        }
        
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
        
        if (op != -1)
            fData.add(new Integer(op));           
    }        
}
