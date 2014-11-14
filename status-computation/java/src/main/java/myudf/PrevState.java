package myudf;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.impl.logicalLayer.schema.Schema;


public class PrevState extends EvalFunc<Tuple> {
	
	private String endpoint_metric="";
	private String prevState = "";
	private static final String lastState ="LAST";
	
	
	
	
	@Override
	public Tuple exec(Tuple input) throws IOException {
		
		// Get endpoint-metric info
		StringBuilder sb= new StringBuilder();
		sb.append(input.get(5)).append(input.get(4)).append(input.get(3));
				
		// first iteration or change in metric
		if ( (endpoint_metric.equals("")) ) {
			input.append(lastState); //We've jumped to another metric
			endpoint_metric = sb.toString(); //Set the endpoint_metric info
		} else if (!(endpoint_metric.equals(sb.toString()))){
			input.append(lastState);
			endpoint_metric = sb.toString();
			
		} else
		{
			input.append(prevState);
			
			// Calculate integer of date and time
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
		    Date parsedDate = null;
			try {
				parsedDate = dateFormat.parse((String) input.get(1));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    Calendar cal = Calendar.getInstance();
		    cal.setTime(parsedDate);
		    int date_int = (cal.get(Calendar.YEAR) * 10000 ) + ((cal.get(Calendar.MONTH) + 1)*100) + (cal.get(Calendar.DAY_OF_MONTH));
		    int time_int = (cal.get(Calendar.HOUR_OF_DAY) * 10000) + ((cal.get(Calendar.MINUTE)*100)) + (cal.get(Calendar.SECOND));
		    
		    input.append(date_int);
		    input.append(time_int);
		
		}
		
		// set the new previous state before going on
		prevState = (String) input.get(6);
				
		return input;
		
	}
	
	@Override
    public Schema outputSchema(Schema input) {
        Schema.FieldSchema timestamp = new Schema.FieldSchema("timestamp", DataType.CHARARRAY);
        Schema.FieldSchema roc = new Schema.FieldSchema("roc", DataType.CHARARRAY);
        Schema.FieldSchema nagios_host = new Schema.FieldSchema("nagios_host", DataType.CHARARRAY);
        Schema.FieldSchema metric_type = new Schema.FieldSchema("metric_type", DataType.CHARARRAY);
        Schema.FieldSchema service_type = new Schema.FieldSchema("service_type", DataType.CHARARRAY);
        Schema.FieldSchema hostname = new Schema.FieldSchema("hostname", DataType.CHARARRAY);
        Schema.FieldSchema metric_status = new Schema.FieldSchema("metric_status", DataType.CHARARRAY);
        Schema.FieldSchema vo_name = new Schema.FieldSchema("vo_name", DataType.CHARARRAY);
        Schema.FieldSchema vo_fqan = new Schema.FieldSchema("vo_fqan", DataType.CHARARRAY);
        Schema.FieldSchema summary = new Schema.FieldSchema("summary", DataType.CHARARRAY);
        Schema.FieldSchema message = new Schema.FieldSchema("message", DataType.CHARARRAY);
        Schema.FieldSchema prev_state = new Schema.FieldSchema("prev_state", DataType.CHARARRAY);
        Schema.FieldSchema date_int = new Schema.FieldSchema("date_int", DataType.CHARARRAY);
        Schema.FieldSchema time_int = new Schema.FieldSchema("time_int", DataType.CHARARRAY);
        
        Schema status_detail = new Schema();
        
        status_detail.add(timestamp);
        status_detail.add(roc);
        status_detail.add(nagios_host );
        status_detail.add(metric_type);
        status_detail.add(service_type);
        status_detail.add(hostname);
        status_detail.add(metric_status);
        status_detail.add(vo_name);
        status_detail.add(vo_fqan);
        status_detail.add(summary);
        status_detail.add(message);
        status_detail.add(prev_state);
        status_detail.add(date_int);
        status_detail.add(time_int);
        
        try {
            return new Schema(new Schema.FieldSchema("status_detail", status_detail, DataType.TUPLE));
        } catch (FrontendException ex) {
           
        }
        
        return null;
    }
	
	
	

}