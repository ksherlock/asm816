package mpw;
import java.io.InputStream;
import java.util.ArrayList;

import asm816.AsmException;
import asm816.Error;
import asm816.Lexer;
import asm816.Token;
import asm816.TokenIterator;
import asm816.__TokenIterator;
import asm816.ctype;

/*
 * Created on Nov 30, 2006
 * Nov 30, 2006 2:12:38 AM
 */

public class MPW_Lexer extends Lexer
{
    public MPW_Lexer(InputStream io)
    {      
        super(io);
    }
    public MPW_Lexer(String s)
    {
        super(s);
    }
    
    protected Token __NextToken() throws AsmException
    {
        int c = NextChar();

        int next = Peek();
        
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
            {
                if (next == '*')
                {
                    NextChar();
                    return new Token(Token.LOGICAL_AND);
                }
                return new Token(c);  
            }
        case ';':           
        case '\r':
        case '\n':
            Poke(c);
            SkipLine();
            return Token_EOL;
             
        case '<':
            if (next == '<')
            {
                NextChar();
                return new Token(Token.LEFT_SHIFT);
            }
            if (next == '=')
            {
                NextChar();
                return new Token(Token.LE);
            }
            if (next == '>')
            {
                NextChar();
                return new Token(Token.NE);
            }
            return new Token(c);


        case '>':
            if (next == '>')
            {
                NextChar();
                return new Token(Token.RIGHT_SHIFT);
            }
            if (next == '=')
            {
                NextChar();
                return new Token(Token.GE);
            }
            return new Token(c);            
            
        case '-':
            if (next == '-')
            {
                NextChar();
                return new Token(Token.LOGICAL_EOR);
            }
            return new Token(c);
            
        case '+':
            if (next == '+')
            {
                NextChar();
                return new Token(Token.LOGICAL_OR);
            }
            return new Token(c);
            
        case '/':
            if (next == '/')
            {
                NextChar();
                return new Token(Token.MOD);
            }
            return new Token(c);
            
        /* interferes with |absolute mode
        case '|':
            return new Token(Token.LOGICAL_OR);
        */    
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
             * octal number.... or local label.
             */
        case '@':
            {
                int value = 0;
                int i = 0;
                
                if (ctype.isalpha(next) || next == '_')
                {
                    String s = ParseSymbol('@');
                    return new Token(Token.SYMBOL, 
                            fLocalLabel + s , this);
                }
                c = NextChar();
                
                while(ctype.isoctal(c))
                {
                    i++;
                    value <<=  3;
                    value += (c - '0');
                    c = NextChar();
                }
                Poke(c);
                /*
                 * 
                 */
                // TODO -- throw error if i == 0
                return new Token(Token.NUMBER, value, this);
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
        case '\'':
            {
                StringBuffer buff = new StringBuffer();
               
                while(true)
                {
                    c = NextChar();

                    if (c == EOF || c == '\r' || c == '\n')
                    {
                        Poke(c);
                        throw new AsmException(Error.E_UNTERM_STRING, this);
                    }

                    // may be the end or it may be an escaped quote.
                    if (c == '\'')
                    {
                        if (Peek() == '\'')
                        {
                            NextChar();
                        }
                        else
                        {
                            return new Token(Token.STRING, buff.toString(), this);
                        }
                    }
                    buff.append((char)c);                   
                }               
            }
            // never reached.
            

            // Unicode
        case 0xAC: // ¬ &not;
            return new Token(Token.LOGICAL_NOT);
        case 0xf7: // ÷ &divide;
            return new Token('/');
        case 0x2260: // ? &ne;
            return new Token(Token.NE);
        case 0x2264: // ? &le;
            return new Token(Token.LE);
        case 0x2265: // ? &ge;
            return new Token(Token.GE);
        

            /*
             * a symbol or something else to process later.
             */
         default:
             // TODO -- is ~ ok?
            if (ctype.isalpha(c) || c == '_')
            {
                String s = ParseSymbol(c);
 
                return new Token(Token.SYMBOL, s, this);
            } else
                return new Token(c);      
        }   
    }
    
    private String ParseSymbol(int c)
    {
        StringBuffer s = new StringBuffer();

        do
        {
            s.append((char)c);
            c = NextChar();
        }
        while (ctype.isalnum(c) || c == '_' || c == '.');
        /*
         * '.' is allowed to support records.
         */
        Poke (c);
        
        String out = s.toString();
        if (!fCase) out = out.toUpperCase();
        
        return out;
    }    
}

