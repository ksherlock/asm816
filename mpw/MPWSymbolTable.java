package mpw;
import java.util.ArrayList;

import asm816.SymbolTable;

import expression.__Expression;

/*
 * Customized to automatically try any WITH names. This makes
 * everything simpler.
 * 
 */

public class MPWSymbolTable extends SymbolTable
{
    
    MPWSymbolTable()
    {
        super();
        fWith = new ArrayList<String>();
        fMark = new ArrayList<Integer>();
    }
    
    void ClearWith()
    {
        fWith.clear();
        fMark.clear();
    }
    
    void PushWith()
    {
        int size = fWith.size();
        fMark.add(new Integer(size));
    }
    
    void PopWith()
    {
        int size = fMark.size();
        if (fMark.size() > 0)
        {
            Integer I = fMark.remove(size - 1);
            int targetSize = I.intValue();
            
            size = fWith.size();
            while (size > targetSize)
            {
                fWith.remove(--size);
            }
        }
    }
    
    void AddWith(String with)
    {
        fWith.add(with);
    }

    
    /*
     * since we explicitely call super, it won't recurse.
     */
    public __Expression Get(String name)
    {
        int size = fWith.size();
        
        __Expression e = null;
        
        while (size > 0)
        {
           String w = fWith.get(--size); 
            
           e = super.Get(w + "." + name);
           if (e != null) return e;
        }

        // if all else fails, try the default name.
        return super.Get(name);   
    }
    
    private ArrayList<String> fWith;
    private ArrayList<Integer> fMark;
 
}
