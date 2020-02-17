package com.ljnt.fileio.controller;

import com.ljnt.fileio.service.FileHandleDao;
import org.jodconverter.DocumentConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @ Program       :  com.ljnt.fileio.controller.FileController
 * @ Description   :  文件上传下载controller
 * @ Author        :  lj
 * @ CreateDate    :  2020-2-15 7:40
 */
@Controller
public class FileController {
    @Autowired
    private FileHandleDao fileHandleDao;
    @PostMapping("/upload")
    @ResponseBody
    public Object uploadfile(HttpServletRequest request){
        MultipartHttpServletRequest multipartHttpServletRequest= (MultipartHttpServletRequest) request;
        List<MultipartFile> files=multipartHttpServletRequest.getFiles("file");
        return fileHandleDao.fileuploadhandle(files,request);
    }

    @GetMapping("/download/{fileId}")
    @ResponseBody
    public String downloadfile(@PathVariable String fileId, HttpServletResponse response){
        System.out.println(fileId);
        return String.valueOf(fileHandleDao.filedownloadhandle(fileId,response));
    }
//    @RequestMapping("/preview")
//    public String previewfile(){
//        String s=fileHandleDao.generatePDF("20200216","1581862298196_849_win10锁屏.docx","docx");
//        return s;
//    }
//    @RequestMapping(value="/test" , method = RequestMethod.GET)
//    public void test(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        response.sendRedirect("/pdf/web/viewer.html?file=/pdf/test.pdf");
//    }
    //文件下载相关代码
//    @RequestMapping("/download")
//    public String downloadFile(HttpServletRequest request, HttpServletResponse response) {
//        String fileName = "adwards.png";// 设置文件名，根据业务需要替换成要下载的文件名
//        if (fileName != null) {
//            //设置文件路径
//            String realPath = "E://uploads//";
//            File file = new File(realPath , fileName);
//            if (file.exists()) {
//                response.setContentType("application/force-download");// 设置强制下载不打开
//                response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);// 设置文件名
//                byte[] buffer = new byte[1024];
//                FileInputStream fis = null;
//                BufferedInputStream bis = null;
//                try {
//                    fis = new FileInputStream(file);
//                    bis = new BufferedInputStream(fis);
//                    OutputStream os = response.getOutputStream();
//                    int i = bis.read(buffer);
//                    while (i != -1) {
//                        os.write(buffer, 0, i);
//                        i = bis.read(buffer);
//                    }
//                    System.out.println("success");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    if (bis != null) {
//                        try {
//                            bis.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    if (fis != null) {
//                        try {
//                            fis.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        }
//        return null;
//    }

//    @RequestMapping("/generatePDF")
//    @ResponseBody
//    public Object generatePDF() {
//        String realPath = "E://uploads//";
//        File of=new File(realPath,"考核任务.md");
//        File nf=new File(realPath+"/2pdf/","考核任务.pdf");
//        if (!nf.getParentFile().exists()){
//            nf.getParentFile().mkdirs();
//        }
//        HashMap<String, String> map = new HashMap<>();
//        try {
//            converter.convert(of).to(nf).execute();
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("转换出错！");
//            map.put("flag", "false");
//            return map;
//        }
//        System.out.println("转换成功！");
//        map.put("flag", "success");
//        return map;
//    }
}
