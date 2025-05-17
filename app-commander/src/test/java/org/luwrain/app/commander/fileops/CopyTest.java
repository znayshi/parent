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
import java.nio.file.*;

import org.junit.*;

import org.luwrain.core.*;

public class CopyTest extends Assert
{
    private final OperationListener listener = new DummyListener();
    private File testDir = null;

    @Test public void singleFileToExistingDir() throws Exception
    {
	final File srcFile = createTestFile("testfile", 12345);
	final File destDir = createTestDir("dest");
	final Copy copyOp = new Copy(listener, "test", new Path[]{srcFile.toPath()}, destDir.toPath());
	copyOp.run();
	assertNull(copyOp.getException());
	assertEquals(TestingBase.calcSha1(srcFile), TestingBase.calcSha1(new File(destDir, "testfile")));
    }

    @Test public void singleFileToNonExistingPlace() throws Exception
    {
	final File srcFile = createTestFile("testfile", 12345);
	final File destDir = createTestDir("dest");
	final File destFile = new File(destDir, "destfile");
	final Copy copyOp = new Copy(listener, "test", new Path[]{srcFile.toPath()}, destFile.toPath());
	copyOp.run();
	assertNull(copyOp.getException());
	assertEquals(TestingBase.calcSha1(srcFile), TestingBase.calcSha1(destFile));
    }

    @Test public void singleFileToNonExistingPlaceInNonExistingDir() throws Exception
    {
	final File srcFile = createTestFile("testfile", 12345);
	final File destDir = createTestDir("dest");
	final File nonExistingDir = new File(destDir, "non-existing");
	final File destFile = new File(nonExistingDir, "destfile");
	final Copy copyOp = new Copy(listener, "test", new Path[]{srcFile.toPath()}, destFile.toPath());
	copyOp.run();
	assertNull(copyOp.getException());
	assertEquals(TestingBase.calcSha1(srcFile), TestingBase.calcSha1(destFile));
    }

    @Test public void twoFilesToEmptyDir() throws Exception
    {
	final File srcFile1 = createTestFile("testfile1", 12345);
	final File srcFile2 = createTestFile("testfile2", 12345);
	final File destDir = createTestDir("dest");
	final Copy copyOp = new Copy(new DummyListener(), "test", new Path[]{srcFile1.toPath(), srcFile2.toPath()}, destDir.toPath());
	copyOp.run();
	assertNull(copyOp.getException());
	assertEquals(TestingBase.calcSha1(srcFile1), TestingBase.calcSha1(new File(destDir, "testfile1")));
	assertEquals(TestingBase.calcSha1(srcFile2), TestingBase.calcSha1(new File(destDir, "testfile2")));
    }

    @Test public void twoFilesToNonExistingPlaceInNonExistingDir() throws Exception
    {
	final File srcFile1 = createTestFile("testfile1", 12345);
	final File srcFile2 = createTestFile("testfile2", 12345);
	final File destDir = createTestDir("dest");
	final File nonExistingPlace1 = new File(destDir, "non-existing1");
	final File nonExistingPlace2 = new File(nonExistingPlace1, "non-existing2");
	final Copy copyOp = new Copy(new DummyListener(), "test", new Path[]{srcFile1.toPath(), srcFile2.toPath()}, nonExistingPlace2.toPath());
	copyOp.run();
	assertNull(copyOp.getException());
	assertEquals(TestingBase.calcSha1(srcFile1), TestingBase.calcSha1(new File(nonExistingPlace2, "testfile1")));
	assertEquals(TestingBase.calcSha1(srcFile2), TestingBase.calcSha1(new File(nonExistingPlace2, "testfile2")));
    }

        //FIXME:copy single dir to existing dir
    //FIXME:copy single dir to non existing dir
    //FIXME:copy multiple dirs to non existing dir
    //FIXME:copy multiple dirs to existing dirs
    //FIXME:symlinks
    //FIXME:non existing dest, must be an error

    private File createTestFile(String name, int len) throws IOException
    {
	assertNotNull(testDir);
	final File file = new File(testDir, name);
	final TestingBase base = new TestingBase();
	base.writeRandFile(file, len);
	return file;
    }

    private File createTestDir(String name) throws IOException
    {
	assertNotNull(testDir);
	final File dir = new File(testDir, name);
	dir.mkdir();
	return dir;
    }

    @Before public void createTestDir() throws IOException
    {
	testDir = File.createTempFile(".lwr-junit-commander-fileops-", "");
	testDir.delete();
	testDir.mkdir();
    }
}
