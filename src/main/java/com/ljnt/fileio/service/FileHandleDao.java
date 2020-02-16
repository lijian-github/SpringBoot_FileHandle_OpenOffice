package com.ljnt.fileio.service;

import com.ljnt.fileio.entity.FileMsg;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public interface FileHandleDao {
    ArrayList<FileMsg> fileuploadhandle(List<MultipartFile> fileList, HttpServletRequest request);
    boolean filedownloadhandle(String fileId, HttpServletResponse response);
    String generatePDF(String fileAdd,String realfileName,String fileType);
}
