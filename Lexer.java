import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

/*
 * Created on Feb 24, 2006
 * Feb 24, 2006 2:33:50 AM
 */

public abstract class Lexer
{
    private Reader fData;
    private int fPeek;
    private int fLine;
    private int fColumn;
    protected Token fToken;
    protected Token fLast;
    protected boolean fMacro;
    
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
        fPeek = UNKNOWN;
        fColumn = 0;
        fLine = 1;
        fToken = null;
        fLast = null;
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
    public Token NextToken()
    {
        Token t;
        t = fToken == null ? __NextToken() : fToken;
        fToken = null;
        
        if (t.Type() == Token.EOL)
        {
            do
            {
                fToken = __NextToken();
            }
            while (fToken.Type() == Token.EOL);
        }
        fLast = t;
        return t;
    }    

    
    
    
    public int NextChar()
    {
        int rv;
        if (fPeek == EOF) return EOF;
        
        rv = (fPeek == UNKNOWN) ? __NextChar() : fPeek;
        
        fPeek = UNKNOWN;
        fColumn++;
        
        return rv;       
    }

    private int __NextChar()
    {

        try
        {
            return fData.read();
        } catch (IOException e)
        {
            return -1;
        }

    }
    
    
    public int Peek()
    {
        if (fPeek == UNKNOWN)
        {
            fPeek = __NextChar();           
        }
        
        return fPeek;
    }
    public void Poke(int c)
    {
        fPeek = c;
        fColumn--;
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
        fLine++;
        fColumn = 0;
    }
    
    abstract protected Token __NextToken();   
    
    protected static final int EOF = -1;
    private static final int UNKNOWN = -2;
    
}
