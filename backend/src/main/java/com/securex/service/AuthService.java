package com.securex.service;

import com.securex.dto.AuthResponse;
import com.securex.dto.LoginRequest;
import com.securex.dto.RegisterRequest;
import com.securex.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);
    UserDto getCurrentUserDto();
}
