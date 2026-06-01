package com.example.project_.ELECTRONIC_OFFICE.mapper;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.RegisterRequestAdmin;
import com.example.project_.ELECTRONIC_OFFICE.entity.Users;
import org.mapstruct.Mapper;


public class AdminMapper {
    public static Users toUserEntity(RegisterRequestAdmin registerRequestAdmin) {
        if(registerRequestAdmin == null) {
            return null;
        }
        Users users = new Users();
        users.setUserName(registerRequestAdmin.getUsername());
        users.setEmail(registerRequestAdmin.getEmail());
        users.setNumber(registerRequestAdmin.getNumberPhone());
        users.setAddress(registerRequestAdmin.getAddress());
        return  users;
    }

}
