package asm816;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

/*
 * Created on Feb 24, 2006
 * Feb 24, 2006 2:33:50 AM
 */

public abstract class Lexer
{
    private Reader fData;

    private int fLine;
    private int fColumn;
    protected Token fToken;
    protected Token fLast;
    protected boolean fMacro;
    boolean fEof;
    protected boolean fCase;
    
    int[] fPushBack;
    int fPushPtr;

    private boolean fIWS;

    protected String fLocalLabel;
    
    protected static final Token Token_EOL = new Token(Token.EOL);
    protected static final Token Token_EOF = new Token(Token.EOF);
    
    public Lexer(InputStream io)
    {
        this(new InputStreamReader(io));
    }
    public Lexer(String s)
    {
        this(new StringReader(s));
    }
    public Lexer(Reader io)
    {
        fData = io;
        fColumn = 0;
        fLine = 1;
        fToken = null;
        fLast = null;
        fEof = false;
        fCase = false;
        
        fPushBack = new int[8];
        fPushPtr = -1;
        fLocalLabel = "";
    }
    
    public void SetCase(boolean tf)
    {
        fCase = tf;
    }

    public void IgnoreWhiteSpace(boolean bool)
    {
        fIWS = bool;
    }

    public void SetLocalLabel(String lab)
    {
        if (lab == null) lab = "";
        fLocalLabel = lab;
    }    
    
    public int Line()
    {
        return fLine;
    }
    public int Column()
    {
        return fColumn;
    }

    public Token LastToken()
    {
        return fLast;
    }    
 
    public __TokenIterator Arguments(boolean ignorespace) throws AsmException
    {
        ArrayList<Token> out = new ArrayList<Token>();
 
        Token t = Expect(Token.EOL, Token.SPACE);
        int type = t.Type();
        // null or a blank Array?
        if (type == Token.EOL) return new NullTokenIterator();
        
        for(;;)
        {
            t = NextToken();
            type = t.Type();
            if (ignorespace && type == Token.SPACE) 
                continue;
            if (type == Token.EOL)
                break;
            out.add(t);
        }
        
        return new TokenIterator(out);
    }    
    
    
    /*
     * returns 1-based index
     * 
     */
    public int ExpectSymbol(String name) throws AsmException
    {
        Token t = Expect(Token.SYMBOL);
        return t.ExpectSymbol(name);
    }

    public int ExpectSymbol(String... names) throws AsmException
    {
        Token t = Expect(Token.SYMBOL);
        return t.ExpectSymbol(names);
    }    
    
    public Token Expect(int[] arg) throws AsmException
    {
        Token t = NextToken();
        t.Expect(arg);
        return t;
    }
    public Token Expect(char c) throws AsmException
    {
        return Expect((int)c);
    }
    public Token Expect(Integer... arg) throws AsmException
    {
        Token t = NextToken();
        t.Expect(arg);
        return t;
    }    
    /*
     * front end to collapse multiple EOLs
     * not sure if needed ... it definitely could make Peek() useless in some 
     * instances.
     */
    public Token NextToken() throws AsmException
    {
        Token t;
        if (fEof) return Token_EOF;
        
        for(;;)
        {
            t = (fToken == null) ? __NextToken() : fToken;
            fToken = null;
            fLast = t;
            
            if (fIWS && t.Type() == Token.SPACE) continue;
            break;
        }
        
        // return EOL prior to EOF.
        if (t.Type() == Token.EOF)
        {
            fEof = true;
            fToken = Token_EOF; // return it next time, too.
            if (fLast == null || fLast.Type() != Token.EOL)
            {
                return Token_EOL;
            }
            return Token_EOF;
        }
        /*
        if (t.Type() == Token.EOL)
        {
            do
            {
                fToken = __NextToken();
            }
            while (fToken.Type() == Token.EOL);
        }
        */

        return t;
    }    

    
    
    
    public int NextChar()
    {
        int rv;
               
        if (fPushPtr != -1)
        {
            rv = fPushBack[fPushPtr--];
        }
        else rv = __NextChar();
        
        fColumn++;
        return rv;
    }

    private int __NextChar()
    {
        if (fEof) return EOF;
        
        try
        {

            return fData.read();
        }
        catch (IOException e)
        {
            return EOF;
        }

    }
    
    
    public int Peek()
    {
        if (fPushPtr != -1)
            return fPushBack[fPushPtr];
        
        int rv = fPushBack[++fPushPtr] = __NextChar();
        return rv;
    }
    public void Poke(int c)
    {
        if (fPushPtr < 8)
        {
            fPushBack[++fPushPtr] = c;
            fColumn--;
        }
    }   
    
    // skip ahead past the end of the line
    // this handles \r, \n, or any combination thereof.
    public void SkipLine()
    {
        int c;
        while(true)
        {
            c = NextChar();
            if (c == EOF) return;
            
            
            if (c == '\r')
            {
                c = NextChar();
                if (c != '\n') Poke(c);
                break;
            }
            else if (c == '\n')
            {
                c = NextChar();
                if (c != '\r') Poke(c);    
                break;
            }                
        }
        fIWS = false;
        fLine++;
        fColumn = 0;
    }
    
    abstract protected Token __NextToken() throws AsmException;   
    


    protected static final int EOF = -1;
    
}
