/*
 * Created on Dec 1, 2006
 * Dec 1, 2006 4:29:52 PM
 */
package expression;

import java.util.ArrayList;

import omf.OMF_Expression;

import asm816.AsmException;
import asm816.Lexer;
import asm816.Token;

public class ExpressionParser
{
    private int fPC;
    private ArrayList fData;
    
    public static ComplexExpression Parse(Lexer lex, int pc) 
        throws AsmException
    {
        ExpressionParser ep = new ExpressionParser(pc);
        return ep.ParseExpression(lex);
    }
    
    private ExpressionParser(int pc)
    {
        fPC = pc;
        fData = new ArrayList();
    }
    
    @SuppressWarnings("unchecked")
    private ComplexExpression ParseExpression(Lexer lex) 
        throws AsmException
        {
            int c;
        
            SimpleExpression(lex);
        
            for (;;)
            {
                c = lex.Peek();
                if (c == '=')
                {
                    lex.NextChar();
                    SimpleExpression(lex);
                    fData.add(new Integer(OMF_Expression.EXPR_EQ));
                }
                else if (c == '<')
                {
                    int op = OMF_Expression.EXPR_LT;
                    lex.NextChar();
                    c = lex.Peek();
                    if (c == '=')
                    {
                        lex.NextChar();
                        op = OMF_Expression.EXPR_LE;
                    }
                    if (c == '>')
                    {
                        lex.NextChar();
                        op = OMF_Expression.EXPR_NE;
                    }
                    SimpleExpression(lex);
                    fData.add(new Integer(op));              
                }
                else if (c == '>')
                {
                    int op = OMF_Expression.EXPR_GT;
                    lex.NextChar();
                    c = lex.Peek();
                    if (c == '=')
                    {
                        lex.NextChar();
                        op = OMF_Expression.EXPR_GE;
                    }
                    SimpleExpression(lex);
                    fData.add(new Integer(op));                
                }
                
                else
                    break;
            }
            
            return new ComplexExpression(fData);
        }

    @SuppressWarnings("unchecked")
    private void SimpleExpression(Lexer lex) throws AsmException
    {
        int c;
    
        Term(lex);
    
        for (;;)
        {
            c = lex.Peek();
    
            if (c == '+')
            {
                lex.NextChar();
                Term(lex);
                fData.add(new Integer(OMF_Expression.EXPR_ADD));
            }
            else if (c == '-')
            {
                lex.NextChar();
                Term(lex);
                fData.add(new Integer(OMF_Expression.EXPR_SUB));
            }
            else
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void Term(Lexer lex) throws AsmException
    {
        int c;
    
        Factor(lex);
        for (;;)
        {
            c = lex.Peek();
            if (c == '*')
            {
                lex.NextChar();
                Factor(lex);
                fData.add(new Integer(OMF_Expression.EXPR_MUL));
            }
            else if (c == '/')
            {
                lex.NextChar();
                Factor(lex);
                fData.add(new Integer(OMF_Expression.EXPR_DIV));
            }
            else if (c == '|')
            {
                lex.NextChar();
                Factor(lex);
                fData.add(new Integer(OMF_Expression.EXPR_SHIFT));
            }
            else
                break;
        }
    
    }

    @SuppressWarnings({"unchecked","unchecked"})
    private void Factor(Lexer lex) throws AsmException
    {
        int c;
        Token t;
        String s;
        boolean uminus = false;
        
        c = lex.Peek();
        if (c == '+' || c == '-')
        {
            uminus = c == '-';
            lex.NextChar();
            c = lex.Peek();
        }
    
        if (c == '(')
        {
            lex.NextChar();
            ParseExpression(lex);
            lex.Expect((int) ')');
        }
        else if (c == '*')
        {
            lex.NextChar();
            fData.add(new RelExpression(fPC));
            // should be an error if not in a data segment(?)
        }
        else
        {
            t = lex.Expect(Token.NUMBER, Token.STRING, Token.SYMBOL);
    
            switch (t.Type()) 
            {
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
        if (uminus) fData.add(new Integer(OMF_Expression.EXPR_NEG));
    }
    
}
