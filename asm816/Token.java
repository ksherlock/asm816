package asm816;
/*
 * Created on Feb 24, 2006
 * Feb 24, 2006 2:31:33 AM
 */

public final class Token
{
    private int fType;
    private int fValue;
    private String fString;
    private int fLine;

    public Token(int type)
    {
        fType = type;
        fValue = 0;
        fString = null;
        fLine = 0;
        if (ctype.isprint(type))
            fString = new String( new byte[] {(byte)type});
    }
    public Token(int type, Lexer lex)
    {
        fType = type;
        fValue = 0;
        fString = null;
        fLine = lex == null ? 0 : lex.Line();
    }
    public Token(int type, String string, Lexer lex)
    {
        this(type, lex);
        fString = string;
    }
    public Token(int type, int value, Lexer lex)
    {
        this(type, lex);
        fValue = value;
    }  
    
    public String toString()
    {
        return fString;
    }
    public int Line()
    {
        return fLine;
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
    
    /*
     * checks if a token is a register
     * returns 'a', 'x', 'y', 's', or 0.
     */
    public int Register()
    {
        if (fType != SYMBOL) return 0;
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
    
    public int ExpectSymbol(String name) throws AsmException
    {
        if (fType == SYMBOL)
        {
            if (fString.compareToIgnoreCase(name) == 0)
                return 1;
        }
        throw new AsmException(Error.E_UNEXPECTED, this);
    }
    public int ExpectSymbol(String... names) throws AsmException
    {
        if (fType == SYMBOL)
        {
            for (int i = 0; i < names.length; i++)
            {
                if (fString.compareToIgnoreCase(names[i]) == 0)
                    return i + 1;
            }
        }
        throw new AsmException(Error.E_UNEXPECTED, this);
    }    
    
    public static final int EOF = -1;
    public static final int SPACE = 256;
    public static final int NUMBER = 257;
    public static final int SYMBOL = 258;
    public static final int EOL = 259;
    public static final int STRING = 260;

    
    

    
    // multi-byte expression operators
    public static final int LOGICAL_AND = 261;
    public static final int LOGICAL_OR = 262;
    public static final int LOGICAL_NOT = 263;
    public static final int LOGICAL_EOR = 264;
    
    public static final int LE = 265;
    public static final int GE = 266;
    public static final int GT = '>';
    public static final int LT = '<';
    
    public static final int LEFT_SHIFT = 267;
    public static final int RIGHT_SHIFT = 268;
    
    public static final int EQ = '=';
    public static final int NE = 269;
    
    public static final int MOD = 270;
    
    
    
    public static final int MACRO_PARM = 300;
    public static final int MACRO_LAB = 301;
    
}
