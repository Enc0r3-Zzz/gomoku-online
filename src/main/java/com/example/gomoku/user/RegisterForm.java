package com.example.gomoku.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterForm {

    @NotBlank(message = "请输入用户名")
    @Pattern(regexp = "^[A-Za-z0-9_]{3,16}$", message = "用户名须为 3-16 位字母、数字或下划线")
    private String username;

    @NotBlank(message = "请输入昵称")
    @Size(max = 32, message = "昵称最多 32 个字符")
    private String nickname;

    @NotBlank(message = "请输入密码")
    @Size(min = 8, max = 72, message = "密码长度须为 8-72 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码须同时包含字母和数字")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
