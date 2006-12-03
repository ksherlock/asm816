package merlin;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import asm816.AsmException;
import asm816.Error;
import asm816.INSTR;
import asm816.JunkPile;
import asm816.Token;
import asm816.ctype;

/*
 * Created on Nov 9, 2006
 * Nov 9, 2006 8:02:12 PM
 */

public class Merlin_Parser
{
    
    private JunkPile fData;
    private HashMap<String, INSTR> fOpcodes;
    private HashMap<String, Merlin_Directive> fDirectives;  
    private FileOutputStream fFile;
    private int fPC;
    private int fError;
    
    public Merlin_Parser()
    {
        fOpcodes = new HashMap<String, INSTR>();
        fDirectives = new HashMap<String, Merlin_Directive>();
        
        
        for (INSTR i: INSTR.values())
        {
            fOpcodes.put(i.name(), i);
        }

        for(Merlin_Directive d: Merlin_Directive.values())
        {
            fDirectives.put(d.name(), d);
        }
        // TODO -- facility for LDAL, et alia

        fError = 0;
        try
        {
            File f = File.createTempFile("asm65816", "tmp");
            
            fFile = new FileOutputStream(f);
        }
        catch (Exception e)
        {}        
    }
    
    void Parse(Merlin_Lexer lex)
    {
        Token t;
        String lab;
        String s;
        
        fPC = 0;
        
        for(;;)
        {
            try
            {
                
                /*
                 * [label] EOL
                 * [label] opcode ... EOL
                 *  opcode ... EOL
                 */
                // [label] space opcode  
                t = lex.NextToken();
                lab = null;
                switch (t.Type())
                {
                case Token.EOF:
                    return;
                case Token.EOL:
                    break; 
                case Token.SPACE:
                    break;
                case Token.SYMBOL:
                    lab = t.toString();
                    t = lex.Expect(Token.EOL, Token.SPACE);
                    break;
                default:
                    throw new AsmException(Error.E_UNEXPECTED, t);                
                }
                
                if (lab != null)
                {
                    // add to table.
                }
                
                if (t.Type() == Token.EOL)
                {
                    continue;
                }
                
                t = lex.Expect(Token.SYMBOL);
                // check directives, instructions, macros,
                // then truncate to 3 chars and repeat iff needed.
                
                s = t.toString().toUpperCase();
                
                for (int foo = 0; foo < 2; foo++)
                {
                    
                    Merlin_Directive dir = fDirectives.get(s);
                    if (dir != null)
                    {
                        DoDirective(lex, dir);
                        break;
                    }
                    INSTR instr = fOpcodes.get(s);
                    if (instr != null)
                    {
                        DoInstr(lex, instr);
                        break;
                    }
                    // TODO -- macros.
                    if (s.length() > 3)
                    {
                        s = s.substring(0, 3);
                    }
                    else break;
                }
                
                
            }
            catch (AsmException e)
            {

            }
        }       
    }
    
    private void DoDirective(Merlin_Lexer lex, Merlin_Directive dir) throws AsmException
    {
        switch(dir)
        {
        case ASC:
            DoASC(lex);
            break;
        case HEX:
            DoHex(lex, true);
            break;
        case REV:
            DoREV(lex);
            break;
        case STR:
            DoSTR(lex, false);
            break;
        case STRL:
            DoSTR(lex, true);
            break;
            
        }
    }
    private void DoInstr(Merlin_Lexer lex, INSTR instr)
    {
        
    }
    
    private void DoASC(Merlin_Lexer lex) throws AsmException
    {
        Token t;
        String s;
        int c;
        
        
        t = lex.Expect(Token.SPACE);
        t = lex.ParseString();
        
        s = t.toString();
        
        fData.add(s);
        fPC += s.length();
        
        // may have hexdata afterwards.
        c = lex.Peek();
        if (c == ',')
        {
            lex.NextChar();
            DoHex(lex, false);
        }
        lex.Expect(Token.EOL);
    }
    
    private void DoSTR(Merlin_Lexer lex, boolean l) throws AsmException
    {
        Token t;
        String s;
        int c;
        
        t = lex.Expect(Token.SPACE);
        t = lex.ParseString();
        s = t.toString();
        
        if (l) // longword
        {
            // add in 16-bit length.
            int len = s.length();           
            fData.add16(len, 0);
            fPC += 2;
        }
        else
        {
            // 8-bit length
            s = s.substring(0, 255);
            fData.add8(s.length(), 0);
            fPC += 1;
        }
        
        fData.add(s);
        fPC += s.length();
        
        // may have hexdata afterwards.
        c = lex.Peek();
        if (c == ',')
        {
            lex.NextChar();
            DoHex(lex, false);
        }
        lex.Expect(Token.EOL);        
    }
    
    private void DoREV(Merlin_Lexer lex) throws AsmException
    {
        Token t;
        StringBuffer sb;
        
        t = lex.Expect(Token.SPACE);
        t = lex.ParseString(); 
        lex.Expect(Token.EOL);
        
        sb = new StringBuffer(t.toString());
        
        sb.reverse();
        fData.add(sb.toString());
        fPC += sb.length();
    }
    
    private void DoHex(Merlin_Lexer lex, boolean isHex) throws AsmException
    {
        int c;      
        
        if (isHex)
        {
            lex.Expect(Token.SPACE);
        }
        
        for (;;)
        {
            int value = 0;
            c = lex.NextChar(); 
            if (!ctype.isxdigit(c)) break;
            value = ctype.toint(c);
            
            c = lex.NextChar();
            if (!ctype.isxdigit(c)) 
            {
                // TODO -- error?
                fData.add((byte)value);
                fPC++;
                break;
            }
            value <<= 4;
            value += ctype.toint(c);           

            // store the value.
            fData.add((byte)value);
            fPC++;           
            
            c = lex.NextChar();
            if (c == ',') continue;
            if (ctype.isxdigit(c))
            {
                lex.Poke(c);
                continue;
            }
            break;
        }
               
        lex.Poke(c);
        
        if (isHex)
        {
            lex.Expect(Token.EOL);
        }
    }
    
    private void Save()
    {
        
        // go through the arraylist and reduce any expressions,
        // then save it to disk.
    }
}
