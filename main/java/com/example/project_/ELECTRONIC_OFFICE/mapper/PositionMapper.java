package com.example.project_.ELECTRONIC_OFFICE.mapper;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.PositionRequest;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.PositionResponse;
import com.example.project_.ELECTRONIC_OFFICE.entity.Position;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PositionMapper {

    public Position toEntity(PositionRequest request) {
        if (request == null) return null;

        Position position = new Position();
        position.setPositionName(request.getPositionName());
        position.setCompanyId(request.getCompanyId());

        return position;
    }

    public PositionResponse toResponse(Position position) {
        if (position == null) return null;

        PositionResponse response = new PositionResponse();
        response.setPositionId(position.getPositionId());
        response.setPositionName(position.getPositionName());
        response.setCompanyId(position.getCompanyId());

        return response;
    }

    public void updateEntity(PositionRequest request, Position position) {
        if (request.getPositionName() != null) {
            position.setPositionName(request.getPositionName());
        }
    }
}