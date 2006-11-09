import java.util.ArrayList;

/*
 * Created on Feb 7, 2006
 * Feb 7, 2006 1:25:45 AM
 */

class Stack<E> extends ArrayList<E>
{
    private static final long serialVersionUID = 1L;
    
    public E pop() throws Exception
    {
        int n = this.size();
        if (n < 1) throw new Exception();
        return this.remove(n - 1);
    }

    public void push(E o)
    {
        add(o);
    }
}
