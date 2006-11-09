import java.util.ArrayList;
import java.util.HashMap;

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
            //if (outf == null) 
            //{
            //   System.err.println("output file not specified.");
            //    return;
            //}
            Lexer_Orca lex = new Lexer_Orca(System.in);
            Parser p = new Parser();
            p.Parse(lex);
        }
        else
        {
            
        }

    }

}
