package main;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.util.FileManager;

import db.Device;
import db.Room_Device;
import db.Thi_meaning;
import db.getOntology_sql;
import db.getValue_sql;
import db.updateRoomValue_sql;
import db.updateRoom_sql;
import sparql.getSparql;

public class Main {
	private static String fname = "file:///home/iii/Desktop/intelligent_house/empty_ontology/room19.owl";//一開始帶入空的知識本體論
	static OntModel data = ModelFactory.createOntologyModel();
	static String NS = "http://www.owl-ontologies.com/Ontology1467007647.owl#"; // 為一個namespace,通常為要推論的網站網址

	static int AbstractTemp_tmie = 1;
	static int AbstractHumidity_time = 1;
	static int AbstractWindSpeed_time = 1;

	//獲取顯然的值                                 溼度              溫度          風速
	//回傳體感溫度？
	public static String getApparentValue(double humidity, double temp, double windspeed) throws Exception {
		double apparentValue = 0;
		//不知道是什麼
		double e = 0;
		//
		e = (humidity / 100) * 0.6105 * (Math.exp(17.27 * temp / (237.7 + temp)));
		apparentValue = temp + 0.33 * e - 0.7 * windspeed - 4;
		//轉換成字串並保留一位小數
	    /*
	     private String getWeight(String sex,float stature){
		String weight=""; //保存体重
		NumberFormat format=new DecimalFormat();
		
		if(sex.equals("男")){ //计算男士标准体重
		weight=format.format((stature-80)*0.7);
		}else{ //计算女士标准体重
		weight=format.format((stature-70)*0.6);
		}
		return weight;
		}
		NumberFormat format的就是将数字转成字符串，format有很多用法，例如可以指定保留多少位小数点等等
	     */
		NumberFormat formatter = new DecimalFormat("#0.0");
		// System.out.println("體感溫度:" + formatter.format(apparentValue)+" °C");

		return formatter.format(apparentValue);
	}
	//回傳舒適度                                  溼度              溫度
	public static String getComfortValue(double humidity, double temp) throws Exception {
		double comfortValue = 0;
		comfortValue = temp - 0.55 * (1 - (humidity / 100)) * (temp - 14);
		NumberFormat formatter = new DecimalFormat("#0.0");
		// System.out.println("舒適度:" + formatter.format(comfortValue));
		return formatter.format(comfortValue);

	}
	//獲取溫溼度指標值                         溼度              溫度
	public static String getTHIValue(double humidity, double temp) throws Exception {
		double THIValue = 0;
		double td = 0;
		double tmp = 0;

		td = (Math.pow(humidity, 0.125)) * ((112 + (0.9 * temp))) + (0.1 * temp) - 112;
		tmp = Math.exp((17.269 * td) / (td + 237.3)) / Math.exp(17.269 * temp) / (temp + 237.3);
		THIValue = temp - (0.55 * (1 - tmp) * (temp - 14));
		// System.out.println("THI:" + (int)(THIValue));
		//將THIValue先轉換成整形再轉換成字串
		return String.valueOf((int) (THIValue));
	}
	//根據輸入的int值獲取表中溫溼度指標意義
	public static String getTHIMeaning(int THIValue) throws Exception {
		//創建Thi_meaning類別的List
		List<Thi_meaning> thi_meaning = new ArrayList<Thi_meaning>();
		//獲取thi_meaning表中所有的Thi_meaning類別
		thi_meaning = getValue_sql.getTHIMeaning();
		//創建一個int的List
		List<Integer> value = new ArrayList<Integer>();
		//創建一個字串的List
		List<String> meaning_list = new ArrayList<String>();
		//表中有多少個Thi_meaning類別,即有多少個欄位就跑多少次
		for (int i = 0; i < thi_meaning.size(); i++) {
			//Thi_meaning類別的溫溼度指標值
			//indexOf() 方法可返回某个指定的字符串值在字符串中首次出现的位置。从0开始！没有返回-1；方便判断和截取字符串！
			//即沒有~的
			if (thi_meaning.get(i).getValue().indexOf("~") < 0) {
				//將Thi_meaning類別的溫溼度指標值轉換成int,後加入到int List中
				value.add(Integer.valueOf(thi_meaning.get(i).getValue()));
				//將Thi_meaning類別的溫溼度指標意義加入到String List中
				meaning_list.add(thi_meaning.get(i).getValueMeaning());

			} else {
				String tmp = thi_meaning.get(i).getValue();
				//砍掉中間的~
				String value2[] = tmp.split("~");
				value.add(Integer.valueOf(value2[0]));
				value.add(Integer.valueOf(value2[1]));
				//將Thi_meaning類別的溫溼度指標意義加入到String List中
				meaning_list.add(thi_meaning.get(i).getValueMeaning());
			}

		}
		String meaning = "";
		if (THIValue <= value.get(0)) {
			meaning = meaning_list.get(0);
		} else if (THIValue >= value.get(1) && THIValue <= value.get(2)) {
			meaning = meaning_list.get(1);
		} else if (THIValue >= value.get(3) && THIValue <= value.get(4)) {
			meaning = meaning_list.get(2);
		} else if (THIValue >= value.get(5) && THIValue <= value.get(6)) {
			meaning = meaning_list.get(3);
		} else if (THIValue >= value.get(7) && THIValue <= value.get(8)) {
			meaning = meaning_list.get(4);
		} else if (THIValue >= value.get(9)) {
			meaning = meaning_list.get(5);
		}
		// System.out.println("THI Meaning:" + meaning);
		return meaning;
	}
	// 將資料庫的值塞進本體論中,傳入房間和Room_Device類別的List
	public static void setOntologyRoom(String room, List<Room_Device> device_list)// 將資料庫的值塞進本體論中
			throws Exception {
		//static OntModel data = ModelFactory.createOntologyModel();  即data是model
		//創建一個hasPhysicalDevice屬性
		Property hasPhysicalDevice = data.getProperty(NS + "hasPhysicalDevice");
		//創建一個Room class
		OntClass Room = data.getOntClass(NS + "Room");
		//創建一個Room類別的實體room_type
		Individual room_type = Room.createIndividual(NS + room);

		OntClass Device = data.getOntClass(NS + "Device");
		OntClass Sensor = data.getOntClass(NS + "Sensor");
		OntClass SensorProperty = data.getOntClass(NS + "SensorProperty");
		Property hasValue = data.getProperty(NS + "hasValue");

		OntClass AbstractDevice = data.getOntClass(NS + "AbstractDevice");
		Property hasOperation = data.getProperty(NS + "hasOperation");
		Property match = data.getProperty(NS + "match");

		OntClass SensorOperation = data.getOntClass(NS + "SensorOperation");
		Property hasOperationParameter = data.getProperty(NS + "hasOperationParameter");

		//創建實體類別的List抽象風速
		List<Individual> AbstractWindSpeed = new ArrayList<Individual>();
		//創建實體類別的List抽象溼度
		List<Individual> AbstractHumidity = new ArrayList<Individual>();
		//創建實體類別的List抽象溫度
		List<Individual> AbstractTemp = new ArrayList<Individual>();

		
		//Room_Device類別的List裏面有多少個Room_Device
		for (int i = 0; i < device_list.size(); i++) {
			//創建一個Device類別的device實體,把傳進來的room和Room_Device類別的DeviceName裝入實體的命名空間
			Individual device = Device.createIndividual(NS + room + "_" + device_list.get(i).getDeviceName());
			//Room類別的實體的hasPhysicalDevice屬性添加實體device
			room_type.addProperty(hasPhysicalDevice, device);
			//創建一個Device類別的List
			List<Device> device_inf = new ArrayList<Device>();
			//得到Device_Id之後傳進函數,之後在device表中回傳所有滿足傳入的device_id的Device類別
			device_inf = getOntology_sql.getDevice_inf(device_list.get(i).getDevice_Id());
			
			//Device類別的List裏面有多少個Device物件
			for (int j = 0; j < device_inf.size(); j++) {
				//Sensor的實體,命名空間用傳入的room和Device類別的List的成員變量sensor
				Individual sensor_instance = Sensor.createIndividual(NS + room + "_" + device_inf.get(j).getSensor());
				
				//device表中sensor欄位字串中'Temp'出現的位置,即sensor欄位字串中有Temp這幾個字
				if (device_inf.get(j).getSensor().indexOf("Temp") >= 0) {
					//最前面static int AbstractTemp_tmie = 1;
					//抽象溫度_實體_名字
					String AbstractTemp_instance_name = "Temp_Sensor" + Integer.valueOf(AbstractTemp_tmie);
					AbstractTemp_tmie++;
					//創建AbstractDevice的實體,命名空間改爲Temp_Sensor加數字
					Individual AbstractTemp_instance = AbstractDevice.createIndividual(NS + AbstractTemp_instance_name);
					//實體類別的抽象溫度List加入這個實體
					AbstractTemp.add(AbstractTemp_instance);
					//sensor的實體添加這兩個屬性
					sensor_instance.addProperty(match, AbstractTemp_instance);
					
					//創建SensorProperty的實體
					Individual sensorProperty_instance = SensorProperty
							.createIndividual(NS + room + "_" + device_inf.get(j).getSensorProperty());
					//獲取device類別的value轉換成float
					float value = Float.parseFloat(device_inf.get(j).getValue());
					
					
					
					//
					sensorProperty_instance.addLiteral(hasValue, value);
					
					//創建SensorOperation的實體
					Individual SensorOperation_instance = SensorOperation
							.createIndividual(NS + room + "_" + device_inf.get(j).getSensorOperation());
					//添加屬性,中間的是屬性
					SensorOperation_instance.addProperty(hasOperationParameter, sensorProperty_instance);
					AbstractTemp_instance.addProperty(hasOperation, SensorOperation_instance);
					;
				}
				//device表中sensor欄位字串中'Humidity'出現的位置,即sensor欄位字串中有Humidity這幾個字
				if (device_inf.get(j).getSensor().indexOf("Humidity") >= 0) {
					//最前面static int AbstractHumidity_time = 1;
					//抽象溼度_實體_名字
					String AbstractHumidity_instance_name = "Humidity_Sensor" + Integer.valueOf(AbstractHumidity_time);
					AbstractHumidity_time++;
					//創建AbstractHumidity的實體,命名空間改爲Humidity_Sensor加數字
					Individual AbstractHumidity_instance = AbstractDevice
							.createIndividual(NS + AbstractHumidity_instance_name);
					//實體類別的抽象溼度List加入這個實體
					AbstractHumidity.add(AbstractHumidity_instance);
					//sensor的實體添加這兩個屬性
					sensor_instance.addProperty(match, AbstractHumidity_instance);

					//創建SensorProperty的實體
					Individual sensorProperty_instance = SensorProperty
							.createIndividual(NS + room + "_" + device_inf.get(j).getSensorProperty());
					//獲取device類別的value轉換成float
					float value = Float.parseFloat(device_inf.get(j).getValue());
					sensorProperty_instance.addLiteral(hasValue, value);

					//創建SensorOperation的實體
					Individual SensorOperation_instance = SensorOperation
							.createIndividual(NS + room + "_" + device_inf.get(j).getSensorOperation());
					//添加屬性,中間的是屬性
					SensorOperation_instance.addProperty(hasOperationParameter, sensorProperty_instance);
					AbstractHumidity_instance.addProperty(hasOperation, SensorOperation_instance);

				}
				//device表中sensor欄位字串中'Wind_Speed'出現的位置,即sensor欄位字串中有Wind_Speed這幾個字
				if (device_inf.get(j).getSensor().indexOf("Wind_Speed") >= 0) {
					//最前面static int AbstractWindSpeed_time = 1;
					//抽象風速_實體_名字
					String AbstractWind_Speed_instance_name = "Wind_Speed_Sensor"
							+ Integer.valueOf(AbstractWindSpeed_time);
					AbstractWindSpeed_time++;
					//創建AbstractWind_Speed的實體,命名空間改爲Wind_Speed_Sensor加數字
					Individual AbstractWind_Speed_instance = AbstractDevice
							.createIndividual(NS + AbstractWind_Speed_instance_name);
					//實體類別的抽象風速List加入這個實體
					AbstractWindSpeed.add(AbstractWind_Speed_instance);
					//sensor的實體添加這兩個屬性
					sensor_instance.addProperty(match, AbstractWind_Speed_instance);

					//創建SensorProperty的實體
					Individual sensorProperty_instance = SensorProperty
							.createIndividual(NS + room + "_" + device_inf.get(j).getSensorProperty());
					//獲取device類別的value轉換成float
					float value = Float.parseFloat(device_inf.get(j).getValue());
					sensorProperty_instance.addLiteral(hasValue, value);

					//創建SensorOperation的實體
					Individual SensorOperation_instance = SensorOperation
							.createIndividual(NS + room + "_" + device_inf.get(j).getSensorOperation());
					//添加屬性,中間的是屬性
					SensorOperation_instance.addProperty(hasOperationParameter, sensorProperty_instance);
					AbstractWind_Speed_instance.addProperty(hasOperation, SensorOperation_instance);

				}
			}
		}
		//這個方法在後面兩個
		createVirtualDevice(room, AbstractWindSpeed, AbstractHumidity, AbstractTemp);
	}

	//回傳SensorProperty的字串List,傳入虛擬裝置的名字和抽象裝置的實體List
	public static List<String> getSensorProperty(String virtual_device_name, List<Individual> AbstractDevice)
			throws Exception {
		//SensorProperty的字串List
		List<String> SensorProperty = new ArrayList<String>();
		//Operation的字串List
		List<String> Operation = new ArrayList<String>();
		//抽象裝置的List裏有多少個抽象裝置就跑多少次
		for (int i = 0; i < AbstractDevice.size(); i++) {
			//傳入抽象裝置的名稱,加入到sparql查詢的三元組裏面,查詢傳入的抽象裝置的hasOperation屬性有什麼operation
			String queryString = getSparql.getOperation(AbstractDevice.get(i).asIndividual().getLocalName());
			
			//創建查詢對象來操作SPARQL查詢
			Query query = QueryFactory.create(queryString);
			//創建查詢執行對象QueryExecution,將查詢對象連接到指定模型上
			QueryExecution qexec = QueryExecutionFactory.create(query, data);
			//生成查詢結果
			/*
			 * 即查詢了抽象裝置的hasOperation屬性是什麼
			 */
			ResultSet results = qexec.execSelect();

			
			
			while (results.hasNext()) {
				QuerySolution solution = results.next();
				Operation.add(solution.get("Operation").asResource().getLocalName());
			}
		}
		for (int i = 0; i < Operation.size(); i++) {
			String queryString2 = getSparql.getParameter(Operation.get(i));
			Query query2 = QueryFactory.create(queryString2);
			QueryExecution qexec2 = QueryExecutionFactory.create(query2, data);
			ResultSet results2 = qexec2.execSelect();
			while (results2.hasNext()) {
				QuerySolution solution = results2.next();
				SensorProperty.add(solution.get("Parameter").asResource().getLocalName());
			}
		}
		return SensorProperty;

	}

	//創建虛擬裝置,傳入房間 抽象風速的實體List 抽象溼度的實體List 抽象溫度的實體List
	public static void createVirtualDevice(String room, List<Individual> AbstractWindSpeed,
			List<Individual> AbstractHumidity, List<Individual> AbstractTemp) throws Exception {
		//創建抽象裝置的類別
		OntClass VirtualDevice = data.getOntClass(NS + "VirtualDevice");
		//創建屬性
		Property compose = data.getProperty(NS + "compose");
		Property hasRule = data.getProperty(NS + "hasRule");

		String Apparent_Temp_name = room + "_Apparent_Temp";
		String Average_Humidity_name = room + "_Average_Humidity";
		String Average_Temp_name = room + "_Average_Temp";
		String Average_Wind_Speed_name = room + "_Average_Wind_Speed";
		String Comfort_name = room + "_Comfort";
		String THI_name = room + "_THI";
		String THI_Meaning_name = room + "_THI_Meaning";

		List<String> SensorProperty_WindSpeed = new ArrayList<String>();
		List<String> SensorProperty_Humidity = new ArrayList<String>();
		List<String> SensorProperty_Temp = new ArrayList<String>();

		int humidity_flag = 0, wind_flag = 0, temp_flag = 0;

		if (AbstractWindSpeed.size() > 0) {
			//創建虛擬裝置平均風速的實體
			Individual Average_Wind_Speed = VirtualDevice.createIndividual(NS + Average_Wind_Speed_name);

			//抽象風速的List有多少個抽象風速的實體就跑多少次
			for (int i = 0; i < AbstractWindSpeed.size(); i++) {
				//實體的屬性設置
				AbstractWindSpeed.get(i).addProperty(compose, Average_Wind_Speed);
			}
			wind_flag = 1;
			
			//回傳SensorProperty的字串List,傳入虛擬裝置的名字和抽象裝置的實體List
			SensorProperty_WindSpeed = getSensorProperty(Average_Wind_Speed_name, AbstractWindSpeed);

			
			
			String rule = createRule(Average_Wind_Speed, SensorProperty_WindSpeed);
			Average_Wind_Speed.addLiteral(hasRule, rule);

		}
		if (AbstractHumidity.size() > 0) {
			//創建虛擬裝置平均溼度的實體
			Individual Average_Humidity = VirtualDevice.createIndividual(NS + Average_Humidity_name);
			//抽象溼度的List有多少個抽象溼度的實體就跑多少次
			for (int i = 0; i < AbstractHumidity.size(); i++) {
				//實體的屬性設置
				AbstractHumidity.get(i).addProperty(compose, Average_Humidity);
			}
			humidity_flag = 1;
			//回傳SensorProperty的字串List,傳入虛擬裝置的名字和抽象裝置的實體List
			SensorProperty_Humidity = getSensorProperty(Average_Humidity_name, AbstractHumidity);
			String rule = createRule(Average_Humidity, SensorProperty_Humidity);
			Average_Humidity.addLiteral(hasRule, rule);
		}
		if (AbstractTemp.size() > 0) {
			//創建虛擬裝置平均溫度的實體
			Individual Average_Temp = VirtualDevice.createIndividual(NS + Average_Temp_name);
			//抽象溫度的List有多少個抽象溫度的實體就跑多少次
			for (int i = 0; i < AbstractTemp.size(); i++) {
				//實體的屬性設置
				AbstractTemp.get(i).addProperty(compose, Average_Temp);
			}
			temp_flag = 1;
			//回傳SensorProperty的字串List,傳入虛擬裝置的名字和抽象裝置的實體List
			SensorProperty_Temp = getSensorProperty(Average_Temp_name, AbstractTemp);
			String rule = createRule(Average_Temp, SensorProperty_Temp);
			Average_Temp.addLiteral(hasRule, rule);
		}

		if (wind_flag == 1 && humidity_flag == 1 && temp_flag == 1) {
			Individual Apparent_Temp = VirtualDevice.createIndividual(NS + Apparent_Temp_name);
			Apparent_Temp.addLiteral(hasRule, "getApparentValue");

		}
		if (humidity_flag == 1 && temp_flag == 1) {
			Individual Comfort = VirtualDevice.createIndividual(NS + Comfort_name);
			Comfort.addLiteral(hasRule, "getComfortValue");
			Individual THI = VirtualDevice.createIndividual(NS + THI_name);
			THI.addLiteral(hasRule, "getTHIValue");
			Individual THI_Meaning = VirtualDevice.createIndividual(NS + THI_Meaning_name);
			THI_Meaning.addLiteral(hasRule, "getTHIMeaning");
		}

	}

	//回傳創建好的rule,傳入虛擬裝置的名字和SensorProperty_type的字串List
	public static String createRule(Individual VirtualDevice_name, List<String> SensorProperty_type) {
		//一個()裏面要有三個東西,成爲一個三元組
		//[rule1:(?h rdf:type NS:Herbivore)(?c rdf:type NS:Carnivore)->(?c NS:hunt ?h)]
		//三元組的第一個
		String rule = "[rule1: (" + NS + SensorProperty_type.get(0) + " " + NS + "hasValue ?0)";
		int time = 1;
		//三元組的第二個
		for (int i = 1; i < SensorProperty_type.size(); i++) {
			rule = rule + "(" + NS + SensorProperty_type.get(i) + " " + NS + "hasValue ?" + Integer.valueOf(i) + ")";
		}
		
		for (int i = 1; i < SensorProperty_type.size(); i++) {
			if (i == 1) {
				rule = rule + ",sum(?" + Integer.valueOf(i - 1) + ",?" + Integer.valueOf(i) + ",?total"
						+ Integer.valueOf(time) + ")";
				time++;
			} else {
				rule = rule + ",sum(?" + Integer.valueOf(i) + ",?total" + Integer.valueOf(time - 1) + ",?total"
						+ Integer.valueOf(time) + ")";
				time++;
			}

		}
		//推論的結果?
		if (SensorProperty_type.size() > 1) {
			rule = rule + ",quotient(?total" + Integer.valueOf(time - 1) + ","
					+ Integer.valueOf(SensorProperty_type.size()) + ",?sum)" + "->(" + VirtualDevice_name + " " + NS
					+ "hasAverage ?sum)]";
		} else {
			rule = rule + "->(" + VirtualDevice_name + " " + NS + "hasAverage ?0)]";
		}
		return rule;
	}

	//
	public static void scanDatabase() throws Exception {
		List<Integer> room_list = new ArrayList<Integer>();
		//獲取room表中，ontology欄爲Y的的room-id的List
		room_list = getOntology_sql.getRoomID(); // 拿到在資料庫中ontology欄位標示為Y的房間ID

		//room_list中有多少個room-id就跑多少次
		for (int i = 0; i < room_list.size(); i++) {
			//根據傳入的room_id來回傳room表中的room_name_en
			String room_en = getOntology_sql.getRoomEn(room_list.get(i));
			//創建Room_Device的List
			List<Room_Device> device_list = new ArrayList<Room_Device>();
			//在room_device表中回傳所有符合ontology爲Y和傳入的room-id的Room_Device類別
			device_list = getOntology_sql.getDeviceID(room_list.get(i));
			//在很前面的function
			setOntologyRoom(room_en, device_list);
		}
	}

	//傳入虛擬裝置和房間名字
	public static String getValue(String virtual_device, String room_name) throws Exception {
		String rule = "";
		String value = "";
		//傳入虛擬裝置的名稱,加入到sparql查詢的三元組裏面,查詢傳入的虛擬裝置有什麼rule
		String queryString = getSparql.getRule(virtual_device);

		//創建查詢對象來操作SPARQL查詢
		Query query = QueryFactory.create(queryString);
		//將查詢對象連接到指定模型上
		QueryExecution qexec = QueryExecutionFactory.create(query, data);
		//生成查詢結果
		ResultSet results = qexec.execSelect();
		
		while (results.hasNext()) {
			QuerySolution solution = results.next();
			rule = solution.get("rule").asLiteral().getLexicalForm();
		}
		//如果字串rule的內容是"getComfortValue"
		if (rule.equals("getComfortValue")) {
			double average_temp, average_humidity;
			//平均溫度遞回
			average_temp = Double
					.parseDouble(getValue(getValue_sql.getVirtualDevicebyWord(room_name + "的平均溫度"), room_name));
			//平均溼度遞回
			average_humidity = Double
					.parseDouble(getValue(getValue_sql.getVirtualDevicebyWord(room_name + "的平均濕度"), room_name));
			//方法在最前面,得到舒適度
			value = getComfortValue(average_humidity, average_temp);
			return value;
		}
		//如果字串rule的內容是"getTHIValue"
		else if (rule.equals("getTHIValue")) {
			double average_temp, average_humidity;
			//平均溫度遞回
			average_temp = Double
					.parseDouble(getValue(getValue_sql.getVirtualDevicebyWord(room_name + "的平均溫度"), room_name));
			//平均溼度遞回
			average_humidity = Double
					.parseDouble(getValue(getValue_sql.getVirtualDevicebyWord(room_name + "的平均濕度"), room_name));
			//方法在最前面,得到溫溼度指標值
			value = getTHIValue(average_humidity, average_temp);
			return value;
		} else if (rule.equals("getTHIMeaning")) {
			int THI;
			THI = Integer.parseInt(getValue(getValue_sql.getVirtualDevicebyWord(room_name + "的溫溼度指標"), room_name));
			value = getTHIMeaning(THI);
			//方法在最前面,得到溫溼度指標意義
			return value;
		} else if (rule.equals("getApparentValue")) {
			double average_temp, average_humidity, average_windspeed;
			average_temp = Double
					.parseDouble(getValue(getValue_sql.getVirtualDevicebyWord(room_name + "的平均溫度"), room_name));
			average_humidity = Double
					.parseDouble(getValue(getValue_sql.getVirtualDevicebyWord(room_name + "的平均濕度"), room_name));
			average_windspeed = Double
					.parseDouble(getValue(getValue_sql.getVirtualDevicebyWord(room_name + "的平均出口風速"), room_name));
			//得到體感溫度
			value = getApparentValue(average_humidity, average_temp, average_windspeed);
			return value;

		} else {
			Reasoner reasoners = new GenericRuleReasoner(Rule.parseRules(rule));
			InfModel infModel = ModelFactory.createInfModel(reasoners, data);
			String sparql = getSparql.getAverage(virtual_device);

			Query query2 = QueryFactory.create(sparql);
			QueryExecution qexec2 = QueryExecutionFactory.create(query2, infModel);
			ResultSet results2 = qexec2.execSelect();
			while (results2.hasNext()) {
				QuerySolution solution = results2.next();
				String expressionValue = solution.get("target").asLiteral().getLexicalForm();
				NumberFormat formatter = new DecimalFormat("#0.0");
				value = formatter.format(Double.parseDouble(expressionValue));
			}
		}

		return value;
	}
	
	
	
	public static void test01() throws Exception {
		try {
			InputStream in = FileManager.get().open(fname);
			data.read(in, null);
			List<String> random_tmp = new ArrayList<String>();
			List<String> random_humidity = new ArrayList<String>();
			List<String> random_wind_speed = new ArrayList<String>();

			random_tmp = updateRoomValue_sql.getTmpRandom();
			random_humidity = updateRoomValue_sql.getHumidityRandom();
			random_wind_speed = updateRoomValue_sql.getWindSpeedRandom();

			updateRoomValue_sql.getType(random_tmp, "°C");
			updateRoomValue_sql.getType(random_humidity, "%");
			updateRoomValue_sql.getType(random_wind_speed, "m/s");

			scanDatabase();

			String average_temp, average_humidity, average_windspeed, Comfort, Apparent_Temp, THI, THI_Meaning;

			List<String> name_list = new ArrayList<String>();
			name_list = getOntology_sql.getRoomName();

			for (int i = 0; i < name_list.size(); i++) {
				List<String> value_list = new ArrayList<String>();
				average_temp = getValue(getValue_sql.getVirtualDevicebyWord(name_list.get(i) + "的平均溫度"),
						name_list.get(i));
				average_humidity = getValue(getValue_sql.getVirtualDevicebyWord(name_list.get(i) + "的平均濕度"),
						name_list.get(i));
				average_windspeed = getValue(getValue_sql.getVirtualDevicebyWord(name_list.get(i) + "的平均出口風速"),
						name_list.get(i));
				Comfort = getValue(getValue_sql.getVirtualDevicebyWord(name_list.get(i) + "的舒適度"),
						name_list.get(i));
				Apparent_Temp = getValue(getValue_sql.getVirtualDevicebyWord(name_list.get(i) + "的體感溫度"),
						name_list.get(i));
				THI = getValue(getValue_sql.getVirtualDevicebyWord(name_list.get(i) + "的溫溼度指標"),
						name_list.get(i));
				THI_Meaning = getValue(getValue_sql.getVirtualDevicebyWord(name_list.get(i) + "的溫溼度指標意義"),
						name_list.get(i));
				value_list.add(String.valueOf(average_temp));
				value_list.add(String.valueOf(average_humidity));
				value_list.add(String.valueOf(average_windspeed));
				value_list.add(Apparent_Temp);
				value_list.add(Comfort);
				value_list.add(THI);
				value_list.add(THI_Meaning);
				updateRoom_sql.updateRoomStatus(value_list, name_list.get(i));
				System.out.println(name_list.get(i) + "的平均溫度: " + average_temp);
				System.out.println(name_list.get(i) + "的平均濕度: " + average_humidity);
				System.out.println(name_list.get(i) + "的平均出口風速: " + average_windspeed);
				System.out.println(name_list.get(i) + "的舒適度: " + Comfort);
				System.out.println(name_list.get(i) + "的體感溫度: " + Apparent_Temp);
				System.out.println(name_list.get(i) + "的溫溼度指標: " + THI);
				System.out.println(name_list.get(i) + "的溫溼度指標意義: " + THI_Meaning);

			}

			List<String> unselect = new ArrayList<String>();
			unselect = getOntology_sql.unselectRoomName();
			for (int i = 0; i < unselect.size(); i++) {
				List<String> value_list = new ArrayList<String>();
				value_list.add("請選擇房間");
				value_list.add("請選擇房間");
				value_list.add("請選擇房間");
				value_list.add("請選擇房間");
				value_list.add("請選擇房間");
				value_list.add("請選擇房間");
				value_list.add("請選擇房間");
				updateRoom_sql.updateRoomStatus(value_list, unselect.get(i));
			}
			System.out.println("現在時間：" + new Date() + " 更新value!!");
			data = ModelFactory.createOntologyModel();
			AbstractTemp_tmie = 1;
			AbstractHumidity_time = 1;
			AbstractWindSpeed_time = 1;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void main(String args[]) throws Exception {
		//獲取當前計算機時間
		Date time = new Date();
		Timer timer = new Timer();
		//TimerTask類別實現public void run（）方法
		TimerTask task = new TimerTask() {
			public void run() {
				try {
					test01();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		//第二個參數：在特定時間之後第一次執行任務，第三個參數：間隔多少時間後再執行一次，500=0.5秒
		timer.schedule(task, time, 500);
	}
}
