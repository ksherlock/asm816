/*
 * Created on Dec 5, 2006
 * Dec 5, 2006 10:04:08 PM
 */
package asm816;

import java.util.ArrayList;

/*
 * abstract base class (and common helper methods) 
 * for a token iterator. 
 * This class makes dealing with operands easier. 
 */


public abstract class __TokenIterator
{
    abstract public Token Next();
    abstract public Token Peek();
    abstract public boolean EndOfLine();
    abstract public void Reset();
    abstract public boolean Contains(int type);
    abstract public boolean Contains(int... types);
    
    public int PeekType()
    {
        Token t = Peek();
        return t.Type();
    }
    
    public Token Expect(int t1) throws AsmException
    {
        Token t = Next();
        t.Expect(t1);
        return t;
    }
    public Token Expect(int t1, int t2) throws AsmException
    {
        Token t = Next();
        t.Expect(t1, t2);
        return t;
    }    
    public Token Expect(int... type) throws AsmException
    {
        Token t = Next();
        t.Expect(type);
        return t;
    }
    
    public int ExpectSymbol(String name) throws AsmException
    {
        Token t = Next();
        return t.ExpectSymbol(name);
    }
    public int ExpectSymbol(String... names) throws AsmException
    {
        Token t = Next();
        return t.ExpectSymbol(names);
    }
    
    /*
     * convert a comma-separated list to an ArrayList.
     * type is the type of it (Token.SYMBOL, Token.STRING, etc).
     */
    public ArrayList<Token> toList(int type)
    throws AsmException
    {
        ArrayList<Token> out = new ArrayList<Token>();
        Token t;
        for (boolean b = true; ; b = !b)
        {
            if (b)
            {
                t = Expect(type);
                out.add(t);
            }
            else
            {
                t = Expect(',', Token.EOL);
                if (t.Type() == Token.EOL) break;
            }
        }        
        return out;       
    }
    
    public ArrayList<Token> toList(int... types)
    throws AsmException
    {
        ArrayList<Token> out = new ArrayList<Token>();
        Token t;
        for (boolean b = true; ; b = !b)
        {
            if (b)
            {
                t = Expect(types);
                out.add(t);
            }
            else
            {
                t = Expect(',', Token.EOL);
                if (t.Type() == Token.EOL) break;
            }
        }        
        return out;       
    }    
    
}
