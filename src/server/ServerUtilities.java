package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import common.FileDetails;
import common.Messages;

public class ServerUtilities {

	String getUserHome(String username) {
		//return (String) System.getProperty("user.dir") + "/" + username;
		String path = Paths.get("").toAbsolutePath().toString();
		return path+"/server/users/"+username;
	}

	private boolean createOSDirectory(String dirPath) {
		File dir = new File(dirPath);
		if(dir.exists()) {
			return true;
		}
		return dir.mkdir();
	}

	private boolean moveOSFile(String srcPath, String destPath) {
		// File srcFile = new File(srcPath);
		// return srcFile.renameTo(new File(destPath));
		try {
			Path temp = Files.move(Paths.get(srcPath), Paths.get(destPath));
			if(temp != null) {
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private void listFiles(String path, String userHomeDir, List<FileDetails> fileList) {
		File folder = new File(path);
		File[] files = folder.listFiles();

		for (File file : files) {
			if (file.isFile()) {
				String fileName = file.getName();
				String currPath = file.getAbsolutePath().replaceAll(userHomeDir, "").replaceAll(fileName, "");
				fileList.add(new FileDetails(fileName, currPath));
			} else if (file.isDirectory()) {
				listFiles(file.getAbsolutePath(), userHomeDir, fileList);
			}
		}
	}

	private List<FileDetails> listAllFilesByUser(String username){
		String userHomeDir = getUserHome(username);
		System.out.println("User homeDir is: "+userHomeDir);
		List<FileDetails> fileList = new ArrayList<FileDetails>();
		listFiles(userHomeDir, userHomeDir, fileList);
		return fileList;
	} 
	
	String createUser(String username) throws Exception {
		if (ServerStructures.usersMap.containsKey(username)) {
			return Messages.USERNAME_ALREADY_EXIST;
		}
		String homeDir = getUserHome(username);
		System.out.println("Folder to be created at: "+homeDir);
		boolean rv = createOSDirectory(homeDir);
		if (!rv) {
			System.out.println("Error while creating folder for new user..");
			return Messages.CREATE_USER_ERROR;
		}
		ServerStructures.usersMap.put(username, new HashSet<String>());
		return Messages.CREATE_USER_SUCCESS;
	}
	
	
	String createFolderByUser(String username, String folderPath) {
		String homeDir = getUserHome(username);
		String dirPath = homeDir + "/" + folderPath;
		if (createOSDirectory(dirPath)) {
			return Messages.FOLDER_CREATE_SUCCESS;
		}
		return Messages.FOLDER_CREATE_ERROR;
	}

	String moveFileByUser(String username, String srcPath, String destPath) {
		System.out.println("Inside moveFileByUser()");
		String homeDir = getUserHome(username);
		srcPath = homeDir + "/" + srcPath;
		destPath = homeDir + "/" + destPath;
		
		System.out.println("srcPath is: "+srcPath);
		String fileName = destPath.substring(destPath.lastIndexOf('/'));
		destPath = destPath.replaceAll(fileName, "");

		System.out.println("Filename is: "+fileName);
		System.out.println("destPath is: "+destPath);
		
		if(new File(srcPath).exists() == false) {
			return Messages.SRCPATH_NOT_EXIST;
		}
		if(new File(destPath).exists() == false) {
			return Messages.DESTPATH_NOT_EXIST;
		}
		
		destPath += fileName;
		if(moveOSFile(srcPath, destPath)) {
			return Messages.MOVE_FILE_SUCCESS;
		}
		return Messages.MOVE_FILE_ERROR;
	}
	
	String createGroup(String username, String groupname) {
		if(ServerStructures.groupMap.containsKey(groupname)) {
			return Messages.GROUP_ALREADY_EXIST;
		}
		Set<String> set = new HashSet<String>();
		set.add(username);
		ServerStructures.groupMap.put(groupname, set);
		
		ServerStructures.usersMap.get(username).add(groupname);
		return Messages.CREATE_GROUP_SUCCESS;
	}
	
	List<String> listGroups(){
		if(ServerStructures.groupMap.isEmpty()) {
			return null;
		}
		List<String> groupNames = new ArrayList<String>();
		for(String name : ServerStructures.groupMap.keySet()) {
			groupNames.add(name);
		}
		return groupNames;
	}
	
	String joinGroup(String username, String groupname) {
		if(ServerStructures.groupMap.containsKey(groupname) == false) {
			return Messages.GROUP_NOT_EXIST;
		}
		if(ServerStructures.groupMap.get(groupname).contains(username)) {
			return Messages.USER_EXIST_IN_GROUP;
		}
		ServerStructures.groupMap.get(groupname).add(username);
		ServerStructures.usersMap.get(username).add(groupname);
		return Messages.JOIN_GROUP_SUCCESS;
	}
	
	String leaveGroup(String username, String groupname) {
		if(ServerStructures.groupMap.containsKey(groupname) == false) {
			return Messages.GROUP_NOT_EXIST;
		}
		if(ServerStructures.groupMap.get(groupname).contains(username) == false) {
			return Messages.USER_NOT_IN_GROUP;
		}
		ServerStructures.groupMap.get(groupname).remove(username);
		ServerStructures.usersMap.get(username).remove(groupname);
		return Messages.LEAVE_GROUP_SUCCESS;
	}
	
	
	
	Map<String, List<FileDetails>> listGroupDetails(String groupname) {
		if(ServerStructures.groupMap.containsKey(groupname) == false) {
			return null;
		}
		Map<String, List<FileDetails>> groupDetails = new HashMap<String, List<FileDetails>>();
		Set<String> userSet = ServerStructures.groupMap.get(groupname);
		List<FileDetails> fileDetails= null;
		
		for(String username : userSet) {
			fileDetails = listAllFilesByUser(username);
			groupDetails.put(username, fileDetails);
		}
		
		return groupDetails; 
	}
	
}
