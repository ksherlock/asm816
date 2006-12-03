package expression;

/*
 * container for expressions that are simply the program counter. 
 *
 */

public final class RelExpression extends MExpression
{
    public RelExpression(int value)
    {
        super(value, true);
    }
}
