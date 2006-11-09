/*
 * Created on Mar 4, 2006
 * Mar 4, 2006 11:47:16 PM
 */

public class AsmException extends Exception
{



    public AsmException(Error error)
    {      
        this(error, null);
    }
    public AsmException(Error error, Token t)
    {
        fError = error;
        fToken = t;
    }
    
    public String toString()
    {
        StringBuffer s = new StringBuffer();
        
        switch (fError)
        {
        case E_UNTERM_STRING:
            s.append("Unterminated string");
            break;
        case E_EXPRESSION:
            s.append("Expression too complicated.");
            break;
        case E_UNEXPECTED:
            s.append("Unexpected ");
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
                break;
            case Token.SPACE:
                s.append("space");
                break;
            case Token.STRING:
                s.append("string");
                break;
            case Token.SYMBOL:
                s.append("symbol");
                break;
            default:
                s.append((char)fToken.Value());
            
            }
            break;
        }
        return s.toString();
    }
    
    private static final long serialVersionUID = 1L;
    private Error fError;
    private Token fToken;
}
