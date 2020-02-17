# SpringBoot_FileHandle_OpenOffice

> SpringBoot文件上传下载，整合OpenOffice+pdf.js实现office文件预览

文件上传使用MultipartFile对象、下载使用传统IO流、jobconverter结合OpenOffice把office文件转为pdf文件、通过pdf.js实现在线预览pdf文件。（核心代码直接跳5.Service层）



[TOC]

### 1.	导入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>org.junit.vintage</groupId>
            <artifactId>junit-vintage-engine</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>

<!-- openOffice 和 jobconverter-->
<dependency>
    <groupId>org.jodconverter</groupId>
    <artifactId>jodconverter-core</artifactId>
    <version>4.2.2</version>
</dependency>
<dependency>
    <groupId>org.jodconverter</groupId>
    <artifactId>jodconverter-spring-boot-starter</artifactId>
    <version>4.2.2</version>
</dependency>
<dependency>
    <groupId>org.jodconverter</groupId>
    <artifactId>jodconverter-local</artifactId>
    <version>4.2.2</version>
</dependency>
```

### 2.  安装OpenOffice
OpenOffice下载地址: http://www.openoffice.org/download/

**（Linux）安装**

*1.下载安装包到Linux opt文件夹*

*2.解压：tar -zxvf Apache_OpenOffice_4.1.6_Linux_x86-64_install-rpm_zh-CN.tar.gz*

*3.进入cd zh-CN/RPMS*

*4.安装：rpm -ivh *.rpm*

*5.进入openOffice安装目录，命令：cd /opt/openoffice4/program/*

*6.开启服务（永久启动）：soffice --headless --accept=“socket,host=127.0.0.1,port=8100;urp;” --nofirststartwizard &*

*7.查看进程：netstat -lnp |grep 8100*

*8.查询指定端口是否已开（开放8100端口）： firewall-cmd --query-port=8100/tcp*

​    *提示 yes，表示开启；no表示未开启*

*9.添加指定需要开放的端口：firewall-cmd --add-port=8100/tcp --permanent*

*10.重载入添加的端口：firewall-cmd --reload*

*11.查询指定端口是否开启成功：firewall-cmd --query-port=8100/tcp*

**（Windows）安装**

1.一直按下一步就ok

2.启动服务，cmd进入安装目录

```cmd
cd C:\Program Files (x86)\OpenOffice 4\program soffice -headless -accept="socket,host=127.0.0.1,port=8100;urp;" -nofirststartwizard
```
### 3.	编写配置文件application.yml

1. static-path-pattern设置静态资源访问根路径，默认“/**”，resources.static-locations设置静态资源位置，默认"classpath:/META-INF/resources/","classpath:/resources/","classpath:/static/","classpath:/public/"，但是这里配置了resources.static-locations会覆盖掉默认配置，所以要加上，“file:${files.uploadDir}”配置外部资源，可以直接访问上传的文件，这里的“file:”代表访问文件系统。
2. 配置jodconverter，port-numbers配置openoffice服务端口，office-home为openoffice安装路径。

```yml
# 上传文件存储路径
files.uploadDir: E:/uploads/
spring:
  servlet:
    # multipartfile设置文件大小、上传数据大小
    multipart:
      enabled: true
      max-file-size: 100MB
      max-request-size: 200MB
  # 配置静态资源
  mvc:
    static-path-pattern: /**
  resources:
    static-locations: ["classpath:/META-INF/resources/","classpath:/resources/","classpath:/static/","classpath:/public/","file:${files.uploadDir}"]
  # 配置thymeleaf模板
  thymeleaf:
    cache: false
    mode: HTML
# 配置jodconverter
jodconverter:
  local:
    enabled: true
    max-tasks-per-process: 10
    port-numbers: 8100
    office-home: E:/Program Files/OpenOffice 4.1.7/openofficedata
```

### 4.	实体类FileMsg

```java
import java.sql.Timestamp;

public class FileMsg {
    private String fileName;
    private long fileId;
    private String fileUrl;
    private String fileType;
    private long fileSize;
    private Timestamp uploadTime;
    private String previewUrl;
    //uploadUser



    public FileMsg(String fileName, long fileId, String fileUrl, String fileType, long fileSize, Timestamp uploadTime) {
        this.fileName = fileName;
        this.fileId = fileId;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadTime = uploadTime;
    }

    public FileMsg() {
    }
    //·····省略setter、getter、toString
```

### 5.	Service层

#### Dao（FileHandleDao）

```java
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
```

#### Impl（FileHandleImpl）文件上传下载、转pdf、编码转换等

```java
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
```

### 6.	Controller层

```java
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
    private DocumentConverter converter;
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
}
```

### 7. 	导入pdf.js

pdf.js下载地址：https://gitee.com/xinxi17_admin/pdf.js/releases

解压文件导入到项目中static/pdf/中，目录结构：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200217112724858.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2xqbGo4ODg4,size_16,color_FFFFFF,t_70)

### 8.	编写主页

#### index.html

```html
<!DOCTYPE html>
<html lang="zh-cn">
<head>
    <!-- Required meta tags -->
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Spring Boot File Upload / Download Rest API Example</title>

    <!-- Bootstrap CSS -->
    <link href="/css/main.css" rel="stylesheet"/>
</head>
<body>

<noscript>
    <h2>Sorry! Your browser doesn't support Javascript</h2>
</noscript>

<div class="upload-container">
    <div class="upload-header">
        <h2>Spring Boot File Upload / Download Rest API Example</h2>
    </div>
    <div class="upload-content">
        <div class="single-upload">
            <h3>Upload Single File</h3>
            <form id="singleUploadForm" name="singleUploadForm">
                <input id="singleFileUploadInput" type="file" name="file" class="file-input" required/>
                <button type="submit" class="primary submit-btn">Submit</button>
            </form>
            <div class="upload-response">
                <div id="singleFileUploadError"></div>
                <div id="singleFileUploadSuccess"></div>
            </div>
        </div>
        <div class="multiple-upload">
            <h3>Upload Multiple Files</h3>
            <form id="multipleUploadForm" name="multipleUploadForm">
                <input id="multipleFileUploadInput" type="file" name="files" class="file-input" multiple required/>
                <button type="submit" class="primary submit-btn">Submit</button>
            </form>
            <div class="upload-response">
                <div id="multipleFileUploadError"></div>
                <div id="multipleFileUploadSuccess"></div>
            </div>
        </div>
    </div>
</div>

<!-- Optional JavaScript -->
<script src="/js/main.js"></script>
</body>
</html>
```

#### main.css

```css
* {
    -webkit-box-sizing: border-box;
    -moz-box-sizing: border-box;
    box-sizing: border-box;
}

body {
    margin: 0;
    padding: 0;
    font-weight: 400;
    font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
    font-size: 1rem;
    line-height: 1.58;
    color: #333;
    background-color: #f4f4f4;
}

body:before {
    height: 50%;
    width: 100%;
    position: absolute;
    top: 0;
    left: 0;
    background: #128ff2;
    content: "";
    z-index: 0;
}

.clearfix:after {
    display: block;
    content: "";
    clear: both;
}


h1, h2, h3, h4, h5, h6 {
    margin-top: 20px;
    margin-bottom: 20px;
}

h1 {
    font-size: 1.7em;
}

a {
    color: #128ff2;
}

button {
    box-shadow: none;
    border: 1px solid transparent;
    font-size: 14px;
    outline: none;
    line-height: 100%;
    white-space: nowrap;
    vertical-align: middle;
    padding: 0.6rem 1rem;
    border-radius: 2px;
    transition: all 0.2s ease-in-out;
    cursor: pointer;
    min-height: 38px;
}

button.primary {
    background-color: #128ff2;
    box-shadow: 0 2px 2px 0 rgba(0, 0, 0, 0.12);
    color: #fff;
}

input {
    font-size: 1rem;
}

input[type="file"] {
    border: 1px solid #128ff2;
    padding: 6px;
    max-width: 100%;
}

.file-input {
    width: 100%;
}

.submit-btn {
    display: block;
    margin-top: 15px;
    min-width: 100px;
}

@media screen and (min-width: 500px) {
    .file-input {
        width: calc(100% - 115px);
    }

    .submit-btn {
        display: inline-block;
        margin-top: 0;
        margin-left: 10px;
    }
}

.upload-container {
    max-width: 700px;
    margin-left: auto;
    margin-right: auto;
    background-color: #fff;
    box-shadow: 0 1px 11px rgba(0, 0, 0, 0.27);
    margin-top: 60px;
    min-height: 400px;
    position: relative;
    padding: 20px;
}

.upload-header {
    border-bottom: 1px solid #ececec;
}

.upload-header h2 {
    font-weight: 500;
}

.single-upload {
    padding-bottom: 20px;
    margin-bottom: 20px;
    border-bottom: 1px solid #e8e8e8;
}

.upload-response {
    overflow-x: hidden;
    word-break: break-all;
}
```

#### main.js

```js
'use strict';

var singleUploadForm = document.querySelector('#singleUploadForm');
var singleFileUploadInput = document.querySelector('#singleFileUploadInput');
var singleFileUploadError = document.querySelector('#singleFileUploadError');
var singleFileUploadSuccess = document.querySelector('#singleFileUploadSuccess');

var multipleUploadForm = document.querySelector('#multipleUploadForm');
var multipleFileUploadInput = document.querySelector('#multipleFileUploadInput');
var multipleFileUploadError = document.querySelector('#multipleFileUploadError');
var multipleFileUploadSuccess = document.querySelector('#multipleFileUploadSuccess');

function uploadSingleFile(file) {
    var formData = new FormData();
    formData.append("file", file);

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/upload");

    xhr.onload = function() {
        console.log(xhr.responseText);
        var response = JSON.parse(xhr.responseText);
        console.log(response[0]);
        if(xhr.status == 200) {
            singleFileUploadError.style.display = "none";
            singleFileUploadSuccess.innerHTML = "<p>File Uploaded Successfully.</p><p>DownloadUrl : <a href='" + response[0].fileUrl + "' target='_blank'>" + response[0].fileUrl + "</a></p>"+"<p>PreviewUrl : <a href='" + response[0].previewUrl + "' target='_blank'>" + response[0].previewUrl + "</a></p>";
            singleFileUploadSuccess.style.display = "block";
        } else {
            singleFileUploadSuccess.style.display = "none";
            singleFileUploadError.innerHTML = (response && response.message) || "Some Error Occurred";
        }
    }

    xhr.send(formData);
}

function uploadMultipleFiles(files) {
    var formData = new FormData();
    for(var index = 0; index < files.length; index++) {
        formData.append("file", files[index]);
    }

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/upload");

    xhr.onload = function() {
        console.log(xhr.responseText);
        var response = JSON.parse(xhr.responseText);
        if(xhr.status == 200) {
            multipleFileUploadError.style.display = "none";
            var content = "<p>All Files Uploaded Successfully</p>";
            for(var i = 0; i < response.length; i++) {
                content += "<p>DownloadUrl : <a href='" + response[i].fileUrl + "' target='_blank'>" + response[i].fileUrl + "</a></p>"+"<p>PreviewUrl : <a href='" + response[i].previewUrl + "' target='_blank'>" + response[i].previewUrl + "</a></p>";
            }
            multipleFileUploadSuccess.innerHTML = content;
            multipleFileUploadSuccess.style.display = "block";
        } else {
            multipleFileUploadSuccess.style.display = "none";
            multipleFileUploadError.innerHTML = (response && response.message) || "Some Error Occurred";
        }
    }

    xhr.send(formData);
}

singleUploadForm.addEventListener('submit', function(event){
    var files = singleFileUploadInput.files;
    if(files.length === 0) {
        singleFileUploadError.innerHTML = "Please select a file";
        singleFileUploadError.style.display = "block";
    }
    uploadSingleFile(files[0]);
    event.preventDefault();
}, true);

multipleUploadForm.addEventListener('submit', function(event){
    var files = multipleFileUploadInput.files;
    if(files.length === 0) {
        multipleFileUploadError.innerHTML = "Please select at least one file";
        multipleFileUploadError.style.display = "block";
    }
    uploadMultipleFiles(files);
    event.preventDefault();
}, true);
```

### 9.	测试

创建test.docx

<img src="https://img-blog.csdnimg.cn/20200217113635896.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2xqbGo4ODg4,size_16,color_FFFFFF,t_70" alt="在这里插入图片描述" style="zoom: 25%;" />

主页上传：

![在这里插入图片描述](https://img-blog.csdnimg.cn/2020021711391928.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2xqbGo4ODg4,size_16,color_FFFFFF,t_70)

点击PreviewUrl

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200217114114314.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2xqbGo4ODg4,size_16,color_FFFFFF,t_70)

