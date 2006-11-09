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
    };