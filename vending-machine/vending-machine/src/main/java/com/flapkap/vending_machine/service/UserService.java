package com.flapkap.vending_machine.service;

import com.flapkap.vending_machine.dto.User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
     void updateUser(String userName, User user);
     void deleteUser(String userName);
}
