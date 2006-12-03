package asm816;
    public enum AddressMode
    {
        IMPLIED,
        IMMEDIATE,
        ABS,
        ABS_X,
        
        ABS_Y,
        ABSLONG,
        ABSLONG_X,
        DP,
        
        DP_X,           // dp,x
        DP_Y,           // dp,y
        INDIRECT,       // (dp)
        LINDIRECT,      // [dp]
        
        INDIRECT_X,     // (dp,x)
        INDIRECT_Y,     // (dp),y
        INDIRECT_S_Y,   // (dp,s),y
        LINDIRECT_Y,    // [dp],y
        
        STACK,          // #,s
        
        /*
         * these are for cases where a no qualifier is given.
         * Therefore, we must base it on operand size, 
         * check other modes, etc.
         */
        ASSUMED_ABS,
        ASSUMED_ABS_X,
        ASSUMED_ABS_Y
    };