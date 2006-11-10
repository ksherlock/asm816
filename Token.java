/*
 * Created on Feb 24, 2006
 * Feb 24, 2006 2:31:33 AM
 */

public final class Token
{
    private int fType;
    private int fValue;
    private String fString;

    public Token(int type)
    {
        fType = type;
        fValue = 0;
        fString = null;
    }
    public Token(int type, String string)
    {
        fType = type;
        fValue = 0;
        fString = string;
    }
    public Token(int type, int value)
    {
        fType = type;
        fValue = value;
        fString = null;
    }   
    public String toString()
    {
        return fString;
    }
    public int Value()
    {
        if (fType == STRING)
        {
            int l = fString.length();
            int v = 0;
            for (int i = 0; i < l; i++)
            {
                v = v << 8;
                v |= (fString.charAt(i) & 0xff);
            }
            return v;
        }
        return fValue;
    }
    public int Type()
    {
        return fType;
    }
    
    public int Register()
    {
        if (fType != STRING) return 0;
        if (fString.length() != 1) return 0;
        int c = ctype.tolower(fString.charAt(0));
        if (c == 'a' || c == 'x' || c == 'y' || c == 's') return c;
        return 0;
    }
    
    /*
     * throw an error if this isn't the expected token type.
     */
    public void Expect(Integer... arg) throws AsmException
    {
        for (int i = 0; i < arg.length; i++)
        {
            int type = arg[i].intValue();
            if (type == fType) return;
        }
        throw new AsmException(Error.E_UNEXPECTED, this);
    }
    public void Expect(int t1) throws AsmException
    {
        if (fType != t1) 
            throw new AsmException(Error.E_UNEXPECTED, this);
    }
    public void Expect(int t1, int t2) throws AsmException
    {
        if (fType != t1 && fType != t2)
            throw new AsmException(Error.E_UNEXPECTED, this);
    }
    public void Expect(int t1, int t2, int t3) throws AsmException
    {
        if (fType != t1 && fType != t2 && fType != t3)
            throw new AsmException(Error.E_UNEXPECTED, this);
    }
    public void Expect(int[] arg) throws AsmException
    {
        for(int type: arg)
        {
            if (type == fType) return;
        }
       throw new AsmException(Error.E_UNEXPECTED, this);
    }
    
    public static final int EOF = -1;
    public static final int SPACE = 256;
    public static final int NUMBER = 257;
    public static final int SYMBOL = 258;
    public static final int EOL = 259;
    public static final int STRING = 260;
    public static final int MACRO_PARM = 261;
    public static final int MACRO_LAB = 262;
}
