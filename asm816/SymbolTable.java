package asm816;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import expression.__Expression;

/*
 * Created on Nov 30, 2006
 * Nov 30, 2006 5:30:02 AM
 */

public class SymbolTable extends ContextMap<String, __Expression>
{
    public void PrintTable(PrintStream ps)
    {
        Set<Entry<String, __Expression>> set;
        
        // TODO -- needs to be alphabatized....
        //which means sorting into an array,
        // or an alphabetized binary tree...
        
        //AlphaTree<__Expression> at = new AlphaTree<__Expression>();
 
        
        class SCompare implements Comparator<String>
        {
            public int compare(String arg0, String arg1)
            {
                return arg0.compareTo(arg1);
            }
            
        }
        TreeMap<String, __Expression> tm;

        tm = new TreeMap<String, __Expression>(new SCompare());
        set = Set();
        for (Entry<String, __Expression> e : set)
        {
            tm.put(e.getKey(), e.getValue());
        }
          
        set = tm.entrySet();
       
        for (Entry<String, __Expression> e : set)
        {
            ps.printf("%1$-40s", e.getKey());
            
            e.getValue().PrintValue(ps); 
            ps.println();
        } 
    }
}
