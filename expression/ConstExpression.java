package expression;

/*
 * container for expressions that are simply integers. 
 *
 */

public final class ConstExpression extends MExpression
{
    public ConstExpression(int value)
    {
        super(value, false);
    }
}
