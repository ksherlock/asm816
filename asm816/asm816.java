package asm816;
import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;

import orca.Orca_Parser;
import mpw.MPW_Parser;
import merlin.Merlin_Parser;

/*
 * Created on Mar 15, 2006
 * Mar 15, 2006 10:37:34 PM
 */

public class asm816
{

    static private void usage()
    {
        System.out.println("asm816 v 0.1");
        System.out.println("usage: asm816 [options] file.asm");
        System.out.println("options");
        System.out.println("\t-Dxxx          Define value");
        System.out.println("\t-I path        include directory");
        System.out.println("\t-o file        save output to file");
        System.out.println("\t-s merlin      Merlin syntax");
        System.out.println("\t-s mpw         MPW IIgs syntax");
        System.out.println("\t-s orca        Orca/APW syntax");       
    }
    
    public static void main(String[] args)
    {
        String outf = null;
        ArrayList<String> fIncludes;
        HashMap<String,String> fDefines;
        
        
        Parser p = null;
        
        
        fDefines = new HashMap<String, String>();
        fIncludes = new ArrayList<String>();
        
        GetOpt go = new GetOpt(args, "o:s:I:D:h");
        int c;
        while ((c = go.Next()) != -1)
        {
            switch(c)
            {
            case 'o':
                outf = go.Argument();
                if (outf.length() == 0) 
                    outf = null;
                break;
            case 's':
                {
                    String s;
                    s = go.Argument();
                    
                    if (s.compareToIgnoreCase("orca") == 0)
                    {
                        p = new Orca_Parser();
                    }
                    else if (s.compareToIgnoreCase("merlin") == 0)
                    {
                        p = new Merlin_Parser();
                    }
                    else if (s.compareToIgnoreCase("mpw") == 0)
                    {
                        p = new MPW_Parser();
                    }
                    else
                    {
                        System.err.printf("asm816: %1$s: unknown syntax\n, s");
                        return;
                    }
                    
                }
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
                {
                    String s = go.Argument();
                    if (!fIncludes.contains(s))
                        fIncludes.add(s);
                }
                break;
            case 'h':
            case '?':
                usage();
                return;
            }
        }
        
        if (p == null)
            p = new MPW_Parser();
        
        p.SetIncludes(fIncludes);
        
        args = go.CommandLine();
        go = null;
        if (args.length > 1)
        {
             outf = null; // multiple files, can't use -o
        }
        
        if (args.length == 0)
        {
            p.Reset();
            p.SetOutFile(outf != null ? outf : "object.o");
            p.ParseFile(System.in);
        }
        else
        {
            for (String s : args)
            {
                File in;
                
                in = new File(s);
                if (!in.exists())
                {
                    System.err.printf("asm816: file %1$s does not exist\n", s);
                    continue;
                }
                if (outf == null)
                {
                    String fname = in.getName();
                    int i = fname.lastIndexOf('.');
                    
                    if (i > 0)
                        fname = fname.substring(0, i);
                    outf = fname + ".o";
                }
                
                p.Reset();
                p.SetOutFile(outf);
                p.ParseFile(in);
                outf = null;
            }
        }
    }

}
