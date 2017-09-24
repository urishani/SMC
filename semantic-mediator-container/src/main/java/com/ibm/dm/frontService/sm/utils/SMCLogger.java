
/*
  Copyright 2011-2016 IBM
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	http://www.apache.org/licenses/LICENSE-2.0
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

*/
/*
 *+------------------------------------------------------------------------+
 
 *| Copyright IBM Corp. 2011, 2013.
 *|                                                                        |
 *+------------------------------------------------------------------------+
 */

package com.ibm.dm.frontService.sm.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.Database;

public class SMCLogger {

	private static Database db = null;

	//private final static String REL_FILE_NAME = "SM.logs/SMCLogs.html";
	public final static File getFileName(Database db, String...pProject) {
		//String File_Name =  + "/logs/" + REL_FILE_NAME;
		File f = new File(new File(new File(db.getFolder(), "logs"), "SM.logs"),"SMCLogs.html");
		f.getParentFile().mkdirs();
		return f;
	}
	
	protected static enum STATE {REGISTERED, STARTED, ENDED, CANCELLED}

	private static final Logger l = Logger.getLogger(SMCLogger.class);
	private static final SimpleDateFormat f = new SimpleDateFormat();
	private static final List<SMCJobLogger> jobs = new ArrayList<SMCJobLogger>();
	protected String job = null, task = null, phase = null;
	protected long duration = -1;
	protected final String id;
	public static void init(Database aDb) {
		System.err.println("Calling SMCLogger.init()");
		if (db == null) synchronized (SMCLogger.class) {
			doInit(aDb);
		}
	}
	
	private static void doInit(Database aDb) {
		System.err.println("CAlling SMCLogger.doInit()");
		db = aDb;
		f.applyPattern("HH:mm:ss.SSS");
		SMCRollingFileAppender appender = new SMCRollingFileAppender();
		appender.setAppend(true);
		//l.removeAllAppenders();
		l.addAppender(appender);
		SMCLayout layout = new SMCLayout(Database.getName());
		appender.setLayout(layout);
		String propsStr = Utils.loadFromClassPath("log4j.properties");
		propsStr += Utils.loadFromClassPath("log4j.properties_");
		propsStr = Utils.replaceAll(propsStr, "_smcLogFile_", SMCLogger.getFileName(db).getAbsolutePath()); //Database.SM_MODEL_FOLDER);
        InputStream in = new ByteArrayInputStream(propsStr.getBytes()); //cl.getResourceAsStream("log4j.properties");
		Properties log4jProps = new Properties();
		try {
			log4jProps.load(in);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String file = SMCLogger.getFileName(db).getAbsolutePath(); // log4jProps.getProperty("log4j.appender.SMC_HTML.File"); // To avoid the backslash probkem in the file path when processed in a properties file.
		String size = log4jProps.getProperty("log4j.appender.SMC_HTML.MaxFileSize"); //=1MB
		String maxbui = log4jProps.getProperty("log4j.appender.SMC_HTML.MaxBackupIndex"); //=99
		String locinfo = log4jProps.getProperty("log4j.appender.SMC_HTML.layout.LocationInfo"); //=false
		appender.setFile(file);
		appender.setMaxFileSize(size);
		appender.setMaxBackupIndex(Integer.parseInt(maxbui));
		layout.setLocationInfo(Boolean.parseBoolean(locinfo));
		appender.activateOptions();
		l.setLevel(Level.ALL);
//		main(new String[0]);
//		appender.writeHeader();
	}

	protected SMCLogger(String id) {
		this.id = id;
		myState = STATE.REGISTERED;
	}
	
	/**
	 * Gets the present logging level of SMC events.
	 * @return present Level
	 */
	public Level getLevel() {
		return l.getLevel();
	}
	
	protected STATE myState;
	
	public STATE getState() {
		return myState;
	}
	/**
	 * Sets the new logging level, returning the old value.
	 * @param newLevel new Level to be set.
	 * @return old Level.
	 */
	public Level setLevel(Level newLevel) {
		Level level = getLevel();
		l.setLevel(newLevel);
		return level;
	}
	private long startTime = 0;
	private void Start(String phase) {
		startTime = System.currentTimeMillis();
		l.trace(new MyMessage("Start", this, null));
		myState = STATE.STARTED;
	}

	private void Ended(String phase, String info) {
		myState = STATE.ENDED;
		if (0 == startTime) {
			l.error(new MyMessage("Ended", this, "Ended w/out starting " + identify() + " info: " + info));
			return;
		}
		duration = System.currentTimeMillis() - startTime;
		l.trace(new MyMessage("Ended", this, info));
	}

	public void Error(String info) {
		l.error(new MyMessage("Error", this, info));
	}
	protected String identify() {
		return "";
	}

	public String getDuration() {
		if (duration >= 0)
			return String.format("%02d:%02d:%02d.%03d", duration/3600000L, (duration%3600000L)/60000L, (duration%60000L/1000L), duration%1000L);
		else
			return "";
	}
	
	public String getPhase() {
		return phase;
	}

	public String getTask() {
		return task;
	}

	public String getJob() {
		return job;
	}

	public String getId() {
		return id;
	}

	public boolean isActive() {
		return myState == STATE.STARTED || myState == STATE.REGISTERED;
	}
	
	public boolean ended() {
		return myState == STATE.ENDED;
	}
	
	/**
	 * Answers with a new job logger for a job name
	 * @param theJob String name of the job
	 * @return SMCJobLogger to log events of a certain job.
	 */
	public static SMCJobLogger createJobLogger(String theJob) {
		SMCLogger logger = getInstance();
		return logger.newJobLogger(theJob);
	}

	private SMCJobLogger newJobLogger(String theJob) {
		SMCJobLogger logger = new SMCJobLogger(theJob);
		jobs.add(logger);
		return logger;
	}

	static SMCLogger logger = null;
	private static SMCLogger getInstance() {
		if (null == logger) synchronized (SMCLogger.class) {
			if (null == logger)
				logger = new SMCLogger("root");
		}
		return logger;
	}

	private static String pref(String id) {
		if (id.indexOf("@") >= 0)
			return id.split("@")[0];
		return id;
	}
	

	public static class SMCJobLogger extends SMCLogger {
		private final List<SMCTaskLogger> tasks = new ArrayList<SMCTaskLogger>();
		protected SMCJobLogger(String job) {
			super(pref(job));
			this.job = job;
		}

		
		/**
		 * Goes over the registered tasks to decide if job has ended, and signal it as such.
		 */
		public void checkTasks() {
			for (SMCTaskLogger tlog: tasks) {
				if (tlog.isActive())
					return;
			}
			ended("Done with all tasks");
		}


		protected String identify() {
			String iden = super.identify();
			if (false == Strings.isNullOrEmpty(iden))
				iden = " for " + iden;
//			if (iden.length() > 0 && false == Character.isWhitespace(iden.charAt(iden.length()-1)))
//				iden += " ";
			return "job [" + job + "]" + iden;
		}
		/**
		 * Starts the job
		 */
		public void start() {
			super.Start("job processing");
		}
		/**
		 * Ends the job with conclusion information
		 * @param info
		 */
		public void ended(String info) {
			super.Ended("job processing", info);
		}

		/**
		 * Answers with a task logger for this job, for the names task.
		 * @param task String name to identify this task in that job.
		 * @return
		 */
		public SMCTaskLogger newTask(String task, String...info) {
			SMCTaskLogger logger = new SMCTaskLogger(task, this, info);
			tasks.add(logger);
			logger.registered();
			return logger;
		}


		public void failed(String string) {
			Error(string);
		}

	}


	public static class SMCTaskLogger extends SMCLogger {
		private final SMCJobLogger myJobLogger;
		private final List<SMCPhaseLogger> phases = new ArrayList<SMCPhaseLogger>();
		protected SMCTaskLogger(String taskId, SMCJobLogger myJobLogger, String...info) {
			super (pref(taskId));	
			super.addInfo(info);
			this.task = taskId;
			this.myJobLogger = myJobLogger;
		}
		/**
		 * Marks the event that the logger was created to signal that a task
		 * is scheduled eventually to run.
		 */
		public void registered() {
			l.trace(new MyMessage("Registered", this, this.info));
		}
		/**
		 * Marks an event that a planned task could not be done as the job
		 * ended prematurely.
		 * @param why optional String to explain reason for canceling the Task.
		 */
		public void cancelled(String...why) {
			addInfo(why);
			l.trace(new MyMessage("Cancelled", this, this.info));
			myState = STATE.CANCELLED;
			myJobLogger.checkTasks();
		}
		
		/**
		 * Answers with a new phase logger
		 * @param phase phase name of that logger
		 * @return Logger for a phase in a task in a job.
		 */
		public SMCPhaseLogger newPhase(String phase) {
			SMCPhaseLogger logger = new SMCPhaseLogger(phase, this);
			phases.add(logger);
			return logger;
		}
		/**
		 * Marks the start of the task
		 */
		public void start() {
			super.Start("task processing");
		}
		/**
		 * Marks the end of the task
		 * @param info String of concluding information for the task execution.
		 */
		public void ended(String info) {
			super.Ended("task processing", info);
			myJobLogger.checkTasks();
		}

		protected String identify() {
			return "task [" + task + "] for " + myJobLogger.identify();
		}
		@Override
		public String getJob() {
			return myJobLogger.getJob();
		}
	}


	public class SMCPhaseLogger extends SMCLogger {
		private final SMCTaskLogger myTaskLogger;
		protected SMCPhaseLogger(String phase, SMCTaskLogger myTaskLogger) {
			super(phase);
			this.phase = phase;
			this.myTaskLogger = myTaskLogger;
		}

		
		@Override
		public String getJob() {
			return myTaskLogger.getJob();
		}


		@Override
		public String getTask() {
			return myTaskLogger.getTask();
		}


		protected String identify() {
			return "phase [" + phase + "] for " + myTaskLogger.identify();
		}
		/**
		 * Marks the start of the phase in a task of a job
		 */
		public void start() {
			super.Start(phase);
		}
		/**
		 * Marks the end of a phase in a task of a job
		 * @param info String of concluding information for the task execution.
		 */
		public void ended(String info) {
			super.Ended(phase, info);
		}

	}

	public static class MyMessage {
		private final String info;
		private final SMCLogger logger;
		private final String event;
		public MyMessage(String event, SMCLogger logger, String info) {
			this.info = info;
			this.logger = logger;
			this.event = event;
		}
		String getJob()  {
			return logger.getJob();
		}
		String getTask() {
			return logger.getTask();
		}
		String getPhase() {
			return logger.getPhase();
		}
		String getInfo() {
			return info;
		}
		String getEvent() {
			return event;
		}
		public String toString() {
			String duration = logger.getDuration();
//			if (false == Strings.isNullOrEmpty(duration))
				duration = "[" + duration + " ]";
			return logger.identify() + duration + ((null != info)? (" info: " + info):"");
		}
		
		public String getId() {
			return logger.getId();
		}
		public String getDuration() {
			return logger.getDuration();
		}
	}

	// ------------------ Testing ------------------
	public static void main(String args[]) {
//		try {
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		}
		String jobs[] = {"J1", "J2", "J3"}; //db.createJobId(), db.createJobId(), db.createJobId() };
		String tasks[] = {"T1", "T2", "T3"}; //db.createId("Tsk"), db.createId("Tsk"), db.createId("Tsk")};
		String phases[] = {"phase1", "phase2" };

		System.out.println("------------------------- A ---------------------");
		for (int j= 0; j < jobs.length; j++) {
			SMCJobLogger lj = SMCLogger.createJobLogger(jobs[j]);
			lj.start(); pause(100);
			for (int t=0; t < tasks.length; t++) {
				SMCJobLogger.SMCTaskLogger lt = lj.newTask(tasks[t]);
				lt.start(); pause(100);
				for (int p= 0; p < phases.length; p++) {
					SMCTaskLogger.SMCPhaseLogger lp = lt.newPhase(phases[p]);
					lp.start();
					//    				System.out.print(".");
					pause(200);
					lp.Error("Just teezing!!");
					lp.ended("Mediator X, Blocks 80, Model rhap");  
					pause(50);
				}
				lt.ended(null);
			}
			lj.ended(null);
		}
		System.out.println("------------------------- B ---------------------");
		doJobs(jobs, tasks, phases);
		System.out.println("------------------------- C ---------------------");
	}
	
	protected String info;
	public void addInfo(String[] info) {
		String v = Utils.concat(info, "\n ");
		if (null != this.info)
			this.info += v;
		else
			this.info = v;
	}

	private static boolean useThreads = true;
	private static void doJobs(String[] jobs, final String[] tasks, final String[] phases) {
		final Thread me = Thread.currentThread();
		Set<Thread> threads = new HashSet<Thread>();
		for (int j= 0; j < jobs.length; j++) {
			final SMCJobLogger lj = SMCLogger.createJobLogger(jobs[j]);
			if (useThreads) {
				Thread th = new Thread() {
					@Override
					public void run() {
						lj.start(); pause(100);
						doTasks(tasks, phases, lj);
						lj.ended(null);
						synchronized (me) {me.notify();}
					}
				};
				threads.add(th);
				th.start();
			} else {
				lj.start(); pause(100);
				doTasks(tasks, phases, lj);
				lj.ended(null);
			}
		}		
		if (useThreads) {
			join(threads, me);
		}
	}

	private static void doTasks(String[] tasks, final String[] phases, SMCJobLogger lj) {
		final Thread me = Thread.currentThread();
		Set<Thread> threads = new HashSet<Thread>();
		for (int t=0; t < tasks.length; t++) {
			final SMCJobLogger.SMCTaskLogger lt = lj.newTask(tasks[t]);
			if (useThreads) {
				Thread th = new Thread() {
					@Override
					public void run() {
						lt.start(); pause(100);
						doPhases(phases, lt);
						lt.ended(null);
						synchronized (me) {me.notify();}
					}
				};
				threads.add(th);
				th.start();
			} else {
				lt.start(); pause(100);
				doPhases(phases, lt);
				lt.ended(null);
			}
		}
		if (useThreads) {
			join(threads, me);
		}
		
	}

	private static void doPhases(String[] phases, SMCTaskLogger lt) {
		final Thread me = Thread.currentThread();
		Set<Thread> threads = new HashSet<Thread>();
		for (int p= 0; p < phases.length; p++) {
			final SMCTaskLogger.SMCPhaseLogger lp = lt.newPhase(phases[p]);
			if (useThreads) {
				Thread th = new Thread() {
					@Override
					public void run() {
						lp.start();
						pause(200);
						lp.ended("Mediator X, Blocks 80, Model rhap");  
						pause(50);
						synchronized (me) {me.notify();}
					}
				};
				threads.add(th);
				th.start();
			} else {
				lp.start();
				pause(200);
				lp.ended("Mediator X, Blocks 80, Model rhap");  
				pause(50);
			}
		}
		if (useThreads) {
			join(threads, me);
		}
	}

	private static void join(Set<Thread> threads, Thread me) {
		while(true) {
			int cnt = 0;
			for (Thread thread : threads) {
				if (thread.isAlive()) break;
				cnt++;
			}
			if (cnt == threads.size()) break;
			synchronized (me) {
				try {
//					long t = System.currentTimeMillis();
					me.wait(100);
//					System.out.println(me + " notified [" + (System.currentTimeMillis() - t) + "]");
				} catch (InterruptedException e) {
//					System.out.println(me + " notified [" + e.getClass().getName() + " - " + e.getCause() + "]");
					e.printStackTrace();
				}
			}
		}
	}

	private static void pause(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
//		Object timer = new Object();
//		synchronized (timer) {
//			try {
//				timer.wait(ms);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//	}

}
