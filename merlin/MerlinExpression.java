/*
 * Created on Dec 5, 2006
 * Dec 5, 2006 10:23:39 PM
 */
package merlin;

import java.util.ArrayList;

import omf.OMF_Expression;

import expression.*;

import asm816.AsmException;
import asm816.Token;
import asm816.__TokenIterator;

/*
 * Merlin has 6 levels of precedence, when an expression is in
 * {} braces:
 *
 * 1) comparison: < = > #
 * 2) + -
 * 3) * /
 * 4) & . ! (binary and, or, eor)(1)
 * 5) unary minus
 * 6) {}
 * 
 * if an expression is not in {} braces, it is evaluated
 * left-to-right. Unless the PARM setting says to always
 * use algabreic evaluation.  
 */


@SuppressWarnings("unchecked")
public class MerlinExpression
{
    private int fPC;
    private ArrayList fData;
 
    private MerlinExpression(int pc)
    {
        fPC = pc;
        fData = new ArrayList();
    }    
    
    public static ComplexExpression Parse(__TokenIterator ti, int pc) throws AsmException
    {
        MerlinExpression ep = new MerlinExpression(pc);
        return ep.ParseExpression(ti);       
    }
    
    private ComplexExpression ParseExpression(__TokenIterator ti) throws AsmException
    {        
        Expression0(ti);
        
        return new ComplexExpression(fData);
    }
    
    /*
     * parse left-to-right, no order of operation.
     * 
     */
    private void Expression0(__TokenIterator ti) throws AsmException
    {
        Token t;
        int type;
        
        Expression5(ti);
        
        for(;;)
        {        
            int op = -1;
            t = ti.Peek();
            type = t.Type();
            switch (type)
            {
            case '+':
                op = OMF_Expression.EXPR_ADD;
                break;
            case '-':
                op = OMF_Expression.EXPR_SUB;
                break;
            case '*':
                op = OMF_Expression.EXPR_MUL;
                break;
            case '/':
                op = OMF_Expression.EXPR_DIV;
                break;
            case '.':
                op = OMF_Expression.EXPR_BOR;
                break;
            case '!':
                op = OMF_Expression.EXPR_BEOR;
                break;
            case '&':
                op = OMF_Expression.EXPR_BAND;
                break;
            case '<':
                op = OMF_Expression.EXPR_LT;
                break;
            case '>':
                op = OMF_Expression.EXPR_GT;
                break;
            case '=':
                op = OMF_Expression.EXPR_EQ;
                break;
            case '#':
                op = OMF_Expression.EXPR_NE;
                break;
                default : op = -1;
            }
            if (op == -1) break;
            
            Expression5(ti);
            fData.add(new Integer(op));
        }
        
    }  

    /*
    // 0 point 5.  
    private void Expression0p5(__TokenIterator ti) throws AsmException
    {
        Token t;
        int type;
        boolean uminus = false;
        
        t = ti.Peek();
        type = t.Type();
        if (type == '{')
        {
            Expression6(ti);
            return;
        }
        if (type == '+')
        {
            ti.Next();
            t = ti.Peek();
        }
        else if (type == '-')
        {
            uminus = true;
            ti.Next();
            t = ti.Peek();
        }
        // +* or -* is illogical and not allowed.
        else if (type == '*')
        {
            ti.Next();
            fData.add(new RelExpression(fPC));
            return;
        }
        t = ti.Expect(Token.NUMBER, Token.STRING, Token.SYMBOL);
        
        switch (t.Type())
        {
        case Token.STRING:
        case Token.NUMBER:
            fData.add(new ConstExpression(t.Value()));
            break;
        case Token.SYMBOL:
            fData.add(new SymbolExpression(t.toString()));
        }
        if (uminus)
            fData.add(new Integer(OMF_Expression.EXPR_NEG));
    }
    */
    
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
            case '#':
                op = OMF_Expression.EXPR_NE;
                break;
            case Token.LT:
                op = OMF_Expression.EXPR_LT;
                break;
            case Token.GT:
                op = OMF_Expression.EXPR_GT;
                break;
            default: op = -1;              
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
                op =OMF_Expression.EXPR_SUB;
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
        int op;
        
        Expression5(ti);
        
        for(;;)
        {
            op = -1;
            t = ti.Peek();
            switch(t.Type())
            {
            case '!':
                op = OMF_Expression.EXPR_BEOR;
                break;
            case '.':
                op = OMF_Expression.EXPR_BOR;
                break;
            case '&':
                op = OMF_Expression.EXPR_BAND;
                break;                
            default:
                op = -1;
            }
            
            if (op == -1) break;
            
            ti.Next();
            Expression5(ti);
            fData.add(new Integer(op));          
        }       
    }   
    private void Expression5(__TokenIterator ti) throws AsmException
    {
        Token t;
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
        default: 
            op = -1;    
        }
        
        if (op != -1)
             ti.Next();

        Expression6(ti);
        
        if (op != -1)
            fData.add(new Integer(op));           
    }    
    
    private void Expression6(__TokenIterator ti) throws AsmException
    {
        Token t;
        String s;
        
        t = ti.Expect('{', '*', Token.SYMBOL, Token.STRING, Token.NUMBER);

        switch(t.Type())
        {
        case '{':
            Expression1(ti);
            ti.Expect((int)'}');
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
