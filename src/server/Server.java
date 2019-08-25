package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import common.Constants;
import common.FileDetails;
import common.Messages;

class ClientHandler extends Thread {
	final ObjectInputStream ois;
	final ObjectOutputStream oos;
	final Socket socket;
	private volatile boolean flag = true;

	public ClientHandler(Socket s, ObjectInputStream ois, ObjectOutputStream oos) {
		this.socket = s;
		this.ois = ois;
		this.oos = oos;
	}

	// This method will set flag as false
	public void stopRunning() {
		flag = false;
	}

	String receiveFile(String username, ServerUtilities su) throws Exception {
		FileOutputStream fos = null;
		byte[] buffer = new byte[Constants.BUFFER_SIZE];
		String retValue = null;
		String filePath = su.getUserHome(username);
		// 1. Read file name.
		Object o = ois.readObject();
		if (o instanceof String) {
			filePath += "/" + o.toString();
			fos = new FileOutputStream(filePath);
		} else {
			return Messages.FILENAME_READ_ERROR;
		}

		System.out.println("Location where the file to be written: " + filePath);

		// 2. Read file to the end.
		Integer bytesRead = 0;
		do {
			o = ois.readObject();
			if (!(o instanceof Integer)) {
				retValue = Messages.CURR_CHUNK_SIZE_READ_ERROR;
				break;
			}

			bytesRead = (Integer) o;
			o = ois.readObject();
			if (!(o instanceof byte[])) {
				retValue = Messages.CURR_CHUNK_DATA_READ_ERROR;
			}

			buffer = (byte[]) o;
			// 3. Write data to output file.
			fos.write(buffer, 0, bytesRead);

		} while (bytesRead == Constants.BUFFER_SIZE);

		fos.close();
		if (retValue != null) {
			return retValue;
		}
		return Messages.FILE_UPLOAD_SUCCESS;
	}

	String sendFile(String grpUsrPath, ServerUtilities su) {
		int index = grpUsrPath.indexOf("/");
		String grpName = grpUsrPath.substring(0, index);
		grpUsrPath = grpUsrPath.substring(index+1);
		index = grpUsrPath.indexOf("/");
		String username = grpUsrPath.substring(0, index);
		grpUsrPath = grpUsrPath.substring(index+1);
		String filePath = grpUsrPath;
		String homeDir = su.getUserHome(username);
		
		filePath = homeDir+"/"+filePath;
		File file = new File(filePath);
		System.out.println("File path to send is: "+filePath);
		if(file.exists() == false) {
			return Messages.FILEPATH_NOT_EXIST;
		}
		FileInputStream fis = null;

		try {

			oos.writeObject(file.getName());

			fis = new FileInputStream(file);
			byte[] buffer = new byte[Constants.BUFFER_SIZE];
			Integer bytesRead = 0;
			int count = 1;
			while ((bytesRead = fis.read(buffer)) > 0) {
				oos.writeObject(bytesRead);
				oos.writeObject(Arrays.copyOf(buffer, buffer.length));
				System.out.println("Chunk: " + count + " sent.");
				count++;
			}
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Messages.FILE_DOWNLOAD_SUCCESS;
	}
	
	@Override
	public void run() {

		String requestMsg = null;
		String replyMsg = null;
		String username = null;
		ServerUtilities su = new ServerUtilities();
		Object ob = null;

		while (true) {

			try {
				oos.writeObject("Enter the command: ");
				ob = ois.readObject();
				if (!(ob instanceof String)) {
					oos.writeObject(Messages.INVALID_FORMAT);
					return;
				}
				requestMsg = (String) ob;
				System.out.println("Request msg is: " + requestMsg);
				String[] tokens = requestMsg.split(" ");
				String command = tokens[0];

				if (command.equalsIgnoreCase(Constants.CREATE_USER) && tokens.length == 2) {
					username = tokens[1];
					if (ServerStructures.usersMap.containsKey(username)
							&& ServerStructures.activeUserSet.contains(username)) {
						replyMsg = Messages.USER_ALREADY_CONNECTED;

					} else if (ServerStructures.usersMap.containsKey(username)
							&& ServerStructures.activeUserSet.contains(username) == false) {
						ServerStructures.activeUserSet.add(username);
						replyMsg = Messages.USER_LOGIN_SUCCESS;

					} else {
						replyMsg = su.createUser(tokens[1]);
						ServerStructures.activeUserSet.add(username);
					}
					System.out.println(replyMsg);
					oos.writeObject(replyMsg);

				} else if (command.equalsIgnoreCase(Constants.UPLOAD_FILE) && tokens.length == 2) {
					if (username == null) {
						replyMsg = Messages.USER_NOT_CONNECTED;
					} else {
						replyMsg = receiveFile(username, su);
					}
					System.out.println(replyMsg);
					oos.writeObject(replyMsg);

				} else if (command.equalsIgnoreCase(Constants.UPLOAD_FILE_UDP) && tokens.length == 2) {

				} else if (command.equalsIgnoreCase(Constants.CREATE_FOLDER) && tokens.length == 2) {
					if (username == null) {
						replyMsg = Messages.USER_NOT_CONNECTED;
					} else {
						replyMsg = su.createFolderByUser(username, tokens[1]);
					}
					oos.writeObject(replyMsg);

				} else if (command.equalsIgnoreCase(Constants.MOVE_FILE) && tokens.length == 3) {
					if (username == null) {
						replyMsg = Messages.USER_NOT_CONNECTED;
					} else {
						replyMsg = su.moveFileByUser(username, tokens[1], tokens[2]);
					}
					oos.writeObject(replyMsg);

				} else if (command.equalsIgnoreCase(Constants.CREATE_GROUP) && tokens.length == 2) {
					if (username == null) {
						replyMsg = Messages.USER_NOT_CONNECTED;
					} else {
						replyMsg = su.createGroup(username, tokens[1]);
					}
					oos.writeObject(replyMsg);

				} else if (command.equalsIgnoreCase(Constants.LIST_GROUPS) && tokens.length == 1) {
					if (username == null) {
						replyMsg = Messages.USER_NOT_CONNECTED;
						oos.writeObject(replyMsg);
					} else {
						List<String> groupList = su.listGroups();
						oos.writeObject(groupList);
						// Serialize and send list object
					}

				} else if (command.equalsIgnoreCase(Constants.LIST_GROUP_DETAILS) && tokens.length == 2) {
					if (username == null) {
						replyMsg = Messages.USER_NOT_CONNECTED;
						oos.writeObject(replyMsg);
					} else {
						Map<String, List<FileDetails>> groupDetails = su.listGroupDetails(tokens[1]);
						oos.writeObject(groupDetails);
						// Serialize and send map object
					}

				} else if (command.equalsIgnoreCase(Constants.JOIN_GROUP) && tokens.length == 2) {
					if (username == null) {
						replyMsg = Messages.USER_NOT_CONNECTED;
					} else {
						replyMsg = su.joinGroup(username, tokens[1]);
					}
					oos.writeObject(replyMsg);

				} else if (command.equalsIgnoreCase(Constants.LEAVE_GROUP) && tokens.length == 2) {
					if (username == null) {
						replyMsg = Messages.USER_NOT_CONNECTED;
					} else {
						replyMsg = su.leaveGroup(username, tokens[1]);
					}
					oos.writeObject(replyMsg);

				} else if (command.equalsIgnoreCase(Constants.SHARE_MSG) && tokens.length == 2) {
					if (username == null) {
						replyMsg = Messages.USER_NOT_CONNECTED;
					} else {

					}
					oos.writeObject(replyMsg);

				} else if (command.equalsIgnoreCase(Constants.GET_FILE) && tokens.length == 2) {
					if (username == null) {
						replyMsg = Messages.USER_NOT_CONNECTED;
					} else {
						replyMsg = sendFile(tokens[1], su);
					}
					oos.writeObject(replyMsg);

				} else if (command.equalsIgnoreCase(Constants.EXIT) && tokens.length == 1) {
					if(ServerStructures.activeUserSet.contains(username)) {
						ServerStructures.activeUserSet.remove(username);
					}
					replyMsg = Messages.CONNECTION_CLOSE_SUCCESS;
					oos.writeObject(replyMsg);
					break;
				} else {
					replyMsg = Messages.INVALID_COMMAND;
					oos.writeObject(replyMsg);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Exiting thread for connection: "+socket.getRemoteSocketAddress());
		try {
			oos.close();
			ois.close();
			socket.close();
			// Thread.sleep(100);
			// stopRunning();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// this.stopRunning();
	}
}

public class Server {

	public static void main(String[] args) throws IOException {
		int localPort = Integer.parseInt(args[0]);
		@SuppressWarnings("resource")
		ServerSocket serverSocket = new ServerSocket(localPort);
		System.out.println("Server socket created and server listening at: " + serverSocket.getLocalSocketAddress());
		while (true) {
			Socket socket = null;
			try {
				socket = serverSocket.accept();
				System.out.println("Server accepted the client: " + socket.getRemoteSocketAddress());

				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				System.out.println("Assigning new thread for this client..");
				new ClientHandler(socket, ois, oos).start();
			} catch (Exception e) {
				socket.close();
				e.printStackTrace();
			}
		}
	}
}