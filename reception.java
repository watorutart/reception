package reception;

import reception.Save.save;
import telecommunication.Receiver;
import telecommunication.Telecommunication;
import telecommunication.code.Reception_Collector;
import telecommunication.code.Reception_HQ;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fragile.*;
import fragile.deliRecord.DeliStats;
import fragile.deliRecord.ObsStats;

public class Reception {

	private Queue<Fragile> fragile = new LinkedList<Fragile>();

	private ReceptionMonitor recpMonitor = new ReceptionMonitor();

	private Fragile deliWaitFrgl;

	private Telecommunication boundary = new Telecommunication();

	private Boolean judgeClient(ClientInfo info) {
		if(info.getClientName() == null || info.getClientTel() == null || info.getHouseName() == null || info.getHouseTel() == null)
			return false;
		else if (!judgeAddress(info.getClientAddr()))
			return false;
		else if (!judgeAddress(info.getHouseAddr()))
			return false;
		else
			return true;
	}

	// 文字列が数値かどうか判定するメソッド
	public boolean isNumber(String num) {
		try {
			Integer.parseInt(num);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	// 住所が正しいか判定するメソッド
	public boolean judgeAddress(String str) {
		if (str.length() == 3) {
			if (isNumber(String.valueOf(str.charAt(0)))
					&& isNumber(String.valueOf(str.charAt(2)))
					&& str.charAt(1) == '-') {

				// i1,i2 に住所の番号を格納する
				int i1 = Integer.parseInt(String.valueOf(str.charAt(0)));
				int i2 = Integer.parseInt(String.valueOf(str.charAt(2)));

				// 住所の番号の値が範囲内（0 から 3）か判定する
				if (0 <= i1 && i1 <= 3 && 0 <= i2 && i2 <= 3) {
					return true;
				}
			}
		}

		return false;
	}

	private long calcFrglNum(Calendar cal) {
		long frglNum;
		frglNum = cal.get(Calendar.SECOND);
		frglNum += 100 * cal.get(Calendar.MINUTE);
		frglNum += 10000 * cal.get(Calendar.HOUR_OF_DAY);
		frglNum += 1000000 * cal.get(Calendar.DATE);
		frglNum += 100000000 * cal.get(Calendar.MONTH) + 1;
		frglNum += 10000000000L * (Long.valueOf(cal.get(Calendar.YEAR)));
		return frglNum;
	}

	private void emptyFragile() {
		this.deliWaitFrgl = null;
	}

	private Fragile pickFragile() {
		Fragile frgl = this.deliWaitFrgl;
		this.deliWaitFrgl = null;
		return frgl;
	}

	/*必要ないメソッド
	private void moveFragile(Fragile frgl) {
		this.deliWaitFrgl = frgl;
	}
	*/

	private Fragile dequeueFragile() {
		return this.fragile.peek();
	}

	private void queueFragile(Fragile frgl) {
		this.fragile.add(frgl);
	}

	private void empFirstFrgl() {
		this.fragile.remove();
	}

	private void save(String content, save order, Fragile frgl) {
		String[] str = content.split("\\|", 0);
		if (Save.save.valueOf(str[0]) == Save.save.deliCompFrglNum) {
			// frgl.frglNum = Long.parseLong(str[1]);
			frgl.setDeliStats(DeliStats.delivered);
		}
	}

	// 情報を加工するメソッド
	private String adjustInfo(String order) {
		String data = null;

		switch (order) {

		// 荷物番号を送るための情報を文字列にまとめる処理
		case "setDeliCompFrglNum":
			// Fragile frgl = this.dequeueFragile();
			long num = (this.fragile.peek()).getFrglNum();
			String frglNum = String.valueOf(num);
			data = order + "\\|" + frglNum;
			break;

		// 中継所引き渡し完了についての情報を文字列にまとめる処理
		case "setFailedPassing":
			data = "setFailedPassing";
			break;

		case "makeFragile":
			String[] clientInfo = this.deliWaitFrgl.getClientInfo();
			String[] houseInfo = this.deliWaitFrgl.getHouseInfo();
			try {
				data = order
						+ "\\|"
						+ String.valueOf(this.deliWaitFrgl.getFrglNum())
						+ "\\|"
						+ clientInfo[0]
						+ "\\|"
						+ clientInfo[1]
						+ "\\|"
						+ clientInfo[2]
						+ "\\|"
						+ houseInfo[0]
						+ "\\|"
						+ houseInfo[1]
						+ "\\|"
						+ houseInfo[2]
						+ "\\|"
						+ (this.deliWaitFrgl.getStrTime("receptionTime"))
						+ "\\|"
						+ (this.deliWaitFrgl.getStrTime("sendTime"));
			} catch (Exception e) {
				System.out.println(e);
			}
			break;
		}

		return data;
	}

	// 荷物の依頼を行うメソッド
	private void requestFrgl() {

		ClientInfo clientInfo = this.recpMonitor.demandClientInfo();

		if (clientInfo != null && !this.judgeClient(clientInfo)) {
			this.recpMonitor.displayErr("依頼情報が間違っています");
		} else {
			Calendar recpTime = Calendar.getInstance();
			long frglNum = this.calcFrglNum(recpTime);
			Fragile frgl = new Fragile(recpTime, clientInfo, frglNum,DeliStats.awaiting, ObsStats.none);
			this.queueFragile(frgl);
			this.recpMonitor.displayAccepted();
			this.recpMonitor.displayFrglNum(frgl.getFrglNum());
		}
	}

	// 荷物を渡すメソッド
	private String sendFragile() throws IllegalArgumentException, IOException, RuntimeException {
		String frglData = this.adjustInfo(Reception_Collector.setDeliCompFrglNum.toString());
		Fragile sendFrgl = this.dequeueFragile();
		if (this.boundary.sendSignal(frglData, Receiver.collector,Receiver.reception, 1)) {
			
			//現在時刻を取得し、String型へ変換
			Calendar currentTime = Calendar.getInstance();
			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHHmm");
			String sendTime = sdf1.format(currentTime.getTime());//#=> 201106150449
			
			sendFrgl.setDeliStats(DeliStats.delivering);
			sendFrgl.setObsStats(ObsStats.none);
			sendFrgl.saveTime("sendTime", sendTime);
			this.deliWaitFrgl = sendFrgl;
			this.empFirstFrgl();
			String sendContent = this.adjustInfo(Reception_HQ.makeFragile.toString());
			return sendContent;
		}
		return null;
	}

	// 中継所引き渡し結果を受け取るメソッド
	private String receivePassingResult() throws IOException, RuntimeException {
		System.out.println("start");
		String recpContent = this.boundary.receiveSignal(Receiver.collector,Receiver.reception, 120);
		System.out.println(recpContent);
		this.save(recpContent, save.deliCompFrglNum, this.deliWaitFrgl);
		String sendContent = null;
		if (this.deliWaitFrgl.getObsStats() == ObsStats.failedPassing) {
			// 中継所引き渡し失敗の場合
			Fragile frgl = this.pickFragile();
			frgl.setDeliStats(DeliStats.awaiting);
			this.fragile.add(frgl);
			sendContent = this.adjustInfo(Reception_HQ.setFailedPassing.toString());
			if (this.boundary.sendSignal(sendContent, Receiver.hq, Receiver.reception, 1)) {
				// sendContentToHQ.add(sendContent);
				return null;
			}
		} else {
			// 中継所引き渡し完了の場合
			sendContent = this.adjustInfo(Reception_HQ.setFailedPassing.toString());
			this.emptyFragile();
			if (this.boundary.sendSignal(sendContent, Receiver.hq, Receiver.reception, 1)) {
				// sendContentToHQ.add(sendContent);
				return null;
			}
		}
		return sendContent;
	}

	/*
	//中継所引き渡し完了を受け取るのテストプログラム　
	public static void main(String args[]) throws IOException, RuntimeException{
		Reception rcp = new Reception();
		Queue<String> sendContentToHQ = new LinkedList<String>(); // 本部へ送れなかったデータを格納するメソッド
		String sendContent = null;
		//Calendar receptionTime, ClientInfo clientInfo, long frglNum, DeliStats deliStats, ObsStats obsStats
		Calendar testTime = Calendar.getInstance();
		
		ClientInfo clInfo = rcp.recpMonitor.demandClientInfo();
		
		
		rcp.deliWaitFrgl = new Fragile(testTime, clInfo, rcp.calcFrglNum(testTime), DeliStats.delivering, ObsStats.none);
		
   		// 中継所引き渡し結果を受け取る
		if (rcp.deliWaitFrgl != null) {
			sendContent = rcp.receivePassingResult();
			if (sendContent != null) {
				sendContentToHQ.add(sendContent);
			}
		}
	}
	*/
	

	// コントロール
	public static void main(String args[]) throws IllegalArgumentException,
			IOException, RuntimeException, InterruptedException, ExecutionException {
		Reception rcp = new Reception();
		Queue<String> sendContentToHQ = new LinkedList<String>(); // 本部へ送れなかったデータを格納するデータ
		String sendContent = null;	//送信内容を格納するデータ

        ExecutorService executor = Executors.newSingleThreadExecutor();


        // 10秒でタイムアウトさせる
        long timeout = 10;
        String result = null;

        while(true){
        	Future<String> future = executor.submit(new Worker());
	    
        	try {
        		//System.out.println("try!");
        		System.out.println("依頼情報を入力するにはEnterを押してください");
        		//Enterが押されず10秒経ったらcatch内へ
        		result = future.get(timeout, TimeUnit.SECONDS);
			
        		System.out.println("accepted!");
	    	
        		// 荷物の依頼を行う
        		if(!result.equals("end"))
        			rcp.requestFrgl();
			
        	} catch (TimeoutException e) {
        		//System.out.println("catch!!");
        		System.out.println("タイムアウトしました。");
        	} finally {	
        		//以下はタイムアウトしてもしなくても動作する処理
        		System.out.println("処理中");
	    	
        		// 荷物を渡す
        		if (!rcp.fragile.isEmpty()) {
        			sendContent = rcp.sendFragile();
        			if (sendContent != null) {
        				if (!rcp.boundary.sendSignal(sendContent, Receiver.hq,Receiver.reception, 1)) {
        					sendContentToHQ.add(sendContent);
        				}
        			}
        			sendContent = null;
        		}

        		// 中継所引き渡し結果を受け取る
        		if (rcp.deliWaitFrgl != null) {
        			sendContent = rcp.receivePassingResult();
        			if (sendContent != null) {
        				sendContentToHQ.add(sendContent);
        			}
        		}

        		// 本部へ報告する
        		if (!sendContentToHQ.isEmpty()) {
        			if (rcp.boundary.sendSignal(sendContentToHQ.peek(), Receiver.hq, Receiver.reception, 1)) {
        				sendContentToHQ.remove();
        			}
        		}
        		
        		if(result != null && result.equals("end"))break;
        		Thread.sleep(1000);
        	}//ここまでfinally
        }//ここまでwhile
        System.out.println("動作を終了します");
    	executor.shutdown();
	}


	//タイムアウトに関係するクラス
	private static class Worker implements Callable<String> {
		
		public Worker() {
			// TODO 自動生成されたコンストラクター・スタブ
		}

		@Override
		public String call() throws Exception {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			return reader.readLine();
		}
	}

}
