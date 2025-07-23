package com.flapkap.vending_machine.controller;

import com.flapkap.vending_machine.dto.User;
import com.flapkap.vending_machine.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping(
            value = "/{userName}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateUser(
            @PathVariable String userName,
            @Valid @RequestBody User user) {
        userService.updateUser(userName, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userName}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteUser(@PathVariable String userName) {
        userService.deleteUser(userName);
        return ResponseEntity.noContent().build();
    }
}
