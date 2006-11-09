import java.io.InputStream;

/*
 * Created on Nov 8, 2006
 * Nov 8, 2006 5:10:02 PM
 */

public class Lexer_Merlin extends Lexer
{
    public Lexer_Merlin(InputStream io)
    {
        super(io);
    }

    @Override
    protected Token __NextToken()
    {
        int c = NextChar();
        
        switch(c)
        {
        
        /*
         * hexadecimal number.
         */
    case '$':
        {
            int value = 0;
            int i = 0;
            c = NextChar();
            
            while(ctype.isxdigit(c))
            {
                i++;
                value <<=  4;
                value += ctype.toint(c);
                c = NextChar();
            }
            Poke(c);
            // TODO -- throw error if i == 0
            return new Token(Token.NUMBER, value);
        }        
        /*
         * binary number.  _ is allowed (and ignored)
         */
        case '%':
            {
            int value = 0;
            int i = 0;
            c = NextChar();
            
            while(c == '1' || c == '0' || c == '_')
            {
                if (c == '_') continue;
                
                i++;
                value <<=  1;
                value += (c - '0');
                c = NextChar();
            }
            Poke(c);
            // TODO -- throw error if i == 0
            return new Token(Token.NUMBER, value);
            }
            /*
             * decimal number.
             */
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            {
                int value = 0;
                int i = 0;
                
                while(ctype.isdigit(c))
                {
                    i++;
                    value *= 10;
                    value += (c - '0');
                    c = NextChar();
                }
                // TODO -- if c == '.' or 'e' then this should be a REAL number??
                Poke(c);
                
                return new Token(Token.NUMBER, value);
            }            
            
        }
        return null;
    }
}
