/*
 * Created on Dec 12, 2006
 * Dec 12, 2006 3:11:12 AM
 */
package mpw;

import java.util.HashMap;

import asm816.Token;
import asm816.__TokenIterator;

public class MacroIterator extends __TokenIterator
{
    private int STATE_UNKNOWN = -1;  
    
    public MacroIterator(__TokenIterator base, HashMap<String, Object> parms)
    {
        fBase = base;
        fParms = parms;
        fReplace = null;
        fState = STATE_UNKNOWN;
    }
    
    public Token Next()
    {
        
        if (fT == null) __Next();
        Token t = fT;
        fT = null;
        fState = STATE_UNKNOWN;
        return t;

    }
    
    

    // TODO -- move most of this to private function.
    public Token Peek()
    {
        if (fT == null) __Next();
        return fT;
 
    }

    public boolean EndOfLine()
    {
        if (fT == null) __Next();
        return (fT.Type() == Token.EOL);

    }

    public void Reset()
    {
        fState = STATE_UNKNOWN;
        fT = null;
        fReplace = null;
        fBase.Reset();
    }
    
    private void __Next()
    {
        if (fState >= 0)
        {
            fState++;
            if (fState >= fReplace.length)
                fState = STATE_UNKNOWN;
        }
        
        if (fState == STATE_UNKNOWN)
        {
            fReplace = null;
            Token t = fBase.Next();
            if (t.Type() == Token.MACRO_PARM)
            {
                String s = t.toString();
                Object o = fParms.get(s);
                // TODO -- if o == null?
                if (o instanceof Token)
                {
                    fT = (Token)o;
                    return;
                }
                else
                {
                    fState = 0;
                    fReplace = (Object[])o;
                }
            }
            else
            {
                fT = t;
                return;
            }
        }
        
        // TODO -- do variable interpolation in text strings.
        
        fT = (Token)fReplace[fState++];   
        
        
    }
    
    
    private __TokenIterator fBase;
    private HashMap<String, Object> fParms;
    private Token fT;
    private Object[] fReplace;
    private int fState;

    @Override
    public boolean Contains(int type)
    {
        return false;
    }

    @Override
    public boolean Contains(int... types)
    {
        return false;
    }
}
