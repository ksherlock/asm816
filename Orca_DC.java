import java.util.ArrayList;

import omf.OMF_Expr;
import omf.OMF_Expression;
import omf.OMF_Label;
import omf.OMF;
import omf.OMF_Strong;

/*
 * Created on Nov 11, 2006
 * Nov 11, 2006 4:08:11 PM
 */

/*
 * these functions parse the string from a DC x statement.
 * they return an object (arraylist, expression, byte[], omf_opcode)
 */
public class Orca_DC
{
    private Orca_DC()
    {
    }
    
    /*
     * character constant.
     * return the string data as is, but may set MSB.
     */
    public static Object DC_C(String s, boolean MSB)
    {
        // if fMSB is true, we must set bit 8 on all chars.
        if (MSB)
        {
            int i;
            int l = s.length();
            StringBuffer s2 = new StringBuffer(s);
            for (i = 0; i < s.length(); i++)
            {
                char c = s.charAt(i);
                s2.setCharAt(i, (char)(c | 0x80));
            }
            s = s2.toString();
        }
        return s.getBytes();        
    }

    /*
     * Hex constant.  
     * if odd number of digits, last digit is 0 padded on the right.
     *
     * String: [ HEXDIGIT | SPACE]+
     */
    public static Object DC_H(String s)
    {
        ByteBuffer buffer = new ByteBuffer();
        int l = s.length();
        int i = 0;
        int temp = 0;
        boolean first = true;
        
        for (i = 0; i < l; i++)
        {
            int c = s.charAt(i);
            if (ctype.isspace(c)) continue;
            
            if (!ctype.isxdigit(c)) continue; /* error */
            
            if (first)
            {
                temp = ctype.toint(c);
                first = false;
            }
            else
            {
                temp = (temp << 4) | ctype.toint(c);
                buffer.append(temp);
                first = true;
            }
        }
        if (!first)
        {
            temp = temp << 4;
            buffer.append(temp);
        }
        return buffer.getBytes();
    }
    
    
    /*
     * binary constant.  
     * if < 8 bits, it is right padded with 0s.
     * 
     * String: [1 | 0 | SPACE ]+
     */
    public static Object DC_B(String s)
    {
        ByteBuffer buffer = new ByteBuffer();
        int l = s.length();
        int pos = 0;
        int temp = 0;
        for (int i = 0; i < l; i++)
        {
            int c = s.charAt(i);
            if (ctype.isspace(c)) continue;
            
            
            temp = temp << 1;
            if (c == '1') temp |= 0x01;
            else if (c == '0') ;
            else ; /* error */
            if (pos++ == 7)
            {
                buffer.append(temp);
                pos = 0;
            }            
        }
        
        if (pos > 0)
        {
            temp = temp << (7-pos);
            buffer.append(temp);
        }
        
        return buffer.getBytes();
    }    
 

    /* 
     * integer constant (also address)
     * String: expression [, expression]+
     * 
     */
    public static Object DC_I(String s, int size, int pc, boolean case_sensitive, int mod) throws AsmException
    {
        Lexer_Orca lex = new Lexer_Orca(s);        
        int c;
        if (size == 0) size = 2;
        ArrayList<Expression> out = new ArrayList<Expression>();
        for(;;)
        {
            Expression ex = new Expression(case_sensitive);
            ex.ParseExpression(lex);
            
            if (mod == '^')
                ex.Shift(-16);
            else if (mod == '>')
                ex.Shift(-8);
            
            ex.SetSize(size);
            ex.SetPC(pc);

            
            
            out.add(ex);
            
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                continue;
            }
            break;
        }
        
        lex.Expect(Token.EOL, Token.EOF);
        
        return out;
    }
    
    /*
     * reference an address.
     * String: label [, label]
     */
    
    public static Object DC_R(String s, boolean case_sensitive) throws AsmException
    {
        Lexer_Orca lex = new Lexer_Orca(s);
        Token t;
        int c;
        
        ArrayList<OMF_Strong> out = new ArrayList<OMF_Strong>();
        for(;;)
        {           
            t = lex.Expect(Token.SYMBOL);
 
            s = t.toString();
            if (!case_sensitive)
                s = s.toUpperCase();
            out.add(new OMF_Strong(s));

            
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                continue;
            }
            break;
        }
        t = lex.Expect(Token.EOL, Token.EOF);
        
        return out;
    }
    /*
     * soft reference an address.
     * String: label [, label]
     * 
     * Note - orca docs list this as expression [, expression]
     */
    
    @SuppressWarnings("unchecked")
    public static Object DC_S(String s, int size, boolean case_sensitive) throws AsmException
    {
        Lexer_Orca lex = new Lexer_Orca(s);
        Token t;
        int c;
        
        
        ArrayList<OMF_Expr> out = new ArrayList<OMF_Expr>();
        
        if (size < 1) size = 2;
        
        for(;;)
        {           
            t = lex.Expect(Token.SYMBOL);
 
            ArrayList tmp = new ArrayList();
            s = t.toString();
            if (!case_sensitive)
                s = s.toUpperCase();
            
            
            tmp.add(new OMF_Label(OMF_Expression.EXPR_WEAK, s));
            tmp.add(new Integer(OMF_Expression.EXPR_END));
            out.add(new OMF_Expr(OMF.OMF_EXPR, size, tmp));
            
            c = lex.Peek();
            if (c == ',')
            {
                lex.NextChar();
                continue;
            }
            break;
        }
        t = lex.Expect(Token.EOL, Token.EOF);
        
        return out;
    }    
    
}
