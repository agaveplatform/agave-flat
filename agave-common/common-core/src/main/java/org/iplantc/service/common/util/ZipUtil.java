/**
 * 
 */
package org.iplantc.service.common.util;

import java.io.*;
import java.net.URI;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author dooley
 * 
 */
public class ZipUtil {

	public static void zip(File directory, File zipFile) throws IOException
	{
		URI base = directory.toURI();
		LinkedList<File> queue = new LinkedList<File>();
		queue.add(directory);
		OutputStream out = new FileOutputStream(zipFile);
		Closeable res = out;
		ZipOutputStream zout = null;
		try
		{
			zout = new ZipOutputStream(out);
			res = zout;
			while (!queue.isEmpty())
			{
				directory = queue.removeLast();
				for (File kid : directory.listFiles())
				{
					String name = base.relativize(kid.toURI()).getPath();
					if (kid.isDirectory())
					{
						queue.add(kid);
						name = name.endsWith("/") ? name : name + "/";
						zout.putNextEntry(new ZipEntry(name));
					}
					else
					{
						zout.putNextEntry(new ZipEntry(name));
						copy(kid, zout);
						zout.closeEntry();
					}
				}
			}
		}
		finally
		{
			try {res.close();} catch (Exception e) {}
			try {out.close();} catch (Exception e) {}
			try {zout.close();} catch (Exception e) {}
		}
	}

	public static void unzip(File zipFile, File directory) throws IOException
	{
		ZipFile zFile = null;
		try
		{
			zFile = new ZipFile(zipFile);
			Enumeration<? extends ZipEntry> entries = zFile.entries();
			while (entries.hasMoreElements())
			{
				ZipEntry entry = entries.nextElement();
				File file = new File(directory, entry.getName());
				if (entry.isDirectory())
				{
					file.mkdirs();
				}
				else
				{
					file.getParentFile().mkdirs();
					InputStream in = zFile.getInputStream(entry);
					try
					{
						copy(in, file);
					}
					finally
					{
						in.close();
					}
				}
			}
		}
		finally {
			try { zFile.close(); } catch (Exception e) {}
		}
	}

	private static void copy(InputStream in, OutputStream out)
			throws IOException
	{
		byte[] buffer = new byte[1024];
		while (true)
		{
			int readCount = in.read(buffer);
			if (readCount < 0)
			{
				break;
			}
			out.write(buffer, 0, readCount);
		}
	}

	private static void copy(File file, OutputStream out) throws IOException
	{
		InputStream in = new FileInputStream(file);
		try
		{
			copy(in, out);
		}
		finally
		{
			in.close();
		}
	}

	private static void copy(InputStream in, File file) throws IOException
	{
		OutputStream out = new FileOutputStream(file);
		try
		{
			copy(in, out);
		}
		finally
		{
			out.close();
		}
	}

}
