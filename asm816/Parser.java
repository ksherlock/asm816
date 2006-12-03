package asm816;
import java.util.HashMap;


import expression.ComplexExpression;
import expression.ExpressionParser;
import expression.__Expression;

/*
 * Created on Nov 29, 2006
 * Nov 29, 2006 10:35:12 PM
 */

public abstract class Parser
{
    protected HashMap<String, INSTR> fOpcodes;
    protected HashMap<String, Enum> fDirectives;
    
    protected boolean fCase;
    protected int fMachine;
    protected boolean fM;
    protected boolean fX;
    protected int fPC;
    
    protected int fErrors;
    
    public Parser()
    {
        
        fOpcodes = new HashMap<String, INSTR>();
        fDirectives = new HashMap<String, Enum>();

        AddDirectives();
    }
    
    protected abstract void AddDirectives();
    protected void AddOpcodes()
    {
        for (INSTR i: INSTR.values())
        {
            fOpcodes.put(i.name(), i);
        }       
    }
    
    
    protected void ParseLine(Lexer l)
    {
    }
    protected boolean ParseDirective(Lexer l)
    {
        return false;
    }
    protected boolean ParseInstruction(String s,Lexer l, INSTR i)
        throws AsmException
    {
        return false;
    }
    protected void Reduce()
    {
        // reduce: convert any Relative expressions to segment + relative
    }
    
    protected operand ParseOperand(Lexer lex)
    throws AsmException
    {
        int c;
        Token t;
        
        operand oper = new operand();
        oper.expression = null;
        oper.mode = AddressMode.IMPLIED;
        
        ComplexExpression e = null;
        
    
        t = lex.NextToken();
        if (t.Type() == Token.EOL)
        {
            return oper;
        }
        t.Expect(Token.SPACE);
        
        c = lex.Peek();
    
        
        switch (c) {
        /*
         * immediate mode. 
         * '#' [ '<' | '>' | '|' ]? <expression>
         */
        case '#':
            lex.NextChar();
    
            c = lex.Peek();
            if (c == '^' || c == '>' || c == '<')
            {
                lex.NextChar();
            }
            oper.mode = AddressMode.IMMEDIATE;
            e = ExpressionParser.Parse(lex, fPC);
    
            // ^ > --> shift it.
            if (c == '^')
                e.Shift(-16);
            else if (c == '>')
                e.Shift(-8);
            break;
            
            /*
             * indirect
             * '(' ['<']? <expression> ')' [',' 'y']?
             * '(' ['<']? <expression> ',' 'x' ')'
             * '(' ['<']? <expression> ',' 's' ')' ',' 'y'
             */
        case '(':
            boolean stack = false;
            lex.NextChar();
            
            c = lex.Peek();
            if (c == '<')
                lex.NextChar();
            oper.mode = AddressMode.INDIRECT;
            e = ExpressionParser.Parse(lex, fPC);
    
            // next char must be , or )
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                t = lex.NextToken();
                c = t.Register();
                if (c == 'x')
                {
                    oper.mode = AddressMode.INDIRECT_X;
                    lex.Expect((int)')');
                    break;
                }
                
                else if (c == 's')
                {
                    stack = true;
                }
                else 
                    throw new AsmException(Error.E_UNEXPECTED, t);
                    
            }
            lex.Expect((int)')');
    
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
    
                t = lex.NextToken();
                c = t.Register();
                if (c == 'y')
                {
                    oper.mode = stack 
                        ? AddressMode.INDIRECT_S_Y 
                        : AddressMode.INDIRECT_Y;
                    stack = false;
                }
                else 
                    throw new AsmException(Error.E_UNEXPECTED, t);
            }
            if (stack) 
                throw new AsmException(Error.E_UNEXPECTED, t);
            break;
            
            /*
             * long indirect
             * '[' ['<']? <expression> ']' [',' 'y']?
             */
        case '[':
            lex.NextChar();
            
            c = lex.Peek();
            if (c == '<')
                lex.NextChar();
            
            oper.mode = AddressMode.LINDIRECT;
            e = ExpressionParser.Parse(lex, fPC);
            lex.Expect((int)']');
    
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                t = lex.NextToken();
                c = t.Register();
                if (c == 'y')
                    oper.mode = AddressMode.LINDIRECT_Y;
                    
                else
                    throw new AsmException(Error.E_UNEXPECTED, t);
                
                
            }
            break;
            
            /*
             * absolute mode
             * '|' <expression> [',' ['x' | 'y'] ]?
             */
        case '|':
            lex.NextChar();
            oper.mode = AddressMode.ABS;
            e = ExpressionParser.Parse(lex, fPC);
    
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                t = lex.NextToken();
    
                // t must be a symbol, either X or Y
                c = t.Register();
                if (c == 'y')
                    oper.mode = AddressMode.ABS_Y;
                else if (c == 'x')
                    oper.mode = AddressMode.ABS_X;
    
                else
                    throw new AsmException(Error.E_UNEXPECTED, t);
    
            }
            break;
            
            /*
             * absolute long mode
             * '>' <expression> [',' 'x']?
             */
        case '>':
            lex.NextChar();
            
            oper.mode = AddressMode.ABSLONG;
            e = ExpressionParser.Parse(lex, fPC);
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                t = lex.NextToken();
    
                c = t.Register();
    
                if (c == 'x')
                    oper.mode = AddressMode.ABSLONG_X;
    
                else
                    throw new AsmException(Error.E_UNEXPECTED, t);
            }
            break;
            
            /*
             * dp mode
             * <dp,s is allowed as a convenience.
             * '<' <expression> [',' ['x' | 'y' | 's']]?
             */
        case '<':
            lex.NextChar();
            
            oper.mode = AddressMode.DP;
            e = ExpressionParser.Parse(lex, fPC);
    
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                t = lex.NextToken();
                // t must be a symbol, either S, X, or Y
                c = t.Register();
                
                if (c == 's')
                    oper.mode = AddressMode.STACK;
                else if (c == 'x')
                    oper.mode = AddressMode.DP_X;
                else if (c == 'y')
                    oper.mode = AddressMode.DP_Y;
    
                else
                    throw new AsmException(Error.E_UNEXPECTED, t);
            }
            
            break;
            
            /*
             * just another expression...
             * <expression> [',' ['x' | 'y' | 's']]?
             */
        default:
            // TODO -- reduce, convert to dp/abslong ???
            // or mark as possibly unknown??
            oper.mode = AddressMode.ASSUMED_ABS;
        
            /* TODO -- if the operand is a known value, set to DP or long as appropriate
            * eg: 
            * blah equ $e12000
            *      lda blah
            * should be handled as lda >blah
            */ 
        
            e = ExpressionParser.Parse(lex, fPC);
            // check for ,x,y
            // ,s --> stack relative (ie, dp)
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                t = lex.NextToken();
                c = t.Register();
    
                // t should be x,y,s
    
                if (c == 'x')
                    oper.mode = AddressMode.ASSUMED_ABS_X;
                else if (c == 'y')
                    oper.mode = AddressMode.ASSUMED_ABS_Y;
                else if (c == 's')
                   oper.mode = AddressMode.STACK;
                else
                    throw new AsmException(Error.E_UNEXPECTED, t);
            }
        }
    
        lex.Expect(Token.EOL);
        oper.expression = e;
        return oper;
         
    }
    
    
    protected class operand
    {
    public AddressMode mode;
    public __Expression expression;
    }
    
}
