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
                
                switch(op)
                {
                    /*
                     * unary minus.
                     */
                case OMF_Expression.EXPR_NEG:
                    if (!doNegate(stack))
                            stack.push(o);
                    break;
                    
                case OMF_Expression.EXPR_BNOT:
                    if (!doNot(stack))
                        stack.push(o);
                    break;
                    
                case OMF_Expression.EXPR_LNOT:
                    if (!doLNot(stack))
                        stack.push(o);
                    break; 
                    
                    
                    
                case OMF_Expression.EXPR_ADD:
                    if (!doAdd(stack))
                        stack.push(o);
                    break;

                case OMF_Expression.EXPR_SUB:
                    if (!doSub(stack))
                        stack.push(o);
                    break;
                    
                case OMF_Expression.EXPR_MUL:
                    if (!doMul(stack))
                        stack.push(o);
                    break;
                    
                case OMF_Expression.EXPR_DIV:
                    if (!doDiv(stack))
                        stack.push(o);
                    break;
                    
                case OMF_Expression.EXPR_MOD:
                    if (!doMod(stack))
                        stack.push(o);
                    break;
                    
                case OMF_Expression.EXPR_SHIFT:
                    if (!doShift(stack))
                        stack.push(o);
                    break;

                case OMF_Expression.EXPR_LAND:
                    if (!doLAnd(stack))
                        stack.push(o);
                    break;                    
 
                case OMF_Expression.EXPR_LOR:
                    if (!doLOr(stack))
                        stack.push(o);
                    break;                     
                case OMF_Expression.EXPR_LEOR:
                    if (!doLEor(stack))
                        stack.push(o);
                    break;
                    
                default:
                    System.out.println("Not yet implemented " + op);
                }   
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
     * negate the top item.  returns true if processed, 
     * false if not processed.
     */
    @SuppressWarnings("unchecked")
    private static boolean doNegate(Stack stack)
    {
        int top = stack.size();
        
        Object o = (stack.get(top - 1));
        if (o instanceof MExpression)
        {
            MExpression copy = MExpression.opNegate((MExpression)o);
            if (copy != null)
            {
                stack.set(top -1, copy);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static boolean doNot(Stack stack)
    {
        int top = stack.size();
        
        Object o = (stack.get(top - 1));
        if (o instanceof MExpression)
        {
            MExpression copy = MExpression.opNot((MExpression)o);
            if (copy != null)
            {
                stack.set(top -1, copy);
                return true;
            }
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private static boolean doLNot(Stack stack)
    {
        int top = stack.size();
        
        Object o = (stack.get(top - 1));
        if (o instanceof MExpression)
        {
            MExpression copy = MExpression.opLNot((MExpression)o);
            if (copy != null)
            {
                stack.set(top -1, copy);
                return true;
            }
        }
        return false;
    }    
    @SuppressWarnings("unchecked")
    private static boolean doAdd(Stack stack)
    {
        int top = stack.size();
        
        Object o1, o2;
        o1 = stack.get(top - 2);
        o2 = stack.get(top - 1);
    
        if (o1 instanceof MExpression && o2 instanceof MExpression)
        {
            MExpression copy = MExpression
                .opAdd((MExpression)o1,(MExpression)o2);
            if (copy != null)
            {
                stack.remove(top - 1);
                stack.set(top - 2, copy);
                return true;
            }
        }
        return false;     
    }

    @SuppressWarnings("unchecked")
    private static boolean doSub(Stack stack)
    {
        int top = stack.size();
        
        Object o1, o2;
        o1 = stack.get(top - 2);
        o2 = stack.get(top - 1);
    
        if (o1 instanceof MExpression && o2 instanceof MExpression)
        {
            MExpression copy = MExpression
                .opSub((MExpression)o1,(MExpression)o2);
            if (copy != null)
            {
                stack.remove(top - 1);
                stack.set(top - 2, copy);
                return true;
            }
        }
        return false;     
    }
    @SuppressWarnings("unchecked")
    private static boolean doMul(Stack stack)
    {
        int top = stack.size();
        
        Object o1, o2;
        o1 = stack.get(top - 2);
        o2 = stack.get(top - 1);
    
        if (o1 instanceof MExpression && o2 instanceof MExpression)
        {
            MExpression copy = MExpression
                .opMul((MExpression)o1,(MExpression)o2);
            if (copy != null)
            {
                stack.remove(top - 1);
                stack.set(top - 2, copy);
                return true;
            }
        }
        return false;     
    }

    @SuppressWarnings("unchecked")
    private static boolean doDiv(Stack stack)
    {
        int top = stack.size();
        
        Object o1, o2;
        o1 = stack.get(top - 2);
        o2 = stack.get(top - 1);
    
        if (o1 instanceof MExpression && o2 instanceof MExpression)
        {
            MExpression copy = MExpression
                .opDiv((MExpression)o1,(MExpression)o2);
            if (copy != null)
            {
                stack.remove(top - 1);
                stack.set(top - 2, copy);
                return true;
            }
        }
        return false;     
    }

    @SuppressWarnings("unchecked")
    private static boolean doMod(Stack stack)
    {
        int top = stack.size();
        
        Object o1, o2;
        o1 = stack.get(top - 2);
        o2 = stack.get(top - 1);
    
        if (o1 instanceof MExpression && o2 instanceof MExpression)
        {
            MExpression copy = MExpression
                .opMod((MExpression)o1,(MExpression)o2);
            if (copy != null)
            {
                stack.remove(top - 1);
                stack.set(top - 2, copy);
                return true;
            }
        }
        return false;     
    }

    @SuppressWarnings("unchecked")
    private static boolean doShift(Stack stack)
    {
        int top = stack.size();
        
        Object o1, o2;
        o1 = stack.get(top - 2);
        o2 = stack.get(top - 1);
    
        if (o1 instanceof MExpression && o2 instanceof MExpression)
        {
            MExpression copy = MExpression
                .opShift((MExpression)o1,(MExpression)o2);
            if (copy != null)
            {
                stack.remove(top - 1);
                stack.set(top - 2, copy);
                return true;
            }
        }
        return false;     
    }   

    
    @SuppressWarnings("unchecked")
    private static boolean doLAnd(Stack stack)
    {
        int top = stack.size();
        
        Object o1, o2;
        o1 = stack.get(top - 2);
        o2 = stack.get(top - 1);
    
        if (o1 instanceof MExpression && o2 instanceof MExpression)
        {
            MExpression copy = MExpression
                .opLAnd((MExpression)o1,(MExpression)o2);
            if (copy != null)
            {
                stack.remove(top - 1);
                stack.set(top - 2, copy);
                return true;
            }
        }
        return false;     
    }    
    @SuppressWarnings("unchecked")
    private static boolean doLOr(Stack stack)
    {
        int top = stack.size();
        
        Object o1, o2;
        o1 = stack.get(top - 2);
        o2 = stack.get(top - 1);
    
        if (o1 instanceof MExpression && o2 instanceof MExpression)
        {
            MExpression copy = MExpression
                .opLOr((MExpression)o1,(MExpression)o2);
            if (copy != null)
            {
                stack.remove(top - 1);
                stack.set(top - 2, copy);
                return true;
            }
        }
        return false;     
    }    

    @SuppressWarnings("unchecked")
    private static boolean doLEor(Stack stack)
    {
        int top = stack.size();
        
        Object o1, o2;
        o1 = stack.get(top - 2);
        o2 = stack.get(top - 1);
    
        if (o1 instanceof MExpression && o2 instanceof MExpression)
        {
            MExpression copy = MExpression
                .opLEor((MExpression)o1,(MExpression)o2);
            if (copy != null)
            {
                stack.remove(top - 1);
                stack.set(top - 2, copy);
                return true;
            }
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
        "-- ", //unary minus
        "<< ", // shift
        "&& ",
        "|| ",
        "^^ ", //logical eor
        "! ",
        "<= ",
        ">= ",
        "!= ",
        "< ",
        "> ",
        "== ",
        "& ",
        "| ",
        "^ ",
        "~ " 
    };
}
