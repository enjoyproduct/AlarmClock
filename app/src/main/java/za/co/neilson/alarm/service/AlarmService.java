/* Copyright 2014 Sheldon Neilson www.neilson.co.za
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package za.co.neilson.alarm.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import za.co.neilson.alarm.Alarm;
import za.co.neilson.alarm.alert.AlarmAlertBroadcastReciever;
import za.co.neilson.alarm.database.Database;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AlarmService extends Service {

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		Log.d(this.getClass().getSimpleName(),"onCreate()");
		super.onCreate();		
	}

	private Alarm getNext(){
		Set<Alarm> alarmQueue = new TreeSet<Alarm>(new Comparator<Alarm>() {
			@Override
			public int compare(Alarm lhs, Alarm rhs) {
				int result = 0;
				long diff = lhs.getAlarmTime().getTimeInMillis() - rhs.getAlarmTime().getTimeInMillis();				
				if(diff>0){
					return 1;
				}else if (diff < 0){
					return -1;
				}
				return result;
			}
		});
				
		Database.init(getApplicationContext());
		List<Alarm> alarms = Database.getAll();
		
		for(Alarm alarm : alarms){
			if(alarm.getAlarmActive())
				alarmQueue.add(alarm);
		}
		if(alarmQueue.iterator().hasNext()){
			return alarmQueue.iterator().next();
		}else{
			return null;
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		Database.deactivate();
		super.onDestroy();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(this.getClass().getSimpleName(),"onStartCommand()");
		Alarm alarm = getNext();
		if(null != alarm){
			alarm.schedule(getApplicationContext());
			Log.d(this.getClass().getSimpleName(),alarm.getTimeUntilNextAlarmMessage());
			
		}else{
			Intent myIntent = new Intent(getApplicationContext(), AlarmAlertBroadcastReciever.class);
			myIntent.putExtra("alarm", new Alarm());
			
			PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, myIntent,PendingIntent.FLAG_CANCEL_CURRENT);			
			AlarmManager alarmManager = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
			
			alarmManager.cancel(pendingIntent);
		}
		return START_NOT_STICKY;
	}

}
