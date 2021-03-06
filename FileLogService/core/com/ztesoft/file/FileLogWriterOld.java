package com.ztesoft.file;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.ztesoft.config.Configure;


public class FileLogWriterOld {
	

	
	protected final static Logger logger = Logger.getLogger(FileLogWriter.class);

	private static final long MAX_FILE_SIZE = 1024 * 1024 * 1024 * 1L;

	private String fileDir = null;

	private File currFile = null;

	private RandomAccessFile raf = null;

	private FileChannel fc = null;

	private String moduleCode = "iom";

	private String writeType = "msg";
	
	public FileLogWriterOld(){


		String sysBasePath = Configure.IOM_LOG_PATH;
		if(sysBasePath.equals("")){
			setFileDir("./LinuxFileLog"); 
			logger.info("使用默认地址");
		}else{
			logger.info("使用预设地址");
			setFileDir(sysBasePath);
		}
		
		init();
		
		
		
	}

	public String getWriteType() {
		return writeType;
	}

	public void setWriteType(String writeType) {
		this.writeType = writeType;
	}

	public String getFileDir() {
		return fileDir;
	}

	public void setFileDir(String fileDir) {
		this.fileDir = fileDir;
	}
	
	public String getCurrentFile(){
		return currFile.getPath();
	}

	public synchronized void init() {
		if (System.getProperty("APP_NAME") != null) {
			moduleCode = System.getProperty("APP_NAME");
		}

		File baseDir = new File(fileDir);
		if (!baseDir.exists()) {
			baseDir.mkdir();
		}

		File baseDirType = new File(fileDir + "/" + moduleCode);
		if (!baseDirType.exists()) {
			baseDirType.mkdir();
		}

		String dirPath = fileDir + "/" + moduleCode + "/"
				+ currentDateToString("yyyy-MM-dd");
		File file0 = new File(dirPath);
		if (!file0.exists()) {
			file0.mkdir();

		}

		// 文件處理
		File[] files = file0.listFiles(new NpiFileFileter());
		if (files.length == 0) {
			currFile = makeFile(dirPath);
		} else if (files.length == 1) {
			File file = files[0];
			if (file.length() >= MAX_FILE_SIZE) {// file size over setting
				// temp文件大于最大長度时，將文件改名，并重新創建一個新的文件
				String fileName = file.getAbsolutePath();// temp
				fileName = fileName.substring(0, fileName.length() - 5);
				file.renameTo(new File(fileName));

				// create new file
				currFile = makeFile(dirPath);
			} else {
				currFile = file;
			}
		} else {// 多个文件,对其处理
			File f = null;
			for (File file : files) {
				if (file.length() < MAX_FILE_SIZE) {
					if (f == null) {
						f = file;
					}
				} else {// 直接改名
					String fileName = file.getName();// temp
					fileName = fileName.substring(0, fileName.length() - 5);
					file.renameTo(new File(fileName));
				}
			}
			currFile = f;
		}

		// 初始化异常
		try {
			raf = new RandomAccessFile(currFile, "rw");
			fc = raf.getChannel();// 获取通道
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
		
	}

	private File makeFile(String dirPath) {
		String newFileName = writeType + "-"
				+ currentDateToString("yyyyMMddHHmmss") + ".dat_temp";
		File file = new File(dirPath + "/" + newFileName);
		try {
			file.createNewFile();
			//创建文件记录到pb_ispp_file_config
			String file_path = dirPath + "/" + newFileName;
//			ModuleConfig moduleConfig = orderDefineDao.getModuleConfig(System.getProperty("APP_NAME").trim());
//			orderDefineDao.insertPbFileConfig(file_path.substring(0, file_path.lastIndexOf("_temp")), moduleConfig.getIp());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
		return file;
	}

	public static String currentDateToString(String timeRegex) {
		SimpleDateFormat formatDate = new SimpleDateFormat(timeRegex);
		String str = formatDate.format(new Date());
		return str;
	}

	public synchronized FileInfo writeFile(byte[] b) {
		FileInfo info = null;
		long position = -1;
		int logLength = -1;
		try {
			position = fc.position();
			if (position == 0) {
				fc.position(fc.size());
				position = fc.position();
			}
			ByteBuffer buffer = ByteBuffer.wrap(b);
			fc.write(buffer);
			buffer.clear();

			
			//减去传入参数头部和尾部长度后为log实际长度
			logLength=b.length-64-9;
			
			info = new FileInfo();
			String fileName = currFile.getPath();
			info.setFileName(fileName.substring(0, fileName.length() - 5));
			info.setPosition(position);
			info.setLogLength(logLength);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			close();
			init();
		}

		if (position + b.length > MAX_FILE_SIZE) {
			// close io
			close();
			// rename
			String fileName = currFile.getAbsolutePath();// temp
			fileName = fileName.substring(0, fileName.length() - 5);
			currFile.renameTo(new File(fileName));
			// create new file
			init();
		}

		return info;
	}

	private void close() {
		if (fc != null) {
			try {
				fc.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (raf != null) {
			try {
				raf.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	class NpiFileFileter implements FileFilter {

		public boolean accept(File pathname) {
			String fileName = pathname.getName();
			if (fileName.indexOf(writeType) != -1
					&& fileName.indexOf("_temp") != -1) {
				return true;
			} else {
				return false;
			}

		}
	}

//	static FileLogWriter f = new FileLogWriter();
//	
//	public class Worker implements Runnable{
//		private int threadflag;
//		public Worker(int i){
//			this.threadflag = i;
//		}
//
//		@Override
//		public void run() {
//
//			
//			System.out.println("++++++++++++++线程："+threadflag+"+++++++++++++++++");
//			f.init();
//			for (int j = 0; j < 10; j++) {
//				FileInfo info = f.writeFile(FileHelper.createBytes(
//						"lbp is handsome boy...", "v1.0"));
//				System.out.println("线程"+threadflag+":"+info.getFileName() + " & "
//						+ info.getPosition());
//			
//		}
//		
//	
//			
//		}
//		
//	}
//	public static int threadnum; 
//	private static ExecutorService exe=null;
	public static void main(String[] args) throws IOException {
//		System.out.println("start");
//		// System.out.println(Long.MAX_VALUE);
//		// System.out.println(1024 * 1024 * 1024 * 4l);
//		FileLogWriter f = new FileLogWriter();
//		
//		f.init();
//		// for (int i = 0; i < 1000; i++) {
//		// long s = System.currentTimeMillis();
//		// f.readMsg(447045938L);
//		// System.out.println(System.currentTimeMillis() - s);
//		// }
//		
//		
//		
//		int size = 2;
//		int i = 0;
//		while(i<size){
//			
//			exe=Executors.newFixedThreadPool(10);
//			exe.execute(f.new Worker(i));
//			i++;
//		}
		
		FileLogWriterOld writer = new FileLogWriterOld();

		FileInfo finfo =null;
		for(int i= 0;i<10;i++){
			finfo = writer
					.writeFile(FileHelper
							.createBytes(
									"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n	<soapenv:Header>\n		<Esb>\n			<Route>\n				<Sender>46.1001</Sender>\n				<Time>2015-09-01 20:29:57</Time>\n				<ServCode>46.1109.CrmToExternalArchiveSync</ServCode>\n				<MsgId>oip20150901000006751497</MsgId>\n				<From>CRM</From>\n				<To>OIP</To>\n			</Route>\n		</Esb>\n	</soapenv:Header>\n	<soapenv:Body>\n		<handler xmlns=\"http://service.ispp.ztesoft.com/\">\n			<inXML xmlns=\"\"><![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Root>\n	<MsgHead>\n		<RequestTime>20150901202917</RequestTime>\n		<From>CRM</From>\n		<To>EAI</To>\n		<SysSign>CRM</SysSign>\n		<ServiceName>CrmToExternalArchiveSync</ServiceName>\n		<Serial>oip20150901000006751497</Serial>\n	</MsgHead>\n	<MsgBody>\n		<Request>\n			<serialNbr>oip20150901000006751497</serialNbr>\n			<depNbr>oip20150901000006751497</depNbr>\n			<input_date>20150901202916</input_date>\n				\n			<orderItem>\n				<orderItemId>888487739</orderItemId>\n				<custOrderId>461647031</custOrderId>\n				<classId>6</classId>\n				<orderItemObjId>4447031871</orderItemObjId>\n				<createDate>20150901202028</createDate>\n				<updateDate>20150901202912</updateDate>\n				<finishDate>20150901202751</finishDate>\n				<delevopStaffId>318007</delevopStaffId>\n				<delevopTeamId>2016547</delevopTeamId>\n				<delevopStaffCode>dx23850</delevopStaffCode>\n				<delevopStaffName>高振</delevopStaffName>\n				<serviceOfferId>500</serviceOfferId>\n				<delevopTeamCode>846003800050001</delevopTeamCode>\n				<delevopTeamName>市直管中小学分部</delevopTeamName>\n				<saleStaffId>318007</saleStaffId>\n				<operatorsId>10501</operatorsId>\n				<entSpecType>BO</entSpecType>\n				<entSpecId>900000067</entSpecId>\n			</orderItem>\n			<orderItem>\n				<orderItemId>888487805</orderItemId>\n				<custOrderId>461647031</custOrderId>\n				<classId>6</classId>\n				<orderItemObjId>4447033646</orderItemObjId>\n				<createDate>20150901202028</createDate>\n				<updateDate>20150901202912</updateDate>\n				<finishDate>20150901202751</finishDate>\n				<delevopStaffId>318007</delevopStaffId>\n				<delevopTeamId>2016547</delevopTeamId>\n				<delevopStaffCode>dx23850</delevopStaffCode>\n				<delevopStaffName>高振</delevopStaffName>\n				<serviceOfferId>500</serviceOfferId>\n				<delevopTeamCode>846003800050001</delevopTeamCode>\n				<delevopTeamName>市直管中小学分部</delevopTeamName>\n				<saleStaffId>318007</saleStaffId>\n				<operatorsId>10501</operatorsId>\n				<entSpecType/>\n				<entSpecId/>\n			</orderItem>\n			<orderItem>\n				<orderItemId>888487700</orderItemId>\n				<custOrderId>461647031</custOrderId>\n				<classId>6</classId>\n				<orderItemObjId>4447031867</orderItemObjId>\n				<createDate>20150901202028</createDate>\n				<updateDate>20150901202914</updateDate>\n				<finishDate>20150901202751</finishDate>\n				<delevopStaffId>318007</delevopStaffId>\n				<delevopTeamId>2016547</delevopTeamId>\n				<delevopStaffCode>dx23850</delevopStaffCode>\n				<delevopStaffName>高振</delevopStaffName>\n				<serviceOfferId>500</serviceOfferId>\n				<delevopTeamCode>846003800050001</delevopTeamCode>\n				<delevopTeamName>市直管中小学分部</delevopTeamName>\n				<saleStaffId>318007</saleStaffId>\n				<operatorsId>10501</operatorsId>\n				<entSpecType>BO</entSpecType>\n				<entSpecId>900000152</entSpecId>\n			</orderItem>\n			<orderItem>\n				<orderItemId>888487655</orderItemId>\n				<custOrderId>461647031</custOrderId>\n				<classId>6</classId>\n				<orderItemObjId>4447031865</orderItemObjId>\n				<createDate>20150901202028</createDate>\n				<updateDate>20150901202914</updateDate>\n				<finishDate>20150901202751</finishDate>\n				<delevopStaffId>318007</delevopStaffId>\n				<delevopTeamId>2016547</delevopTeamId>\n				<delevopStaffCode>dx23850</delevopStaffCode>\n				<delevopStaffName>高振</delevopStaffName>\n				<serviceOfferId>500</serviceOfferId>\n				<delevopTeamCode>846003800050001</delevopTeamCode>\n				<delevopTeamName>市直管中小学分部</delevopTeamName>\n				<saleStaffId>318007</saleStaffId>\n				<operatorsId>10501</operatorsId>\n				<entSpecType>BO</entSpecType>\n				<entSpecId>900000114</entSpecId>\n			</orderItem>\n			<orderItem>\n				<orderItemId>888487539</orderItemId>\n				<custOrderId>461647031</custOrderId>\n				<classId>4</classId>\n				<orderItemObjId>3089617403</orderItemObjId>\n				<createDate>20150901202028</createDate>\n				<updateDate>20150901202915</updateDate>\n				<finishDate>20150901202751</finishDate>\n				<delevopStaffId>318007</delevopStaffId>\n				<delevopTeamId>2016547</delevopTeamId>\n				<delevopStaffCode>dx23850</delevopStaffCode>\n				<delevopStaffName>高振</delevopStaffName>\n				<serviceOfferId>100</serviceOfferId>\n				<delevopTeamCode>846003800050001</delevopTeamCode>\n				<delevopTeamName>市直管中小学分部</delevopTeamName>\n				<saleStaffId>318007</saleStaffId>\n				<operatorsId>10501</operatorsId>\n				<entSpecType/>\n				<entSpecId/>\n			</orderItem>\n			<orderItem>\n				<orderItemId>888487542</orderItemId>\n				<custOrderId>461647031</custOrderId>\n				<classId>6</classId>\n				<orderItemObjId>4447031862</orderItemObjId>\n				<createDate>20150901202028</createDate>\n				<updateDate>20150901202915</updateDate>\n				<finishDate>20150901202751</finishDate>\n				<delevopStaffId>318007</delevopStaffId>\n				<delevopTeamId>2016547</delevopTeamId>\n				<delevopStaffCode>dx23850</delevopStaffCode>\n				<delevopStaffName>高振</delevopStaffName>\n				<serviceOfferId>500</serviceOfferId>\n				<delevopTeamCode>846003800050001</delevopTeamCode>\n				<delevopTeamName>市直管中小学分部</delevopTeamName>\n				<saleStaffId>318007</saleStaffId>\n				<operatorsId>10501</operatorsId>\n				<entSpecType>BO</entSpecType>\n				<entSpecId>900000065</entSpecId>\n			</orderItem>\n			<orderItem>\n				<orderItemId>888487729</orderItemId>\n				<custOrderId>461647031</custOrderId>\n				<classId>4</classId>\n				<orderItemObjId>3089617430</orderItemObjId>\n				<createDate>20150901202028</createDate>\n				<updateDate>20150901202915</updateDate>\n				<finishDate>20150901202751</finishDate>\n				<delevopStaffId>318007</delevopStaffId>\n				<delevopTeamId>2016547</delevopTeamId>\n				<delevopStaffCode>dx23850</delevopStaffCode>\n				<delevopStaffName>高振</delevopStaffName>\n				<serviceOfferId>100</serviceOfferId>\n				<delevopTeamCode>846003800050001</delevopTeamCode>\n				<delevopTeamName>市直管中小学分部</delevopTeamName>\n				<saleStaffId>318007</saleStaffId>\n				<operatorsId>10501</operatorsId>\n				<entSpecType/>\n				<entSpecId/>\n			</orderItem>\n			<orderItem>\n				<orderItemId>888487736</orderItemId>\n				<custOrderId>461647031</custOrderId>\n				<classId>4</classId>\n				<orderItemObjId>3089617432</orderItemObjId>\n				<createDate>20150901202028</createDate>\n				<updateDate>20150901202915</updateDate>\n				<finishDate>20150901202751</finishDate>\n				<delevopStaffId>318007</delevopStaffId>\n				<delevopTeamId>2016547</delevopTeamId>\n				<delevopStaffCode>dx23850</delevopStaffCode>\n				<delevopStaffName>高振</delevopStaffName>\n				<serviceOfferId>100</serviceOfferId>\n				<delevopTeamCode>846003800050001</delevopTeamCode>\n				<delevopTeamName>市直管中小学分部</delevopTeamName>\n				<saleStaffId>318007</saleStaffId>\n				<operatorsId>10501</operatorsId>\n				<entSpecType/>\n				<entSpecId/>\n			</orderItem>\n			<orderItem>\n				<orderItemId>888487649</orderItemId>\n				<custOrderId>461647031</custOrderId>\n				<classId>4</classId>\n				<orderItemObjId>3089617426</orderItemObjId>\n				<createDate>20150901202028</createDate>\n				<updateDate>20150901202915</updateDate>\n				<finishDate>20150901202751</finishDate>\n				<delevopStaffId>318007</delevopStaffId>\n				<delevopTeamId>2016547</delevopTeamId>\n				<delevopStaffCode>dx23850</delevopStaffCode>\n				<delevopStaffName>高振</delevopStaffName>\n				<serviceOfferId>100</serviceOfferId>\n				<delevopTeamCode>846003800050001</delevopTeamCode>\n				<delevopTeamName>市直管中小学分部</delevopTeamName>\n				<saleStaffId>318007</saleStaffId>\n				<operatorsId>10501</operatorsId>\n				<entSpecType/>\n				<entSpecId/>\n			</orderItem>\n			<orderItem>\n				<orderItemId>888487688</orderItemId>\n				<custOrderId>461647031</custOrderId>\n				<classId>6</classId>\n				<orderItemObjId>4447031866</orderItemObjId>\n				<createDate>20150901202028</createDate>\n				<updateDate>20150901202916</updateDate>\n				<finishDate>20150901202751</finishDate>\n				<delevopStaffId>318007</delevopStaffId>\n				<delevopTeamId>2016547</delevopTeamId>\n				<delevopStaffCode>dx23850</delevopStaffCode>\n				<delevopStaffName>高振</delevopStaffName>\n				<serviceOfferId>500</serviceOfferId>\n				<delevopTeamCode>846003800050001</delevopTeamCode>\n				<delevopTeamName>市直管中小学分部</delevopTeamName>\n				<saleStaffId>318007</saleStaffId>\n				<operatorsId>10501</operatorsId>\n				<entSpecType>BO</entSpecType>\n				<entSpecId>900000062</entSpecId>\n			</orderItem>\n			<orderItem>\n				<orderItemId>888487538</orderItemId>\n				<custOrderId>461647031</custOrderId>\n				<classId>78</classId>\n				<orderItemObjId>2521625830</orderItemObjId>\n				<createDate>20150901202028</createDate>\n				<updateDate>20150901202916</updateDate>\n				<finishDate>20150901202751</finishDate>\n				<delevopStaffId/>\n				<delevopTeamId/>\n				<delevopStaffCode/>\n				<delevopStaffName/>\n				<serviceOfferId>100</serviceOfferId>\n				<delevopTeamCode/>\n				<delevopTeamName/>\n				<saleStaffId/>\n				<operatorsId/>\n				<entSpecType/>\n				<entSpecId/>\n			</orderItem>\n			<orderItem>\n				<orderItemId>888487648</orderItemId>\n				<custOrderId>461647031</custOrderId>\n				<classId>6</classId>\n				<orderItemObjId>4447031864</orderItemObjId>\n				<createDate>20150901202028</createDate>\n				<updateDate>20150901202916</updateDate>\n				<finishDate>20150901202751</finishDate>\n				<delevopStaffId>318007</delevopStaffId>\n				<delevopTeamId>2016547</delevopTeamId>\n				<delevopStaffCode>dx23850</delevopStaffCode>\n				<delevopStaffName>高振</delevopStaffName>\n				<serviceOfferId>500</serviceOfferId>\n				<delevopTeamCode>846003800050001</delevopTeamCode>\n				<delevopTeamName>市直管中小学分部</delevopTeamName>\n				<saleStaffId>318007</saleStaffId>\n				<operatorsId>10501</operatorsId>\n				<entSpecType>BO</entSpecType>\n				<entSpecId>900000068</entSpecId>\n			</orderItem>\n		</Request>\n	</MsgBody>\n</Root>\n]]></inXML>\n		</handler>\n	</soapenv:Body>\n</soapenv:Envelope>\n",
									"v1.0"));
		}

		System.out.println(finfo.toString());
		
		
	}

	

}
