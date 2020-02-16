package com.ljnt.fileio.entity;


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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public Timestamp getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Timestamp uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    @Override
    public String toString() {
        return "FileMsg{" +
                "fileName='" + fileName + '\'' +
                ", fileId=" + fileId +
                ", fileUrl='" + fileUrl + '\'' +
                ", fileType='" + fileType + '\'' +
                ", fileSize=" + fileSize +
                ", uploadTime=" + uploadTime +
                ", previewUrl='" + previewUrl + '\'' +
                '}';
    }
}
