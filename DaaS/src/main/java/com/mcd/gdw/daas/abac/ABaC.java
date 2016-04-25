package com.mcd.gdw.daas.abac;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Trash;


import com.mcd.gdw.daas.DaaSConstants;
import com.mcd.gdw.daas.util.DaaSConfig;
import com.mcd.gdw.daas.util.HDFSUtil;
import com.mcd.gdw.daas.util.RDBMS;

@SuppressWarnings("unused")
public class ABaC {

	public enum CodeType {VALIDATED
		                 ,READY
		                 ,PROCESSING
		                 ,SUCCESSFUL
		                 ,REJECTED
		                 ,FAILED
		                 ,NON_MCOPCO
		                 ,INTERBATCH_DUPLICATE
		                 ,CORRUPTED_COMPRESSED_FORMAT
		                 ,MALFORMED_XML
		                 ,MISSING
		                 ,LATE_ARRIVAL,ARRIVED,UPDATED}
	
	private static final String TBL_DW_AUDT_STUS_TYP           = "dw_audt_stus_typ";
	private static final String TBL_DW_AUDT_RJCT_RESN          = "dw_audt_rjct_resn";
	private static final String TBL_DW_FILE_LIST_TYP          = "dw_file_list_typ";
	private static final String TBL_DW_FILE                    = "dw_file";
	private static final String TBL_DW_DATA_TYP                = "dw_data_typ";
	private static final String TBL_DW_FILE_JOB_XECT_ASSC      = "dw_file_job_xect_assc";
	private static final String TBL_DW_FILE_RJCT_RESN_ASSC     = "dw_file_rjct_resn_assc";
	public static final String TBL_DW_JOB_GRP_XECT             = "dw_job_grp_xect";
	private static final String TBL_DW_JOB_XECT                = "dw_job_xect";
	private static final String TBL_DW_JOB_XECT_TRGT           = "dw_job_xect_trgt";
	private static final String TBL_DW_SUB_FILE                = "dw_sub_file";
	private static final String TBL_DW_SUB_FILE_DATA_TYP       = "dw_sub_file_data_typ";
	private static final String TBL_GBAL_LCAT                  = "gbal_lcat";
	private static final String TBL_REST_OWSH                  = "rest_owsh";
	private static final String TBL_REST_OWSH_XCPT_LIST        = "rest_owsh_xcpt_list";
	private static final String TBL_DW_SUB_FILE_RJCT_RESN_ASSC = "dw_sub_file_rjct_resn_assc";

	private static final String COL_DW_AUDT_STUS_TYP_DS     = "DW_AUDT_STUS_TYP_DS";
	private static final String COL_DW_AUDT_RJCT_RESN_ID    = "DW_AUDT_RJCT_RESN_ID";
	private static final String COL_DW_FILE_LIST_DS         = "DW_FILE_LIST_DS";
	private static final String COL_DW_FILE_LIST_ID         = "DW_FILE_LIST_ID";
	private static final String COL_DW_AUDT_RJCT_RESN_DS    = "DW_AUDT_RJCT_RESN_DS";
	private static final String COL_DW_FILE_NA              = "DW_FILE_NA";
	private static final String COL_FILE_INCM_OUTG_CD       = "FILE_INCM_OUTG_CD";
	private static final String COL_FILE_MKT_OGIN_TS        = "FILE_MKT_OGIN_TS";
	private static final String COL_FILE_DW_ARRV_TS         = "FILE_DW_ARRV_TS";
	private static final String COL_FILE_PRCS_STRT_TS       = "FILE_PRCS_STRT_TS";
	private static final String COL_FILE_PRCS_END_TS        = "FILE_PRCS_END_TS";
	private static final String COL_MCD_GBAL_LCAT_ID_NU     = "MCD_GBAL_LCAT_ID_NU";
	private static final String COL_TERR_CD                 = "TERR_CD";
	private static final String COL_LGCY_LCL_RFR_DEF_CD     = "LGCY_LCL_RFR_DEF_CD";
	private static final String COL_FILE_PATH_DS            = "FILE_PATH_DS";
	private static final String COL_FILE_REC_CNT_QT         = "FILE_REC_CNT_QT";
	private static final String COL_CAL_DT                  = "CAL_DT";
	private static final String COL_DW_DATA_TYP_DS          = "DW_DATA_TYP_DS";
	private static final String COL_DW_LOAD_TBLE_NA         = "DW_LOAD_TBLE_NA";
	private static final String COL_DW_VLD_TYP_CD           = "DW_VLD_TYP_CD";
	private static final String COL_DW_FILE_CPNT_CNT_QT     = "DW_FILE_CPNT_CNT_QT";
	private static final String COL_DW_APLC_TYP_CNT_QT      = "DW_APLC_TYP_CNT_QT";
	private static final String COL_REST_CPNT_PSTN_NU       = "REST_CPNT_PSTN_NU";
	private static final String COL_CTRY_CPNT_PSTN_NU       = "CTRY_CPNT_PSTN_NU";
	private static final String COL_BUSN_DT_CPNT_PSTN_NU    = "BUSN_DT_CPNT_PSTN_NU";
	private static final String COL_DW_FILE_ID              = "DW_FILE_ID";
	private static final String COL_DW_FILE_STUS_TYP_ID     = "DW_FILE_STUS_TYP_ID";
	private static final String COL_DW_JOB_GRP_SUBJ_DS      = "DW_JOB_GRP_SUBJ_DS";
	public static final String COL_DW_JOB_GRP_STRT_TS       = "DW_JOB_GRP_STRT_TS";
	public static final String COL_DW_JOB_GRP_END_TS        = "DW_JOB_GRP_END_TS";
	private static final String COL_DW_JOB_XECT_SEQ_NU      = "DW_JOB_XECT_SEQ_NU";
	private static final String COL_DW_JOB_NA               = "DW_JOB_NA";
	private static final String COL_DW_JOB_STRT_TS          = "DW_JOB_STRT_TS";
	private static final String COL_DW_JOB_END_TS           = "DW_JOB_END_TS";
	public static final String COL_DW_JOB_GRP_XECT_ID       = "DW_JOB_GRP_XECT_ID";
	private static final String COL_LAST_DW_ERR_RESN_CD     = "LAST_DW_ERR_RESN_CD";
	private static final String COL_LAST_DW_ERR_RESN_DS     = "LAST_DW_ERR_RESN_DS";
	private static final String COL_DW_JOB_XECT_ID          = "DW_JOB_XECT_ID";
	private static final String COL_DW_JOB_TRGT_SEQ_NU      = "DW_JOB_TRGT_SEQ_NU";
	private static final String COL_DW_JOB_TRGT_NA          = "DW_JOB_TRGT_NA";
	private static final String COL_DW_JOB_TRGT_DS          = "DW_JOB_TRGT_DS";
	private static final String COL_DW_JOB_TRGT_TYP_DS      = "DW_JOB_TRGT_TYP_DS";
	private static final String COL_DW_TRGT_REC_CNT_QT      = "DW_TRGT_REC_CNT_QT";
	private static final String COL_DW_TRGT_TOT_NET_SLS_AM  = "DW_TRGT_TOT_NET_SLS_AM";
	private static final String COL_DW_TRGT_TOT_GRSS_SLS_AM = "DW_TRGT_TOT_GRSS_SLS_AM";
	private static final String COL_DW_TRGT_TRN_ORD_CNT_QT  = "DW_TRGT_TRN_ORD_CNT_QT";
	private static final String COL_DW_AUDT_STUS_TYP_ID     = "DW_AUDT_STUS_TYP_ID";
	private static final String COL_DW_SUB_FILE_NA          = "DW_SUB_FILE_NA";
	private static final String COL_FILE_SIZE_NU            = "FILE_SIZE_NU";
	private static final String COL_DW_SUB_FILE_DATA_TYP_ID = "DW_SUB_FILE_DATA_TYP_ID";
	private static final String COL_DW_SUB_FILE_DATA_TYP_CD = "DW_SUB_FILE_DATA_TYP_CD";
	private static final String COL_DW_SUB_FILE_DATA_TYP_DS = "DW_SUB_FILE_DATA_TYP_DS";
	private static final String COL_DW_DATA_TYP_ID          = "DW_DATA_TYP_ID";
	private static final String COL_CTRY_ISO_NU             = "CTRY_ISO_NU";
	private static final String COL_REST_OWSH_EFF_DT        = "REST_OWSH_EFF_DT";
	private static final String COL_REST_OWSH_END_DT        = "REST_OWSH_END_DT";
	private static final String COL_XCPT_EFF_DT             = "XCPT_EFF_DT";
	private static final String COL_XCPT_END_DT             = "XCPT_END_DT";
	private static final String COL_REST_OWSH_TYP_SHRT_DS   = "REST_OWSH_TYP_SHRT_DS";
	private static final String COL_DW_SUB_FILE_RJCT_RESN_ERR_TX = "DW_SUB_FILE_RJCT_RESN_ERR_TX";
	private static final String COL_DW_TRGT_UNIQ_REST_CNT_QT="DW_TRGT_UNIQ_REST_CNT_QT";
	private static final String COL_DW_SRCE_FILE_ID="DW_SRCE_FILE_ID";

	private static final BigDecimal DECIMAL_ZERO = new BigDecimal("0.00");
	
	private final static char[] HEX_ARRAY = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	private RDBMS abac;
	private DaaSConfig daasConfig;
	
	private StringBuffer sql = new StringBuffer();
	
	private HashMap<String,Integer> statusTypes = new HashMap<String,Integer>();
	private HashMap<String,Integer> rejectReasons = new HashMap<String,Integer>();
	private HashMap<CodeType,Integer> typeCodes = new HashMap<CodeType,Integer>();
	private HashMap<String,Integer> fileListCodeType = new HashMap<String,Integer>();

	private String errMsg = "";
	
	public ABaC(DaaSConfig daasConfig) throws Exception {
		
		ResultSet rset;
		
		this.daasConfig = daasConfig;
		
		abac = new RDBMS(RDBMS.ConnectionType.SQLServer, this.daasConfig.abacSqlServerServerName(), this.daasConfig.abacSqlServerUserId(), this.daasConfig.abacSqlServerPassword());
		abac.setBatchSize(this.daasConfig.abacSqlServerBatchSize());
		abac.setAutoCommit(false);
		
		sql.setLength(0);
		sql.append("select\n");
		sql.append("   " + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_STUS_TYP_DS + "\n");
		sql.append("from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_AUDT_STUS_TYP +" with (NOLOCK)\n");
		
		rset = abac.resultSet(sql.toString());
		
		while ( rset.next() ) {
			statusTypes.put(rset.getString(COL_DW_AUDT_STUS_TYP_DS), rset.getInt(COL_DW_AUDT_STUS_TYP_ID));
		}
		
		rset.close();
		
		sql.setLength(0);
		sql.append("select\n");
		sql.append("   " + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_RJCT_RESN_DS + "\n");
		sql.append("from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_AUDT_RJCT_RESN +" with (NOLOCK)\n");
		
		rset = abac.resultSet(sql.toString());
		
		while ( rset.next() ) {
			rejectReasons.put(rset.getString(COL_DW_AUDT_RJCT_RESN_DS), rset.getInt(COL_DW_AUDT_RJCT_RESN_ID));
		}
		
		rset.close();
		
		sql.setLength(0);
		sql.append("select\n");
		sql.append("   " + COL_DW_FILE_LIST_ID + "\n");
		sql.append("  ," + COL_DW_FILE_LIST_DS + "\n");
		sql.append("from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE_LIST_TYP +" with (NOLOCK)\n");
		
		rset = abac.resultSet(sql.toString());
		System.out.println(sql.toString());
		
		while ( rset.next() ) {
			fileListCodeType.put(rset.getString(COL_DW_FILE_LIST_DS), rset.getInt(COL_DW_FILE_LIST_ID));
		}
		
		rset.close();
		
		typeCodes.put(CodeType.VALIDATED, validateCode(daasConfig.abacSqlServerStatusValidated(),TBL_DW_AUDT_STUS_TYP,COL_DW_AUDT_STUS_TYP_DS,statusTypes));
		typeCodes.put(CodeType.READY, validateCode(daasConfig.abacSqlServerStatusReady(),TBL_DW_AUDT_STUS_TYP,COL_DW_AUDT_STUS_TYP_DS,statusTypes));
		typeCodes.put(CodeType.PROCESSING, validateCode(daasConfig.abacSqlServerStatusProcessing(),TBL_DW_AUDT_STUS_TYP,COL_DW_AUDT_STUS_TYP_DS,statusTypes));
		typeCodes.put(CodeType.SUCCESSFUL, validateCode(daasConfig.abacSqlServerStatusSuccessful(),TBL_DW_AUDT_STUS_TYP,COL_DW_AUDT_STUS_TYP_DS,statusTypes));
		typeCodes.put(CodeType.REJECTED, validateCode(daasConfig.abacSqlServerStatusRejected(),TBL_DW_AUDT_STUS_TYP,COL_DW_AUDT_STUS_TYP_DS,statusTypes));
		typeCodes.put(CodeType.FAILED, validateCode(daasConfig.abacSqlServerStatusFailed(),TBL_DW_AUDT_STUS_TYP,COL_DW_AUDT_STUS_TYP_DS,statusTypes));
		typeCodes.put(CodeType.NON_MCOPCO, validateCode(daasConfig.abacSqlServerReasonNonMcOpCo(),TBL_DW_AUDT_RJCT_RESN,COL_DW_AUDT_RJCT_RESN_DS,rejectReasons));
		typeCodes.put(CodeType.INTERBATCH_DUPLICATE, validateCode(daasConfig.abacSqlServerReasonInterbatchDuplicate(),TBL_DW_AUDT_RJCT_RESN,COL_DW_AUDT_RJCT_RESN_DS,rejectReasons));
		typeCodes.put(CodeType.CORRUPTED_COMPRESSED_FORMAT, validateCode(daasConfig.abacSqlServerReasonCorruptedCompressedFormat(),TBL_DW_AUDT_RJCT_RESN,COL_DW_AUDT_RJCT_RESN_DS,rejectReasons));
		typeCodes.put(CodeType.MALFORMED_XML, validateCode(daasConfig.abacSqlServerReasonMalformedXml(),TBL_DW_AUDT_RJCT_RESN,COL_DW_AUDT_RJCT_RESN_DS,rejectReasons));
		typeCodes.put(CodeType.MISSING, validateCode(daasConfig.abacSqlServerReasonMissing(),TBL_DW_AUDT_RJCT_RESN,COL_DW_AUDT_RJCT_RESN_DS,rejectReasons));
		typeCodes.put(CodeType.LATE_ARRIVAL, validateCode(daasConfig.abacSqlServerReasonLateArrival(),TBL_DW_AUDT_RJCT_RESN,COL_DW_AUDT_RJCT_RESN_DS,rejectReasons));
		typeCodes.put(CodeType.ARRIVED,validateCode(daasConfig.abacSqlServerStatusArrived(),TBL_DW_FILE_LIST_TYP,COL_DW_FILE_LIST_DS,fileListCodeType));
		typeCodes.put(CodeType.UPDATED,validateCode(daasConfig.abacSqlServerStatusUpdated(),TBL_DW_FILE_LIST_TYP,COL_DW_FILE_LIST_ID,fileListCodeType));
			
		if ( errMsg.length() > 0 ) {
			throw new Exception(errMsg);
		}

	}
	
	public void dispose() {
		
		try {
			abac.dispose();
		} catch (Exception ex) {
			
		}
	}

	public int getTypeCode(CodeType typeCode) throws Exception {
		
		if ( !typeCodes.containsKey(typeCode) ) {
			throw new Exception("Type Code: " + typeCode + " not found");
		} 
		
		return(typeCodes.get(typeCode));
	}
	
	public CodeType getCodeType(int statusId) throws Exception {
		
		Iterator<Map.Entry<CodeType,Integer>> it = typeCodes.entrySet().iterator();
		CodeType codeType = null;
	
		while(it.hasNext()){
			Map.Entry<CodeType,Integer> codeTypeEntry = it.next();
			if(codeTypeEntry.getValue().intValue() == statusId){
				return codeTypeEntry.getKey();
			}
					
		}
		
		return codeType;
	}
	public int createJobGroup(String subject) throws Exception {
		
		int jobGroupId = -1;
		ResultSet rset;
		
		sql.setLength(0);
		sql.append("select\n");
		sql.append("   a." + COL_DW_JOB_GRP_STRT_TS + "\n");
		sql.append("from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_GRP_XECT + " a with (NOLOCK)\n");
		sql.append("where a." + COL_DW_JOB_GRP_SUBJ_DS + " = '" + subject.replaceAll("'", "''") + "'\n" );
		sql.append("and   a." + COL_DW_JOB_GRP_END_TS + " is null\n");
		sql.append("order by\n");
		sql.append("   a." + COL_DW_JOB_GRP_STRT_TS + " desc\n");
		
		rset = abac.resultSet(sql.toString());
		
		if ( rset.next() ) {
			throw new Exception("Found existing open Job Group for " + subject + " started at " + rset.getTimestamp(COL_DW_JOB_GRP_STRT_TS));
		}
		
		rset.close();
		
		sql.setLength(0);
		sql.append("insert into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_GRP_XECT + "\n");
		sql.append("   (" + COL_DW_JOB_GRP_SUBJ_DS + "\n");
		sql.append("   ," + COL_DW_JOB_GRP_STRT_TS + ")\n");
		sql.append("  values ('" + subject.replaceAll("'", "''") + "'\n");
		sql.append("         ,CURRENT_TIMESTAMP);\n");
		
		abac.executeUpdate(sql.toString());
		
		sql.setLength(0);
		sql.append("select @@IDENTITY as " + COL_DW_JOB_GRP_XECT_ID + "\n");
		
		rset = abac.resultSet(sql.toString());
		
		rset.next();

		jobGroupId = rset.getInt(COL_DW_JOB_GRP_XECT_ID);
		
		rset.close();
		
		abac.commit();
		
		return(jobGroupId);
		
	}

	public void closeJobGroup(int jobId
			                 ,short reasonCode
			                 ,String reasonDesc) throws Exception {
		
		sql.setLength(0);
		sql.append("update " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_GRP_XECT + "\n");
		sql.append("  set " + COL_DW_JOB_GRP_END_TS + " = CURRENT_TIMESTAMP\n");
		sql.append("     ," + COL_LAST_DW_ERR_RESN_CD + " = " + reasonCode + "\n");
	sql.append("     ," + COL_LAST_DW_ERR_RESN_DS + " = '" + reasonDesc.toUpperCase().replaceAll("'", "''") + "'\n");
		sql.append("where " + COL_DW_JOB_GRP_XECT_ID + " = " + jobId + "\n");
		
		abac.executeUpdate(sql.toString());
		
		abac.commit();
	}
	
	public int createJob(int jobGroupId
			            ,int seqNum
			            ,String jobName) throws Exception {
		
		int jobId = -1;
		ResultSet rset;
		
		sql.setLength(0);
		sql.append("select\n");
		sql.append("   a." + COL_DW_JOB_STRT_TS + "\n");
		sql.append("  ,a." + COL_DW_JOB_XECT_SEQ_NU + "\n");
		sql.append("from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_XECT + " a with (NOLOCK)\n");
		sql.append("where a." + COL_DW_JOB_GRP_XECT_ID + " = " + jobGroupId + "\n" );
		sql.append("and   a." + COL_DW_JOB_END_TS + " is null\n");
		sql.append("order by\n");
		sql.append("   a." + COL_DW_JOB_STRT_TS + " desc\n");
		
		rset = abac.resultSet(sql.toString());
		
		if ( rset.next() ) {
			throw new Exception("Found existing open Job for Job Group ID = " + jobGroupId + " Sequence Number = " + rset.getShort(COL_DW_JOB_XECT_SEQ_NU) + " started at " + rset.getTimestamp(COL_DW_JOB_STRT_TS));
		}
		
		rset.close();
		
		sql.setLength(0);
		sql.append("insert into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_XECT + "\n");
		sql.append("   (" + COL_DW_JOB_GRP_XECT_ID + "\n");
		sql.append("   ," + COL_DW_JOB_XECT_SEQ_NU + "\n");
		sql.append("   ," + COL_DW_JOB_NA + "\n");
		sql.append("   ," + COL_DW_JOB_STRT_TS + ")\n");
		sql.append("  values (" + jobGroupId + "\n");
		sql.append("         ," + seqNum + "\n");
		sql.append("         ,'" + jobName.replaceAll("'", "''") + "'\n");
		sql.append("         ,CURRENT_TIMESTAMP);\n");
		
		abac.executeUpdate(sql.toString());
		
		sql.setLength(0);
		sql.append("select @@IDENTITY as " + COL_DW_JOB_XECT_ID + "\n");
		
		rset = abac.resultSet(sql.toString());
		
		rset.next();

		jobId = rset.getInt(COL_DW_JOB_XECT_ID);
		
		rset.close();
		
		abac.commit();
		
		return(jobId);
		
	}

	public void closeJob(int jobId
			            ,short reasonCode
			            ,String reasonDesc) throws Exception {
		
		sql.setLength(0);
		sql.append("update " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_XECT + "\n");
		sql.append("  set " + COL_DW_JOB_END_TS + " = CURRENT_TIMESTAMP\n");
		sql.append("     ," + COL_LAST_DW_ERR_RESN_CD + " = " + reasonCode + "\n");
		sql.append("     ," + COL_LAST_DW_ERR_RESN_DS + " = '" + reasonDesc.toUpperCase().replaceAll("'", "''") + "'\n");
		sql.append("where " + COL_DW_JOB_XECT_ID + " = " + jobId + "\n");
		
		abac.executeUpdate(sql.toString());
		
		abac.commit();
	}
	
	public ABaCList setupList(int jobId
			             ,FileSystem hdfs) throws Exception {

		StringBuffer holdSql = new StringBuffer();
		
		Path destPathRoot = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + jobId);
		Path destPathSource = new Path(destPathRoot.toString() + Path.SEPARATOR + "source");
		Path destPathReject = new Path(destPathRoot.toString() + Path.SEPARATOR + "reject");
		
		HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathRoot, daasConfig.displayMsgs());
		HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathSource, daasConfig.displayMsgs());
		HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathReject, daasConfig.displayMsgs());
		
		sql.setLength(0);
		sql.append("CREATE TABLE #DW_FILE_ID_LIST(\n");
		sql.append("   DW_FILE_ID int NOT NULL PRIMARY KEY);\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());

		sql.setLength(0);
		sql.append("insert into #DW_FILE_ID_LIST (\n");
		sql.append("   " + COL_DW_FILE_ID + ")\n");
		sql.append("     select\n"); 
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("     from (\n");
		sql.append("          select\n"); 
		sql.append("             a." + COL_DW_FILE_ID + "\n");
		sql.append("            ,a." + COL_CAL_DT + "\n");
		sql.append("            ,a." + COL_TERR_CD + "\n");
		sql.append("            ,a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("            ,a." + COL_FILE_DW_ARRV_TS + "\n");
		sql.append("            ,a." + COL_FILE_MKT_OGIN_TS + "\n");	  
		sql.append("            ,ROW_NUMBER() over(order by a." + COL_CAL_DT + ", a." + COL_FILE_DW_ARRV_TS + ", a." + COL_FILE_MKT_OGIN_TS + ") as ROW_NUM\n");
		sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE +  " a with (NOLOCK)\n");
		sql.append("          inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_DATA_TYP + " b with (NOLOCK)\n");
		sql.append("            on (b." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + ")\n");
		sql.append("          where b." + COL_DW_DATA_TYP_DS + " = '" + daasConfig.fileType() + "'\n");
		sql.append("          and   a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.VALIDATED) + "\n");
		sql.append("          and   a." + COL_CAL_DT + " <= GETDATE()) a\n");
		sql.append("          where a.ROW_NUM <= " + daasConfig.abacSqlServerNumFilesLimit() + ";\n");		

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
		sql.setLength(0);
		sql.append("update " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + "\n");
		sql.append("   set " + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.READY) + "\n");
		sql.append("where " + COL_DW_FILE_ID + " in (select " + COL_DW_FILE_ID + " from #DW_FILE_ID_LIST);\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());

		sql.setLength(0);
		sql.append("insert into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + "\n");
		sql.append("  (" + COL_DW_FILE_ID + "\n");
		sql.append("  ," + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("  ," + COL_DW_SUB_FILE_NA + ")\n");
		sql.append("     select\n");
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,b." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("       ,b." + COL_DW_SUB_FILE_NA + "\n");
		sql.append("     from (\n");
		sql.append("          select\n");
		sql.append("             a." + COL_DW_FILE_ID + "\n");
		sql.append("            ,a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
		sql.append("          inner join #DW_FILE_ID_LIST b\n");
		sql.append("            on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")) a\n");
		sql.append("     inner join (\n");
		sql.append("          select\n");
		sql.append("             b." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("            ,a." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("            ,cast('' as varchar(80)) as " + COL_DW_SUB_FILE_NA + "\n");
		sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_DATA_TYP + " a with (NOLOCK)\n");
		sql.append("          inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_DATA_TYP + " b with (NOLOCK)\n");
		sql.append("            on (b." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + ")) b\n");
		sql.append("       on (b." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + ")\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
		sql.setLength(0);
		sql.append("CREATE TABLE #DW_FILE_STUS(\n");
		sql.append("   DW_FILE_ID int NOT NULL PRIMARY KEY\n");
		sql.append("  ,DW_AUDT_STUS_TYP_ID tinyint NOT NULL\n");
		sql.append("  ,DW_AUDT_RJCT_RESN_ID tinyint NOT NULL\n");
		sql.append("  ,UPDT_FL tinyint NOT NULL);\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());

		if ( daasConfig.fileApplyCompanyOwnedFilterTerrList().length() > 0 ) {
			sql.setLength(0);
			sql.append("insert into #DW_FILE_STUS\n");
			sql.append("  (" + COL_DW_FILE_ID + "\n");
			sql.append("  ," + COL_DW_AUDT_STUS_TYP_ID + "\n");
			sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + "\n");
			sql.append("  ,UPDT_FL)\n");
			sql.append("     select\n"); 
			sql.append("        a." + COL_DW_FILE_ID + "\n");
			sql.append("       ,cast(" + typeCodes.get(CodeType.REJECTED) + " as tinyint) as " + COL_DW_AUDT_STUS_TYP_ID + "\n");
			sql.append("       ,cast(" + typeCodes.get(CodeType.NON_MCOPCO) + " as tinyint) as " + COL_DW_AUDT_RJCT_RESN_ID + "\n");
			sql.append("       ,cast(0 as tinyint) as UPDT_FL\n");
			sql.append("     from (\n");
			sql.append("          select\n");
			sql.append("             a." + COL_DW_FILE_ID + "\n");
			sql.append("            ,a." + COL_CAL_DT + "\n");
			sql.append("            ,a." + COL_TERR_CD + "\n");
			sql.append("            ,case when a." + COL_TERR_CD + " = 840 then\n");
			sql.append("               case len(a." + COL_LGCY_LCL_RFR_DEF_CD + ") when 1 then '0000' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
			sql.append("                                               when 2 then '000' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n"); 
			sql.append("                                               when 3 then '00' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");  
			sql.append("                                               when 4 then '0' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n"); 
			sql.append("                                               else a." + COL_LGCY_LCL_RFR_DEF_CD + " end\n");
			sql.append("               else a." + COL_LGCY_LCL_RFR_DEF_CD + " end as " + COL_LGCY_LCL_RFR_DEF_CD + "\n");
			sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
			sql.append("          inner join #DW_FILE_ID_LIST b\n");
			sql.append("            on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");
			sql.append("          where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.READY) + ") a\n");
			sql.append("     left outer join " + daasConfig.abacSqlServerDb() + "." + TBL_REST_OWSH + " b with (NOLOCK)\n");
			sql.append("       on (b." + COL_CTRY_ISO_NU + " = a." + COL_TERR_CD + "\n");
			sql.append("           and b." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
			sql.append("           and a." + COL_CAL_DT + " between b." + COL_REST_OWSH_EFF_DT + " and b." + COL_REST_OWSH_END_DT + ")\n");
			sql.append("     left outer join " + daasConfig.abacSqlServerDb() + "." + TBL_REST_OWSH_XCPT_LIST + " c with (NOLOCK)\n");
			sql.append("       on (c." + COL_CTRY_ISO_NU + " = a." + COL_TERR_CD + "\n");
			sql.append("           and c." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
			sql.append("           and a." + COL_CAL_DT + " between c." + COL_XCPT_EFF_DT + " and c." + COL_XCPT_END_DT + ")\n");
			sql.append("     where a." + COL_TERR_CD + " in (" + daasConfig.fileApplyCompanyOwnedFilterTerrList() + ")\n");
			sql.append("     and   coalesce(b." + COL_REST_OWSH_TYP_SHRT_DS + ",'X') <> 'M'\n");
			sql.append("     and   c." + COL_LGCY_LCL_RFR_DEF_CD  + " is null\n");

			if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
				System.out.println(sql.toString());
			}
			
			abac.executeUpdate(sql.toString());
			
			updateStatusReason(hdfs,jobId,destPathSource,destPathReject);
		}

		sql.setLength(0);
		sql.append("insert into #DW_FILE_STUS\n");
		sql.append("  (" + COL_DW_FILE_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("  ,UPDT_FL)\n");
		sql.append("     select\n"); 
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,cast(" + typeCodes.get(CodeType.REJECTED) + " as tinyint) as " + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("       ,cast(" + typeCodes.get(CodeType.LATE_ARRIVAL) + " as tinyint) as " + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("       ,cast(0 as tinyint) as UPDT_FL\n");
		sql.append("     from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
		sql.append("     inner join #DW_FILE_ID_LIST b\n");
		sql.append("       on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");
		sql.append("     inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a1 with (NOLOCK)\n");
		sql.append("       on (a1." + COL_TERR_CD + " = a." + COL_TERR_CD + "\n");
		sql.append("           and a1." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("           and a1." + COL_CAL_DT + " = a." + COL_CAL_DT + "\n");
		sql.append("           and a1." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + ")\n");
		sql.append("     where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.READY) + "\n");
		sql.append("     and   a1." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.SUCCESSFUL) + "\n");
		sql.append("     and ((a." + COL_FILE_MKT_OGIN_TS + " < a1." + COL_FILE_MKT_OGIN_TS + ")\n");
		sql.append("     or   (a." + COL_FILE_MKT_OGIN_TS + " = a1." + COL_FILE_MKT_OGIN_TS + "\n");
		sql.append("     and   a." + COL_FILE_DW_ARRV_TS + " <= a1." + COL_FILE_DW_ARRV_TS + "))\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
		updateStatusReason(hdfs,jobId,destPathSource,destPathReject);

		sql.setLength(0);
		sql.append("insert into #DW_FILE_STUS\n");
		sql.append("  (" + COL_DW_FILE_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("  ,UPDT_FL)\n");
		sql.append("     select\n"); 
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,cast(" + typeCodes.get(CodeType.REJECTED) + " as tinyint) as " + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("       ,cast(" + typeCodes.get(CodeType.INTERBATCH_DUPLICATE) + " as tinyint) as " + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("       ,cast(0 as tinyint) as UPDT_FL\n");
		sql.append("     from (\n");
		sql.append("       select\n");
		sql.append("          a." + COL_DW_FILE_ID + "\n");
		sql.append("         ,ROW_NUMBER() over(partition by a." + COL_TERR_CD + ", a." + COL_LGCY_LCL_RFR_DEF_CD + ", a." + COL_CAL_DT + " order by a." + COL_FILE_MKT_OGIN_TS + " desc, a." + COL_FILE_DW_ARRV_TS + " desc) as ROW_NUM\n");
		sql.append("       from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
		sql.append("       inner join #DW_FILE_ID_LIST b\n");
		sql.append("         on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");
		sql.append("       where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.READY) + ") a\n");
		sql.append("     where ROW_NUM > 1\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
		updateStatusReason(hdfs,jobId,destPathSource,destPathReject);

		sql.setLength(0);
		sql.append("insert into #DW_FILE_STUS\n");
		sql.append("  (" + COL_DW_FILE_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("  ,UPDT_FL)\n");
		sql.append("     select\n");
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,cast(" + typeCodes.get(CodeType.PROCESSING) + " as TINYINT) as " + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("       ,cast(0 as tinyint) as " + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("       ,cast(0 as tinyint) as UPDT_FL\n");
		sql.append("     from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
		sql.append("     inner join #DW_FILE_ID_LIST b\n");
		sql.append("       on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");
		sql.append("     where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.READY) + ";\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
		updateStatusReason(hdfs,jobId,destPathSource,null);

		sql.setLength(0);
		sql.append("insert into " + daasConfig.abacSqlServerDb() + "." +  TBL_DW_FILE_JOB_XECT_ASSC + "\n");
		sql.append("  select\n");
		sql.append("     cast(" + jobId + " as int) as " + COL_DW_JOB_XECT_ID + "\n");
		sql.append("    ,a." + COL_DW_FILE_ID + "\n");
		sql.append("    ,a." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("  from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " a with (NOLOCK)\n");
		sql.append("  inner join #DW_FILE_STUS b\n");
		sql.append("    on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());

		ResultSet rset;
		ABaCList list=new ABaCList(daasConfig.fileFileSeparatorCharacter());
		ABaCListItem itm=null;
		ABaCListSubItem subItm;
		int lastFileId = -1;

		String fileMaskNonCompanyOwnedTerrList = daasConfig.fileMaskNonCompanyOwnedTerrList(); 
		String fileMaskAllTerrList = daasConfig.fileMaskAllTerrList(); 
		String fileEncryptNonCompanyOwnedTerrList = daasConfig.fileEncryptNonCompanyOwnedTerrList();
		String fileEncryptAllTerrList = daasConfig.fileEncryptAllTerrList();
		
		if ( fileMaskNonCompanyOwnedTerrList.length() == 0 ) {
			fileMaskNonCompanyOwnedTerrList = "-9999";
		}
		
		if ( fileMaskAllTerrList.length() == 0 ) {
			fileMaskAllTerrList = "-9999";
		}
		
		if ( fileEncryptNonCompanyOwnedTerrList.length() == 0 ) {
			fileEncryptNonCompanyOwnedTerrList = "-9999";
		}
		
		if ( fileEncryptAllTerrList.length() == 0 ) {
			fileEncryptAllTerrList = "-9999";
		}
		
		sql.setLength(0);
		sql.append("select\n");
		sql.append("   a." + COL_DW_FILE_ID + "\n");
		sql.append("  ,a." + COL_DW_FILE_NA + "\n");
		sql.append("  ,a." + COL_FILE_PATH_DS + "\n");
		sql.append("  ,a." + COL_CAL_DT + "\n");
		sql.append("  ,a." + COL_TERR_CD + "\n");
		sql.append("  ,a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("  ,a." + COL_MCD_GBAL_LCAT_ID_NU + "\n");
		sql.append("  ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ,b." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("  ,c." + COL_DW_SUB_FILE_DATA_TYP_CD + "\n");
		sql.append("  ,b." + COL_DW_AUDT_STUS_TYP_ID + " as SUB_FILE" + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ,cast(\n");
		sql.append("    case when a.TERR_CD in (" + fileMaskAllTerrList + ") then 'M' else\n");
		sql.append("      case when a.TERR_CD in (" + fileMaskNonCompanyOwnedTerrList + ") then case when coalesce(d." + COL_REST_OWSH_TYP_SHRT_DS + ",' ') <> 'M' then 'M' else ' ' end\n");
		sql.append("    else\n");
		sql.append("      case when a.TERR_CD in (" + fileEncryptAllTerrList + ") then 'E' else\n");
		sql.append("        case when a.TERR_CD in (" + fileEncryptNonCompanyOwnedTerrList + ") then case when coalesce(d." + COL_REST_OWSH_TYP_SHRT_DS + ",' ') <> 'M' then 'E' else ' ' end else\n");
		sql.append("        ' ' end end end end as CHAR(1)) as OBFUSCATE_TYP_CD\n");
		sql.append("  ,coalesce(d." + COL_REST_OWSH_TYP_SHRT_DS + ",' ') as " + COL_REST_OWSH_TYP_SHRT_DS + "\n");
		sql.append("from (\n");
		sql.append("     select\n");
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,a." + COL_DW_FILE_NA + "\n");
		sql.append("       ,a." + COL_FILE_PATH_DS + "\n");
		sql.append("       ,a." + COL_CAL_DT + "\n");
		sql.append("       ,a." + COL_TERR_CD + "\n");
		sql.append("       ,a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("       ,coalesce(a." + COL_MCD_GBAL_LCAT_ID_NU + ",b." + COL_MCD_GBAL_LCAT_ID_NU + ",0) as " + COL_MCD_GBAL_LCAT_ID_NU + "\n");
		sql.append("       ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("       ,a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("     from (\n");
		sql.append("          select\n");
		sql.append("             a." + COL_DW_FILE_ID + "\n");
		sql.append("            ,a." + COL_DW_FILE_NA + "\n");
		sql.append("            ,a." + COL_FILE_PATH_DS + "\n");
		sql.append("            ,substring(cast(a." + COL_CAL_DT + " as varchar(10)),1,4) + substring(cast(a." + COL_CAL_DT + " as varchar(10)),6,2) + substring(cast(a." + COL_CAL_DT + " as varchar(10)),9,2) as " + COL_CAL_DT + "\n");
		sql.append("            ,a." + COL_TERR_CD + "\n");
		sql.append("            ,case when a." + COL_TERR_CD + "= 840 then\n");
		sql.append("               case len(a." + COL_LGCY_LCL_RFR_DEF_CD + ") when 1 then '0000' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           when 2 then '000' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           when 3 then '00' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           when 4 then '0' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           else a." + COL_LGCY_LCL_RFR_DEF_CD + " end\n");
		sql.append("              else a." + COL_LGCY_LCL_RFR_DEF_CD + " end as " + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("            ,a." + COL_MCD_GBAL_LCAT_ID_NU + "\n");
		sql.append("            ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("            ,a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");

		sql.append("          inner join #DW_FILE_ID_LIST b\n");
		sql.append("            on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");

		sql.append("          where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.PROCESSING) + ") a\n");
		
//		sql.append("          inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_DATA_TYP + " b with (NOLOCK)\n");
//		sql.append("            on (b." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + ")\n");
//		sql.append("          where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.PROCESSING) + "\n");
//		sql.append("          and   b." + COL_DW_DATA_TYP_DS + " = '" + daasConfig.fileType() + "') a\n");
		
		sql.append("     left outer join " + daasConfig.abacSqlServerDb() + "." + TBL_GBAL_LCAT + " b with (NOLOCK)\n");
		sql.append("     on (b." + COL_CTRY_ISO_NU + " = a." + COL_TERR_CD + "\n");
		sql.append("         and b." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + ")) a\n");
		sql.append("inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " b with (NOLOCK)\n");
		sql.append("  on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");    
		sql.append("inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_DATA_TYP + " c with (NOLOCK)\n");
		sql.append("  on (c." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("      and c." + COL_DW_SUB_FILE_DATA_TYP_ID + " = b." + COL_DW_SUB_FILE_DATA_TYP_ID + ")\n");
		sql.append("left outer join " + daasConfig.abacSqlServerDb() + "." + TBL_REST_OWSH + " d with (NOLOCK)\n");
		sql.append("  on (d." + COL_CTRY_ISO_NU + " = a." + COL_TERR_CD + "\n");
		sql.append("      and d." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + ")\n");
		sql.append("order by\n"); 
		sql.append("   a." + COL_DW_FILE_ID + "\n");
		sql.append("  ,b." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		rset = abac.resultSet(sql.toString());
		
		while (rset.next()) {
			if ( rset.getInt(COL_DW_FILE_ID) != lastFileId ) {
				lastFileId = rset.getInt(COL_DW_FILE_ID);

				itm = list.addItem(rset.getInt(COL_DW_FILE_ID)
				          ,rset.getString(COL_DW_FILE_NA)
					      ,rset.getString(COL_FILE_PATH_DS)
				          ,rset.getString(COL_CAL_DT)
				          ,rset.getInt(COL_TERR_CD)
				          ,rset.getString(COL_LGCY_LCL_RFR_DEF_CD)
				          ,rset.getBigDecimal(COL_MCD_GBAL_LCAT_ID_NU)
				          ,rset.getInt(COL_DW_AUDT_STUS_TYP_ID)
				          ,0
				          ,rset.getString("OBFUSCATE_TYP_CD")
				          ,rset.getString(COL_REST_OWSH_TYP_SHRT_DS)
				          ,typeCodes);
			}
			
			subItm = itm.addSubItem(rset.getInt(COL_DW_SUB_FILE_DATA_TYP_ID), rset.getString(COL_DW_SUB_FILE_DATA_TYP_CD), rset.getInt("SUB_FILE" + COL_DW_AUDT_STUS_TYP_ID));
		}
		
		rset.close();

		sql.setLength(0);
		sql.append("DROP TABLE #DW_FILE_ID_LIST\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
		sql.setLength(0);
		sql.append("DROP TABLE #DW_FILE_STUS\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());

		abac.commit();
		
		return(list);
		
	}
	
	//@mc41946: Modified for Merge Process -Multiple runs
	public ABaCList setupList2(int jobId
			                  ,FileSystem hdfs,boolean multipleInject ,String terrCDList) throws Exception {

		StringBuffer holdSql = new StringBuffer();
		
		Path destPathRoot = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + jobId);
		Path destPathSource = new Path(destPathRoot.toString() + Path.SEPARATOR + "source");
		Path destPathReject = new Path(destPathRoot.toString() + Path.SEPARATOR + "reject");
		Path destPathCache = new Path(destPathRoot.toString() + Path.SEPARATOR + "cache");
		//Path destPathCurrentCache = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsWorkSubDir()+ Path.SEPARATOR + "current" + Path.SEPARATOR + "cache");
		
		HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathRoot, daasConfig.displayMsgs());
		HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathSource, daasConfig.displayMsgs());
		HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathReject, daasConfig.displayMsgs());
		HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathCache, daasConfig.displayMsgs());
		//HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathCurrentCache, daasConfig.displayMsgs());
			
		/*sql.setLength(0);
		sql.append("CREATE TABLE #DW_FILE_ID_LIST(\n");
		sql.append("   DW_FILE_ID int NOT NULL PRIMARY KEY);\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());*/
		
        sql.setLength(0);
		
		sql.append("TRUNCATE TABLE "+ daasConfig.abacSqlServerDb() + ".DW_FILE_STUS \n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		abac.commit();

		sql.setLength(0);
		sql.append("insert into "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST (\n");
		sql.append("   " + COL_DW_FILE_ID + ",UPDT_FL)\n");
		sql.append("     select\n"); 
		sql.append("        a." + COL_DW_FILE_ID + " ,"+ typeCodes.get(CodeType.ARRIVED)+"\n");
		sql.append("     from (\n");
		sql.append("          select\n"); 
		sql.append("             a." + COL_DW_FILE_ID + "\n");
		sql.append("            ,a." + COL_CAL_DT + "\n");
		sql.append("            ,a." + COL_TERR_CD + "\n");
		sql.append("            ,a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("            ,a." + COL_FILE_DW_ARRV_TS + "\n");
		sql.append("            ,a." + COL_FILE_MKT_OGIN_TS + "\n");	  
		sql.append("            ,ROW_NUMBER() over(order by a." + COL_CAL_DT + ", a." + COL_FILE_DW_ARRV_TS + ", a." + COL_FILE_MKT_OGIN_TS + ") as ROW_NUM\n");
		sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE +  " a with (NOLOCK)\n");
		sql.append("          inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_DATA_TYP + " b with (NOLOCK)\n");
		sql.append("            on (b." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + ")\n");
		sql.append("          where b." + COL_DW_DATA_TYP_DS + " = '" + daasConfig.fileType() + "'\n");
		sql.append("          and   a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.VALIDATED) + "\n");
		//@mc41946: Modified for Merge Process -Multiple runs
		if (multipleInject) {
			sql.append("          and   a." + COL_TERR_CD + " IN (" + terrCDList + ")\n");
		} else {
			if(daasConfig.nonmergedZipFile())
			{
			  sql.append("          and   a." + COL_TERR_CD + " IN (" + terrCDList + ")\n");
			}else
			{
				sql.append("          and   a." + COL_TERR_CD + " NOT IN (" + terrCDList	+ ")\n");
			}
			
		}
		//@mc41946: Modified for Merge Process -Multiple runs
		sql.append("          and   a." + COL_CAL_DT + " <= GETDATE()) a\n");
		sql.append("          where a.ROW_NUM <= " + daasConfig.abacSqlServerNumFilesLimit() + "; \n");	
		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
		sql.setLength(0);
		sql.append("delete from "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST where DW_FILE_ID in (\n");
		sql.append("select	a." + COL_DW_FILE_ID + " from ( \n");
		sql.append("     select\n"); 
		sql.append("             a." + COL_DW_FILE_ID + "\n");
		sql.append("            , a." + COL_TERR_CD + "\n");
		sql.append("            ,a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("            ,a." + COL_CAL_DT + "\n");
		sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE +  " a with (NOLOCK)\n");
		sql.append("          inner join "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST b \n");
		sql.append("         on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + " AND b.UPDT_FL="+ typeCodes.get(CodeType.ARRIVED)+")) a\n");
		sql.append("          inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE +  " b with (NOLOCK)\n");
		sql.append("            on (b." + COL_TERR_CD + " = a." + COL_TERR_CD + " \n");
		sql.append("            and b." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("            and b." + COL_CAL_DT + " = a." + COL_CAL_DT + ")\n");
		sql.append("          where b." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.PROCESSING) + " );\n");
		
		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());				
		
		sql.setLength(0);
		sql.append("update " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + "\n");
		sql.append("   set " + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.READY) + "\n");
		sql.append("where " + COL_DW_FILE_ID + " in (select " + COL_DW_FILE_ID + " from "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST where UPDT_FL="+ typeCodes.get(CodeType.ARRIVED)+");\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());

		sql.setLength(0);
		sql.append("insert into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + "\n");
		sql.append("  (" + COL_DW_FILE_ID + "\n");
		sql.append("  ," + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("  ," + COL_DW_SUB_FILE_NA + ")\n");
		sql.append("     select\n");
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,b." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("       ,b." + COL_DW_SUB_FILE_NA + "\n");
		sql.append("     from (\n");
		sql.append("          select\n");
		sql.append("             a." + COL_DW_FILE_ID + "\n");
		sql.append("            ,a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
		sql.append("          inner join "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST b\n");
		sql.append("            on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + "  AND b.UPDT_FL="+ typeCodes.get(CodeType.ARRIVED)+")) a\n");
		sql.append("     inner join (\n");
		sql.append("          select\n");
		sql.append("             b." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("            ,a." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("            ,cast('' as varchar(80)) as " + COL_DW_SUB_FILE_NA + "\n");
		sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_DATA_TYP + " a with (NOLOCK)\n");
		sql.append("          inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_DATA_TYP + " b with (NOLOCK)\n");
		sql.append("            on (b." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + ")) b\n");
		sql.append("       on (b." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + ")\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
	/*	sql.setLength(0);
		sql.append("CREATE TABLE #DW_FILE_STUS(\n");
		sql.append("   DW_FILE_ID int NOT NULL PRIMARY KEY\n");
		sql.append("  ,DW_AUDT_STUS_TYP_ID tinyint NOT NULL\n");
		sql.append("  ,DW_AUDT_RJCT_RESN_ID tinyint NOT NULL\n");
		sql.append("  ,UPDT_FL tinyint NOT NULL);\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		//abac.executeUpdate(sql.toString());
*/
		if ( daasConfig.fileApplyCompanyOwnedFilterTerrList().length() > 0 ) {
			sql.setLength(0);
			sql.append("insert into "+ daasConfig.abacSqlServerDb() + ".DW_FILE_STUS\n");
			sql.append("  (" + COL_DW_FILE_ID + "\n");
			sql.append("  ," + COL_DW_AUDT_STUS_TYP_ID + "\n");
			sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + "\n");
			sql.append("  ,UPDT_FL)\n");
			sql.append("     select\n"); 
			sql.append("        a." + COL_DW_FILE_ID + "\n");
			sql.append("       ,cast(" + typeCodes.get(CodeType.REJECTED) + " as tinyint) as " + COL_DW_AUDT_STUS_TYP_ID + "\n");
			sql.append("       ,cast(" + typeCodes.get(CodeType.NON_MCOPCO) + " as tinyint) as " + COL_DW_AUDT_RJCT_RESN_ID + "\n");
			sql.append("       ,cast(0 as tinyint) as UPDT_FL\n");
			sql.append("     from (\n");
			sql.append("          select\n");
			sql.append("             a." + COL_DW_FILE_ID + "\n");
			sql.append("            ,a." + COL_CAL_DT + "\n");
			sql.append("            ,a." + COL_TERR_CD + "\n");
			sql.append("            ,case when a." + COL_TERR_CD + " = 840 then\n");
			sql.append("               case len(a." + COL_LGCY_LCL_RFR_DEF_CD + ") when 1 then '0000' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
			sql.append("                                               when 2 then '000' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n"); 
			sql.append("                                               when 3 then '00' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");  
			sql.append("                                               when 4 then '0' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n"); 
			sql.append("                                               else a." + COL_LGCY_LCL_RFR_DEF_CD + " end\n");
			sql.append("               else a." + COL_LGCY_LCL_RFR_DEF_CD + " end as " + COL_LGCY_LCL_RFR_DEF_CD + "\n");
			sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
			sql.append("          inner join "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST b\n");
			sql.append("            on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + " AND b.UPDT_FL="+ typeCodes.get(CodeType.ARRIVED)+")\n");
			sql.append("          where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.READY) + ") a\n");
			sql.append("     left outer join " + daasConfig.abacSqlServerDb() + "." + TBL_REST_OWSH + " b with (NOLOCK)\n");
			sql.append("       on (b." + COL_CTRY_ISO_NU + " = a." + COL_TERR_CD + "\n");
			sql.append("           and b." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
			sql.append("           and a." + COL_CAL_DT + " between b." + COL_REST_OWSH_EFF_DT + " and b." + COL_REST_OWSH_END_DT + ")\n");
			sql.append("     left outer join " + daasConfig.abacSqlServerDb() + "." + TBL_REST_OWSH_XCPT_LIST + " c with (NOLOCK)\n");
			sql.append("       on (c." + COL_CTRY_ISO_NU + " = a." + COL_TERR_CD + "\n");
			sql.append("           and c." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
			sql.append("           and a." + COL_CAL_DT + " between c." + COL_XCPT_EFF_DT + " and c." + COL_XCPT_END_DT + ")\n");
			sql.append("     where a." + COL_TERR_CD + " in (" + daasConfig.fileApplyCompanyOwnedFilterTerrList() + ")\n");
			sql.append("     and   coalesce(b." + COL_REST_OWSH_TYP_SHRT_DS + ",'X') <> 'M'\n");
			sql.append("     and   c." + COL_LGCY_LCL_RFR_DEF_CD  + " is null\n");

			if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
				System.out.println(sql.toString());
			}
			
			abac.executeUpdate(sql.toString());
			
			updateStatusReason(true);
		}

		
		sql.setLength(0);
		/**
		sql.append("insert into #DW_FILE_STUS\n");
		sql.append("  (" + COL_DW_FILE_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("  ,UPDT_FL)\n");
		sql.append("     select\n"); 
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,cast(" + typeCodes.get(CodeType.REJECTED) + " as tinyint) as " + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("       ,cast(" + typeCodes.get(CodeType.LATE_ARRIVAL) + " as tinyint) as " + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("       ,cast(0 as tinyint) as UPDT_FL\n");
		sql.append("     from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
		sql.append("     inner join #DW_FILE_ID_LIST b\n");
		sql.append("       on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");
		sql.append("     inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a1 with (NOLOCK)\n");
		sql.append("       on (a1." + COL_TERR_CD + " = a." + COL_TERR_CD + "\n");
		sql.append("           and a1." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("           and a1." + COL_CAL_DT + " = a." + COL_CAL_DT + "\n");
		sql.append("           and a1." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + ")\n");
		sql.append("     where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.READY) + "\n");
		sql.append("     and   a1." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.SUCCESSFUL) + "\n");
		sql.append("     and ((a." + COL_FILE_MKT_OGIN_TS + " < a1." + COL_FILE_MKT_OGIN_TS + ")\n");
		sql.append("     or   (a." + COL_FILE_MKT_OGIN_TS + " = a1." + COL_FILE_MKT_OGIN_TS + "\n");
		sql.append("     and   a." + COL_FILE_DW_ARRV_TS + " <= a1." + COL_FILE_DW_ARRV_TS + "))\n");
		**/
		
		sql.append("insert into "+ daasConfig.abacSqlServerDb() + ".DW_FILE_STUS\n");
        sql.append("  (" + COL_DW_FILE_ID + "\n");
        sql.append("  ," + COL_DW_AUDT_STUS_TYP_ID + "\n");
        sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID +"\n");
        sql.append("  ,UPDT_FL)\n");
        sql.append("     select\n");
        sql.append("        a." + COL_DW_FILE_ID +"\n");
        sql.append("       ,a." + COL_DW_AUDT_STUS_TYP_ID +"\n");
        sql.append("       ,a." + COL_DW_AUDT_RJCT_RESN_ID +"\n");
        sql.append("       ,a.UPDT_FL\n");
        sql.append("     from (\n");
        sql.append("          select\n"); 
        sql.append("             a." + COL_DW_FILE_ID +"\n");
        sql.append("            ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
        sql.append("            ,a." + COL_DW_AUDT_RJCT_RESN_ID + "\n");
        sql.append("            ,a.UPDT_FL\n");
        sql.append("            ,a." + COL_FILE_MKT_OGIN_TS + "\n");
        sql.append("            ,a." + COL_FILE_DW_ARRV_TS + "\n");
        sql.append("            ,a.EXISTING_" + COL_FILE_MKT_OGIN_TS + "\n");
        sql.append("            ,a.EXISTING_" + COL_FILE_DW_ARRV_TS + "\n");
        sql.append("          from (\n");
        sql.append("               select\n");
        sql.append("                  a." + COL_DW_FILE_ID + "\n");
        sql.append("                 ,cast(" + typeCodes.get(CodeType.REJECTED) + " as tinyint) as " + COL_DW_AUDT_STUS_TYP_ID + "\n");
        sql.append("                 ,cast(" + typeCodes.get(CodeType.LATE_ARRIVAL) + " as tinyint) as " + COL_DW_AUDT_RJCT_RESN_ID + "\n");
        sql.append("                 ,cast(0 as tinyint) as UPDT_FL\n");
        sql.append("                 ,a." + COL_FILE_MKT_OGIN_TS + "\n");
        sql.append("                 ,a." + COL_FILE_DW_ARRV_TS + "\n");
        sql.append("                 ,a1." + COL_FILE_MKT_OGIN_TS + " as EXISTING_" + COL_FILE_MKT_OGIN_TS + "\n");
        sql.append("                 ,a1." + COL_FILE_DW_ARRV_TS + " as EXISTING_" + COL_FILE_DW_ARRV_TS + "\n");
        sql.append("                 ,row_number() over(partition by a." + COL_DW_FILE_ID + " order by a1." + COL_FILE_MKT_OGIN_TS + " desc, a1." + COL_FILE_DW_ARRV_TS + " desc) as ROW_NUM\n");
        sql.append("               from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
        sql.append("               inner join "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST b\n");
        sql.append("                 on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + " AND b.UPDT_FL="+ typeCodes.get(CodeType.ARRIVED)+")\n");
        sql.append("               inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a1 with (NOLOCK)\n");
        sql.append("                 on (a1." + COL_TERR_CD + " =    a." + COL_TERR_CD + "\n");
        sql.append("                     and a1." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
        sql.append("                     and a1." + COL_CAL_DT + " = a." + COL_CAL_DT + "\n");
        sql.append("                     and a1." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + ")\n");
        sql.append("               where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.READY) + "\n");
        sql.append("               and   a1." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.SUCCESSFUL) + ") a\n");
        sql.append("          where a.ROW_NUM = 1) a\n");
        sql.append("     where ((a." + COL_FILE_MKT_OGIN_TS + " < a.EXISTING_" + COL_FILE_MKT_OGIN_TS + ")\n");
        sql.append("     or     (a." + COL_FILE_MKT_OGIN_TS + " = a.EXISTING_" + COL_FILE_MKT_OGIN_TS + "\n");
        sql.append("     and     a." + COL_FILE_DW_ARRV_TS + " <= a.EXISTING_" + COL_FILE_DW_ARRV_TS + "))\n");


		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
		updateStatusReason(true);

		sql.setLength(0);
		sql.append("insert into "+ daasConfig.abacSqlServerDb() + ".DW_FILE_STUS\n");
		sql.append("  (" + COL_DW_FILE_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("  ,UPDT_FL)\n");
		sql.append("     select\n"); 
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,cast(" + typeCodes.get(CodeType.REJECTED) + " as tinyint) as " + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("       ,cast(" + typeCodes.get(CodeType.INTERBATCH_DUPLICATE) + " as tinyint) as " + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("       ,cast(0 as tinyint) as UPDT_FL\n");
		sql.append("     from (\n");
		sql.append("       select\n");
		sql.append("          a." + COL_DW_FILE_ID + "\n");
		sql.append("         ,ROW_NUMBER() over(partition by a." + COL_TERR_CD + ", a." + COL_LGCY_LCL_RFR_DEF_CD + ", a." + COL_CAL_DT + " order by a." + COL_FILE_MKT_OGIN_TS + " desc, a." + COL_FILE_DW_ARRV_TS + " desc) as ROW_NUM\n");
		sql.append("       from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
		sql.append("       inner join "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST b\n");
		sql.append("         on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + " AND b.UPDT_FL="+ typeCodes.get(CodeType.ARRIVED)+")\n");
		sql.append("       where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.READY) + ") a\n");
		sql.append("     where ROW_NUM > 1\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
		updateStatusReason(true);

		sql.setLength(0);
		sql.append("insert into "+ daasConfig.abacSqlServerDb() + ".DW_FILE_STUS\n");
		sql.append("  (" + COL_DW_FILE_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("  ,UPDT_FL)\n");
		sql.append("     select\n");
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,cast(" + typeCodes.get(CodeType.PROCESSING) + " as TINYINT) as " + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("       ,cast(0 as tinyint) as " + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		sql.append("       ,cast(0 as tinyint) as UPDT_FL\n");
		sql.append("     from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
		sql.append("     inner join "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST b\n");
		sql.append("       on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + " AND b.UPDT_FL="+ typeCodes.get(CodeType.ARRIVED)+")\n");
		sql.append("     where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.READY) + ";\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
		updateStatusReason(false);

		sql.setLength(0);
		sql.append("insert into " + daasConfig.abacSqlServerDb() + "." +  TBL_DW_FILE_JOB_XECT_ASSC + "\n");
		sql.append("  select\n");
		sql.append("     cast(" + jobId + " as int) as " + COL_DW_JOB_XECT_ID + "\n");
		sql.append("    ,a." + COL_DW_FILE_ID + "\n");
		sql.append("    ,a." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("  from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " a with (NOLOCK)\n");
		sql.append("  inner join "+ daasConfig.abacSqlServerDb() + ".DW_FILE_STUS b\n");
		sql.append("    on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());

		ResultSet rset;
		ABaCList list=new ABaCList(daasConfig.fileFileSeparatorCharacter());
		ABaCListItem itm=null;
		ABaCListSubItem subItm;
		int lastFileId = -1;
		String listLine = "";
		boolean firstSubItem = false;
		int rowCount = 0;

		String typeCodesValues = "";
		CodeType codeTypeValue;
		
		for (Map.Entry<CodeType, Integer> entry : typeCodes.entrySet()) {
			if ( typeCodesValues.length() > 0 ) {
				typeCodesValues += ",";
			}
			codeTypeValue = (CodeType)entry.getKey();
			typeCodesValues += codeTypeValue.ordinal() + "|" + entry.getValue(); 
		}

		String fileMaskNonCompanyOwnedTerrList = daasConfig.fileMaskNonCompanyOwnedTerrList(); 
		String fileMaskAllTerrList = daasConfig.fileMaskAllTerrList(); 
		String fileEncryptNonCompanyOwnedTerrList = daasConfig.fileEncryptNonCompanyOwnedTerrList();
		String fileEncryptAllTerrList = daasConfig.fileEncryptAllTerrList();
		
		if ( fileMaskNonCompanyOwnedTerrList.length() == 0 ) {
			fileMaskNonCompanyOwnedTerrList = "-9999";
		}
		
		if ( fileMaskAllTerrList.length() == 0 ) {
			fileMaskAllTerrList = "-9999";
		}
		
		if ( fileEncryptNonCompanyOwnedTerrList.length() == 0 ) {
			fileEncryptNonCompanyOwnedTerrList = "-9999";
		}
		
		if ( fileEncryptAllTerrList.length() == 0 ) {
			fileEncryptAllTerrList = "-9999";
		}

		sql.setLength(0);
		sql.append("select\n");
		sql.append("   a." + COL_DW_FILE_ID + "\n");
		sql.append("  ,a." + COL_DW_FILE_NA + "\n");
		sql.append("  ,a." + COL_FILE_PATH_DS + "\n");
		sql.append("  ,a." + COL_CAL_DT + "\n");
		sql.append("  ,a." + COL_TERR_CD + "\n");
		sql.append("  ,a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("  ,a." + COL_MCD_GBAL_LCAT_ID_NU + "\n");
		sql.append("  ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ,b." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("  ,c." + COL_DW_SUB_FILE_DATA_TYP_CD + "\n");
		sql.append("  ,b." + COL_DW_AUDT_STUS_TYP_ID + " as SUB_FILE" + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ,cast(\n");
		sql.append("    case when a.TERR_CD in (" + fileMaskAllTerrList + ") then 'M' else\n");
		sql.append("      case when a.TERR_CD in (" + fileMaskNonCompanyOwnedTerrList + ") then case when coalesce(d." + COL_REST_OWSH_TYP_SHRT_DS + ",' ') <> 'M' then 'M' else ' ' end\n");
		sql.append("    else\n");
		sql.append("      case when a.TERR_CD in (" + fileEncryptAllTerrList + ") then 'E' else\n");
		sql.append("        case when a.TERR_CD in (" + fileEncryptNonCompanyOwnedTerrList + ") then case when coalesce(d." + COL_REST_OWSH_TYP_SHRT_DS + ",' ') <> 'M' then 'E' else ' ' end else\n");
		sql.append("        ' ' end end end end as CHAR(1)) as OBFUSCATE_TYP_CD\n");
		sql.append("  ,coalesce(d." + COL_REST_OWSH_TYP_SHRT_DS + ",' ') as " + COL_REST_OWSH_TYP_SHRT_DS + "\n");
		sql.append("from (\n");
		sql.append("     select\n");
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,a." + COL_DW_FILE_NA + "\n");
		sql.append("       ,a." + COL_FILE_PATH_DS + "\n");
		sql.append("       ,a." + COL_CAL_DT + "\n");
		sql.append("       ,a." + COL_TERR_CD + "\n");
		sql.append("       ,a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("       ,coalesce(a." + COL_MCD_GBAL_LCAT_ID_NU + ",b." + COL_MCD_GBAL_LCAT_ID_NU + ",0) as " + COL_MCD_GBAL_LCAT_ID_NU + "\n");
		sql.append("       ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("       ,a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("     from (\n");
		sql.append("          select\n");
		sql.append("             a." + COL_DW_FILE_ID + "\n");
		sql.append("            ,a." + COL_DW_FILE_NA + "\n");
		sql.append("            ,a." + COL_FILE_PATH_DS + "\n");
		sql.append("            ,substring(cast(a." + COL_CAL_DT + " as varchar(10)),1,4) + substring(cast(a." + COL_CAL_DT + " as varchar(10)),6,2) + substring(cast(a." + COL_CAL_DT + " as varchar(10)),9,2) as " + COL_CAL_DT + "\n");
		sql.append("            ,a." + COL_TERR_CD + "\n");
		sql.append("            ,case when a." + COL_TERR_CD + "= 840 then\n");
		sql.append("               case len(a." + COL_LGCY_LCL_RFR_DEF_CD + ") when 1 then '0000' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           when 2 then '000' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           when 3 then '00' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           when 4 then '0' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           else a." + COL_LGCY_LCL_RFR_DEF_CD + " end\n");
		sql.append("              else a." + COL_LGCY_LCL_RFR_DEF_CD + " end as " + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("            ,a." + COL_MCD_GBAL_LCAT_ID_NU + "\n");
		sql.append("            ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("            ,a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
		sql.append("          inner join "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST b\n");
		sql.append("            on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID +" AND b.UPDT_FL="+ typeCodes.get(CodeType.ARRIVED)+" )\n");
		//sql.append("          where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.PROCESSING) + ") a\n");
		sql.append("            ) a\n");
		sql.append("     left outer join " + daasConfig.abacSqlServerDb() + "." + TBL_GBAL_LCAT + " b with (NOLOCK)\n");
		sql.append("     on (b." + COL_CTRY_ISO_NU + " = a." + COL_TERR_CD + "\n");
		sql.append("         and b." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + ")) a\n");
		sql.append("inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " b with (NOLOCK)\n");
		sql.append("  on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");    
		sql.append("inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_DATA_TYP + " c with (NOLOCK)\n");
		sql.append("  on (c." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("      and c." + COL_DW_SUB_FILE_DATA_TYP_ID + " = b." + COL_DW_SUB_FILE_DATA_TYP_ID + ")\n");
		sql.append("left outer join " + daasConfig.abacSqlServerDb() + "." + TBL_REST_OWSH + " d with (NOLOCK)\n");
		sql.append("  on (d." + COL_CTRY_ISO_NU + " = a." + COL_TERR_CD + "\n");
		sql.append("      and d." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("      and a." + COL_CAL_DT + " between d." + COL_REST_OWSH_EFF_DT + " and d." + COL_REST_OWSH_END_DT + ")\n");
		sql.append("order by\n"); 
		sql.append("   a." + COL_DW_FILE_ID + "\n");
		sql.append("  ,b." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		rowCount = 9999;
		int fileCount=0;
		int lastStatus=0;
		BufferedWriter br=new BufferedWriter(new OutputStreamWriter(hdfs.create(new Path(destPathCache.toString() + Path.SEPARATOR + "move" + String.format("%04d", fileCount) + "_" + daasConfig.abacSqlServerCacheFileName()),true)));
		BufferedWriter brList=new BufferedWriter(new OutputStreamWriter(hdfs.create(new Path(destPathCache.toString() + Path.SEPARATOR + daasConfig.abacSqlServerCacheFileName()),true)));
		
		rset = abac.resultSet(sql.toString());
		
		while (rset.next()) {
			if ( rset.getInt(COL_DW_FILE_ID) != lastFileId ) {

				rowCount++;
				
				if ( listLine.length() > 0  ) {

					if ( rowCount > 100 ) {
						if ( fileCount > 0 ) {
							br.close();
						}
						rowCount=1;
						fileCount++;
						
						br=new BufferedWriter(new OutputStreamWriter(hdfs.create(new Path(destPathCache.toString() + Path.SEPARATOR + "move" + String.format("%04d", fileCount) + "_" + daasConfig.abacSqlServerCacheFileName()),true)));
					}
					
					br.write(listLine + "\n");
					
					if ( lastStatus == typeCodes.get(CodeType.PROCESSING) ) {
						brList.write(listLine + "\n");
					}
				}
				
				lastFileId = rset.getInt(COL_DW_FILE_ID);
				lastStatus = rset.getInt(COL_DW_AUDT_STUS_TYP_ID);

				itm = list.addItem(lastFileId
				                  ,rset.getString(COL_DW_FILE_NA)
					              ,rset.getString(COL_FILE_PATH_DS)
				                  ,rset.getString(COL_CAL_DT)
				                  ,rset.getInt(COL_TERR_CD)
				                  ,rset.getString(COL_LGCY_LCL_RFR_DEF_CD)
				                  ,rset.getBigDecimal(COL_MCD_GBAL_LCAT_ID_NU)
				                  ,lastStatus
				                  ,0
				                  ,rset.getString("OBFUSCATE_TYP_CD")
				                  ,rset.getString(COL_REST_OWSH_TYP_SHRT_DS)
				                  ,typeCodes);
				
				listLine = lastFileId + "\t" +
				           rset.getString(COL_DW_FILE_NA) + "\t" + 
				           rset.getString(COL_FILE_PATH_DS) + "\t" +
				           rset.getString(COL_CAL_DT) + "\t" + 
				           rset.getInt(COL_TERR_CD) + "\t" +
				           rset.getString(COL_LGCY_LCL_RFR_DEF_CD) + "\t" +
				           rset.getBigDecimal(COL_MCD_GBAL_LCAT_ID_NU) + "\t" + 
				           lastStatus + "\t" + 
				           rset.getString("OBFUSCATE_TYP_CD") + "\t" +
				           rset.getString(COL_REST_OWSH_TYP_SHRT_DS) + "\t" +
				           typeCodesValues + "\t";
				
				firstSubItem = true;
			}

			if ( !firstSubItem ) {
				listLine += ",";
			}
			listLine += rset.getInt(COL_DW_SUB_FILE_DATA_TYP_ID) + "|" + rset.getString(COL_DW_SUB_FILE_DATA_TYP_CD); 
			
			subItm = itm.addSubItem(rset.getInt(COL_DW_SUB_FILE_DATA_TYP_ID), rset.getString(COL_DW_SUB_FILE_DATA_TYP_CD), rset.getInt("SUB_FILE" + COL_DW_AUDT_STUS_TYP_ID));
			
			firstSubItem = false;
		}

		if ( listLine.length() > 0  ) {
			br.write(listLine + "\n");
			
			if ( lastStatus == typeCodes.get(CodeType.PROCESSING) ) {
				brList.write(listLine + "\n");
			}
		}

		br.close();
		brList.close();
		
		rset.close();
	
		/*sql.setLength(0);
		sql.append("DROP TABLE DW_FILE_ID_LIST\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
		sql.setLength(0);
		sql.append("DROP TABLE DW_FILE_STUS\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());

		abac.commit();*/
		
		sql.setLength(0);
		
		sql.append("UPDATE "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST SET UPDT_FL="+typeCodes.get(CodeType.UPDATED)+" WHERE UPDT_FL="+typeCodes.get(CodeType.ARRIVED)+"\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		abac.commit();
		
		sql.setLength(0);
		
		sql.append("DELETE FROM "+ daasConfig.abacSqlServerDb() + ".DW_FILE_STUS \n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		abac.commit();
		
		return(list);
	
		
	}

	public ABaCList updateABaCFileList(int jobId
            ,FileSystem hdfs,boolean multipleInject ,String terrCDList) throws Exception {
	
		String fileMaskNonCompanyOwnedTerrList = daasConfig.fileMaskNonCompanyOwnedTerrList(); 
		String fileMaskAllTerrList = daasConfig.fileMaskAllTerrList(); 
		String fileEncryptNonCompanyOwnedTerrList = daasConfig.fileEncryptNonCompanyOwnedTerrList();
		String fileEncryptAllTerrList = daasConfig.fileEncryptAllTerrList();
		Path destPathRoot = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + jobId);
		Path destPathSource = new Path(destPathRoot.toString() + Path.SEPARATOR + "source");
		Path destPathReject = new Path(destPathRoot.toString() + Path.SEPARATOR + "reject");
		Path destPathCache = new Path(destPathRoot.toString() + Path.SEPARATOR + "cache");
		//Path destPathCurrentCache = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsWorkSubDir()+ Path.SEPARATOR + "current" + Path.SEPARATOR + "cache");
		
		HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathRoot, daasConfig.displayMsgs());
		HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathSource, daasConfig.displayMsgs());
		HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathReject, daasConfig.displayMsgs());
		HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathCache, daasConfig.displayMsgs());
		//HDFSUtil.createHdfsSubDirIfNecessary(hdfs, destPathCurrentCache, daasConfig.displayMsgs());
		
		if ( fileMaskNonCompanyOwnedTerrList.length() == 0 ) {
			fileMaskNonCompanyOwnedTerrList = "-9999";
		}
		
		if ( fileMaskAllTerrList.length() == 0 ) {
			fileMaskAllTerrList = "-9999";
		}
		
		if ( fileEncryptNonCompanyOwnedTerrList.length() == 0 ) {
			fileEncryptNonCompanyOwnedTerrList = "-9999";
		}
		
		if ( fileEncryptAllTerrList.length() == 0 ) {
			fileEncryptAllTerrList = "-9999";
		}
		
		String typeCodesValues = "";
		CodeType codeTypeValue;
		
		for (Map.Entry<CodeType, Integer> entry : typeCodes.entrySet()) {
			if ( typeCodesValues.length() > 0 ) {
				typeCodesValues += ",";
			}
			codeTypeValue = (CodeType)entry.getKey();
			typeCodesValues += codeTypeValue.ordinal() + "|" + entry.getValue(); 
		}
		
		int rowCount = 0;
		rowCount = 9999;
		int fileCount=0;
		int lastStatus=0;
		ResultSet rset;		
		//rset = abac.resultSet(sql.toString());
		int lastFileId = -1;
		String listLine = "";
		boolean firstSubItem = false;
		ABaCList list=new ABaCList(daasConfig.fileFileSeparatorCharacter());
		ABaCListItem itm=null;
		ABaCListSubItem subItm;
		
		sql.setLength(0);
		sql.append("select\n");
		sql.append("   a." + COL_DW_FILE_ID + "\n");
		sql.append("  ,a." + COL_DW_FILE_NA + "\n");
		sql.append("  ,a." + COL_FILE_PATH_DS + "\n");
		sql.append("  ,a." + COL_CAL_DT + "\n");
		sql.append("  ,a." + COL_TERR_CD + "\n");
		sql.append("  ,a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("  ,a." + COL_MCD_GBAL_LCAT_ID_NU + "\n");
		sql.append("  ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ,b." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("  ,c." + COL_DW_SUB_FILE_DATA_TYP_CD + "\n");
		sql.append("  ,b." + COL_DW_AUDT_STUS_TYP_ID + " as SUB_FILE" + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ,cast(\n");
		sql.append("    case when a.TERR_CD in (" + fileMaskAllTerrList + ") then 'M' else\n");
		sql.append("      case when a.TERR_CD in (" + fileMaskNonCompanyOwnedTerrList + ") then case when coalesce(d." + COL_REST_OWSH_TYP_SHRT_DS + ",' ') <> 'M' then 'M' else ' ' end\n");
		sql.append("    else\n");
		sql.append("      case when a.TERR_CD in (" + fileEncryptAllTerrList + ") then 'E' else\n");
		sql.append("        case when a.TERR_CD in (" + fileEncryptNonCompanyOwnedTerrList + ") then case when coalesce(d." + COL_REST_OWSH_TYP_SHRT_DS + ",' ') <> 'M' then 'E' else ' ' end else\n");
		sql.append("        ' ' end end end end as CHAR(1)) as OBFUSCATE_TYP_CD\n");
		sql.append("  ,coalesce(d." + COL_REST_OWSH_TYP_SHRT_DS + ",' ') as " + COL_REST_OWSH_TYP_SHRT_DS + "\n");
		sql.append("from (\n");
		sql.append("     select\n");
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,a." + COL_DW_FILE_NA + "\n");
		sql.append("       ,a." + COL_FILE_PATH_DS + "\n");
		sql.append("       ,a." + COL_CAL_DT + "\n");
		sql.append("       ,a." + COL_TERR_CD + "\n");
		sql.append("       ,a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("       ,coalesce(a." + COL_MCD_GBAL_LCAT_ID_NU + ",b." + COL_MCD_GBAL_LCAT_ID_NU + ",0) as " + COL_MCD_GBAL_LCAT_ID_NU + "\n");
		sql.append("       ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("       ,a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("     from (\n");
		sql.append("          select\n");
		sql.append("             a." + COL_DW_FILE_ID + "\n");
		sql.append("            ,a." + COL_DW_FILE_NA + "\n");
		sql.append("            ,a." + COL_FILE_PATH_DS + "\n");
		sql.append("            ,substring(cast(a." + COL_CAL_DT + " as varchar(10)),1,4) + substring(cast(a." + COL_CAL_DT + " as varchar(10)),6,2) + substring(cast(a." + COL_CAL_DT + " as varchar(10)),9,2) as " + COL_CAL_DT + "\n");
		sql.append("            ,a." + COL_TERR_CD + "\n");
		sql.append("            ,case when a." + COL_TERR_CD + "= 840 then\n");
		sql.append("               case len(a." + COL_LGCY_LCL_RFR_DEF_CD + ") when 1 then '0000' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           when 2 then '000' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           when 3 then '00' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           when 4 then '0' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           else a." + COL_LGCY_LCL_RFR_DEF_CD + " end\n");
		sql.append("              else a." + COL_LGCY_LCL_RFR_DEF_CD + " end as " + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("            ,a." + COL_MCD_GBAL_LCAT_ID_NU + "\n");
		sql.append("            ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("            ,a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
		sql.append("          inner join "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST b\n");
		sql.append("            on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID +" )\n");
		//sql.append("          where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.PROCESSING) + ") a\n");
		sql.append("            ) a\n");
		sql.append("     left outer join " + daasConfig.abacSqlServerDb() + "." + TBL_GBAL_LCAT + " b with (NOLOCK)\n");
		sql.append("     on (b." + COL_CTRY_ISO_NU + " = a." + COL_TERR_CD + "\n");
		sql.append("         and b." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + ")) a\n");
		sql.append("inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " b with (NOLOCK)\n");
		sql.append("  on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");    
		sql.append("inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_DATA_TYP + " c with (NOLOCK)\n");
		sql.append("  on (c." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("      and c." + COL_DW_SUB_FILE_DATA_TYP_ID + " = b." + COL_DW_SUB_FILE_DATA_TYP_ID + ")\n");
		sql.append("left outer join " + daasConfig.abacSqlServerDb() + "." + TBL_REST_OWSH + " d with (NOLOCK)\n");
		sql.append("  on (d." + COL_CTRY_ISO_NU + " = a." + COL_TERR_CD + "\n");
		sql.append("      and d." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("      and a." + COL_CAL_DT + " between d." + COL_REST_OWSH_EFF_DT + " and d." + COL_REST_OWSH_END_DT + ")\n");
		sql.append("order by\n"); 
		sql.append("   a." + COL_DW_FILE_ID + "\n");
		sql.append("  ,b." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		rowCount = 9999;
		//BufferedWriter br=new BufferedWriter(new OutputStreamWriter(hdfs.create(new Path(destPathCache.toString() + Path.SEPARATOR + "move" + String.format("%04d", fileCount) + "_" + daasConfig.abacSqlServerCacheFileName()),true)));
		BufferedWriter brList=new BufferedWriter(new OutputStreamWriter(hdfs.create(new Path(destPathCache.toString() + Path.SEPARATOR + daasConfig.abacSqlServerCacheFileName()),true)));
		
		rset = abac.resultSet(sql.toString());
		
		while (rset.next()) {
			if ( rset.getInt(COL_DW_FILE_ID) != lastFileId ) {

				rowCount++;
				
				if ( listLine.length() > 0  ) {

					if ( rowCount > 100 ) {
						if ( fileCount > 0 ) {
							//br.close();
						}
						rowCount=1;
						fileCount++;
						
						//br=new BufferedWriter(new OutputStreamWriter(hdfs.create(new Path(destPathCache.toString() + Path.SEPARATOR + "move" + String.format("%04d", fileCount) + "_" + daasConfig.abacSqlServerCacheFileName()),true)));
					}
					
					//br.write(listLine + "\n");
					
					if ( lastStatus == typeCodes.get(CodeType.PROCESSING) ) {
						brList.write(listLine + "\n");
					}
				}
				
				lastFileId = rset.getInt(COL_DW_FILE_ID);
				lastStatus = rset.getInt(COL_DW_AUDT_STUS_TYP_ID);

				itm = list.addItem(lastFileId
				                  ,rset.getString(COL_DW_FILE_NA)
					              ,rset.getString(COL_FILE_PATH_DS)
				                  ,rset.getString(COL_CAL_DT)
				                  ,rset.getInt(COL_TERR_CD)
				                  ,rset.getString(COL_LGCY_LCL_RFR_DEF_CD)
				                  ,rset.getBigDecimal(COL_MCD_GBAL_LCAT_ID_NU)
				                  ,lastStatus
				                  ,0
				                  ,rset.getString("OBFUSCATE_TYP_CD")
				                  ,rset.getString(COL_REST_OWSH_TYP_SHRT_DS)
				                  ,typeCodes);
				
				listLine = lastFileId + "\t" +
				           rset.getString(COL_DW_FILE_NA) + "\t" + 
				           rset.getString(COL_FILE_PATH_DS) + "\t" +
				           rset.getString(COL_CAL_DT) + "\t" + 
				           rset.getInt(COL_TERR_CD) + "\t" +
				           rset.getString(COL_LGCY_LCL_RFR_DEF_CD) + "\t" +
				           rset.getBigDecimal(COL_MCD_GBAL_LCAT_ID_NU) + "\t" + 
				           lastStatus + "\t" + 
				           rset.getString("OBFUSCATE_TYP_CD") + "\t" +
				           rset.getString(COL_REST_OWSH_TYP_SHRT_DS) + "\t" +
				           typeCodesValues + "\t";
				
				firstSubItem = true;
			}

			if ( !firstSubItem ) {
				listLine += ",";
			}
			listLine += rset.getInt(COL_DW_SUB_FILE_DATA_TYP_ID) + "|" + rset.getString(COL_DW_SUB_FILE_DATA_TYP_CD); 
			
			subItm = itm.addSubItem(rset.getInt(COL_DW_SUB_FILE_DATA_TYP_ID), rset.getString(COL_DW_SUB_FILE_DATA_TYP_CD), rset.getInt("SUB_FILE" + COL_DW_AUDT_STUS_TYP_ID));
			
			firstSubItem = false;
		}

		if ( listLine.length() > 0  ) {
			//br.write(listLine + "\n");
			
			if ( lastStatus == typeCodes.get(CodeType.PROCESSING) ) {
				brList.write(listLine + "\n");
			}
		}

		//br.close();
		brList.close();
		
		rset.close();
		
		if(!multipleInject){
			
			sql.setLength(0);
			sql.append("DELETE FROM "+ daasConfig.abacSqlServerDb() + ".DW_FILE_ID_LIST\n");

			if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
				System.out.println(sql.toString());
			}
			
			abac.executeUpdate(sql.toString());
		}
		return(list);
	}
	
	/*
	public ABaCList fileList() throws Exception {

		ResultSet rset;
		ABaCList list=new ABaCList(daasConfig.fileFileSeparatorCharacter());
		ABaCListItem itm=null;
		ABaCListSubItem subItm;
		int lastFileId = -1;

		String fileMaskNonCompanyOwnedTerrList = daasConfig.fileMaskNonCompanyOwnedTerrList(); 
		String fileMaskAllTerrList = daasConfig.fileMaskAllTerrList(); 
		String fileEncryptNonCompanyOwnedTerrList = daasConfig.fileEncryptNonCompanyOwnedTerrList();
		String fileEncryptAllTerrList = daasConfig.fileEncryptAllTerrList();
		
		if ( fileMaskNonCompanyOwnedTerrList.length() == 0 ) {
			fileMaskNonCompanyOwnedTerrList = "-9999";
		}
		
		if ( fileMaskAllTerrList.length() == 0 ) {
			fileMaskAllTerrList = "-9999";
		}
		
		if ( fileEncryptNonCompanyOwnedTerrList.length() == 0 ) {
			fileEncryptNonCompanyOwnedTerrList = "-9999";
		}
		
		if ( fileEncryptAllTerrList.length() == 0 ) {
			fileEncryptAllTerrList = "-9999";
		}
		
		sql.setLength(0);
		sql.append("select\n");
		sql.append("   a." + COL_DW_FILE_ID + "\n");
		sql.append("  ,a." + COL_DW_FILE_NA + "\n");
		sql.append("  ,a." + COL_FILE_PATH_DS + "\n");
		sql.append("  ,a." + COL_CAL_DT + "\n");
		sql.append("  ,a." + COL_TERR_CD + "\n");
		sql.append("  ,a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("  ,a." + COL_MCD_GBAL_LCAT_ID_NU + "\n");
		sql.append("  ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ,b." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("  ,c." + COL_DW_SUB_FILE_DATA_TYP_CD + "\n");
		sql.append("  ,b." + COL_DW_AUDT_STUS_TYP_ID + " as SUB_FILE" + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("  ,cast(\n");
		sql.append("    case when a.TERR_CD in (" + fileMaskAllTerrList + ") then 'M' else\n");
		sql.append("      case when a.TERR_CD in (" + fileMaskNonCompanyOwnedTerrList + ") case when coalesce(d." + COL_REST_OWSH_TYP_SHRT_DS + ",' ') <> 'M' then 'M' else ' ' end\n");
		sql.append("    else\n");
		sql.append("      case when a.TERR_CD in (" + fileEncryptAllTerrList + ") then 'E' else\n");
		sql.append("        case when a.TERR_CD in (" + fileEncryptNonCompanyOwnedTerrList + ") then case when coalesce(d." + COL_REST_OWSH_TYP_SHRT_DS + ",' ') <> 'M' then 'E' else ' ' end else\n");
		sql.append("        ' ' end end end end as CHAR(1)) as OBFUSCATE_TYP_CD\n");
		sql.append("  ,coalesce(d." + COL_REST_OWSH_TYP_SHRT_DS + ",' ') as " + COL_REST_OWSH_TYP_SHRT_DS + "\n");
		sql.append("from (\n");
		sql.append("     select\n");
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,a." + COL_DW_FILE_NA + "\n");
		sql.append("       ,a." + COL_FILE_PATH_DS + "\n");
		sql.append("       ,a." + COL_CAL_DT + "\n");
		sql.append("       ,a." + COL_TERR_CD + "\n");
		sql.append("       ,a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("       ,coalesce(a." + COL_MCD_GBAL_LCAT_ID_NU + ",b." + COL_MCD_GBAL_LCAT_ID_NU + ",0) as " + COL_MCD_GBAL_LCAT_ID_NU + "\n");
		sql.append("       ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("       ,a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("     from (\n");
		sql.append("          select\n");
		sql.append("             a." + COL_DW_FILE_ID + "\n");
		sql.append("            ,a." + COL_DW_FILE_NA + "\n");
		sql.append("            ,a." + COL_FILE_PATH_DS + "\n");
		sql.append("            ,substring(cast(a." + COL_CAL_DT + " as varchar(10)),1,4) + substring(cast(a." + COL_CAL_DT + " as varchar(10)),6,2) + substring(cast(a." + COL_CAL_DT + " as varchar(10)),9,2) as " + COL_CAL_DT + "\n");
		sql.append("            ,a." + COL_TERR_CD + "\n");
		sql.append("            ,case when a." + COL_TERR_CD + "= 840 then\n");
		sql.append("               case len(a." + COL_LGCY_LCL_RFR_DEF_CD + ") when 1 then '0000' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           when 2 then '000' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           when 3 then '00' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           when 4 then '0' + a." + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("                                                           else a." + COL_LGCY_LCL_RFR_DEF_CD + " end\n");
		sql.append("              else a." + COL_LGCY_LCL_RFR_DEF_CD + " end as " + COL_LGCY_LCL_RFR_DEF_CD + "\n");
		sql.append("            ,a." + COL_MCD_GBAL_LCAT_ID_NU + "\n");
		sql.append("            ,a." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("            ,a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("          from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
		sql.append("          inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_DATA_TYP + " b with (NOLOCK)\n");
		sql.append("            on (b." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + ")\n");
		sql.append("          where a." + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.PROCESSING) + "\n");
		sql.append("          and   b." + COL_DW_DATA_TYP_DS + " = '" + daasConfig.fileType() + "') a\n");
		sql.append("     left outer join " + daasConfig.abacSqlServerDb() + "." + TBL_GBAL_LCAT + " b with (NOLOCK)\n");
		sql.append("     on (b." + COL_CTRY_ISO_NU + " = a." + COL_TERR_CD + "\n");
		sql.append("         and b." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + ")) a\n");
		sql.append("inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " b with (NOLOCK)\n");
		sql.append("  on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");    
		sql.append("inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_DATA_TYP + " c with (NOLOCK)\n");
		sql.append("  on (c." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + "\n");
		sql.append("      and c." + COL_DW_SUB_FILE_DATA_TYP_ID + " = b." + COL_DW_SUB_FILE_DATA_TYP_ID + ")\n");
		sql.append("left outer join " + daasConfig.abacSqlServerDb() + "." + TBL_REST_OWSH + " d with (NOLOCK)\n");
		sql.append("  on (d." + COL_CTRY_ISO_NU + " = a." + COL_TERR_CD + "\n");
		sql.append("      and d." + COL_LGCY_LCL_RFR_DEF_CD + " = a." + COL_LGCY_LCL_RFR_DEF_CD + ")\n");
		sql.append("order by\n"); 
		sql.append("   a." + COL_DW_FILE_ID + "\n");
		sql.append("  ,b." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		rset = abac.resultSet(sql.toString());
		
		while (rset.next()) {
			if ( rset.getInt(COL_DW_FILE_ID) != lastFileId ) {
				lastFileId = rset.getInt(COL_DW_FILE_ID);

				itm = list.addItem(rset.getInt(COL_DW_FILE_ID)
				          ,rset.getString(COL_DW_FILE_NA)
					      ,rset.getString(COL_FILE_PATH_DS)
				          ,rset.getString(COL_CAL_DT)
				          ,rset.getInt(COL_TERR_CD)
				          ,rset.getString(COL_LGCY_LCL_RFR_DEF_CD)
				          ,rset.getBigDecimal(COL_MCD_GBAL_LCAT_ID_NU)
				          ,rset.getInt(COL_DW_AUDT_STUS_TYP_ID)
				          ,0
				          ,rset.getString("OBFUSCATE_TYP_CD")
				          ,rset.getString(COL_REST_OWSH_TYP_SHRT_DS)
				          ,typeCodes);
			}
			
			subItm = itm.addSubItem(rset.getInt(COL_DW_SUB_FILE_DATA_TYP_ID), rset.getString(COL_DW_SUB_FILE_DATA_TYP_CD), rset.getInt("SUB_FILE" + COL_DW_AUDT_STUS_TYP_ID));
		}
		
		rset.close();
		
		return(list);
		
	}
	*/

	public void updateFileList(ABaCList fileList) throws Exception {
		
		StringBuffer updFile = new StringBuffer();
		StringBuffer updSubFile = new StringBuffer();
		StringBuffer delFileReason = new StringBuffer();
		StringBuffer delSubFileReason = new StringBuffer();
		StringBuffer insFileReason = new StringBuffer();
		StringBuffer insSubFileReason = new StringBuffer();
		StringBuffer insSubFileReason2 = new StringBuffer();
		
		java.sql.Timestamp jobendts  = new java.sql.Timestamp(System.currentTimeMillis());
		
		updFile.setLength(0);
		updFile.append("update " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + "\n");
		updFile.append("  set " + COL_DW_AUDT_STUS_TYP_ID + " = ? , "+COL_FILE_PRCS_STRT_TS+" = ? , "+COL_FILE_PRCS_END_TS+" = ? \n");
//		updFile.append("  set " + COL_DW_AUDT_STUS_TYP_ID + " = ? , "+COL_FILE_PRCS_STRT_TS+" = ?  \n");
		updFile.append("where " + COL_DW_FILE_ID + " = ? \n");
		
		updSubFile.setLength(0);
		updSubFile.append("update " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + "\n");
		updSubFile.append("  set " + COL_DW_AUDT_STUS_TYP_ID + " = ? , " + COL_FILE_SIZE_NU + " = ? , " + COL_DW_SUB_FILE_NA + " = ? ");
		updSubFile.append("where " + COL_DW_FILE_ID + " = ?\n");
		updSubFile.append("and   " + COL_DW_SUB_FILE_DATA_TYP_ID + " = ?\n");

//		delFileReason.setLength(0);
//		delFileReason.append("delete from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE_RJCT_RESN_ASSC + "\n");
//		delFileReason.append("where " + COL_DW_FILE_ID + " = ?\n");
//
//		delSubFileReason.setLength(0);
//		delSubFileReason.append("delete from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_RJCT_RESN_ASSC + "\n");
//		delSubFileReason.append("where " + COL_DW_FILE_ID + " = ?\n");
//		delSubFileReason.append("and   " + COL_DW_SUB_FILE_DATA_TYP_ID + " = ?\n");

		insFileReason.setLength(0);
		insFileReason.append("insert into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE_RJCT_RESN_ASSC + "\n");
		insFileReason.append("  (" + COL_DW_FILE_ID + "\n");
		insFileReason.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
		insFileReason.append("  values (?,?)\n");

		insSubFileReason.setLength(0);
		insSubFileReason.append("insert into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_RJCT_RESN_ASSC + "\n");
		insSubFileReason.append("  (" + COL_DW_FILE_ID + "\n");
		insSubFileReason.append("  ," + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		insSubFileReason.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
		insSubFileReason.append("  values (?,?,?)\n");

		insSubFileReason2.setLength(0);
		insSubFileReason2.append("insert into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_RJCT_RESN_ASSC + "\n");
		insSubFileReason2.append("  (" + COL_DW_FILE_ID + "\n");
		insSubFileReason2.append("  ," + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		insSubFileReason2.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + "\n");
		insSubFileReason2.append("  ," + COL_DW_SUB_FILE_RJCT_RESN_ERR_TX + ")\n");
		insSubFileReason2.append("  values (?,?,?,?)\n");
		
		RDBMS.MultiPreparedStatementSql updFileStmt = abac.new MultiPreparedStatementSql("UPD_FILE", updFile);
		RDBMS.MultiPreparedStatementSql updSubFileStmt = abac.new MultiPreparedStatementSql("UPD_SUB_FILE", updSubFile);
//		RDBMS.MultiPreparedStatementSql delFileReasonStmt = abac.new MultiPreparedStatementSql("DEL_FILE_REASON", delFileReason);
//		RDBMS.MultiPreparedStatementSql delSubFileReasonStmt = abac.new MultiPreparedStatementSql("DEL_SUB_FILE_REASON", delSubFileReason);
		RDBMS.MultiPreparedStatementSql insFileReasonStmt = abac.new MultiPreparedStatementSql("INS_FILE_REASON", insFileReason);
		RDBMS.MultiPreparedStatementSql insSubFileReasonStmt = abac.new MultiPreparedStatementSql("INS_SUB_FILE_REASON", insSubFileReason);
		RDBMS.MultiPreparedStatementSql insSubFileReasonStmt2 = abac.new MultiPreparedStatementSql("INS_SUB_FILE_REASON2", insSubFileReason2);
		
		int tmpStatusCode;
		short statusCode;
		int tmpReasonCode;
		short reasonCode;
		int tmpSubFileDataTypeId;
		short subFileDataTypeId;
		ABaCListItem itm;
		ABaCListSubItem subItm;
		
//		abac.multiSetPreparedStatement(updFileStmt,updSubFileStmt,delFileReasonStmt,delSubFileReasonStmt,insFileReasonStmt,insSubFileReasonStmt,insSubFileReasonStmt2);
		abac.multiSetPreparedStatement(updFileStmt,updSubFileStmt,insFileReasonStmt,insSubFileReasonStmt,insSubFileReasonStmt2);

		/*
		for ( Map.Entry<String, ABaCListItem> entry : fileList) {
			itm = entry.getValue();
			
			if ( itm.getStatusTypeId() != typeCodes.get(CodeType.REJECTED) ) {
				abac.multiAddBatch("DEL_FILE_REASON", itm.getFileId());
			}

			for (Map.Entry<String, ABaCListSubItem> subItemEntry : itm) {
				subItm = subItemEntry.getValue();

				tmpSubFileDataTypeId = subItm.getSubFileDataTypeId();
				subFileDataTypeId = (short)(tmpSubFileDataTypeId);
				
				if ( subItm.getStatusTypeId() != typeCodes.get(CodeType.REJECTED) ) {
					abac.multiAddBatch("DEL_SUB_FILE_REASON", itm.getFileId(), subFileDataTypeId);
				}
			}
		}

		abac.multiFinalizeBatch("DEL_FILE_REASON");
		abac.multiFinalizeBatch("DEL_SUB_FILE_REASON");
		*/
		
		for ( Map.Entry<String, ABaCListItem> entry : fileList) {
			itm = entry.getValue();
			
			tmpStatusCode = itm.getStatusTypeId();
			statusCode = (short)(tmpStatusCode);
			
//			abac.multiAddBatch("UPD_FILE", statusCode, itm.getFileId() );
			abac.multiAddBatch("UPD_FILE", statusCode, itm.getFileProcessStartTimestamp(),jobendts,itm.getFileId() );
			
			if ( tmpStatusCode == typeCodes.get(CodeType.REJECTED) ) {
				tmpReasonCode = itm.getRejectReasonId();
				reasonCode = (short)(tmpReasonCode);
				abac.multiAddBatch("INS_FILE_REASON", itm.getFileId(), reasonCode );
			}
			
			for (Map.Entry<String, ABaCListSubItem> subItemEntry : itm) {
				subItm = subItemEntry.getValue();

				tmpSubFileDataTypeId = subItm.getSubFileDataTypeId();
				subFileDataTypeId = (short)(tmpSubFileDataTypeId);
				
				tmpStatusCode = subItm.getStatusTypeId();
				statusCode = (short)(tmpStatusCode);
				
				abac.multiAddBatch("UPD_SUB_FILE", statusCode, subItm.getFileSizeNum(), subItm.getSubFileName(), itm.getFileId(), subFileDataTypeId);
				
				if ( tmpStatusCode == typeCodes.get(CodeType.REJECTED) ) {
					tmpReasonCode = subItm.getRejectReasonId();
					reasonCode = (short)(tmpReasonCode);
					if ( subItm.getXmlErrorMsg().length() > 0 ) {
						abac.multiAddBatch("INS_SUB_FILE_REASON2", itm.getFileId(), subFileDataTypeId, reasonCode, subItm.getXmlErrorMsg() );
					} else {
						abac.multiAddBatch("INS_SUB_FILE_REASON", itm.getFileId(), subFileDataTypeId, reasonCode );
					}
				}
			}
		}

		abac.multiFinalizeBatch("UPD_FILE");
		abac.multiFinalizeBatch("UPD_SUB_FILE");
		abac.multiFinalizeBatch("INS_FILE_REASON");
		abac.multiFinalizeBatch("INS_SUB_FILE_REASON");
		abac.multiFinalizeBatch("INS_SUB_FILE_REASON2");
	
		abac.commit();
		
	}

	public void updateReasonToMissing(ArrayList<Integer> missingList) throws Exception {
		
		StringBuffer updFile = new StringBuffer();
		StringBuffer insFileReason = new StringBuffer();
		
		updFile.setLength(0);
		updFile.append("update " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + "\n");
		updFile.append("  set " + COL_DW_AUDT_STUS_TYP_ID + " = ?\n");
		updFile.append("where " + COL_DW_FILE_ID + " = ?\n");

		insFileReason.setLength(0);
		insFileReason.append("insert into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE_RJCT_RESN_ASSC + "\n");
		insFileReason.append("  (" + COL_DW_FILE_ID + "\n");
		insFileReason.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
		insFileReason.append("  values (?,?)\n");
		
		RDBMS.MultiPreparedStatementSql updFileStmt = abac.new MultiPreparedStatementSql("UPD_FILE", updFile);
		RDBMS.MultiPreparedStatementSql insFileReasonStmt = abac.new MultiPreparedStatementSql("INS_FILE_REASON", insFileReason);
		
		int tmpStatusCode = typeCodes.get(CodeType.REJECTED);
		short statusCode = (short)tmpStatusCode;
		int tmpReasonCode = typeCodes.get(CodeType.MISSING);
		short reasonCode = (short)tmpReasonCode;
		
		abac.multiSetPreparedStatement(updFileStmt,insFileReasonStmt);
		
		for ( int fileId : missingList) {
			abac.multiAddBatch("UPD_FILE", statusCode, fileId );
			abac.multiAddBatch("INS_FILE_REASON", fileId, reasonCode );
		}

		abac.multiFinalizeBatch("UPD_FILE");
		abac.multiFinalizeBatch("INS_FILE_REASON");
	
		abac.commit();
		
	}
	
	public void saveList(int jobId
			            ,ABaCList fileList
			            ,FileSystem hdfs) throws Exception{

		ObjectOutput saveObj;
	    ABaCListItem itm; 

		Path cachePathRoot = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + jobId + Path.SEPARATOR + "cache");
		HDFSUtil.createHdfsSubDirIfNecessary(hdfs, cachePathRoot, daasConfig.displayMsgs());
		
		//cachePath = new Path(cachePath.toString() + Path.SEPARATOR + daasConfig.abacCacheFileName() );
		//
	    //ObjectOutput saveObj = new ObjectOutputStream(hdfs.create(cachePath));
	    //
	    //saveObj.writeObject(fileList);
	    //saveObj.flush();
	    //saveObj.close();
	    
		Path cachePath;
		
		for ( Map.Entry<String, ABaCListItem> entry : fileList) {
			itm = entry.getValue();

			cachePath = new Path(cachePathRoot.toString() + Path.SEPARATOR + itm.getFileNameHashKey() );
			if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
				System.out.print("Create: " + cachePath.toString() + " ... ");
			}
			saveObj = new ObjectOutputStream(hdfs.create(cachePath));
			saveObj.writeObject(itm);
			saveObj.flush();
			saveObj.close();
			if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
				System.out.println("done");
			}
		}
	    
	}

	/*
	public ABaC2List readList(int jobId
			                 ,FileSystem hdfs) throws Exception {
		
		ABaC2List returnList;
		Path cachePath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + jobId + Path.SEPARATOR + "cache" + Path.SEPARATOR + daasConfig.abacCacheFileName());
		
	    ObjectInputStream savedObj = new ObjectInputStream(hdfs.open(cachePath));
	    returnList = (ABaC2List)savedObj.readObject();

	    return(returnList);
	    
	}
	
	public static ABaC2List readList(FileSystem hdfs
			                        ,Path cachePath) throws Exception {

		ABaC2List returnList;
				
	    ObjectInputStream savedObj = new ObjectInputStream(hdfs.open(cachePath));
	    returnList = (ABaC2List)savedObj.readObject();

	    return(returnList);
		
	}
	*/
	
	public ABaCListItem readListItem(int jobId
			                         ,FileSystem hdfs
			                         ,String fileNameHashKey) throws Exception {
		
		ABaCListItem returnListItem;
	
		Path cachePath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + jobId + Path.SEPARATOR + "cache" + Path.SEPARATOR + fileNameHashKey);
		
	    ObjectInputStream savedObj = new ObjectInputStream(hdfs.open(cachePath));
	    returnListItem = (ABaCListItem)savedObj.readObject();

	    return(returnListItem);
	    
	}
	
	public static ABaCListItem readList(FileSystem hdfs
			                            ,Path cachePath
			                            ,String fileNameHashKey) throws Exception {

		ABaCListItem returnListItem;
		
	    ObjectInputStream savedObj = new ObjectInputStream(hdfs.open(new Path(cachePath.toString() + Path.SEPARATOR + fileNameHashKey)));
	    returnListItem = (ABaCListItem)savedObj.readObject();
	    savedObj.close();

	    return(returnListItem);
		
	}
	
	public static String serializeListItemToHexString(ABaCListItem itm) throws Exception {
		
		byte[] bytes = null;
		char[] hexChars = null;
		int val;
		ByteArrayOutputStream byteArrayOut = null;
		ObjectOutputStream objOutStream = null;
		
		byteArrayOut = new ByteArrayOutputStream();
		objOutStream = new ObjectOutputStream(byteArrayOut);
		objOutStream.writeObject(itm);
		objOutStream.flush();
			
		bytes = byteArrayOut.toByteArray();
			
		byteArrayOut.close();
			
		hexChars = new char[bytes.length * 2];

		for ( int idx = 0; idx < bytes.length; idx++ ) {
			val = bytes[idx] & 0xFF;
			hexChars[idx * 2] = HEX_ARRAY[val >>> 4];
			hexChars[idx * 2 + 1] = HEX_ARRAY[val & 0x0F];
			}			

		return(new String(hexChars));

	}
	
	public static ABaCListItem deserializeListItemFromHexString(String itmHexString) throws Exception {
		
		ABaCListItem itm = null;
		int len = itmHexString.length();
		byte[] data = new byte[len / 2];
		ByteArrayInputStream byteArrayIn = null;
		ObjectInputStream objInStream = null;

		for (int idx = 0; idx < len; idx += 2) {
			data[idx / 2] = (byte) ((Character.digit(itmHexString.charAt(idx), 16) << 4) + Character.digit(itmHexString.charAt(idx+1), 16));
		}
			

		byteArrayIn = new ByteArrayInputStream(data);
		objInStream = new ObjectInputStream(byteArrayIn);			
		itm = (ABaCListItem) objInStream.readObject();
		
		return(itm);
	}
	
	private int validateCode(String type
			                ,String tableName
			                ,String columnName
			                ,HashMap<String,Integer> map) {

		int retValue = 0;
		
		if ( map.containsKey(type) ) {
			retValue = map.get(type);	
		} else {
			errMsg += "\nMissing " + tableName.toUpperCase() + "." + columnName.toUpperCase() + " = '" + type + "'";  
		}
		
		return(retValue);
	}
	
	private void updateStatusReason(FileSystem hdfs
			                       ,int jobId
			                       ,Path destPathSource
			                       ,Path destPathReject) throws Exception {
		
		ResultSet rset;
		ArrayList<Integer> missingFileIds = new ArrayList<Integer>();
		sql.setLength(0);
		sql.append("merge into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " t\n");
		sql.append("using (\n");
		sql.append("     select\n");
		sql.append("        " + COL_DW_FILE_ID + "\n");
		sql.append("       ," + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("     from #DW_FILE_STUS\n");
		sql.append("     where UPDT_FL = 0) s\n");
		sql.append("  on (t." + COL_DW_FILE_ID + " = s." + COL_DW_FILE_ID + ")\n");
		sql.append("when matched then update\n");
		sql.append("   set " + COL_DW_AUDT_STUS_TYP_ID + " = s." + COL_DW_AUDT_STUS_TYP_ID + ";\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}

		abac.executeUpdate(sql.toString());
		
		sql.setLength(0);
		sql.append("merge into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " t\n");
		sql.append("using (\n");
		sql.append("     select\n");
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,a." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("       ,b." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("     from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " a with (NOLOCK)\n");
		sql.append("     inner join #DW_FILE_STUS b\n");
		sql.append("       on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");
		sql.append("     where b.UPDT_FL = 0) s\n");
		sql.append("  on (t." + COL_DW_FILE_ID + " = s." + COL_DW_FILE_ID + "\n");
		sql.append("      and t." + COL_DW_SUB_FILE_DATA_TYP_ID + " = s." + COL_DW_SUB_FILE_DATA_TYP_ID + ")\n");
		sql.append("when matched then update\n");
		sql.append("  set " + COL_DW_AUDT_STUS_TYP_ID + " = s." + COL_DW_AUDT_STUS_TYP_ID + ";\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}

		abac.executeUpdate(sql.toString());

		if ( destPathReject != null ) {
			sql.setLength(0);
			sql.append("merge into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE_RJCT_RESN_ASSC + " t\n");
			sql.append("using (\n");
			sql.append("     select\n");
			sql.append("        " + COL_DW_FILE_ID + "\n");
			sql.append("       ," + COL_DW_AUDT_RJCT_RESN_ID + "\n");
			sql.append("     from #DW_FILE_STUS\n");
			sql.append("     where UPDT_FL = 0) s\n");
			sql.append("  on (t." + COL_DW_FILE_ID + " = s." + COL_DW_FILE_ID + ")\n");
			sql.append("when not matched then insert\n");
			sql.append("  (" + COL_DW_FILE_ID + "\n");
			sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
			sql.append("  values (\n");
			sql.append("          " + COL_DW_FILE_ID + "\n");
			sql.append("         ," + COL_DW_AUDT_RJCT_RESN_ID + ");\n");

			if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
				System.out.println(sql.toString());
			}

			abac.executeUpdate(sql.toString());

			sql.setLength(0);
			sql.append("merge into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_RJCT_RESN_ASSC + " t\n");
			sql.append("using (\n");
			sql.append("     select\n");
			sql.append("        a." + COL_DW_FILE_ID + "\n");
			sql.append("       ,a." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
			sql.append("       ,b." + COL_DW_AUDT_RJCT_RESN_ID + "\n");
			sql.append("     from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " a with (NOLOCK)\n");
			sql.append("     inner join #DW_FILE_STUS b\n");
			sql.append("       on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");
			sql.append("     where b.UPDT_FL = 0) s\n");
			sql.append("  on (t." + COL_DW_FILE_ID + " = s." + COL_DW_FILE_ID + "\n");
			sql.append("      and t." + COL_DW_SUB_FILE_DATA_TYP_ID + " = s." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
			sql.append("      and t." + COL_DW_AUDT_RJCT_RESN_ID + " = s." + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
			sql.append("when not matched then insert\n");
			sql.append("  (" + COL_DW_FILE_ID + "\n");
			sql.append("  ," + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
			sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
			sql.append("  values (\n");
			sql.append("         s." + COL_DW_FILE_ID + "\n");
			sql.append("        ,s." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
			sql.append("        ,s." + COL_DW_AUDT_RJCT_RESN_ID + ");\n");

			if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
				System.out.println(sql.toString());
			}

			abac.executeUpdate(sql.toString());
		
		}

		sql.setLength(0);
		sql.append("select\n");
		sql.append("   a." + COL_DW_FILE_NA + "\n");
		sql.append("  ,a." + COL_DW_FILE_ID + "\n");
		sql.append("from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " a with (NOLOCK)\n");
		sql.append("inner join #DW_FILE_STUS b\n");
		sql.append("  on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");
		sql.append("where b.UPDT_FL = 0\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		rset = abac.resultSet(sql.toString());
		
		Path fromPath;
		Path toPath;
		
		while ( rset.next() ) {
			
			fromPath = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir() + Path.SEPARATOR + daasConfig.hdfsLandingZoneArrivalSubDir() + Path.SEPARATOR + rset.getString(COL_DW_FILE_NA));

			if ( hdfs.exists(fromPath) )  {
				if ( destPathReject != null ) {
					toPath = new Path(destPathReject.toString() + Path.SEPARATOR + rset.getString(COL_DW_FILE_NA) );
					
				} else {
			        toPath = new Path(destPathSource.toString() + Path.SEPARATOR + rset.getString(COL_DW_FILE_NA) );
				}

				
				if (!hdfs.rename(fromPath,toPath)) {
					throw new Exception("File ID = " + rset.getInt(COL_DW_FILE_ID) + " " + rset.getString(COL_DW_FILE_NA) + " move failed to " + toPath.toUri() + " from " + fromPath.toUri() );
				}
				
			} else {
				missingFileIds.add(rset.getInt(COL_DW_FILE_ID));
			}
		}
		
		sql.setLength(0);
		sql.append("update #DW_FILE_STUS\n");
		sql.append("  set UPDT_FL = 1\n");
		sql.append("where UPDT_FL = 0\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		
		if ( missingFileIds.size() > 0 ) { 
		
			String missingFileIdList = "";
			
			for ( int missingFileId : missingFileIds ) {
				if ( missingFileIdList.length() > 0 ) {
					missingFileIdList += ",";
				}
				missingFileIdList += String.valueOf(missingFileId);
			}
			
			sql.setLength(0);
			sql.append("update " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + "\n");
			sql.append("   set " + COL_DW_AUDT_STUS_TYP_ID + " = " + typeCodes.get(CodeType.REJECTED) + "\n");
			sql.append("where " + COL_DW_FILE_ID + " in (" + missingFileIdList + ")\n");

			if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
				System.out.println(sql.toString());
			}

			abac.executeUpdate(sql.toString());

			sql.setLength(0);
			sql.append("merge into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE_RJCT_RESN_ASSC + " t\n");
			sql.append("using (\n");
			sql.append("     select\n");
			sql.append("        " + COL_DW_FILE_ID + "\n");
			sql.append("        ,cast(" + typeCodes.get(CodeType.MISSING) + " as tinyint) as " + COL_DW_AUDT_RJCT_RESN_ID + "\n");
			sql.append("     from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " with (NOLOCK)\n");
			sql.append("     where " + COL_DW_FILE_ID + " in (" + missingFileIdList + ")) s\n");
			sql.append("  on (t." + COL_DW_FILE_ID + " = s." + COL_DW_FILE_ID + "\n");
			sql.append("      and t." + COL_DW_AUDT_RJCT_RESN_ID + " = s." + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
			sql.append("when not matched then insert\n");
			sql.append("  (" + COL_DW_FILE_ID + "\n");
			sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
			sql.append("  values (\n");
			sql.append("          s." + COL_DW_FILE_ID + "\n");
			sql.append("         ,s." + COL_DW_AUDT_RJCT_RESN_ID + ");\n");

			if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
				System.out.println(sql.toString());
			}

			abac.executeUpdate(sql.toString());

			sql.append("merge into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_RJCT_RESN_ASSC + " t\n");
			sql.append("using (\n");
			sql.append("     select\n");
			sql.append("        a." + COL_DW_FILE_ID + "\n");
			sql.append("       ,a." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
			sql.append("       ,cast(" + typeCodes.get(CodeType.MISSING) + " as tinyint) as " + COL_DW_AUDT_RJCT_RESN_ID + "\n");
			sql.append("     from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " a with (NOLOCK)\n");
			sql.append("     inner join #DW_FILE_STUS b\n");
			sql.append("       on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");
			sql.append("     where b.UPDT_FL = 0) s\n");
			sql.append("  on (t." + COL_DW_FILE_ID + " = s." + COL_DW_FILE_ID + "\n");
			sql.append("      and t." + COL_DW_SUB_FILE_DATA_TYP_ID + " = s." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
			sql.append("      and t." + COL_DW_AUDT_RJCT_RESN_ID + " = s." + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
			sql.append("when not matched then insert\n");
			sql.append("  (" + COL_DW_FILE_ID + "\n");
			sql.append("  ," + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
			sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
			sql.append("  values (\n");
			sql.append("         s." + COL_DW_FILE_ID + "\n");
			sql.append("        ,s." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
			sql.append("        ,s." + COL_DW_AUDT_RJCT_RESN_ID + ");\n");

			if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
				System.out.println(sql.toString());
			}

			abac.executeUpdate(sql.toString());
		}
	}
	
	private void updateStatusReason(boolean reject) throws Exception {
		
		sql.setLength(0);
		sql.append("merge into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " t\n");
		sql.append("using (\n");
		sql.append("     select\n");
		sql.append("        " + COL_DW_FILE_ID + "\n");
		sql.append("       ," + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("     from "+ daasConfig.abacSqlServerDb() + ".DW_FILE_STUS\n");
		sql.append("     where UPDT_FL = 0) s\n");
		sql.append("  on (t." + COL_DW_FILE_ID + " = s." + COL_DW_FILE_ID + ")\n");
		sql.append("when matched then update\n");
		sql.append("   set " + COL_DW_AUDT_STUS_TYP_ID + " = s." + COL_DW_AUDT_STUS_TYP_ID + ";\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}

		abac.executeUpdate(sql.toString());
		
		sql.setLength(0);
		sql.append("merge into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " t\n");
		sql.append("using (\n");
		sql.append("     select\n");
		sql.append("        a." + COL_DW_FILE_ID + "\n");
		sql.append("       ,a." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
		sql.append("       ,b." + COL_DW_AUDT_STUS_TYP_ID + "\n");
		sql.append("     from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " a with (NOLOCK)\n");
		sql.append("     inner join "+ daasConfig.abacSqlServerDb() + ".DW_FILE_STUS b\n");
		sql.append("       on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");
		sql.append("     where b.UPDT_FL = 0) s\n");
		sql.append("  on (t." + COL_DW_FILE_ID + " = s." + COL_DW_FILE_ID + "\n");
		sql.append("      and t." + COL_DW_SUB_FILE_DATA_TYP_ID + " = s." + COL_DW_SUB_FILE_DATA_TYP_ID + ")\n");
		sql.append("when matched then update\n");
		sql.append("  set " + COL_DW_AUDT_STUS_TYP_ID + " = s." + COL_DW_AUDT_STUS_TYP_ID + ";\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}

		abac.executeUpdate(sql.toString());

		if ( reject ) {
			sql.setLength(0);
			sql.append("merge into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE_RJCT_RESN_ASSC + " t\n");
			sql.append("using (\n");
			sql.append("     select\n");
			sql.append("        " + COL_DW_FILE_ID + "\n");
			sql.append("       ," + COL_DW_AUDT_RJCT_RESN_ID + "\n");
			sql.append("     from "+ daasConfig.abacSqlServerDb() + ".DW_FILE_STUS\n");
			sql.append("     where UPDT_FL = 0) s\n");
			sql.append("  on (t." + COL_DW_FILE_ID + " = s." + COL_DW_FILE_ID + ")\n");
			sql.append("when not matched then insert\n");
			sql.append("  (" + COL_DW_FILE_ID + "\n");
			sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
			sql.append("  values (\n");
			sql.append("          " + COL_DW_FILE_ID + "\n");
			sql.append("         ," + COL_DW_AUDT_RJCT_RESN_ID + ");\n");

			if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
				System.out.println(sql.toString());
			}

			abac.executeUpdate(sql.toString());

			sql.setLength(0);
			sql.append("merge into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_RJCT_RESN_ASSC + " t\n");
			sql.append("using (\n");
			sql.append("     select\n");
			sql.append("        a." + COL_DW_FILE_ID + "\n");
			sql.append("       ,a." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
			sql.append("       ,b." + COL_DW_AUDT_RJCT_RESN_ID + "\n");
			sql.append("     from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE + " a with (NOLOCK)\n");
			sql.append("     inner join "+ daasConfig.abacSqlServerDb() + ".DW_FILE_STUS b\n");
			sql.append("       on (b." + COL_DW_FILE_ID + " = a." + COL_DW_FILE_ID + ")\n");
			sql.append("     where b.UPDT_FL = 0) s\n");
			sql.append("  on (t." + COL_DW_FILE_ID + " = s." + COL_DW_FILE_ID + "\n");
			sql.append("      and t." + COL_DW_SUB_FILE_DATA_TYP_ID + " = s." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
			sql.append("      and t." + COL_DW_AUDT_RJCT_RESN_ID + " = s." + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
			sql.append("when not matched then insert\n");
			sql.append("  (" + COL_DW_FILE_ID + "\n");
			sql.append("  ," + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
			sql.append("  ," + COL_DW_AUDT_RJCT_RESN_ID + ")\n");
			sql.append("  values (\n");
			sql.append("         s." + COL_DW_FILE_ID + "\n");
			sql.append("        ,s." + COL_DW_SUB_FILE_DATA_TYP_ID + "\n");
			sql.append("        ,s." + COL_DW_AUDT_RJCT_RESN_ID + ");\n");

			if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
				System.out.println(sql.toString());
			}

			abac.executeUpdate(sql.toString());
		
		}
		
		sql.setLength(0);
		sql.append("update "+ daasConfig.abacSqlServerDb() + ".DW_FILE_STUS\n");
		sql.append("  set UPDT_FL = 1\n");
		sql.append("where UPDT_FL = 0\n");

		if ( daasConfig.verboseLevel == DaaSConfig.VerboseLevelType.Maximum ) {
			System.out.println(sql.toString());
		}
		
		abac.executeUpdate(sql.toString());
		//################

	}
	
	public void purgeExpiredLandingZoneFiles(FileSystem fileSystem
										    ,DaaSConfig daasConfig
			                                ,Configuration conf
			                                ,int minDaysOld) throws Exception {
		
		FileStatus[] status = null;
		String dirName;
		int actHours;
		int minHours = minDaysOld * 24;
		ResultSet rset;
		Path lzRoot;
		Trash trash = new Trash(fileSystem,conf);

		sql.setLength(0);
		sql.append("select\n");
		sql.append("   a." + COL_DW_JOB_XECT_ID + "\n");
		sql.append("  ,b." + COL_DW_JOB_GRP_END_TS + "\n");
		sql.append("  ,DATEDIFF(hour,b." + COL_DW_JOB_GRP_END_TS + ",GETDATE()) as HOUR_DIFF\n");
		sql.append("from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_XECT + " a with (NOLOCK)\n");
		sql.append("inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_GRP_XECT + " b with (NOLOCK)\n");
		sql.append("  on (b." + COL_DW_JOB_GRP_XECT_ID +" = a." + COL_DW_JOB_GRP_XECT_ID + ")\n");
		sql.append("where b." + COL_DW_JOB_GRP_END_TS + " is not null\n");
		sql.append("and   b." + COL_LAST_DW_ERR_RESN_CD + " = '1'\n");
		sql.append("and   a." + COL_DW_JOB_XECT_ID + " = {JOBID}\n");
		
		System.out.println("\nChecking existing processed landing zone source files for exipred (more than " + minHours + " hours old):\n"); 
		
		lzRoot = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsLandingZoneSubDir());
		
		status = fileSystem.listStatus(lzRoot);
	
		if ( status != null ) {
			for (int idx=0; idx < status.length; idx++ ) {
				dirName = status[idx].getPath().getName();
				
				if ( dirName.matches("[0-9]+") ) {
					System.out.print(lzRoot.toString() + Path.SEPARATOR + dirName);
					
					rset = abac.resultSet(sql.toString().replaceAll("\\{JOBID\\}", dirName));
					
					if ( rset.next() ) {
						actHours = rset.getInt("HOUR_DIFF");
						System.out.print(" " + actHours + " Hours old,");
						if ( actHours > minHours ) { 
							System.out.print(" removing");
							
							if ( trash.moveToTrash( status[idx].getPath()) ) {
								System.out.println(", moved to trash");
							} else {
								System.out.println(", not moved to trash");
							}
							
							
						} else {
							System.out.println(" keeping");
						}
					} else {
						System.out.println(" not found in ABaC, keeping");
					}
					
					rset.close();
				}
			}
		}		
		

		
		/*
		Trash tt = new Trash(fs,conf);
		
		Path tf = new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + "file1");
		
		System.out.println("Processing file = " + tf.toString());
		
		if ( fs.exists(tf) ) {
			if ( tt.moveToTrash(new Path(daasConfig.hdfsRoot() + Path.SEPARATOR + "file1")) ) {
				System.out.println(tf.toString() + " moved to trash");
			} else {
				System.out.println(tf.toString() + " NOT moved to trash");
			}
				
		} else {
			System.out.println(tf.toString() + " does not exist");
		}
		*/
	}
	
	public int getOpenJobGroupId(String jobName){
		
		ResultSet rs = null;
		String sql = "select "+COL_DW_JOB_GRP_XECT_ID+" from "+daasConfig.abacSqlServerDb() + "." +TBL_DW_JOB_GRP_XECT +" where "+COL_DW_JOB_GRP_SUBJ_DS+"='"+jobName+
					 "' and "+COL_DW_JOB_GRP_END_TS +" is null order by "+ COL_DW_JOB_GRP_XECT_ID +" desc" ;
		try{
		
			
			
			rs = abac.resultSet(sql);
		
			if(rs != null && rs.next()){
				
				return rs.getInt(1);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{
				if((rs !=null))
					rs.close();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		System.out.println(" returning -1");
		return -1;
	}
	
	public ArrayList<String> getSubFileTypeCodes() throws Exception {
		
		ArrayList<String> retList = new ArrayList<String>();

		ResultSet rset;
		
		sql.setLength(0);
		sql.append("select\n");
		sql.append("   a." + COL_DW_SUB_FILE_DATA_TYP_CD + "\n");
		sql.append("from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_SUB_FILE_DATA_TYP + " a with (NOLOCK)\n");
		sql.append("inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_DATA_TYP + " b with (NOLOCK)\n");
		sql.append("  on (b." + COL_DW_DATA_TYP_ID + " = a." + COL_DW_DATA_TYP_ID + ")\n");
		sql.append("where b." + COL_DW_DATA_TYP_DS + "= '" + daasConfig.fileType() +"'\n");
		
		rset = abac.resultSet(sql.toString());
		
		while ( rset.next() ) {
			retList.add(rset.getString(COL_DW_SUB_FILE_DATA_TYP_CD));
		}
		
		rset.close();
		
		return(retList);
	}
	
	public ResultSet resultSet(String sql) throws Exception {
		try{
			return abac.resultSet(sql);
		}catch(Exception ex){
			throw new Exception(ex.getMessage());
		}
		
	}
	
	public ArrayList<String> getChangedTerrBusinessDatesSinceTs(String subject
			                                                   ,boolean force) throws Exception {
		
		ArrayList<String> retList = new ArrayList<String>();

		ResultSet rset;

		sql.setLength(0);
		sql.append("select\n");
		sql.append("   a." + COL_TERR_CD + "\n");
		sql.append("  ,a." + COL_CAL_DT + "\n");
		sql.append("from (\n");
		sql.append("     select\n");
		sql.append("        a." + COL_DW_JOB_GRP_STRT_TS + "\n");
		sql.append("       ,d." + COL_TERR_CD + "\n");
		sql.append("       ,d." + COL_CAL_DT + "\n");
		sql.append("     from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_GRP_XECT + " a with (NOLOCK)\n");
		sql.append("     inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_XECT + " b with (NOLOCK)\n");
		sql.append("       on (b." + COL_DW_JOB_GRP_XECT_ID + " = a." + COL_DW_JOB_GRP_XECT_ID + ")\n");
		sql.append("     inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE_JOB_XECT_ASSC + " c with (NOLOCK)\n");
		sql.append("       on (c." + COL_DW_JOB_XECT_ID + " = b." + COL_DW_JOB_XECT_ID + ")\n");
		sql.append("     inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " d with (NOLOCK)\n");
		sql.append("       on (d." + COL_DW_FILE_ID + " = c." + COL_DW_FILE_ID + ")\n");
		sql.append("     inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_DATA_TYP + " e with (NOLOCK)\n");
		sql.append("       on (e." + COL_DW_DATA_TYP_ID + " = d." + COL_DW_DATA_TYP_ID + ")\n");
		sql.append("     inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_AUDT_STUS_TYP + " f with (NOLOCK)\n");
		sql.append("       on (f." + COL_DW_AUDT_STUS_TYP_ID + " = d." + COL_DW_AUDT_STUS_TYP_ID + ")\n");
		sql.append("     where e." + COL_DW_DATA_TYP_DS + " = 'POS_XML'\n");
		sql.append("     and   f." + COL_DW_AUDT_STUS_TYP_DS + " = 'SUCCESSFUL'\n");
		sql.append("     group by\n");
		sql.append("        a." + COL_DW_JOB_GRP_STRT_TS + "\n");
		sql.append("       ,d." + COL_TERR_CD + "\n");
		sql.append("       ,d." + COL_CAL_DT + ") a\n");
		sql.append("where a." + COL_DW_JOB_GRP_STRT_TS + " >= (\n");
		sql.append("     select\n");
		sql.append("        min(a." + COL_DW_JOB_GRP_STRT_TS + ") as " + COL_DW_JOB_GRP_STRT_TS + "\n");
		sql.append("     from (\n");
		sql.append("          select\n");
		sql.append("             max(a." + COL_DW_JOB_GRP_STRT_TS + ") as " + COL_DW_JOB_GRP_STRT_TS + "\n");
		sql.append("          from " + daasConfig.abacSqlServerDb() + ".dw_job_grp_xect a with (NOLOCK)\n");
		sql.append("          where a." + COL_DW_JOB_GRP_SUBJ_DS + " = '" + subject.replace("'", "''") + "'\n");
		sql.append("          and   a." + COL_DW_JOB_GRP_END_TS + " is not null\n");
		
		if ( force ) {
			sql.append("          union all\n");
			sql.append("          select\n");
			sql.append("             max(a." + COL_DW_JOB_GRP_STRT_TS + ") as " + COL_DW_JOB_GRP_STRT_TS + "\n");
			sql.append("          from " + daasConfig.abacSqlServerDb() + ".dw_job_grp_xect a with (NOLOCK)\n");
			sql.append("          inner join " + daasConfig.abacSqlServerDb() + ".dw_job_xect b with (NOLOCK)\n");
			sql.append("            on (b." + COL_DW_JOB_GRP_XECT_ID + " = a." + COL_DW_JOB_GRP_XECT_ID + ")\n");
			sql.append("          inner join " + daasConfig.abacSqlServerDb() + ".dw_file_job_xect_assc c with (NOLOCK)\n");
			sql.append("            on (c." + COL_DW_JOB_XECT_ID + " = b." + COL_DW_JOB_XECT_ID + ")\n");
			sql.append("          inner join " + daasConfig.abacSqlServerDb() + ".dw_file d with (NOLOCK)\n");
			sql.append("            on (d." + COL_DW_FILE_ID + " = c." + COL_DW_FILE_ID + ")\n");
			sql.append("          inner join " + daasConfig.abacSqlServerDb() + ".dw_data_typ e with (NOLOCK)\n");
			sql.append("            on (e." + COL_DW_DATA_TYP_ID + " = d." + COL_DW_DATA_TYP_ID + ")\n");
			sql.append("          inner join " + daasConfig.abacSqlServerDb() + ".dw_audt_stus_typ f with (NOLOCK)\n");
			sql.append("            on (f." + COL_DW_AUDT_STUS_TYP_ID + " = d." + COL_DW_AUDT_STUS_TYP_ID + ")\n");
			sql.append("          where e." + COL_DW_DATA_TYP_DS + " = 'POS_XML'\n");
			sql.append("          and   f." + COL_DW_AUDT_STUS_TYP_DS + " = 'SUCCESSFUL'\n");
		}
		
		sql.append("          ) a\n");
		sql.append("     )\n");
		sql.append("group by\n");
		sql.append("   a." + COL_TERR_CD + "\n");
		sql.append("  ,a." + COL_CAL_DT + "\n");
		sql.append("order by\n");
		sql.append("   a." + COL_TERR_CD + "\n");
		sql.append("  ,a." + COL_CAL_DT + "\n");
						
		System.out.println(sql.toString());
		
		rset = abac.resultSet(sql.toString());
		
		while ( rset.next() ) {
			retList.add(rset.getString(COL_TERR_CD) + ":" + rset.getString(COL_CAL_DT));
		}
		
		rset.close();
		
		return(retList);
	}

	//AWS START
	public ArrayList<String> getChangedTerrBusinessDatesSinceTs(String timestamp
			                                                   ,String terrList) throws Exception {
		
		ArrayList<String> retList = new ArrayList<String>();
		String ckTimestamp = timestamp;
		
		if ( ckTimestamp.length() > 0 ) {
			ckTimestamp = "1955-04-01 00:00:00";
		}

		ResultSet rset;

		sql.setLength(0);
		sql.append("select\n");
		sql.append("   a." + COL_TERR_CD + "\n");
		sql.append("  ,a." + COL_CAL_DT + "\n");
		sql.append("from (\n");
		sql.append("     select\n");
		sql.append("        a." + COL_DW_JOB_GRP_STRT_TS + "\n");
		sql.append("       ,d." + COL_TERR_CD + "\n");
		sql.append("       ,d." + COL_CAL_DT + "\n");
		sql.append("     from " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_GRP_XECT + " a with (NOLOCK)\n");
		sql.append("     inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_XECT + " b with (NOLOCK)\n");
		sql.append("       on (b." + COL_DW_JOB_GRP_XECT_ID + " = a." + COL_DW_JOB_GRP_XECT_ID + ")\n");
		sql.append("     inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE_JOB_XECT_ASSC + " c with (NOLOCK)\n");
		sql.append("       on (c." + COL_DW_JOB_XECT_ID + " = b." + COL_DW_JOB_XECT_ID + ")\n");
		sql.append("     inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_FILE + " d with (NOLOCK)\n");
		sql.append("       on (d." + COL_DW_FILE_ID + " = c." + COL_DW_FILE_ID + ")\n");
		sql.append("     inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_DATA_TYP + " e with (NOLOCK)\n");
		sql.append("       on (e." + COL_DW_DATA_TYP_ID + " = d." + COL_DW_DATA_TYP_ID + ")\n");
		sql.append("     inner join " + daasConfig.abacSqlServerDb() + "." + TBL_DW_AUDT_STUS_TYP + " f with (NOLOCK)\n");
		sql.append("       on (f." + COL_DW_AUDT_STUS_TYP_ID + " = d." + COL_DW_AUDT_STUS_TYP_ID + ")\n");
		sql.append("     where e." + COL_DW_DATA_TYP_DS + " = 'POS_XML'\n");
		sql.append("     and   f." + COL_DW_AUDT_STUS_TYP_DS + " = 'SUCCESSFUL'\n");
		sql.append("     group by\n");
		sql.append("        a." + COL_DW_JOB_GRP_STRT_TS + "\n");
		sql.append("       ,d." + COL_TERR_CD + "\n");
		sql.append("       ,d." + COL_CAL_DT + ") a\n");
		sql.append("where a." + COL_DW_JOB_GRP_STRT_TS + " >= '" + ckTimestamp + "'\n");

		if ( terrList.length() > 0 ) {
			sql.append("and   a." + COL_TERR_CD + " in ('" + terrList + "')\n");
		}

		sql.append("group by\n");
		sql.append("   a." + COL_TERR_CD + "\n");
		sql.append("  ,a." + COL_CAL_DT + "\n");
		sql.append("order by\n");
		sql.append("   a." + COL_TERR_CD + "\n");
		sql.append("  ,a." + COL_CAL_DT + "\n");
		
		if ( daasConfig.displayMsgs() ) {
			System.out.println(sql.toString());
		}
		
		rset = abac.resultSet(sql.toString());
		
		while ( rset.next() ) {
			retList.add(rset.getString(COL_TERR_CD) + "\t" + rset.getString(COL_CAL_DT));    
		}
		
		rset.close();
		
		return(retList);
	}

	public ArrayList<String> getChangedDatahubPathsSinceLastTs(String timestamp
                                                              ,String terrList) throws Exception {

		ArrayList<String> retList = new ArrayList<String>();
		ArrayList<String> terrDates = getChangedTerrBusinessDatesSinceTs(timestamp,terrList);
		String[] parts;
		
		for ( String terrDate : terrDates ) {
			parts = terrDate.split("\t");
			retList.add(daasConfig.hdfsRoot() + Path.SEPARATOR + daasConfig.hdfsHiveSubDir() + Path.SEPARATOR + "datahub" + Path.SEPARATOR + "tld" + Path.SEPARATOR + "terr_cd=" + parts[0] + Path.SEPARATOR + "pos_busn_dt=" + parts[1]);
		}
		
		return(retList);
		
	}
	//AWS END
	
	public void insertExecutionTargetFile(int jobId
                                         ,int seqNum
                                         ,String targetName
                                         ,String targetDesc
                                         ,String targetTypeDesc
                                         ,int recordCount) throws Exception {

		insertExecutionTargetFile(jobId,seqNum,targetName,targetDesc,targetTypeDesc,recordCount,DECIMAL_ZERO,DECIMAL_ZERO,0);
		
	}
	
	public void insertExecutionTargetFile(int jobId
                                         ,int seqNum
                                         ,String targetName
                                         ,String targetDesc
                                         ,String targetTypeDesc
                                         ,int recordCount
                                         ,BigDecimal netSalesAm
                                         ,BigDecimal grossSalesAm
                                         ,int orderCount) throws Exception {
		
		sql.setLength(0);
		sql.append("insert into " + daasConfig.abacSqlServerDb() + "." + TBL_DW_JOB_XECT_TRGT + " (\n");
		sql.append("   " + COL_DW_JOB_XECT_ID + "\n");
		sql.append("  ," + COL_DW_JOB_TRGT_SEQ_NU + "\n");
		sql.append("  ," + COL_DW_JOB_TRGT_NA + "\n");
		sql.append("  ," + COL_DW_JOB_TRGT_DS + "\n");
		sql.append("  ," + COL_DW_JOB_TRGT_TYP_DS + "\n");
		sql.append("  ," + COL_DW_TRGT_REC_CNT_QT + "\n");
		sql.append("  ," + COL_DW_TRGT_TOT_NET_SLS_AM + "\n");
		sql.append("  ," + COL_DW_TRGT_TOT_GRSS_SLS_AM + "\n");
		sql.append("  ," + COL_DW_TRGT_TRN_ORD_CNT_QT + ")\n");
		sql.append("values (\n");
		sql.append("   " + jobId + "\n");
		sql.append("  ," + seqNum + "\n");
		sql.append("  ,'" + targetName.replace("'","''") + "'\n");
		sql.append("  ,'" + targetDesc.replace("'","''") + "'\n");
		sql.append("  ,'" + targetTypeDesc.replace("'","''") + "'\n");
		sql.append("  ," + recordCount + "\n");
		sql.append("  ," + netSalesAm.toString() + "\n");
		sql.append("  ," + grossSalesAm.toString() + "\n");
		sql.append("  ," + orderCount + "\n");
		sql.append(")");
		
		abac.executeUpdate(sql.toString());
		
		abac.commit();

	}
	
	public void insertExecutionTargetFile(int jobId, int seqNum,
			String targetName, String targetDesc, String targetTypeDesc,
			int recordCount, int uniqueCount) throws Exception {

		insertExecutionTargetFile(jobId, seqNum, targetName, targetDesc,
				targetTypeDesc, recordCount, DECIMAL_ZERO, DECIMAL_ZERO, 0,
				uniqueCount);

	}

	public void insertExecutionTargetFile(int jobId, int seqNum,
			String targetName, String targetDesc, String targetTypeDesc,
			int recordCount, BigDecimal netSalesAm, BigDecimal grossSalesAm,
			int orderCount, int uniqueCount) throws Exception {

		sql.setLength(0);
		sql.append("insert into " + daasConfig.abacSqlServerDb() + "."
				+ TBL_DW_JOB_XECT_TRGT + " (\n");
		sql.append("   " + COL_DW_JOB_XECT_ID + "\n");
		sql.append("  ," + COL_DW_JOB_TRGT_SEQ_NU + "\n");
		sql.append("  ," + COL_DW_JOB_TRGT_NA + "\n");
		sql.append("  ," + COL_DW_JOB_TRGT_DS + "\n");
		sql.append("  ," + COL_DW_JOB_TRGT_TYP_DS + "\n");
		sql.append("  ," + COL_DW_TRGT_REC_CNT_QT + "\n");
		sql.append("  ," + COL_DW_TRGT_TOT_NET_SLS_AM + "\n");
		sql.append("  ," + COL_DW_TRGT_TOT_GRSS_SLS_AM + "\n");
		sql.append("  ," + COL_DW_TRGT_TRN_ORD_CNT_QT + "\n");
		sql.append("  ," + COL_DW_TRGT_UNIQ_REST_CNT_QT + ")\n");
		sql.append("values (\n");
		sql.append("   " + jobId + "\n");
		sql.append("  ," + seqNum + "\n");
		sql.append("  ,'" + targetName.replace("'", "''") + "'\n");
		sql.append("  ,'" + targetDesc.replace("'", "''") + "'\n");
		sql.append("  ,'" + targetTypeDesc.replace("'", "''") + "'\n");
		sql.append("  ," + recordCount + "\n");
		sql.append("  ," + netSalesAm.toString() + "\n");
		sql.append("  ," + grossSalesAm.toString() + "\n");
		sql.append("  ," + orderCount + "\n");
		sql.append("  ," + uniqueCount + "\n");
		sql.append(")");

		abac.executeUpdate(sql.toString());

		abac.commit();

	}
	
	public void insertExecutionTargetFile(int jobId, int seqNum,
			String targetName, String targetDesc, String targetTypeDesc,
			int recordCount, int uniqueCount,String srcFileId) throws Exception {

		insertExecutionTargetFile(jobId, seqNum, targetName, targetDesc,
				targetTypeDesc, recordCount, DECIMAL_ZERO, DECIMAL_ZERO, 0,
				uniqueCount,srcFileId);

	}

	public void insertExecutionTargetFile(int jobId, int seqNum,
			String targetName, String targetDesc, String targetTypeDesc,
			int recordCount, BigDecimal netSalesAm, BigDecimal grossSalesAm,
			int orderCount, int uniqueCount,String srcFileId) throws Exception {

		sql.setLength(0);
		sql.append("insert into " + daasConfig.abacSqlServerDb() + "."
				+ TBL_DW_JOB_XECT_TRGT + " (\n");
		sql.append("   " + COL_DW_JOB_XECT_ID + "\n");
		sql.append("  ," + COL_DW_JOB_TRGT_SEQ_NU + "\n");
		sql.append("  ," + COL_DW_JOB_TRGT_NA + "\n");
		sql.append("  ," + COL_DW_JOB_TRGT_DS + "\n");
		sql.append("  ," + COL_DW_JOB_TRGT_TYP_DS + "\n");
		sql.append("  ," + COL_DW_TRGT_REC_CNT_QT + "\n");
		sql.append("  ," + COL_DW_TRGT_TOT_NET_SLS_AM + "\n");
		sql.append("  ," + COL_DW_TRGT_TOT_GRSS_SLS_AM + "\n");
		sql.append("  ," + COL_DW_TRGT_TRN_ORD_CNT_QT + "\n");
		sql.append("  ," + COL_DW_TRGT_UNIQ_REST_CNT_QT + "\n");
		sql.append("  ," + COL_DW_SRCE_FILE_ID + ")\n");
		sql.append("values (\n");
		sql.append("   " + jobId + "\n");
		sql.append("  ," + seqNum + "\n");
		sql.append("  ,'" + targetName.replace("'", "''") + "'\n");
		sql.append("  ,'" + targetDesc.replace("'", "''") + "'\n");
		sql.append("  ,'" + targetTypeDesc.replace("'", "''") + "'\n");
		sql.append("  ," + recordCount + "\n");
		sql.append("  ," + netSalesAm.toString() + "\n");
		sql.append("  ," + grossSalesAm.toString() + "\n");
		sql.append("  ," + orderCount + "\n");
		sql.append("  ," + uniqueCount + "\n");
		sql.append("  ," + srcFileId + "\n");
		sql.append(")");

		abac.executeUpdate(sql.toString());

		abac.commit();

	}

}

