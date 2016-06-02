import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Scanner;

/**
 * Process in vector time stamp algorithm
 * 
 * @author Akshai Prabhu
 *
 */
class Process {
	int balance; // balance money in the process
	int P[]; // vector times of each process as integer array

	/**
	 * Constructor
	 */
	Process() {
		balance = 1000;
		P = new int[3];
		P[0] = 0;
		P[1] = 0;
		P[2] = 0;
	}

	/**
	 * Updates balance of the process according to transaction performed
	 * 
	 * @param sent
	 * @param p0
	 * @param p1
	 * @param p2
	 * @param money
	 * @param event
	 */
	public void changeBalance(String sent, int p0, int p1, int p2, int money, int event) {
		synchronized (this) { // synchronized block
			if (event == 0) { // Deposit money
				System.out.println("Deposit amount: $" + money);
				System.out.println("Before deposit: $" + balance);
				this.balance += money;
				System.out.println("After deposit: $" + balance);
				updateVectorTime(p0, p1, p2);
				System.out.println("Vector time: (" + P[0] + "," + P[1] + "," + P[2] + ")");
			} else if (event == 3) { // Received money from other process to be
										// added to this process
				System.out.println("Received amount: $" + money);
				System.out.println("Before receiving deposit: $" + balance);
				this.balance += money;
				updateVectorTime(p0, p1, p2);

				// update vector time using vector time of other process
				if (p0 > this.P[0]) {
					this.P[0] = p0;
				}
				if (p1 > this.P[1]) {
					this.P[1] = p1;
				}
				if (p2 > this.P[2]) {
					this.P[2] = p2;
				}
				System.out.println("After receiving deposit: $" + balance);
				System.out.println("Vector time: (" + P[0] + "," + P[1] + "," + P[2] + ")");
			} else if (event == 1) { // withdraw money
				System.out.println("Withdraw amount: $" + money);
				System.out.println("Before withdraw: $" + balance);
				this.balance -= money;
				updateVectorTime(p0, p1, p2);
				System.out.println("After withdraw: $" + balance);
				System.out.println("Vector time: (" + P[0] + "," + P[1] + "," + P[2] + ")");
			} else if (event == 2) { // send money to different process
				System.out.println("Send amount: $" + money);
				System.out.println("Before sending: $" + balance);
				this.balance -= money;
				updateVectorTime(p0, p1, p2);
				sendMoney(money);
				System.out.println("After sending: $" + balance);
				System.out.println("Vector time: (" + P[0] + "," + P[1] + "," + P[2] + ")");
			}
		}

	}

	/**
	 * Updates vector time when an event takes place
	 * 
	 * @param p0
	 * @param p1
	 * @param p2
	 */
	private void updateVectorTime(int p0, int p1, int p2) {
		try {
			if (InetAddress.getLocalHost().getHostName().equals("glados")) {
				p0++;
				this.P[0] = p0;
			} else if (InetAddress.getLocalHost().getHostName().equals("kansas")) {
				p1++;
				this.P[1] = p1;
			} else if (InetAddress.getLocalHost().getHostName().equals("newyork")) {
				p2++;
				this.P[2] = p2;
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * To send money to a different process at random
	 * 
	 * @param money
	 */
	private void sendMoney(int money) {
		Random random = new Random();
		int server = random.nextInt(2);
		try {
			if (InetAddress.getLocalHost().getHostName().equals("glados")) {
				if (server == 0) {
					send("129.21.37.18", 40000, money);
				} else {
					send("129.21.37.16", 50000, money);
				}
			} else if (InetAddress.getLocalHost().getHostName().equals("kansas")) {
				if (server == 0) {
					send("129.21.37.16", 40000, money);
				} else {
					send("129.21.22.196", 50000, money);
				}
			} else if (InetAddress.getLocalHost().getHostName().equals("newyork")) {
				if (server == 0) {
					send("129.21.22.196", 40000, money);
				} else {
					send("129.21.37.18", 50000, money);
				}
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * To send money as a client
	 * 
	 * @param IP
	 * @param port
	 * @param money
	 */
	private void send(String IP, int port, int money) {
		Socket socket;
		try {
			socket = new Socket(IP, port);
			OutputStream outToServer = socket.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			// send vector time and money to other process
			out.writeUTF("" + P[0] + "," + P[1] + "," + P[2] + "," + money);
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}

/**
 * Event in the process
 * 
 * @author Akshai Prabhu
 *
 */
class Event extends Thread {
	Process p;

	/**
	 * Constructor
	 * 
	 * @param p
	 */
	public Event(Process p) {
		this.p = p;
	}

	/**
	 * Thread run method
	 */
	public void run() {
		while (true) {
			Random random = new Random();
			int event = random.nextInt(3);
			int money = random.nextInt(100);
			// update balance according to the event and als update vector time
			p.changeBalance("", p.P[0], p.P[1], p.P[2], money, event);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

/**
 * To receive money from one other process
 * 
 * @author Akshai Prabhu
 *
 */
class ReceiveMoneyOne extends Thread {
	Process p;
	ServerSocket serverSocket;

	/**
	 * Constructor
	 * 
	 * @param p
	 */
	public ReceiveMoneyOne(Process p) {
		this.p = p;
	}

	/**
	 * Thread run method
	 */
	public void run() {
		while (true) {
			Socket socket;
			String message = new String();
			try {
				serverSocket = new ServerSocket(40000);
				socket = serverSocket.accept();
				DataInputStream in = new DataInputStream(socket.getInputStream());
				message = in.readUTF();
				InetAddress sent = socket.getInetAddress();
				socket.close();
				serverSocket.close();
				String msg[] = message.split(",");
				// update vector time using vector time of other process
				p.changeBalance("" + sent, Integer.parseInt(msg[0]), Integer.parseInt(msg[1]), Integer.parseInt(msg[2]),
						Integer.parseInt(msg[3]), 3);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

/**
 * To receive money from one other process
 * 
 * @author Akshai Prabhu
 *
 */
class ReceiveMoneyTwo extends Thread {
	Process p;
	ServerSocket serverSocket;

	/**
	 * Constructor
	 * 
	 * @param p
	 */
	public ReceiveMoneyTwo(Process p) {
		this.p = p;

	}

	/**
	 * Thread run method
	 */
	public void run() {
		while (true) {
			Socket socket;
			String message = new String();
			try {
				serverSocket = new ServerSocket(50000);
				socket = serverSocket.accept();
				DataInputStream in = new DataInputStream(socket.getInputStream());
				message = in.readUTF();
				InetAddress sent = socket.getInetAddress();
				socket.close();
				serverSocket.close();
				String msg[] = message.split(",");
				// update vector time using vector time of other process
				p.changeBalance("" + sent, Integer.parseInt(msg[0]), Integer.parseInt(msg[1]), Integer.parseInt(msg[2]),
						Integer.parseInt(msg[3]), 3);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

/**
 * Vector time main class that intiates all other threads
 * 
 * @author Akshai Prabhu
 *
 */
public class VectorTime {
	public static void main(String args[]) {
		Process p = new Process();
		Event e = new Event(p);
		ReceiveMoneyOne rmo = new ReceiveMoneyOne(p);
		ReceiveMoneyTwo rmt = new ReceiveMoneyTwo(p);
		rmo.start();
		rmt.start();
		Scanner sc = new Scanner(System.in);
		System.out.println("Press Enter to Start");
		String in = sc.nextLine();
		sc.close();
		e.start();
	}
}
