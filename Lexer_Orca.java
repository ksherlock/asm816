
/*
 * Created on Feb 24, 2006
 * Feb 24, 2006 2:44:16 AM
 */

import java.io.InputStream;

public class Lexer_Orca extends Lexer
{

    public Lexer_Orca(InputStream io)
    {
        super(io);
    }
    public Lexer_Orca(String s)
    {
        super(s);
    }       
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
                   return Token_EOL;
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
            return Token_EOL;
            
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
             * octal number.
             */
        case '@':
            {
                int value = 0;
                int i = 0;
                c = NextChar();
                
                while(ctype.isoctal(c))
                {
                    i++;
                    value <<=  3;
                    value += (c - '0');
                    c = NextChar();
                }
                Poke(c);
                // TODO -- throw error if i == 0
                return new Token(Token.NUMBER, value);
            }
            
            /*
             * binary number
             */
        case '%':
            {
                int value = 0;
                int i = 0;
                c = NextChar();
                
                while(c == '1' || c == '0')
                {
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
                Poke(c);
                
                return new Token(Token.NUMBER, value);
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
                    // TODO -- throw error if \r or \n.
                    // may be the end or it may be an escaped quote.
                    if (c == quote)
                    {
                        if (Peek() == quote)
                        {
                            NextChar();
                        }
                        else
                        {
                            return new Token(Token.STRING, buff.toString());
                        }
                    }
                    buff.append((char)c);                   
                }               
            }
            // never reached.
            
            /*
             * symbolic macro parameter
             */
        case '&':
            if (this.fMacro)
            {
                StringBuffer buff = new StringBuffer();
                while (ctype.isalpha(c = NextChar()))
                {
                    buff.append((char)c);
                }
                Poke(c);
                if (buff.length() > 0) 
                    return new Token(Token.MACRO_PARM, buff.toString());
            }
            return new Token(c);
            
            /*
             * in macro context, .label 
             */
        case '.':
            if (this.fMacro && this.Column() == 0)
            {
                StringBuffer buff = new StringBuffer();
                c = NextChar();
                if (ctype.isalpha(c) || c == '_' || c == '~')
                {
                    do
                    {
                        buff.append((char)c);
                        c = NextChar();
                    }
                    while (ctype.isalnum(c) || c == '_' || c == '~');
                }

                Poke(c);
                if (buff.length() > 0) 
                    return new Token(Token.MACRO_LAB, buff.toString());                
            }
            // TODO -- .NOT., etc.
            return new Token(c);
            /*
             * a symbol or something else to process later.
             */
         default:

            if (ctype.isalpha(c) || c == '_' || c == '~')
            {
                StringBuffer buff = new StringBuffer();
                
                do
                {
                    buff.append((char)c);
                    c = NextChar();
                }
                while (ctype.isalnum(c) || c == '_' || c == '~');
                
                Poke(c);
                return new Token(Token.SYMBOL, buff.toString());
            } else
                return new Token(c);      
        }   
    }
    
}
