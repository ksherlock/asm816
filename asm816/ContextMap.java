package asm816;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

/*
 * Container for a hashmap. 
 * 
 * Push() : create a new context
 * Pop() : restore previous context
 * Put() : store item in current context
 * Get() : find item (searches all contexts, newest to oldest)
 */

public class ContextMap<K,V>
{
    public ContextMap()
    {
        fBackbone = null;
        fMap = new HashMap<K, V>();
    }
    
    private ContextMap(ContextMap<K,V> copyFrom)
    {
        fBackbone = copyFrom.fBackbone;
        fMap = copyFrom.fMap;
    }    
    
    public V Get(K key)
    {
        V v;
        if ((v = fMap.get(key)) != null) return v;
        if (fBackbone != null) return fBackbone.Get(key);
        return null;
    }
    
    public boolean Put(K key, V value)
    {
        if (fMap.containsKey(key)) return false;
        fMap.put(key, value);
        return true;
    }     
    
    public Set<Entry<K, V>> Set()
    {
        return fMap.entrySet();
    }
    
    /*
     * Create a new context.
     * 
     */
    public void Push()
    {
        fBackbone = new ContextMap<K, V>(this);
        fMap = new HashMap<K, V>();   
    }
    /*
     * Restore the previous context.
     * 
     */
    public HashMap<K,V> Pop()
    {
        HashMap<K,V> out = fMap;
        
        if (fBackbone == null) return null;
        
        fMap = fBackbone.fMap;
        fBackbone = fBackbone.fBackbone;
        
        return out;
    }
    
    private ContextMap<K,V> fBackbone;
    private HashMap<K,V> fMap;
}
