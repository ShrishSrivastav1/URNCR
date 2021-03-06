package in.globalsoft.urncr;

import in.globalsoft.beans.BeanAvailableAppointment;
import in.globalsoft.urncr.R;
import in.globalsoft.preferences.AppPreferences;
import in.globalsoft.util.CalendarAdapter;
import in.globalsoft.util.Cons;
import in.globalsoft.util.Utility;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

public class AvailableAppointments extends Activity {

	public GregorianCalendar month, itemmonth;// calendar instances.

	public CalendarAdapter adapter;// adapter instance
	public Handler handler;// for grabbing some event values for showing the dot
	// marker.
	public ArrayList<String> items; // container to store calendar items which
	// needs showing the event marker
	//ArrayList<String> event;
	private LinearLayout rLayout;
	private ArrayList<String> date;
	private ArrayList<String> desc;
	private AdapterView<?> parent;
	private View v;
	private int position;
	private BeanAvailableAppointment availAppointments;
	private String appointmentdate;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_available_appointments);
		Locale.setDefault(Locale.US);
		TextView tvTitle = (TextView) findViewById(R.id.title_text);
		tvTitle.setText("AVAILABLE APPOINTMENTS");
		TextView tvAvailableAppointmentsHead = (TextView) findViewById(R.id.availableAppointmentsHead);
		String selectedGridDate = Cons.convertDateToString(new Date(), "yyyy-MM-dd");
		tvAvailableAppointmentsHead.setText("Available Appointments For"+"\n"+selectedGridDate);
		rLayout = (LinearLayout) findViewById(R.id.text);
		month = (GregorianCalendar) GregorianCalendar.getInstance();
		itemmonth = (GregorianCalendar) month.clone();

		items = new ArrayList<String>();

		adapter = new CalendarAdapter(this, month);
		setPreviousButtonVisibility();

		GridView gridview = (GridView) findViewById(R.id.gridview);
		gridview.setAdapter(adapter);

		handler = new Handler();
		handler.post(calendarUpdater);
		setAppointments();

		TextView title = (TextView) findViewById(R.id.title);
		title.setText(android.text.format.DateFormat.format("MMMM yyyy", month));

		RelativeLayout previous = (RelativeLayout) findViewById(R.id.previous);

		previous.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setPreviousMonth();
				refreshCalendar();
			}
		});

		RelativeLayout next = (RelativeLayout) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setNextMonth();
				refreshCalendar();

			}
		});

		gridview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) 
			{
				
				desc = new ArrayList<String>();
				date = new ArrayList<String>();
				
				String selectedGridDate = CalendarAdapter.dayString
						.get(position);
				Calendar cal = Cons.convertStringToCal(selectedGridDate, "yyyy-MM-dd");
				Date calDate = Cons.convertStringToDate(selectedGridDate, "yyyy-MM-dd");
				Date currentDate = Cons.convertStringToDate(Cons.convertDateToString(new Date(), "yyyy-MM-dd"),"yyyy-MM-dd");
				if(calDate.before(currentDate))
				{
					Toast.makeText(AvailableAppointments.this, "Selected date is before current date", Toast.LENGTH_LONG).show();
				}
				else
				{
					if (((LinearLayout) rLayout).getChildCount() > 0) {
						((LinearLayout) rLayout).removeAllViews();
					}
					((CalendarAdapter) parent.getAdapter()).setSelected(v);
					int dayId = cal.get(Calendar.DAY_OF_WEEK)-1;
					System.out.println("selectedGridDate::"+selectedGridDate);
					String[] separatedTime = selectedGridDate.split("-");
					String gridvalueString = separatedTime[2].replaceFirst("^0*",
							"");// taking last part of date. ie; 2 from 2012-12-02.
					int gridvalue = Integer.parseInt(gridvalueString);
					// navigate to next or previous month on clicking offdays.
					if ((gridvalue > 10) && (position < 8)) {
						setPreviousMonth();
						refreshCalendar();
					} else if ((gridvalue < 7) && (position > 28)) {
						setNextMonth();
						refreshCalendar();
					}
					((CalendarAdapter) parent.getAdapter()).setSelected(v);


					if(Cons.isNetworkAvailable(AvailableAppointments.this))
					{
						new GetAvailableAppointments(AvailableAppointments.this, selectedGridDate,dayId).execute();
					}
					else
						Cons.showDialog(AvailableAppointments.this, "Carrxon", "Internet connection is not available.", "OK");

				}
				//				for (int i = 0; i < Utility.startDates.size(); i++) {
				//					if (Utility.startDates.get(i).equals(selectedGridDate)) {
				//						desc.add(Utility.nameOfEvent.get(i));
				//					}
				//				}
				//
				//				if (desc.size() > 0) {
				//					for (int i = 0; i < desc.size(); i++) {
				//						TextView rowTextView = new TextView(CalendarView.this);
				//
				//						// set some properties of rowTextView or something
				//						rowTextView.setText("Event:" + desc.get(i));
				//						rowTextView.setTextColor(Color.BLACK);
				//
				//						// add the textview to the linearlayout
				//						rLayout.addView(rowTextView);
				//
				//					}
				//
				//				}
				//
				//				desc = null;

			}

		});
	}

	protected void setNextMonth() {
		if (month.get(GregorianCalendar.MONTH) == month
				.getActualMaximum(GregorianCalendar.MONTH)) {
			month.set((month.get(GregorianCalendar.YEAR) + 1),
					month.getActualMinimum(GregorianCalendar.MONTH), 1);
		} else {
			month.set(GregorianCalendar.MONTH,
					month.get(GregorianCalendar.MONTH) + 1);
		}
		setPreviousButtonVisibility();



	}

	protected void setPreviousMonth() {
		if (month.get(GregorianCalendar.MONTH) == month
				.getActualMinimum(GregorianCalendar.MONTH)) {
			month.set((month.get(GregorianCalendar.YEAR) - 1),
					month.getActualMaximum(GregorianCalendar.MONTH), 1);
		} else {
			month.set(GregorianCalendar.MONTH,
					month.get(GregorianCalendar.MONTH) - 1);
		}
		setPreviousButtonVisibility();

	}

	protected void showToast(String string) {
		Toast.makeText(this, string, Toast.LENGTH_SHORT).show();

	}

	public void refreshCalendar() {
		TextView title = (TextView) findViewById(R.id.title);

		adapter.refreshDays();
		adapter.notifyDataSetChanged();
		handler.post(calendarUpdater); // generate some calendar items

		title.setText(android.text.format.DateFormat.format("MMMM yyyy", month));
	}

	public Runnable calendarUpdater = new Runnable() {

		@Override
		public void run() {
			items.clear();

			// Print dates of the current week
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
			String itemvalue;
			//			event = Utility.readCalendarEvent(CalendarView.this);
			//			Log.d("=====Event====", event.toString());
			Log.d("=====Date ARRAY====", Utility.startDates.toString());

			for (int i = 0; i < Utility.startDates.size(); i++) {
				itemvalue = df.format(itemmonth.getTime());
				itemmonth.add(GregorianCalendar.DATE, 1);
				items.add(Utility.startDates.get(i).toString());
			}
			adapter.setItems(items);
			adapter.notifyDataSetChanged();
		}
	};

	private void setAppointments()
	{
		if (((LinearLayout) rLayout).getChildCount() > 0) {
			((LinearLayout) rLayout).removeAllViews();
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		int dayId = calendar.get(Calendar.DAY_OF_WEEK)-1;
		String selectedGridDate = Cons.convertDateToString(new Date(), "yyyy-MM-dd");
		if(Cons.isNetworkAvailable(AvailableAppointments.this))
		{
			new GetAvailableAppointments(AvailableAppointments.this, selectedGridDate,dayId).execute();
		}
		else
			Cons.showDialog(AvailableAppointments.this, "Carrxon", "Internet connection is not available.", "OK");


	}


	public class GetAvailableAppointments extends AsyncTask<Void, Void, Void>
	{
		ProgressDialog pd;
		Context con;

		int dayId;


		public GetAvailableAppointments(Context con,String date,int dayId)
		{
			this.con = con;
			AvailableAppointments.this.appointmentdate = date;
			this.dayId = dayId;

			AvailableAppointments.this.parent = parent;
			AvailableAppointments.this.v = v;
			AvailableAppointments.this.position = position;
		}

		@Override
		protected void onPreExecute() 
		{
			pd = ProgressDialog.show(con, null, "Loading...");
			super.onPreExecute();
		}
		@Override
		protected Void doInBackground(Void... params)
		{
			String url = "";

			AppPreferences appPref = new AppPreferences(AvailableAppointments.this);
			url = Cons.url_available_appointments + "doctor_id="+appPref.getDoctorIdByPatient()
					+"&day_id="+dayId
					+"&date="+appointmentdate;
			//			url = Cons.url_available_appointments + "doctor_id=30"
			//					+"&day_id=2"
			//					+"&date=2014-03-11";
			System.out.println("url::"+url);
			String responseString = Cons.http_connection(url);
			if(responseString != null)
			{
				System.out.println("responseString::"+responseString);
				Gson gson = new Gson();
				availAppointments = gson.fromJson(responseString, BeanAvailableAppointment.class);
			}
			//			url = Cons.url_doctorAddress+"query="+Uri.encode(tv_speciality.getText().toString()+" in "+et_doctorState.getText().toString());
			//			System.out.println("url:"+url);
			//
			//			responseString = Cons.http_connection(url);
			//			if(responseString !=null)
			//			System.out.println(responseString);
			//			Gson gson = new Gson();
			//			addressList = gson.fromJson(responseString, BeansDoctorGoogleAddressList.class);
			//		System.out.println(addressList);

			return null;
		}

		@Override
		protected void onPostExecute(Void result) 
		{
			if(pd.isShowing())
			{
				pd.dismiss();
			}
			Message myMessage = new Message(); 
			myMessage.obj = "available_appointments";
			myHandler.sendMessage(myMessage);
			super.onPostExecute(result);

		}

	}

	private Handler myHandler = new Handler() 
	{

		public void handleMessage(Message msg)
		{


			if (msg.obj.toString().equalsIgnoreCase("available_appointments"))
			{
				if (!isFinishing()) 
				{

					if((availAppointments == null)||Cons.isNetAvail==1)

					{

						Cons.isNetAvail = 0;
						Toast.makeText(AvailableAppointments.this, "Connection is slow or some error in apis.", Toast.LENGTH_LONG).show();
					}

					else if(availAppointments.getCode() == 200)
					{
						final List<String> listAvailableAppointments = availAppointments.getAvailable_appointments_list();
						TextView tvAvailableAppointmentsHead = (TextView) findViewById(R.id.availableAppointmentsHead);

						tvAvailableAppointmentsHead.setText("Available Appointments For"+"\n"+appointmentdate);
						int count = 0;
						if(listAvailableAppointments != null && listAvailableAppointments.size()>0)
						{
							for(int i=0;i<listAvailableAppointments.size();i++)
							{
								String availAppointment = listAvailableAppointments.get(i);
								String completeDate = appointmentdate + " " + availAppointment;

								Button rowTextView = new Button(AvailableAppointments.this);
								LayoutParams lp = new    LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
								lp.bottomMargin = 20;

								rowTextView.setBackgroundResource(R.drawable.button_one);
								rowTextView.setTag(i);

								rowTextView.setText(Cons.changeDateFormat(availAppointment, "hh:mm:ss", "hh:mm a"));
								rowTextView.setTextColor(Color.BLACK);
								rowTextView.setPadding(10, 5, 0, 5);
								rowTextView.setTextSize(20);
								rowTextView.setLayoutParams(lp);
								rowTextView.setTextColor(Color.WHITE);
								long cuurent = new Date().getTime();
								long timeInmills = Cons.convertStringToDate(completeDate, "yyyy-MM-dd HH:mm:ss").getTime();
								if(timeInmills>=(new Date()).getTime())
								{
								rLayout.addView(rowTextView);
								count++;
								
								}
								
								
								rowTextView.setOnClickListener(new OnClickListener()
								{

									@Override
									public void onClick(View arg0) 
									{
										AppPreferences appPref = new AppPreferences(AvailableAppointments.this);
										appPref.saveAppointmentDate(appointmentdate);
										int id = Integer.parseInt(((TextView)arg0).getTag().toString());
										String appointmenttime = listAvailableAppointments.get(id);
										appPref.saveAppointmentTime(appointmenttime);
										Intent i = new Intent(AvailableAppointments.this,CheckInOptions.class);
										startActivity(i);

									}
								});



							}
							if(count == 0)
							{
								TextView rowTextView = new TextView(AvailableAppointments.this);
								LayoutParams lp = new    LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
								lp.bottomMargin = 20;
								rowTextView.setText("No appointment available for Today");
								rowTextView.setTextColor(Color.BLACK);
								rowTextView.setGravity(Gravity.CENTER);
								rowTextView.setPadding(10, 5, 0, 5);
								rowTextView.setTextSize(14);
								rowTextView.setLayoutParams(lp);
								rLayout.addView(rowTextView);
							}
						}
					}

					//					else if(BookedAppointments.getCode()==200)
					//					{
					//						List<BeansHospitalSchduleDetails> hospitalBeans = specificHospitalDetailBean.getDoctor_details().getDoctor_schedule_details();
					//						if(bitmap !=null)
					//						{
					//						image_hospital.setImageBitmap(bitmap);
					//						}
					//						else
					//							image_hospital.setBackgroundResource(R.drawable.place_holder);
					//						
					//						
					//						tv_address.setText(specificHospitalDetailBean.getDoctor_details().getAddress());
					//						tv_phone.setText(specificHospitalDetailBean.getDoctor_details().getDoctor_phone());
					//						
					//						
					//						String day_schedule = "";
					//						for(int i=0 ;i<hospitalBeans.size();i++)
					//						{
					//							day_schedule = day_schedule+hospitalBeans.get(i).getDay()+": "+ hospitalBeans.get(i).getDay_starting_time()+"-"+hospitalBeans.get(i).getDay_ending_time();
					//							if(i%2==0)
					//							{
					//								day_schedule = day_schedule+"\n";
					//							}
					//							else
					//								day_schedule = day_schedule+"\n";
					//						}
					//						
					//						tv_clinic_hours.setText(day_schedule);
					//						
					//						
					//					
					//						
					//					
					//
					//					}
					//					else 
					//					{
					//						
					//						Toast.makeText(DisplayDoctorInfo.this, "Hospital is not registered", Toast.LENGTH_LONG).show();
					//						finish();
					//					}



				}
			}


		}
	};

	private void setPreviousButtonVisibility()
	{
		int monthCal = month.get(GregorianCalendar.MONTH);
		int yearCal = month.get(GregorianCalendar.YEAR);
		int yearCurrent = new Date().getYear()+1900;
		int monthCurrent = new Date().getMonth();
		RelativeLayout previous = (RelativeLayout) findViewById(R.id.previous);
		if((yearCal==yearCurrent && monthCal>monthCurrent) || yearCal>yearCurrent)
		{
			previous.setVisibility(View.VISIBLE);
		}
		else
		{
			previous.setVisibility(View.GONE);
		}
	}

}
