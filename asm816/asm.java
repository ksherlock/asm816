package asm816;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import orca.Orca_Lexer;
import orca.Orca_Parser;

import mpw.MPW_Parser;

/*
 * Created on Mar 15, 2006
 * Mar 15, 2006 10:37:34 PM
 */

public class asm
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        String outf = null;
        ArrayList<String> fIncDirs;
        HashMap<String,String> fDefines;
        
        fDefines = new HashMap<String, String>();
        fIncDirs = new ArrayList<String>();
        
        GetOpt go = new GetOpt(args, "o:I:D:");
        int c;
        while ((c = go.Next()) != -1)
        {
            switch(c)
            {
            case 'o':
                outf = go.Argument();
                break;
            case 'D': // -Dvar=value
                {
                    String s = go.Argument();
                    int i;
                    i = s.indexOf('=');
                    if (i == -1)
                        fDefines.put(s, "1");
                    else if (i == 0) ;
                    else
                    {
                        fDefines.put(s.substring(0, i), 
                                s.substring(i + 1));
                    }
                
                }
            case 'I': // -I include path
                fIncDirs.add(go.Argument());
                break;
            }
        
        }
        args = go.CommandLine();
        go = null;
        if (args.length > 1)
        {
            outf = null; // multiple files, can't use -o
        }
        if (args.length == 0)
        {
            // stdin.
            if (outf == null || outf.length() == 0)
            {
                outf = "gsout.o";
            }
            try
            {
                FileOutputStream fout = new FileOutputStream(outf);
                Orca_Parser p = new Orca_Parser(fout);
                Orca_Lexer lex = new Orca_Lexer(System.in);
                p.Parse(lex);
            }
            catch (FileNotFoundException e)
            {
            }
            
        }
        else
        {
            if (outf != null && outf.length() == 0) outf = null;
            for (int i = 0; i < args.length; i++)
            {
                FileInputStream fin;
                FileOutputStream fout;
                try
                {
                    fin = new FileInputStream(args[i]);
                    
                    if (outf != null) fout = new FileOutputStream(outf);
                    else
                    {
                        File inf = new File(args[i]);
                        String name = inf.getName();
                        int idx = name.lastIndexOf('.');
                        if (idx == -1)
                            name = inf + ".o";
                        else
                        {
                            name = name.substring(0, idx) + ".o";
                        }
                        fout = new FileOutputStream(name);
                    }
                    MPW_Parser p = new MPW_Parser();
                    p.ParseFile(new File(args[i]));
                    /*
                    Orca_Lexer lex = new Orca_Lexer(fin);
                    Orca_Parser p = new Orca_Parser(fout);
                    p.Parse(lex);
                    */
                }
                catch (FileNotFoundException e)
                {
                }
                
            }
        }

    }

}
