package com.mcd.gdw.daas.util;

import java.io.BufferedWriter;

import java.sql.ResultSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
//AWS START
//import org.apache.hadoop.hbase.protobuf.generated.HBaseProtos.TableSchema;
//AWS END
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.mcd.gdw.daas.util.DaaSConfig;
import com.mcd.gdw.daas.util.RDBMS;

public class DeleteDataMartTables extends Configured implements Tool {
	static FileSystem fileSystem;
	public static void main(String[] args){
		
		try{	
			int retval = ToolRunner.run(new Configuration(),
					new DeleteDataMartTables(), args);
			
			
			System.out.println(" return value : " + retval);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
	
	
	@Override
	public int run(String[] args) throws Exception {
		String tableSchemaName = "";
		GenericOptionsParser gop = new GenericOptionsParser(args);

		args = gop.getRemainingArgs();

		DaaSConfig daasConfig = new DaaSConfig(args[0]);
		
		Configuration conf = new Configuration();
		for ( int idx=0; idx < args.length; idx++ ) {
			if ( args[idx].equals("-sc") && (idx+1) < args.length ) {
				tableSchemaName = args[idx+1];
			}
		}
		fileSystem = FileSystem.get(conf);
		DeleteDataMartTables ddMT = new DeleteDataMartTables();
		ddMT.deleteDataMartTables(  daasConfig, tableSchemaName);
		return (0);

	}
		
        private void deleteDataMartTables(DaaSConfig daasConfig, String tableSchemaName) {
		
		ResultSet rset = null;
		BufferedWriter bw = null;
		try {
			StringBuffer sql = new StringBuffer();
			

			if ( daasConfig.displayMsgs() ) {
		    	System.out.print("Connecting to GDW ... ");
		    }
		    
			RDBMS gdw = new RDBMS(RDBMS.ConnectionType.Teradata,daasConfig.gblTpid(),daasConfig.gblUserId(),daasConfig.gblPassword(),daasConfig.gblNumSessions());
		    
		    if ( daasConfig.displayMsgs() ) {
		    	System.out.println("done");
		    }
		    
			sql.setLength(0);
			sql.append("delete from "+  tableSchemaName+".DLY_DYPT_POS_TRN all");					
		    gdw.executeUpdate(sql.toString());
		    gdw.commit();	

		    sql.setLength(0);
			sql.append("delete from " + tableSchemaName +".DLY_TM_SEG_POS_TRN_OFFR all");					
		    gdw.executeUpdate(sql.toString());
		    gdw.commit();
		    
		    sql.setLength(0);
			sql.append("delete from "+tableSchemaName+".POS_TRN_OFFR all");					
		    gdw.executeUpdate(sql.toString());
		    gdw.commit();
		    
			
			
		} catch (Exception ex) {
			System.err.println("Error occured in DeleteDataMartTable:");
			System.err.println(ex.toString());
			ex.printStackTrace();
			System.exit(8);
		}finally{
				try{
					if(rset!=null)
						rset.close();
					if(bw != null)
						bw.close();
				}catch(Exception ex){
					ex.printStackTrace();
				}
			
		}
		
		
	}

}