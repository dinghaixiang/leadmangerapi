package com.example.demo.dao;


/**
 * Created by wanglei on 16/11/10.
 */
public class RspBean {
    private String code = "0";
    private String message = "请求成功";
    private Object data;

    public RspBean() {
    }

    public RspBean(Object data) {
        this.data = data;
    }

    public RspBean(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
