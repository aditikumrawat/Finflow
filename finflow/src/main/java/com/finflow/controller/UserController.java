package com.finflow.controller;

import com.finflow.dto.request.CreateUserRequest;
import com.finflow.dto.response.UserResponse;
import com.finflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

//    As We move forward not required these was just for initial method for user module.
//    @PostMapping
//    public ResponseEntity<UserResponse> createUser(
//            @Valid @RequestBody CreateUserRequest request
//    ) {
//        UserResponse response = userService.createUser(request);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(response);
//    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {

        return ResponseEntity.ok(
                userService.getUsers(
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }
}