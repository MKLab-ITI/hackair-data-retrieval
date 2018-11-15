/**
 * Title: File Functions
 * Description: FileFunction class contains various functions for file processing.
 *                 
 * Copyright: Copyright (c) November 2008
 * 
 * @author Anastasia Moumtzidou
 * Company: ITI 
 * @version 1.3
 */


package commonFunctions;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;




public class FileFunctions 
{


	/**
	 * Create a file and insert its content by giving its path and filename.
	 * 
	 * @param filename		string consisting of the directory and filename of the file to be created
	 * @param fileContent	string containing the content of the file to be created
	 * 
	 * @throws IOException
	 */ 
	public void createFile(String filename,String fileContent) throws IOException
	{
		File fi = new File(filename);
		fi.createNewFile();

		BufferedWriter buffWriter = new BufferedWriter(new FileWriter(fi));
		buffWriter.write(fileContent);
		buffWriter.close();
	}


	/**
	 * Reading the content of a file by giving its path and filename.
	 *  
	 * @param filename		string consisting of the directory and filename of the file to be created
	 * 
	 * @return an array of string containing the file
	 * 
	 * @throws IOException
	 */ 
	public String[] readFile(String filename) throws IOException
	{
		int line=0;
		String thisLine = "";
		String[] wholeFile = null;

		int numOfElements = getFileLines(filename);
		wholeFile = new String[numOfElements];

		FileReader fr = new FileReader(filename);
		BufferedReader br = new BufferedReader(fr);

		while ( (thisLine = br.readLine())!=null) 
		{
			wholeFile[line] = thisLine;
			line = line+1;
		}

		br.close();
				
		return wholeFile;
	}


	

	/**
	 * Reading the content of a file by giving its path and filename.
	 *  
	 * @param filename		string consisting of the directory and filename of the file to be created
	 * 
	 * @return string containing the file
	 * 
	 * @throws IOException
	 */ 
	public String readAllFile(String filename) throws IOException
	{
		String thisLine = "";
		StringBuffer wholeFile = new StringBuffer();

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF8"));
		
		while ( (thisLine = br.readLine())!=null) 
		{
			wholeFile.append(thisLine+" ");
		}

		br.close();
				
		return wholeFile.substring(0, wholeFile.length()-1).toString();
	}
	
	
	
		/**
	 * Reading the content of a file by giving its path and filename.
	 *  
	 * @param filename		string consisting of the directory and filename of the file to be created
	 * 
	 * @return string containing the file
	 * 
	 * @throws IOException
	 */ 
	public String readAllFileWithNewLines(String filename) throws IOException
	{
		String thisLine = "";
		StringBuffer wholeFile = new StringBuffer();

		FileReader fr = new FileReader(filename);
		BufferedReader br = new BufferedReader(fr);

		while ( (thisLine = br.readLine())!=null) 
		{
			wholeFile.append(thisLine+"\n");
		}

		br.close();

		return wholeFile.toString();
	}
	
	
	/**
	 * Getting the number of lines of a file by giving its path and filename.
	 *  
	 * @param filename		string consisting of the directory and filename of the file to be created
	 * 
	 * @return integer referring to the number of lines of the file.
	 * 
	 * @throws IOException
	 */ 
	public int getFileLines(String filename) throws IOException
	{
		int line=0;
		FileReader fr = new FileReader(filename);
		BufferedReader br = new BufferedReader(fr);

		while ( br.readLine()!=null) 
		{
			line = line+1;
		}
		br.close();

		return line;
	}


	/**
	 *  Create a file by merging files by row.
	 *  
	 * @param filename		array-list containing the names of the files whose rows will be merged
	 * @param outputFile	string consisting of the directory and filename of the file to be created
	 * 
	 * @throws IOException
	 */ 
	public void concatFiles(ArrayList<String> filename, String outputFile) throws IOException
	{
		String line = "";

		File fi = new File(outputFile);
		fi.createNewFile();

		BufferedWriter buffWriter = new BufferedWriter(new FileWriter(fi));
		BufferedReader[] br = new BufferedReader[filename.size()];
		String[] thisLine = new String[filename.size()];

		for (int i = 0; i < filename.size(); i++) 
		{
			br[i] = new BufferedReader(new FileReader(filename.get(i)));
		}

		while (true) 
		{
			line = "";
			for (int i = 0; i < filename.size(); i++) 
			{
				thisLine[i] = br[i].readLine();
				line = line + thisLine[i] + " ";
			}

			if (thisLine[0] == null) 
			{
				break;
			}

			buffWriter.write(line + "\n");
		}
		buffWriter.close();
		
		for (int i = 0; i < filename.size(); i++) 
		{
			br[i].close();
		}

	}


	/**
	 *  Create a file by merging files by row.
	 *  
	 * @param filename		array-list containing the names of the files whose rows will be merged
	 * @param outputFile	string consisting of the directory and filename of the file to be created
	 * 
	 * @throws IOException
	 */ 
	public void mergeFiles(ArrayList<String> filename, String outputFile) throws IOException
	{
		String content = "";

		
		File fi = new File(outputFile);
		fi.createNewFile();

		BufferedWriter buffWriter = new BufferedWriter(new FileWriter(fi));
		
		
		for (int i = 0; i < filename.size(); i++) 
		{
			content = readAllFileWithNewLines(filename.get(i));
			buffWriter.write(content);
		}
		
		buffWriter.close();

	}
	
	/**
	 *  Open a file to write new content by giving its path and filename.
	 *  
	 * @param filename		string consisting of the directory and filename of the file to be created
	 * 
	 * @return BufferedWriter
	 * 
	 * @see #writeFile(BufferedWriter bw, String content)
	 * @see #closeFile(BufferedWriter bw)
	 * 
	 * @throws IOException
	 */ 
	public BufferedWriter openFile(String filename) throws IOException  
	{
		BufferedWriter bw = null;
		File fi = new File(filename);
		fi.createNewFile();
		bw = new BufferedWriter(new FileWriter(fi));	
		return bw;
	}


	/**
	 *  Open a file in order to append content to it, by giving its path and filename.
	 *  
	 * @param filename		string consisting of the directory and filename of the file to be created
	 * 
	 * @return BufferedWriter
	 * 
	 * @see #writeFile(BufferedWriter bw, String content)
	 * @see #closeFile(BufferedWriter bw)
	 * 
	 * @throws IOException
	 */ 
	public BufferedWriter openFileToAppend(String filename) throws IOException
	{
		BufferedWriter bw = null;
		File fi = new File(filename);
		fi.createNewFile();
		bw = new BufferedWriter(new FileWriter(fi,true));	
		return bw;
	}


	/**
	 * Writing to a file by giving its BufferedWriter (used together with openFile or openFileToAppend)
	 *  
	 * @param bw		BufferedWriter created by openFile or openFileToAppend function in order to write to the openedFile
	 * @param content	string string containing the content of the file to be created
	 * 
	 * @see #openFile(String filename) 
	 * @see #openFileToAppend(String filename)
	 * @see #closeFile(BufferedWriter bw)
	 * 
	 * @throws IOException
	 */ 
	public void writeFile(BufferedWriter bw, String content) throws IOException
	{
		bw.write(content);
		bw.flush();
	}


	/**
	 *  Close an opened file.
	 *  
	 * @param bw		BufferedWriter created by openFile or openFileToAppend function in order to write to the openedFile
	 * 
	 * @see #openFile(String filename) 
	 * @see #openFileToAppend(String filename)
	 * @see #writeFile(BufferedWriter bw, String content)
	 * 
	 * @throws IOException
	 */ 
	public void closeFile(BufferedWriter bw) throws IOException
	{
		bw.close();
	}





	/**
	 *  Delete all files and subfolders existing in a directory.
	 *  
	 * @param dir		Directory containing all files etc.
	 *  
	 */ 
	public boolean deleteDir(File dir)
	{
		if (dir.isDirectory()) 
		{
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) 
			{
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) 
				{
					return false;
				}
			}
		}

		return dir.delete();
	}
	
	
	/**
	 *  Delete all files existing in a directory.
	 *  
	 * @param dir		Directory containing all files etc.
	 *  
	 */ 
	public void emptyFolders(File directory)
	{
		String[] fileList = directory.list();
		File f = null;

		for(int i=0;i<fileList.length;i++)
		{
			f = new File(directory.getPath()+"\\"+fileList[i]);
			f.delete();
		}
	}

	
	/**
	 *  Delete all files existing in a directory apart from files with extension 'ext'.
	 *  
	 * @param dir		Directory containing all files etc.
	 *  
	 */ 
	public void emptySpecificFileTypesFromFolder(File directory, String ext)
	{
		String[] fileList = directory.list();
		File f = null;

		for(int i=0;i<fileList.length;i++)
		{
			if(fileList[i].endsWith(ext)==true){
				f = new File(directory.getPath()+"\\"+fileList[i]);
				f.delete();
			}
		}
	}
	
	
	
	/**
	 *  Delete all files existing in a directory with extension 'ext'.
	 *  
	 * @param dir		Directory containing all files etc.
	 *  
	 */ 
	public void emptyFolders(File directory, String ext)
	{
		String[] fileList = directory.list();
		File f = null;

		for(int i=0;i<fileList.length;i++)
		{
			if(fileList[i].endsWith("txt")==false){
				f = new File(directory.getPath()+"\\"+fileList[i]);
				f.delete();
			}
		}
	}
	
	
	
	/**
	 *  Delete directory
	 *  
	 * @param dir		Directory containing all files etc.
	 *  
	 */ 
	public void deleteFolder(File directory)
	{
		File f = new File(directory.getPath());
		f.delete();
	}
	
	
}

