package asm816;
import java.io.PrintStream;

/*
 * Created on Mar 4, 2006
 * Mar 4, 2006 11:47:16 PM
 */

public class AsmException extends Exception
{



    public AsmException(Error error)
    {      
        fError = error;
        fToken = null;
        fString = null;
        fLine = 0;
    }
    public AsmException(Error error, Lexer lex)
    {      
        fError = error;
        fToken = null;
        fString = null;
        fLine = (lex == null) ? 0 : lex.Line();        
    }    
    public AsmException(Error error, Token t)
    {
        fError = error;
        fToken = t;
        fString = null;
        fLine = (t == null) ? 0 : t.Line();
    }
    
    public AsmException(Error error, String s)
    {
        fError = error;
        fString = s;
        fToken = null;
        fLine = 0;
    }
    
    public void SetLine(int line)
    {
        fLine = line;
    }
    
    public void print(PrintStream s)
    {
        s.println(toString());
    }
    public String toString()
    {
        StringBuffer s = new StringBuffer();
        
        if (fLine != 0) s.append(" Line " + fLine + ": ");
        
        switch (fError)
        {
        case E_FILE_NOT_FOUND:
            s.append("File not found: " + fString);
            break;
        case E_LABEL_REQUIRED:
            s.append("Label is required"); 
            if (fString != null)
                s.append(" for " + fString);
            // todo -- need to mention which directive.
            break;
        case E_ALIGN:
            s.append("Invalid ALIGN value");
            break;
        case E_UNTERM_STRING:
            s.append("Unterminated string");
            break;
        case E_EXPRESSION:
            s.append("Expression too complicated.");
            break;
        case E_UNEXPECTED:
            s.append("Unexpected ");
            if (fToken != null)
            {
                switch (fToken.Type())
                {
                case Token.EOF:
                    s.append("end of file");
                    break;
                case Token.EOL:
                    s.append("end of line");
                    break;
                case Token.NUMBER:
                    s.append("number");
                    s.append(" -- " + fToken.Value());
                    break;
                case Token.SPACE:
                    s.append("space");
                    break;
                case Token.STRING:
                    s.append("string");
                    s.append(" -- " + fToken.toString());
                    break;
                case Token.SYMBOL:
                    s.append("symbol");
                    s.append(" -- " + fToken.toString());
                    break;
                default:
                    s.append("'" + (char)fToken.Value() + "'");
                
                }
                break;
            }
            else if (fString != null)
                s.append(" " + fString);
        }
        return s.toString();
    }
    
    private static final long serialVersionUID = 1L;
    private Error fError;
    private Token fToken;
    private String fString;
    private int fLine;
}
