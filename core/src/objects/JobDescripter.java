package objects;

import java.util.UUID;

public class JobDescripter {
	
	public String belongsTo;
	public UUID identifier;
	public String type;
	public long Start;
	public long End;
	public long timeRunning;
	
	public JobDescripter (UUID n, String to, long s, long e, String t){
		belongsTo = to;
		identifier = n;
		type = t;
		Start = s;
		End = e;
		timeRunning = 0;
	}
	
	public String toString(){
		StringBuilder out = new StringBuilder();
		out.append("Job ID : " + identifier.toString() + "\n");
		out.append("Belongs to : " + belongsTo +"\n");
		out.append("Time Running (s) : " + timeRunning + "\n");
		return out.toString();
	}
}
