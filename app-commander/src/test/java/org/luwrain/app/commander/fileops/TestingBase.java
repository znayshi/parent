/*
   Copyright 2012-2021 Michael Pozhidaev <msp@luwrain.org>

   This file is part of LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.app.commander.fileops;

import java.io.*;
import java.util.*;

class TestingBase
{
    static final File TMP_DIR = new File("/tmp/lwr-linux-testing-data");

    final Random rand = new Random();

    void writeRandFile(File file, int len) throws IOException
    {
	if (len < 0)
	    throw new IllegalArgumentException("len (" + len + ") may not be negative");
	final FileOutputStream f = new FileOutputStream(file);
	final byte[] buf = new byte[2048];
	int written = 0;
	while (written < len)
	{
	    final int chunk = (len - written >= buf.length)?buf.length:len - written;
	    fillBuf(buf, chunk);
	    f.write(buf, 0, chunk);
	    written += chunk;
	}
	f.flush();
	f.close();
    }

    static String calcSha1(File file) throws Exception
    {
	final FileInputStream s = new FileInputStream(file);
	try {
	    return org.luwrain.util.Sha1.getSha1(s);
	}
	finally {
	    s.close();
	}
    }

    private void fillBuf(byte[] buf, int len)
    {
	for(int i = 0;i < len;++i)
	{
	    byte b = (byte)(rand.nextInt(256) - 128);
	    buf[i] = b;
	}
    }
}
