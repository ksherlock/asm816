import java.util.ArrayList;
import java.util.HashMap;

import omf.OMF;
import omf.OMF_Equ;
import omf.OMF_Expr;
import omf.OMF_Expression;
import omf.OMF_Label;
import omf.OMF_Number;
import omf.OMF_Opcode;
import omf.OMF_RelExpr;

/*
 * Created on Mar 11, 2006
 * Mar 11, 2006 12:51:24 AM
 */

public class Expression implements Cloneable
{
    public Expression(boolean case_sensitive)
    {
        fPC = 0;
        fExpr = new ArrayList();
        fEof = false;
        fReducing = false;
        fEType = 0;
        fEName = null;
        fValue = null;
        fCase = case_sensitive;
    }
    @SuppressWarnings("unchecked")
    public Expression(int clc)
    {
        this(false);
        fExpr.add(new OMF_Number(OMF_Expression.EXPR_REL, clc));
        
    }
    
    public Object clone()
    {
        Expression clone;
        try
        {
            clone = (Expression)super.clone();
            clone.fExpr = (ArrayList)fExpr.clone();
        }
        catch (CloneNotSupportedException e)
        {
            return null;
        }
        return clone;
    
    }
    
    @SuppressWarnings("unchecked")
    private final void AddEof()
    {
        if (!fEof)
        {
            // add the EOF
            fExpr.add(new Integer(OMF_Expression.EXPR_END));
            fEof = true;
        }  
    }
    
    
    @SuppressWarnings("unchecked")
    public OMF_Opcode toOpcode()
    {
        AddEof();
        
        switch(fEType)
        {
        case OMF.OMF_GEQU:
        case OMF.OMF_EQU:
            return new OMF_Equ(fEType, fEName, 0, 'N', false, fExpr);
        case OMF.OMF_RELEXPR:
            // TODO -- need to verify relexpressions
            return new OMF_RelExpr(fSize, 1, fExpr);
        case OMF.OMF_EXPR:
        case OMF.OMF_LEXPR:
        case OMF.OMF_BKEXPR:
        case OMF.OMF_ZPEXPR:
            return new OMF_Expr(fEType, fSize, fExpr);
        case 0:
            return new OMF_Expr(OMF.OMF_EXPR, fSize, fExpr);
        }
        return null;
    }
    /*
    public OMF_Opcode toOpcode(int opcode, String label)
    {
        if (!fEof)
        {
            // add the EOF
            fExpr.add(new Integer(OMF_Expression.EXPR_END));
            fEof = true;
        }
        
        switch (opcode)
        {
        case OMF.OMF_GEQU:
        case OMF.OMF_EQU:
            return new OMF_Equ(opcode, label, 0, 'N', false, fExpr);
        }
        return null;
    }
    */

    @SuppressWarnings("unchecked")
    public void ParseExpression(Lexer_Orca lex) throws AsmException
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
                fExpr.add(new Integer(OMF_Expression.EXPR_EQ));
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
                fExpr.add(new Integer(op));              
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
                fExpr.add(new Integer(op));                
            }
            
            else
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void SimpleExpression(Lexer_Orca lex) throws AsmException
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
                fExpr.add(new Integer(OMF_Expression.EXPR_ADD));
            }
            else if (c == '-')
            {
                lex.NextChar();
                Term(lex);
                fExpr.add(new Integer(OMF_Expression.EXPR_SUB));
            }
            else
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void Term(Lexer_Orca lex) throws AsmException
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
                fExpr.add(new Integer(OMF_Expression.EXPR_MUL));
            }
            else if (c == '/')
            {
                lex.NextChar();
                Factor(lex);
                fExpr.add(new Integer(OMF_Expression.EXPR_DIV));
            }
            else if (c == '|')
            {
                lex.NextChar();
                Factor(lex);
                fExpr.add(new Integer(OMF_Expression.EXPR_SHIFT));
            }
            else
                break;
        }
    
    }

    @SuppressWarnings({"unchecked","unchecked"})
    private void Factor(Lexer_Orca lex) throws AsmException
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
            fExpr.add(new OMF_Number(OMF_Expression.EXPR_CLC, 0));
        }
        else
        {
            t = lex.Expect(Token.NUMBER, Token.STRING, Token.SYMBOL);
    
            switch (t.Type()) {
            case Token.STRING: // convert to an integer constant...
                // Value() converts it to an integer.
                // TODO -- verify the length is 0-4 chars?
            case Token.NUMBER:
                fExpr.add(new OMF_Number(OMF_Expression.EXPR_ABS, t.Value()));
                break;
            case Token.SYMBOL:
                s = t.toString();
                if (!fCase)
                    s = s.toUpperCase();
                fExpr.add(new OMF_Label(OMF_Expression.EXPR_LABEL, s));
                break;
    
            }
            
        }
        if (uminus) fExpr.add(new Integer(OMF_Expression.EXPR_NEG));
    }

    private static int check(Object o1, Object o2)
    {
        if (o1 instanceof OMF_Number && o2 instanceof OMF_Number)
        {
            OMF_Number n1 = (OMF_Number) o1;
            OMF_Number n2 = (OMF_Number) o2;
            int bm = 0;
            switch (n1.Opcode())
            {
                case OMF_Expression.EXPR_ABS:
                    break;
                case OMF_Expression.EXPR_REL:
                    bm |= 0x01;
                    break;
                default:
                    return -1;
            }
            switch (n2.Opcode())
            {
                case OMF_Expression.EXPR_ABS:
                    break;
                case OMF_Expression.EXPR_REL:
                    bm |= 0x10;
                    break;
                default:
                    return -1;
            }
            
            return bm;
        }
        return -1;
    }
    private static OMF_Number opAdd(Object o1, Object o2)
    {
        int bm = check(o1, o2);
        if (bm == -1) return null;
        
        OMF_Number n1 = (OMF_Number) o1;
        OMF_Number n2 = (OMF_Number) o2;
            

        // bm -- bit set = relative, unset = abs.
        
        switch(bm)
        {
        case 0x00: //abs + abs
        case 0x11: // rel + rel
            return new OMF_Number(OMF_Expression.EXPR_ABS, 
                    n1.Value() + n2.Value());
        case 0x01: // abs + rel           
        case 0x10: // rel + abs.
        
            return new OMF_Number(OMF_Expression.EXPR_REL, 
                    n1.Value() + n2.Value());                      
        }
        return null;
    }
    private static OMF_Number opSub(Object o1, Object o2)
    {
        int bm = check(o1, o2);
        if (bm == -1) return null;
        
        OMF_Number n1 = (OMF_Number) o1;
        OMF_Number n2 = (OMF_Number) o2;
            

        // bm -- bit set = relative, unset = abs.
        
        switch(bm)
        {
        case 0x00: //abs - abs
        case 0x11: // rel - rel
            return new OMF_Number(OMF_Expression.EXPR_ABS, 
                    n1.Value() - n2.Value());
        case 0x01: // abs - rel           
        case 0x10: // rel - abs.        
            return new OMF_Number(OMF_Expression.EXPR_REL, 
                    n1.Value() - n2.Value());                      
        }
        return null;
    } 
    private static OMF_Number opMul(Object o1, Object o2)
    {
        int bm = check(o1, o2);
        if (bm == -1) return null;
        
        OMF_Number n1 = (OMF_Number) o1;
        OMF_Number n2 = (OMF_Number) o2;
            

        // bm -- bit set = relative, unset = abs.
        
        switch(bm)
        {
        case 0x00: //abs * abs
            return new OMF_Number(OMF_Expression.EXPR_ABS, 
                    n1.Value() * n2.Value());
        case 0x01: // abs - rel           
        case 0x10: // rel - abs.
        case 0x11: // rel - rel              
        }
        return null;
    }
    private static OMF_Number opDiv(Object o1, Object o2)
    {
        int bm = check(o1, o2);
        if (bm == -1) return null;
        
        OMF_Number n1 = (OMF_Number) o1;
        OMF_Number n2 = (OMF_Number) o2;
            

        // bm -- bit set = relative, unset = abs.
        
        switch(bm)
        {
        case 0x00: //abs * abs
            return new OMF_Number(OMF_Expression.EXPR_ABS, 
                    n1.Value() / n2.Value());
        case 0x01: // abs - rel           
        case 0x10: // rel - abs.
        case 0x11: // rel - rel              
        }
        return null;
    }
    private static OMF_Number opMod(Object o1, Object o2)
    {
        int bm = check(o1, o2);
        if (bm == -1) return null;
        
        OMF_Number n1 = (OMF_Number) o1;
        OMF_Number n2 = (OMF_Number) o2;
            

        // bm -- bit set = relative, unset = abs.
        
        switch(bm)
        {
        case 0x00: //abs % abs
            return new OMF_Number(OMF_Expression.EXPR_ABS, 
                    n1.Value() % n2.Value());
        case 0x01: // abs - rel           
        case 0x10: // rel - abs.
        case 0x11: // rel - rel              
        }
        return null;
    }
    private static OMF_Number opShift(Object o1, Object o2)
    {
        int bm = check(o1, o2);
        if (bm == -1) return null;
        
        OMF_Number n1 = (OMF_Number) o1;
        OMF_Number n2 = (OMF_Number) o2;
            

        // bm -- bit set = relative, unset = abs.
        int v;
        int shift;
        switch(bm)
        {
        case 0x00: //abs <<|>> abs
            shift = n2.Value();
            v = n1.Value();
            if (shift <0) v = v >> shift;
            else v = v << shift;
            return new OMF_Number(OMF_Expression.EXPR_ABS, 
                    v);
        case 0x01: // abs - rel           
        case 0x10: // rel - abs.
        case 0x11: // rel - rel              
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public boolean Reduce(HashMap<String, Expression> map, boolean absolute) throws AsmException
    {
        // if it's a constant value, we're done.
        if (Value() != null) return true;
        
        if (fReducing) throw new AsmException(Error.E_EXPRESSION);
        fReducing = true;
        
        boolean ret = false;
    
        AddEof();
        
        
        // replace symbols with their values, if known.
        for (int i = 0; i < fExpr.size(); i++)
        {
            Object o = fExpr.get(i);
            if (o instanceof OMF_Label)
            {
                if (map == null) continue;
                OMF_Label lab = (OMF_Label) o;
                
                if (lab.Opcode() == OMF_Expression.EXPR_LABEL)
                {
                    Expression op = map.get(lab.toString());
                    if (op != null)
                    {
                        // TODO -- add cyclical dependency check.
                        op.Reduce(map, absolute);
                        Integer v = op.Value();
                        if (v != null)
                        {
                            fExpr.set(i, new OMF_Number(OMF_Expression.EXPR_ABS, v
                                .intValue()));
                            ret = true;
                        }
                    }
                }
            }
            else if (o instanceof OMF_Number)
            {
                OMF_Number n = (OMF_Number)o;
                switch(n.Opcode())
                {
                case OMF_Expression.EXPR_CLC:
                    fExpr.set(i, new OMF_Number(absolute ? OMF_Expression.EXPR_ABS : OMF_Expression.EXPR_REL, fPC));
                    ret = true;
                    break;
                
                case OMF_Expression.EXPR_REL:
                    if (absolute)
                    {
                        fExpr.set(i, new OMF_Number(OMF_Expression.EXPR_ABS, n.Value()));
                        ret = true;
                    }
                     break;
                }
    
            }
        }
    
        // now evaluate constant math
        // the expression is expected to be valid.
        Stack stack = new Stack();
        int top = -1;
        for (Object o : fExpr)
        {
            OMF_Number res;
            
            if (o instanceof Integer)
            {
                boolean ok = false;
                OMF_Number n1;
                Object o1, o2;
                Integer i = (Integer) o;
    
                switch (i.intValue()) {
                case OMF_Expression.EXPR_NEG:
                    o1 = stack.get(top);
                    if (o1 instanceof OMF_Number)
                    {
                        n1 = (OMF_Number) o1;
                        if (n1.Opcode() == OMF_Expression.EXPR_ABS)
                        {
                            stack.set(top, new OMF_Number(OMF_Expression.EXPR_ABS, -n1.Value()));
                            ok = true;
                        }
                    }
                    break;
                case OMF_Expression.EXPR_ADD:
                    o2 = stack.get(top);
                    o1 = stack.get(top - 1);
                    res = opAdd(o1, o2);
                    if (res != null)
                    {
                        stack.remove(top--);
                        stack.set(top, res);
                        ok = true;
                    }
                    
                    break;
                case OMF_Expression.EXPR_SUB:
                    o2 = stack.get(top);
                    o1 = stack.get(top - 1);
                    res = opSub(o1, o2);
                    if (res != null)
                    {
                        stack.remove(top--);
                        stack.set(top, res);
                        ok = true;
                    }
                    break;
    
                case OMF_Expression.EXPR_MUL:
                    o2 = stack.get(top);
                    o1 = stack.get(top - 1);
                    res = opMul(o1, o2);
                    if (res != null)
                    {
                        stack.remove(top--);
                        stack.set(top, res);
                        ok = true;
                    }
                    break;
                case OMF_Expression.EXPR_DIV:
                    o2 = stack.get(top);
                    o1 = stack.get(top - 1);
                    res = opDiv(o1, o2);
                    if (res != null)
                    {
                        stack.remove(top--);
                        stack.set(top, res);
                        ok = true;
                    }
                    break;
    
                case OMF_Expression.EXPR_MOD:
                    o2 = stack.get(top);
                    o1 = stack.get(top - 1);
                    res = opMod(o1, o2);
                    if (res != null)
                    {
                        stack.remove(top--);
                        stack.set(top, res);
                        ok = true;
                    }
                    break;
    
                case OMF_Expression.EXPR_SHIFT:
                    
                    o2 = stack.get(top);
                    o1 = stack.get(top - 1);
                    res = opShift(o1, o2);
                    if (res != null)
                    {
                        stack.remove(top--);
                        stack.set(top, res);
                        ok = true;
                    }
                    break;                                      
                    // TODO -- rest of them.
                }
                if (ok)
                {
                    ret = true;
                    continue;
                }
            } // if o instanceof Integer
            stack.add(o);
            top++;
        }
        fExpr = stack;
        fReducing = false;
        return ret;
    }
    
    public String Label()
    {
        AddEof();      
        
        if (fExpr.size() != 2) return null;
        
        Object o = fExpr.get(0);
        if (o instanceof OMF_Label)
        {
            return o.toString();
        }
        
        return null;
    }

    @SuppressWarnings("unchecked")
    public Integer Value()
    {
        // this caches, which makes life faster.
        if (fValue == null)
        {
            AddEof();

            if (fExpr.size() != 2) return null;
            Object o = fExpr.get(0);
            if (!(o instanceof OMF_Number)) return null;
            OMF_Number n = (OMF_Number)o;
            if (n.Opcode() != OMF_Expression.EXPR_ABS) return null;
            
            fValue = new Integer(n.Value());
        }
        return fValue;
    }

    public void SetPC(int pc)
    {
        fPC = pc;
    }

    public void SetExpressionType(int type)
    {
        fEType = type;
    }
    public int ExpressionType()
    {
        return fEType;
    }
    public void SetExpressionName(String s)
    {
        fEName = s;
    }
    public String ExpressionName()
    {
        return fEName;
    }
    
    public void SetSize(int size)
    {
        fSize = size;
    }
    public int Size()
    {
        return fSize;
    }
    
    @SuppressWarnings("unchecked")
    public void Shift(int val)
    {
        fExpr.add(new OMF_Number(OMF_Expression.EXPR_ABS, val));
        fExpr.add(new Integer(OMF_Expression.EXPR_SHIFT));
    }

    private ArrayList fExpr;
    private boolean fEof;
    private int fPC;
    private boolean fReducing;
    private int fEType;
    private String fEName;
    private int fSize;
    private Integer fValue;
    private boolean fCase;
    

}
