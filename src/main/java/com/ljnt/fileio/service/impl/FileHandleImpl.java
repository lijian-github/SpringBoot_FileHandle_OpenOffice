package com.ljnt.fileio.service.impl;

import com.ljnt.fileio.entity.FileMsg;
import com.ljnt.fileio.service.FileHandleDao;
import org.jodconverter.DocumentConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * @ Program       :  com.ljnt.fileio.service.impl.FileHandleImpl
 * @ Description   :  File上传下载service
 * @ Author        :  lj
 * @ CreateDate    :  2020-2-15 8:05
 */
@Service
public class FileHandleImpl implements FileHandleDao {


    @Value("${files.uploadDir}")
    String uploadDir;//文件存储根路径
    @Autowired
    private DocumentConverter converter;
    /**
     * @Description  ：文件上传处理
     * @author       : lj
     * @param        : [fileList, request]
     * @return       : java.util.ArrayList<com.ljnt.fileio.entity.FileMsg>
     * @exception    :
     * @date         : 2020-2-16 11:24
     */
    @Override
    public ArrayList<FileMsg> fileuploadhandle(List<MultipartFile> fileList, HttpServletRequest request) {
        try {
            ArrayList<FileMsg> fileMsgArrayList=new ArrayList<>();
            for(MultipartFile file:fileList){
                if (file.isEmpty()){
                    System.out.println("文件为空");
                    return null;
                }
                //获得实体消息
                String fileName=file.getOriginalFilename();
                long fileId= new Date().getTime();
                int ty=fileName.lastIndexOf(".");
                String fileType=null;
                if (ty!=-1){
                    fileType=fileName.substring(ty+1);
                }
                long fileSize=file.getSize();
                Timestamp uploadTime=new Timestamp(fileId);
                //新建字文件夹路径
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                String fileAdd = sdf.format(new Date());
                String path = uploadDir + File.separator + fileAdd + File.separator;
                File pathfile=new File(path);
                if(!pathfile.exists()&&!pathfile.isDirectory()){
                    pathfile.mkdirs();
                }
                String realfileName = fileId+"_"+new Random().nextInt(1000)+"_"+fileName;
                String rootUrl=request.getScheme()+"://"+request.getRemoteHost()+":"+request.getServerPort()+"/";
                String fileUrl = rootUrl+fileAdd+"/"+realfileName;
                File dest = new File(path,realfileName);
                file.transferTo(dest);// 文件写入
                FileMsg fileMsg=new FileMsg(fileName,fileId,fileUrl,fileType,fileSize,uploadTime);//返回文件消息
                //office文件转pdf
                String preview=generatePDF(fileAdd,realfileName,fileType);
                if (preview!=null){
                    fileMsg.setPreviewUrl(rootUrl+"pdf/web/viewer.html?file="+preview);
                }else {
                    fileMsg.setPreviewUrl(fileUrl);
                }
                fileMsgArrayList.add(fileMsg);
            }
            return fileMsgArrayList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @Description  ：文件下载处理
     * @author       : lj
     * @param        : [fileId, response]
     * @return       : boolean
     * @exception    :
     * @date         : 2020-2-16 13:01
     */
    @Override
    public boolean filedownloadhandle(String fileId, HttpServletResponse response) {
        // 设置文件名，文件相对路径，根据业务需要可根据fileId存数据库获取，这里直接写
        String fileName = "1581827393374_5521.jpg";
        String fileAPath = "/20200216/1581827393374_5521.jpg";
        if (fileAPath != null) {
            //设置文件路径
            String realPath = uploadDir;
            File file = new File(realPath , fileAPath);
            if (file.exists()&&file.isFile()) {
                response.setContentType("application/force-download");// 设置强制下载不打开
                response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);// 设置文件名
                byte[] buffer = new byte[1024];
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                try {
                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);
                    OutputStream os = response.getOutputStream();
                    int i = bis.read(buffer);
                    while (i != -1) {
                        os.write(buffer, 0, i);
                        i = bis.read(buffer);
                    }
                    System.out.println("success");
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }else {
                return false;
            }
        }
        return false;
    }

    /**
     * @Description  ：office文件转换pdf
     * @author       : lj
     * @param        : [fileAPath, realfileName, fileType]
     * @return       : java.lang.String
     * @exception    :
     * @date         : 2020-2-16 14:22
     */
    public String generatePDF(String fileAdd,String realfileName,String fileType){
        if (isOfficeFile(fileType)) {
            File oldfile=new File(uploadDir+File.separator+fileAdd+File.separator,realfileName);
            if (fileType.toLowerCase().equals("pdf")){//本身是pdf不转
                return "/"+fileAdd+"/"+realfileName;
            }else if (fileType.toLowerCase().equals("txt")){
                if (!tranTxt(oldfile)){
                    return null;
                }
            }
            String fileName=realfileName.substring(0,realfileName.lastIndexOf("."))+".pdf";
            File newfile=new File(uploadDir+File.separator+"2pdf"+File.separator,fileName);
            if (!newfile.getParentFile().exists()){
                newfile.getParentFile().mkdirs();
            }
            try {
                //转换核心代码
                converter.convert(oldfile).to(newfile).execute();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("转换出错！");
                return null;
            }
            System.out.println("转换成功！");
            return "/2pdf/"+fileName;
        }else {
            return null;
        }

    }

    /**
     * @Description  ：简单判断是否是office文件
     * @author       : lj
     * @param        : [fileType]
     * @return       : boolean
     * @exception    :
     * @date         : 2020-2-16 14:21
     */
    private boolean isOfficeFile(String fileType){
        fileType=fileType.toLowerCase();
        System.out.println(fileType);
        switch (fileType){
            case "txt":
            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
            case "pdf":
            case "html":
            case "xml":
                return true;
        }
        return false;
    }

    /**
     * @Description  : TXT文件编码转换，解决转pdf中文乱码问题
     * @author       : lj
     * @param        : [file]
     * @return       : boolean
     * @exception    :
     * @date         : 2020-2-17 0:00
     */
    private boolean tranTxt(File file){
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, "GBK");
            BufferedReader br = new BufferedReader(isr);
            String OriStr;
            StringBuilder stringBuilder = new StringBuilder();
            while ((OriStr = br.readLine()) != null) {
                //手动拼接换行符
                OriStr += "\n";
                stringBuilder.append(OriStr);
            }
            String targetStr = stringBuilder.toString();
            //false代表不追加直接覆盖,true代表追加文件
            FileOutputStream fos = new FileOutputStream(file,false);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            osw.write(stringBuilder.toString());
            osw.flush();
            osw.close();
            fos.close();
            br.close();
            isr.close();
            fis.close();
            return true;
        } catch (Exception e) {
            // TODO: handle exception
            return false;
        }

    }
}
