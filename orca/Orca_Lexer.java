package orca;

/*
 * Created on Feb 24, 2006
 * Feb 24, 2006 2:44:16 AM
 */

import java.io.InputStream;

import asm816.AsmException;
import asm816.Error;
import asm816.Lexer;
import asm816.Token;
import asm816.ctype;

public class Orca_Lexer extends Lexer
{

    public Orca_Lexer(InputStream io)
    {
        super(io);
    }
    public Orca_Lexer(String s)
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
                return new Token(Token.NUMBER, value, this);
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
        case '"':
        case '\'':
            {
                StringBuffer buff = new StringBuffer();
                int quote = c; // keep track of the quote char.
                
                while(true)
                {
                    c = NextChar();

                    if (c == EOF || c == '\r' || c == '\n')
                    {
                        Poke(c);
                        throw new AsmException(Error.E_UNTERM_STRING, this);
                    }

                    // may be the end or it may be an escaped quote.
                    if (c == quote)
                    {
                        if (Peek() == quote)
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
            
            /*
             * symbolic macro parameter
             */
        case '&':
            {   
                if (ctype.isalpha(next) || next == '_' || next == '~')
                {
                    String s = ParseSymbol(c);
                    return new Token(Token.MACRO_PARM, s, this);
    
                }
                return new Token(c);
            }
            
            /*
             * might be .label or might be .AND. .OR. .NOT. etc
             */
        case '.':
            {
               if (ctype.isalpha(next) || next == '_' || next == '~')
                {
                    String s = ParseSymbol(c);
                    next = Peek();
                    if (next == '.')
                    {
                        int type = -1;
                        if (s.equalsIgnoreCase(".AND"))
                            type = Token.LOGICAL_AND;
                        else if (s.equalsIgnoreCase(".EOR"))
                            type = Token.LOGICAL_EOR;
                        else if (s.equalsIgnoreCase(".NOT"))
                            type = Token.LOGICAL_NOT;
                        else if (s.equalsIgnoreCase(".OR"))
                            type = Token.LOGICAL_OR;
                        
                        if (type != -1)
                        {
                            NextChar(); // skip over .
                            return new Token(type, this);
                        }
                    }
                    return new Token(Token.MACRO_LAB, s, this);
                }
                return new Token(c, this);
            }
            
        case '<':
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
            if (next == '=')
            {
                NextChar();
                return new Token(Token.GE);
            }
            return new Token(c);

            
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
    
    private String ParseSymbol(int c)
    {
        StringBuffer s = new StringBuffer();

        do
        {
            s.append((char)c);
            c = NextChar();
        }
        while (ctype.isalnum(c) || c == '_' || c == '~');
        /*
         * '.' is allowed to support records.
         */
        Poke (c);
        
        String out = s.toString();
        if (!fCase) out = out.toUpperCase();
        
        return out;
    } 
    
}
