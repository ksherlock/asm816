package merlin;
import java.io.InputStream;

import asm816.Lexer;
import asm816.Token;
import asm816.ctype;

/*
 * Created on Nov 8, 2006
 * Nov 8, 2006 5:10:02 PM
 */

public class Merlin_Lexer extends Lexer
{
    public Merlin_Lexer(InputStream io)
    {
        super(io);
    }

    @Override
    protected Token __NextToken()
    {
        int c = NextChar();
        
        
        switch(c)
        {
        case EOF:
            return Token_EOF;

        case ' ':
        case '\t':
               do
               {
                   c = NextChar();
               }
               while (c == ' ' || c == '\t');

               Poke(c);
               if (c == '\r' || c == '\n' || c == ';')
               {
                   SkipLine();
                   return new Token(Token.EOL);
               }

               return new Token(Token.SPACE);
               
        /*
         * ; -- comment, skip the line
         * * comment if first column, normal char otherwise.
         */
               
        case '*':
            if (Column() > 1)
                return new Token(c);               
        case ';':           
        case '\r':
        case '\n':
            Poke(c);
            SkipLine();
            return new Token(Token.EOL);
            
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
                return new Token(Token.NUMBER, value, this);
            }

            
            /*
             * binary number '_' is allowed and ignored.
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
                return new Token(Token.NUMBER, value, this);
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
                Poke(c);
                
                return new Token(Token.NUMBER, value, this);
            }

            /*
             * character constant -- return as string.
             */
        case '"':
        case '\'':
            {
                StringBuffer buff = new StringBuffer();
                int quote = c; // keep track of the quote char.
                
                while(true)
                {
                    c = NextChar();
                    if (c == EOF)
                    {
                        // TODO -- throw an error.
                        return new Token(Token.EOF);
                    }
                    // may be the end or it may be an escaped quote.
                    if (c == quote)
                    {
                        return new Token(Token.STRING, buff.toString(), this);
                    }
                    if (quote == '"') c |= 0x80; // set the hi-bit
                    buff.append((char)c);                   
                }               
            }            
            
            /*
             * ']' number|letter == variable
             * ']' anything_else == just a ']'
             */
        case ']':
            {
                int next = Peek();
                
                if (ctype.isalnum(next) || next == '_' || next == '~')
                {
                    String s = ParseSymbol(']');
                    
                    return new Token(Token.SYMBOL, s, this);
                }
                return new Token(']');
                
            }
            
            /*
             * : symbol == local label
             */
        case ':':
            {
                int next = Peek();
                if (ctype.isalpha(next) || next == '_' || next == '~')
                {
                    String s = ParseSymbol(':');
                    
                    return new Token(Token.SYMBOL, 
                            fLocalLabel + s , this);
                }
                return new Token(':');
            }
            
            /*
             * a symbol or something else to process later.
             */
         default:

            if (ctype.isalpha(c) || c == '_' || c == '~')
            {
                String s = ParseSymbol(c);
                return new Token(Token.SYMBOL, s, this);
            } else
                return new Token(c);      
        }   
    }
    
    /*
     * *any char may be used as a string delimiter.
     * delimiters < ' will have the hi-bit set, >= '
     * will not.
     * the delimiter may not appear in the string itself. 
     */
    public Token ParseString()
    {
        int c;
        int delim = NextChar();
        
        if (delim == EOF || delim == '\r' || delim == '\n')
        {
            Poke(delim);
            return __NextToken();
        }
        
        StringBuffer buff = new StringBuffer();
        while(true)
        {
            c = NextChar();
            if (c == EOF)
            {
                // TODO -- throw an error.
                return new Token(Token.EOF);
            }
            // may be the end or it may be an escaped quote.
            if (c == delim)
            {
               return new Token(Token.STRING, buff.toString(), this);
            }
            if (delim < (int)'\'')
                c |= 0x80;  // set the hi-bit
            buff.append((char)c);                   
        }               
    }
    /*
     * parse hex data, no leading $.
     */
    public Token ParseHex()
    {
        int c = NextChar();
        int i;
        int value = 0;
        while (ctype.isxdigit(c))
        {
            value = value <<= 4;
            value += ctype.toint(c);
            c = NextChar();
        }
        // todo -- error if i == 0?
        Poke(c);
        return new Token(Token.NUMBER, value, this);
    }
    
    private String ParseSymbol(int c)
    {
        StringBuffer s = new StringBuffer();

        do
        {
            s.append((char)c);
            c = NextChar();
        }
        while (ctype.isalnum(c) || c == '_' || c == '~');

        Poke (c);
        
        String out = s.toString();
        if (!fCase) out = out.toUpperCase();
        
        return out;       
    }
}
