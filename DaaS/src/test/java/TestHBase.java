import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.hbase.HBaseConfiguration;
//import org.apache.hadoop.hbase.client.Get;
//import org.apache.hadoop.hbase.client.HBaseAdmin;
//import org.apache.hadoop.hbase.client.HConnection;
//import org.apache.hadoop.hbase.client.HConnectionManager;
//import org.apache.hadoop.hbase.client.HTable;
//import org.apache.hadoop.hbase.client.Put;
//import org.apache.hadoop.hbase.client.Result;
//import org.apache.hadoop.hbase.snapshot.HBaseSnapshotException;
//import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.io.compress.GzipCodec;


public class TestHBase   {
	
	public static void main(String[] args){
		BufferedReader br =null;
/*
		try{
//			Configuration conf = HBaseConfiguration.create();
			conf.set("hbase.zookeeper.quorum", "hdp001-nn,hdp001-jt,hdp001-8");  
//			conf.set("hbase.master", "hdp001-nn:60000");
//			conf.set("hbase.master", "hdp001-6");
//			conf.set("hbase.master.port", "60000");
			conf.set("hbase.regionserver","hdp001-8,hdp001-9,hdp001-13,hdp001-15,hdp001-16,hdp001-17,hdp001-18,hdp001-19,hdp001-20,hdp001-21,hdp001-22,hdp001-23");
			conf.set("hbase.client.keyvalue.maxsize", "-1");
			conf.set("fs.default.name", "hdfs://hdp001-nn:8020");
			conf.set("user.name","psinkula");
			if(args[0].equalsIgnoreCase("local")){//this will not work. network will not allow.
				conf.addResource(new Path("C:/Users/mc32445/Desktop/DaaS/newclusterconf/conf.empty/core-site.xml"));
				conf.addResource(new Path("C:/Users/mc32445/Desktop/DaaS/newclusterconf/conf.empty/hdfs-site.xml"));
				conf.addResource(new Path("C:/Users/mc32445/Desktop/DaaS/newclusterconf/conf.empty/mapred-site.xml"));
				conf.addResource(new Path("C:/Users/mc32445/Desktop/DaaS/newclusterconf/hbaseconf/HBASE/conf.dist/hbase-site.xml"));
				conf.addResource(new Path("C:/Users/mc32445/Desktop/DaaS/newclusterconf/hbaseconf/HBASE/conf.dist/hbase-policy.xml"));
			}else{
				conf.addResource(new Path("/etc/hadoop/conf/core-site.xml"));
				conf.addResource(new Path("/etc/hadoop/conf/hdfs-site.xml"));
				conf.addResource(new Path("/etc/hadoop/conf/mapred-site.xml"));
				conf.addResource(new Path("/home/psinkula/hbase-conf/conf/hbase-site.xml"));
				conf.addResource(new Path("/home/psinkula/hbase-conf/conf/hbase-policy.xml"));
//				conf.addResource(new Path("/home/psinkula/hbase/conf/regionservers"));
				
				
			}
			
			
			FileSystem fs = FileSystem.get(conf);
			Path path = new Path("/daas/work/np_xml/step1/STLDRxD126840RxD12620140531-r-00006.gz");
			CompressionCodecFactory factory = new CompressionCodecFactory(conf);
			CompressionCodec codec = factory.getCodec(path);
			InputStream inputstream  = codec.createInputStream(fs.open(path));
			
			br = new BufferedReader(new InputStreamReader(inputstream));
			
			String[] lineparts;
			String line;
			boolean found = false;
			HTable htable = new HTable(conf, "stld");
			Put put = null;
			int i;
			int rowcnt = 1;
			while ( (line=br.readLine()) != null && !found ) {
//				System.out.println(line);
				lineparts = line.split("\t");
				i =0;
				put =  new Put(("row"+(rowcnt++)).getBytes());
				put.add("fldnm".getBytes(), "outputtype".getBytes(), lineparts[i++].getBytes());				
				put.add("fldnm".getBytes(), "busdt".getBytes(), lineparts[i++].getBytes());
				put.add("fldnm".getBytes(), "fileid".getBytes(), lineparts[i++].getBytes());
				put.add("fldnm".getBytes(), "mcdGbalBusnLcat".getBytes(), lineparts[i++].getBytes());
				put.add("fldnm".getBytes(), "terrcd".getBytes(), lineparts[i++].getBytes());
				put.add("fldnm".getBytes(), "storeid".getBytes(), lineparts[i++].getBytes());
				put.add("fldnm".getBytes(), "ownershp".getBytes(), lineparts[i++].getBytes());
				put.add("fldnm".getBytes(), "xmlstring".getBytes(), lineparts[i++].getBytes());
				
				htable.put(put);
				
//				found = true;
			}
			
			System.out.println(" added " + rowcnt + " rows to the table ");
			
	
			
			Get g = new Get(Bytes.toBytes("row1"));
			Result r = htable.get(g);
			
			StringBuffer sbf = new StringBuffer();
			
			byte[] value = r.getValue(Bytes.toBytes("fldnm"), Bytes.toBytes("outputtype"));
			sbf.append(Bytes.toString(value)).append("\t");
			value = r.getValue(Bytes.toBytes("fldnm"), Bytes.toBytes("busdt"));
			sbf.append(Bytes.toString(value)).append("\t");
			value = r.getValue(Bytes.toBytes("fldnm"), Bytes.toBytes("fileid"));
			sbf.append(Bytes.toString(value)).append("\t");
			value = r.getValue(Bytes.toBytes("fldnm"), Bytes.toBytes("mcdGbalBusnLcat"));
			sbf.append(Bytes.toString(value)).append("\t");
			value = r.getValue(Bytes.toBytes("fldnm"), Bytes.toBytes("terrcd"));
			sbf.append(Bytes.toString(value)).append("\t");
			value = r.getValue(Bytes.toBytes("fldnm"), Bytes.toBytes("storeid"));
			sbf.append(Bytes.toString(value)).append("\t");
			value = r.getValue(Bytes.toBytes("fldnm"), Bytes.toBytes("ownershp"));
			sbf.append(Bytes.toString(value)).append("\t");
//			value = r.getValue(Bytes.toBytes("fldnm"), Bytes.toBytes("xmlstring"));
//			sbf.append(Bytes.toString(value)).append("\t");
			
			System.out.println("GET: " + sbf.toString());
		}catch(Exception ex){
			ex.printStackTrace();
		}
		*/
	}

}
