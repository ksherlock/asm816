/*
 * Created on Dec 1, 2006
 * Dec 1, 2006 3:06:05 PM
 */
package expression;

import java.io.PrintStream;
import java.util.ArrayList;

import omf.OMF_Expression;

import asm816.Stack;
import asm816.SymbolTable;

/*
 * a complex expression is an RPN array/stack of Integers 
 * (encapsulating OMF_Expression opcodes), 
 * SymbolExpressions, and MExpressions.
 * 
 * The Simplify method will perform any math operations
 * and return a SymbolExpression or MExpression if possible
 */


public class ComplexExpression implements __Expression
{
    public ComplexExpression()
    {
        fData = null;
        fReducing = false;
    }
    @SuppressWarnings("unchecked")
    public ComplexExpression(ArrayList expression)
    {
        fData = new ArrayList();
        if (expression != null)
            fData.addAll(expression);
    }


    public void PrintValue(PrintStream ps)
    {
        for(Object o : fData)
        {
            if (o instanceof __Expression)
            {
                ((__Expression)o).PrintValue(ps);
                ps.print(" ");
            }
            else if (o instanceof Integer)
            {
                int op = ((Integer)o).intValue();
                String ops = "??? ";
                if (op > 0 && op < OpNames.length)
                    ops = OpNames[op];
                ps.print(ops);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList Serialize(String root)
    {
        ArrayList out = new ArrayList();
        
        for (Object o : fData)
        {
            if (o instanceof __Expression)
                out.addAll(((__Expression)o).Serialize(root));
            else out.add(o);
        }
        
        return out;
    }

    @SuppressWarnings("unchecked")
    public void Shift(int val)
    {
        fData.add(new ConstExpression(val));
        fData.add(new Integer(OMF_Expression.EXPR_SHIFT));
    }
    
    
    // rename any SymbolExpressions but do not simplify.
    @SuppressWarnings("unchecked")
    public void Remap(SymbolTable st)
    {
        if (st == null) return;
        
        int len = fData.size();
        for (int i = 0; i < len; i++)
        {
            Object o = fData.get(i);
            if (o instanceof SymbolExpression)
            {
                SymbolExpression s = (SymbolExpression)o;
                fData.set(i, s.Simplify(st, false));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public __Expression Simplify(SymbolTable st, boolean deep)
    {
        __Expression out = null;
        
        // TODO -- throw error.
        if (fReducing) return this; //oops!
        
        fReducing = true;
          
        int length = fData.size();
        
        if (length == 1)
        {
            out = (__Expression)fData.get(0);
            out = out.Simplify(st, deep);
            fReducing = false;
            return out;
        }
        
        /*
         * simplify any symbols in the expression.
         */
        
        Stack stack = new Stack();
        
        for (Object o: fData)
        {
            if (o instanceof __Expression)
            {
                __Expression e = (__Expression)o;
                stack.push(e.Simplify(st, deep));
            }
            else 
            {                
                Integer i = (Integer)o;
                int op = i.intValue();
                
                if (!doOp(op, stack))
                    stack.push(o);  
            }
        } // for (o : Objects)       

        fData = stack;
        length = fData.size();
        if (length == 1)
        {
            out = (__Expression)fData.get(0);
            out = out.Simplify(st, deep);
            fReducing = false;
            return out;
        }
        fReducing = false;
        
        return this;
    }
 
    
    /*
     * verify the arguments are ok and dispatch.
     * 
     *  returns true (and updates the stack)
     *  if we handled it, false if not.
     */
    @SuppressWarnings("unchecked")
    private static boolean doOp(int op, Stack stack)
    {
        int top = stack.size();
        
        if (op == OMF_Expression.EXPR_NEG 
                || op == OMF_Expression.EXPR_BNOT
                || op == OMF_Expression.EXPR_LNOT)
        {
            Object o = (stack.get(top - 1));
            if (!(o instanceof MExpression)) return false;

            MExpression m = (MExpression)o;
            MExpression copy = null;
            
            if (op == OMF_Expression.EXPR_NEG)
                copy = MExpression.opNegate(m);
            else if (op == OMF_Expression.EXPR_BNOT)
                copy = MExpression.opNot(m);
            else if (op == OMF_Expression.EXPR_LNOT)
                copy = MExpression.opLNot(m);
            
            if (copy != null)
            {
                stack.set(top -1, copy);
                return true;
            }           
            return false;
            
        }
        
        Object o1, o2;
        MExpression m1, m2;
        
        /* these are the correct order */
        o1 = stack.get(top - 2);
        o2 = stack.get(top - 1);
        
        if (!(o1 instanceof MExpression)) return false;
        if (!(o2 instanceof MExpression)) return false;
        
        m1 = (MExpression)o1;
        m2 = (MExpression)o2;
        MExpression copy = null;
        
        switch (op)
        {
        case  OMF_Expression.EXPR_ADD:
            copy = MExpression.opAdd(m1, m2);
            break;
        case  OMF_Expression.EXPR_SUB:
            copy = MExpression.opSub(m1, m2);
            break;
        case  OMF_Expression.EXPR_MUL:
            copy = MExpression.opMul(m1, m2);
            break;
        case  OMF_Expression.EXPR_DIV:
            copy = MExpression.opDiv(m1, m2);
            break;
        case  OMF_Expression.EXPR_MOD:
            copy = MExpression.opMod(m1, m2);
            break;
        case  OMF_Expression.EXPR_SHIFT:
            copy = MExpression.opShift(m1, m2);
            break;
        case  OMF_Expression.EXPR_LAND:
            copy = MExpression.opLAnd(m1, m2);
            break;
        case  OMF_Expression.EXPR_LOR:
            copy = MExpression.opLOr(m1, m2);
            break;
        case  OMF_Expression.EXPR_LEOR:
            copy = MExpression.opLEor(m1, m2);
            break;
        case  OMF_Expression.EXPR_LE:
            copy = MExpression.opLE(m1, m2);
            break;
        case  OMF_Expression.EXPR_GE:
            copy = MExpression.opGE(m1, m2);
            break;
        case  OMF_Expression.EXPR_NE:
            copy = MExpression.opNE(m1, m2);
            break;
        case  OMF_Expression.EXPR_LT:
            copy = MExpression.opLT(m1, m2);
            break;
        case  OMF_Expression.EXPR_GT:
            copy = MExpression.opGT(m1, m2);
            break;
        case  OMF_Expression.EXPR_EQ:
            copy = MExpression.opEQ(m1, m2);
            break;
        case  OMF_Expression.EXPR_BAND:
            copy = MExpression.opAnd(m1, m2);
            break;
        case  OMF_Expression.EXPR_BOR:
            copy = MExpression.opOr(m1, m2);
            break;
        case  OMF_Expression.EXPR_BEOR:
            copy = MExpression.opEor(m1, m2);
            break;
        }
        
        if (copy != null)
        {
            stack.remove(top - 1);
            stack.set(top - 2, copy);
            return true;
        }
        
        return false;
    }
    
    public Integer Value()
    {
        return null;
    }
    public String toString()
    {
        return null;
    }
    public boolean isConst()
    {
        return false;
    }    
    

    private ArrayList fData;
    private boolean fReducing;
    
    static final private String OpNames[] =
    {
        "",
        "+ ",
        "- ",
        "* ",
        "/ ",
        "% ",
        "(-) ", //unary minus
        "<< ", // shift
        "and ",
        "or ",
        "eor ", //logical eor
        "not ",
        "<= ",
        ">= ",
        "<> ",
        "< ",
        "> ",
        "== ",
        "& ",
        "| ",
        "^ ",
        "~ " 
    };
}
