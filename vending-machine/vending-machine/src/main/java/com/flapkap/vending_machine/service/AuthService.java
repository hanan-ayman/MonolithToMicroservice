package com.flapkap.vending_machine.service;

import com.flapkap.vending_machine.dto.LoginRequest;
import com.flapkap.vending_machine.dto.response.LoginResponse;
import com.flapkap.vending_machine.dto.User;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    User signup(User request);
}