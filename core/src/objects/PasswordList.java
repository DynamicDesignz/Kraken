package objects;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import constants.Constants;

public class PasswordList {
	
	private String listName;
	private Path listPath;
	private long lineCount;
	private Charset cs;
	private Long size;
	
	private Long B = new Long(1);
	private Long KB = new Long(1000);
	private Long MB = new Long(1000000);
	private Long GB = new Long(1000000000);
	private Long TB = new Long("1000000000000");
	
	public PasswordList(String name) throws Exception{
		cs = null;
		lineCount = -1;
		listName = name;
		listPath = Paths.get(Constants.PasswordListFolderLocation,name);
		if(!Files.exists(listPath)){
			throw new RuntimeException("Could not find password list " + name + " at specified location.");
		}
		if(constructList() == false){
			throw new RuntimeException("Password List " + name + " is using an unsupported encoding.");
		}
	}
	
	public PasswordList(String name, String path) throws Exception{
		cs = null;
		lineCount = -1;
		listName = name;
		listPath = Paths.get(path);
		if(!Files.exists(listPath)){
			throw new RuntimeException("Could not find password list " + name + " at specified location.");
		}
		if(constructList() == false){
			throw new RuntimeException("Password List " + name + " is using an unsupported encoding.");
		}
	}
	
	
	
	private boolean constructList(){
		for(int i=0; i < Constants.SupportedCharsets.length; i++){
			try{
				this.lineCount = Files.lines(this.listPath,Constants.SupportedCharsets[i]).count();
				if(this.lineCount != -1){
					this.cs = Constants.SupportedCharsets[i];
					this.size = new Long(Files.size(this.listPath));
					return true;
				}
			}
			catch(Exception e){/*Unsupported Encoding Exception*/}
		}
		return false;
	}
	
	public String getName(){
		return listName;
	}
	
	public Charset getCharset(){
		return cs;
	}

	public long getLineCount(){
		return lineCount;
	}
	
	public Path getPath(){
		return listPath;
	}
	
	public String getSize(){
		if(size < KB){
			return size/B + " B";
		}
		else if (size < MB){
			return size/KB + " KB";
		}
		else if (size < GB ){
			return size/MB + " MB";
		}
		else if (size < TB){
			return size/GB + " GB";
		}
		return "Error - file size too large";
	}
}
