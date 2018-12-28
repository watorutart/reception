package reception;
//パッケージとインポートは適宜行う
import java.io.IOException;
import lejos.utility.Delay;
//import Deliver.Adjustment;
import telecommunication.Telecommunication;
import telecommunication.Receiver;

class SampleProtocol{
	Telecommunication telecommunication = new Telecommunication();
	
	//////////////////////////////////////////////////////////////////////////////////
	//                               送信側  send protocol                          //
	//////////////////////////////////////////////////////////////////////////////////
	
	//プロトコル通信　partner:希望する通信相手
	//protocol communication, partner:partner user wishes
	void makeProtocol(Receiver partner){
		String syncDetail="";
		boolean flag=false;
		
		
		if(partner==Receiver.collector) syncDetail = "protocol|collector"/*adjust(Adjustment.relayProtocol)*/;
		else if(partner==Receiver.hq) syncDetail="protocol|hq"/*adjust(Adjustment.houseProtocol)*/;
		
		Delay.msDelay(10);
		
		//サブシステム名を要求する
		//demand opponent after established connection
		do{
			try{
				flag=this.telecommunication.sendSignal(syncDetail, partner, Receiver.deliver, 120);
			}
			catch(IOException ioe){
				continue;
			}
		} while(flag==false);
		
		Delay.msDelay(10);
		
		//通信相手が正しいかを知る
		//judge who communicate with
		do{
			try{
				syncDetail=this.telecommunication.receiveSignal(partner, Receiver.deliver, 20);
			}
			catch(IOException ioe){
				continue;
			}
		} while(syncDetail.equals(""));
		
		/*
		//ここで、trueなら次の処理に進む。falseならプロトコル通信を再度繰り返す。
		if(partner==Receiver.relay) this.exeRelayOrder(syncDetail);
		else if(partner==Receiver.house) this.exeHouseOrder(syncDetail);
		else{
			System.err.println("Illegal input on isProtocol(): Input relay or house.");
		}
		*/
	}
	
	////////////////////////////////////////////////////////////////////////////////////
	//                               受信側  receive protocol                         //
	////////////////////////////////////////////////////////////////////////////////////
	
	//protocol communication
	//boolean か void かは　書き方によるかもしれません。
	boolean makeProtocol(){
		String syncDetail="";
		boolean flag=false;
			

		//サブシステム名送信要求を受信
        	//"protocol|collector"を収集ロボットから受信
		do{
			try{
				syncDetail=this.telecommunication.receiveSignal(Receiver.collector, Receiver.reception, 20);
			}
			catch(IOException ioe){
				continue;
			}
		} while(syncDetail.equals(""));
		
		//ここで命令番号分岐→命令分岐メソッド内に加工保存のプログラムを書く
		//例.  this.executeOrder();
		
		//ここで、サブシステム名が合っていれば、"protocol|true"　間違っていれば、"protocol|false"を String adjust()を使って書く
		//例.  syncDetail=this.adjust(Adjustment.issueProtocol);
		if(syncDetail.equals("protocol|collector"))
			syncDetail = "protocol|true";
		else syncDetail = "protocol|false";
			
		//サブシステム名を送り返す
		//send subsystem name established with opponent
		do{
			try{
				flag=this.telecommunication.sendSignal(syncDetail, Receiver.collector, Receiver.reception, 3);
			}
			catch(IOException ioe){
				continue;
			}
		} while(flag==false);
				
		return flag;
	}
	
}



