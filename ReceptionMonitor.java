package reception;

import java.io.*;

import fragile.ClientInfo;


public class ReceptionMonitor {

	/**
	 * PCの画面に依頼情報（依頼人氏名、依頼人電話番号、依頼人住所、受取人氏名、受取人電話番号、受取人住所）が入力でき、それらの情報を表示する。
	 */
	public ClientInfo demandClientInfo(){		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		ClientInfo clientInfo = new ClientInfo();
		try {
			//依頼人情報の入力
			System.out.println("依頼人の氏名を入力してください");
			String name = new String(in.readLine());
			System.out.println("依頼人の電話番号を入力してください");
			String tel = new String(in.readLine());
			System.out.println("依頼人の住所を入力してください");
			String addr = new String(in.readLine());
			
			clientInfo.setClientInfo(name, tel, addr);
			
			//受取人情報の入力
			System.out.println("受取人の氏名を入力してください");
			name = new String(in.readLine());
			System.out.println("受取人の電話番号を入力してください");
			tel = new String(in.readLine());
			System.out.println("受取人の住所を入力してください");
			addr = new String(in.readLine());
			
			clientInfo.setHouseInfo(name, tel, addr);

		} catch (IOException e) {
	        System.out.println("例外" + e + "が発生しました");
		}
		
		return clientInfo;
	}

	/**
	 * PCの画面に「エラー」と表示する
	 */
	public void displayErr(String errDetail) {
		System.out.println("エラー:"+errDetail);
	}

	/**
	 * PCの画面に「依頼を受け付けました」と表示する
	 */
	public void displayAccepted() {
		System.out.println("依頼を受け付けました");
	}

	/**
	 * PCの画面に荷物番号を表示する
	 */
	public void displayFrglNum(long num) {
		System.out.println("荷物番号："+num);
	}

}
