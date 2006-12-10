/*
 * Created on Dec 5, 2006
 * Dec 5, 2006 9:54:36 PM
 */
package asm816;

import java.util.ArrayList;

public class TokenIterator extends __TokenIterator
{
    public TokenIterator()
    {
        fList = null;
        fIndex = 0;
    }
    
    public TokenIterator(ArrayList<Token> list)
    {
        fList = list;
        fIndex = 0;
    }
    public void Reset()
    {
        fIndex = 0;
    }
    public boolean EndOfLine()
    {
        return fList == null ? true : fIndex >= fList.size();
    }
    
    public Token Peek()
    {
        int size = fList == null ? 0 : fList.size();
        if (fIndex >= size)
            return Lexer.Token_EOL;
        
        return fList.get(fIndex);
    }
    
    public Token Next()
    {
      
        int size = fList == null ? 0 : fList.size();
        if (fIndex >= size)
            return Lexer.Token_EOL;
        
        return fList.get(fIndex++);        
    }
    
    int fIndex;
    private ArrayList<Token> fList;
}
