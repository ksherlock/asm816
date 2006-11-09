/*
 * Created on Feb 24, 2006
 * Feb 24, 2006 3:27:21 AM
 */

/*
 * Copyright (c) 1989 The Regents of the University of California.
 * All rights reserved.
 * (c) UNIX System Laboratories, Inc.
 * All or some portions of this file are derived from material licensed
 * to the University of California by American Telephone and Telegraph
 * Co. or Unix System Laboratories, Inc. and are reproduced herein with
 * the permission of UNIX System Laboratories, Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */
public final class ctype
{
    private ctype()
    {       
    }
    
    

    public static boolean isalnum(int c)
    {
        if (c < 0 || c > 255) return false;
        
        return (__map[c] & (_U|_L|_N)) != 0;
    } 
    public static boolean isalpha(int c)
    {
        if (c < 0 || c > 255) return false;
        
        return (__map[c] & (_U|_L)) != 0;
    } 
    public static boolean iscntrl(int c)
    {
        if (c < 0 || c > 255) return false;
        
        return (__map[c] & (_C)) != 0;
    }     
    public static boolean isdigit(int c)
    {
        if (c < 0 || c > 255) return false;
        
        return (__map[c] & (_N)) != 0;
    }
    public static boolean isgraph(int c)
    {
        if (c < 0 || c > 255) return false;
        
        return (__map[c] & (_P|_U|_L|_N)) != 0;
    }  
    public static boolean islower(int c)
    {
        if (c < 0 || c > 255) return false;
        
        return (__map[c] & (_L)) != 0;
    } 
    public static boolean isprint(int c)
    {
        if (c < 0 || c > 255) return false;
        
        return (__map[c] & (_P|_U|_L|_N|_B)) != 0;
    }
    public static boolean ispunct(int c)
    {
        if (c < 0 || c > 255) return false;
        
        return (__map[c] & (_P)) != 0;
    }    
    public static boolean isspace(int c)
    {
        if (c < 0 || c > 255) return false;
        
        return (__map[c] & (_S)) != 0;
    }    
    public static boolean isupper(int c)
    {
        if (c < 0 || c > 255) return false;
        
        return (__map[c] & (_U)) != 0;
    }    
    public static boolean isxdigit(int c)
    {
        if (c < 0 || c > 255) return false;
        
        return (__map[c] &(_N|_X)) != 0;
    }
    
    public static boolean isascii(int c)
    {
        return (c >= 0 && c < 0x80);
    }
    
    /*
     * non-posix
     * a-zA-Z0-9_
     */
    public static boolean iscsym(int c)
    {
        if (c < 0 || c > 255) return false;
        if (c == '_') return true;
        return (__map[c] & (_U|_L|_N)) != 0;
    }
    
    /*
     * non-posix
     * a-zA-Z_
     */
    public static boolean iscsymf(int c)
    {
        if (c < 0 || c > 255) return false;
        if (c == '_') return true;
        return (__map[c] & (_U|_L)) != 0;
    }

    public static boolean isoctal(int c)
    {
        if (c < 0 || c > 255) return false;
        
        return (__map[c] &(_O)) != 0;
    }
    
    /*
     * convert a hex character to a number.
     */
    public static int toint(int c)
    {
        if (c < 0 || c > 255) return 0;
        if ((__map[c] & (_N)) != 0)
            return c - '0';
        
        if ((__map[c] & (_X)) == 0) return 0;
        
        if ((__map[c] & (_U)) != 0)
            return c - 'A' + 10;
        if ((__map[c] & (_L)) != 0)
            return c - 'a' + 10;   
        
        return 0;
    }
    public static int tolower(int c)
    {
        if (c < 0 || c > 255) return c;
        if ((__map[c] & (_U)) != 0)
        {
            c |= 0x20;
        }
        return c;
    }

    public static int toupper(int c)
    {
        if (c < 0 || c > 255) return c;
        if ((__map[c] & (_L)) != 0)
        {
            c &= 0x5f;
        }
        return c;
    }   
    
    private static final int _U = 0x01;
    private static final int _L = 0x02;
    private static final int _N = 0x04;
    private static final int _S = 0x08;
    private static final int _P = 0x10;
    private static final int _C = 0x20;
    private static final int _X = 0x40;
    private static final int _B = 0x80;
    private static final int _O = 0x100;
    
    private static final int __map[] =
    {
        _C, _C, _C, _C, _C, _C, _C, _C,
        _C, _C|_S,  _C|_S,  _C|_S,  _C|_S,  _C|_S,  _C, _C,
        _C, _C, _C, _C, _C, _C, _C, _C,
        _C, _C, _C, _C, _C, _C, _C, _C,
        _S|_B,  _P, _P, _P, _P, _P, _P, _P,
        _P, _P, _P, _P, _P, _P, _P, _P,
        _N|_O, _N|_O, _N|_O, _N|_O, _N|_O, _N|_O, _N|_O, _N|_O,
        _N, _N, _P, _P, _P, _P, _P, _P,
        _P, _U|_X,  _U|_X,  _U|_X,  _U|_X,  _U|_X,  _U|_X,  _U,
        _U, _U, _U, _U, _U, _U, _U, _U,
        _U, _U, _U, _U, _U, _U, _U, _U,
        _U, _U, _U, _P, _P, _P, _P, _P,
        _P, _L|_X,  _L|_X,  _L|_X,  _L|_X,  _L|_X,  _L|_X,  _L,
        _L, _L, _L, _L, _L, _L, _L, _L,
        _L, _L, _L, _L, _L, _L, _L, _L,
        _L, _L, _L, _P, _P, _P, _P, _C
          
    };    
}
