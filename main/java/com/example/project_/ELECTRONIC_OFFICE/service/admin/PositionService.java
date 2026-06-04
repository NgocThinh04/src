package com.example.project_.ELECTRONIC_OFFICE.service.admin;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.PositionRequest;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.PositionResponse;
import com.example.project_.ELECTRONIC_OFFICE.entity.Position;
import com.example.project_.ELECTRONIC_OFFICE.mapper.PositionMapper;
import com.example.project_.ELECTRONIC_OFFICE.repository.admin.PositionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionService {

    private final PositionRepository positionRepository;
    private final PositionMapper positionMapper;

    // Lấy tất cả chức vụ theo companyId
    public List<PositionResponse> getAllPositions(UUID companyId) {
        log.info("Getting all positions for companyId: {}", companyId);

        if (companyId == null) {
            return List.of();
        }

        List<Position> positions = positionRepository.findByCompanyIdOrderByPositionNameAsc(companyId);
        return positions.stream()
                .map(positionMapper::toResponse)
                .collect(Collectors.toList());
    }

    // Lấy chức vụ theo ID
    public PositionResponse getPositionById(UUID positionId) {
        log.info("Getting position by id: {}", positionId);

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("Position not found with id: " + positionId));

        return positionMapper.toResponse(position);
    }


    // Tạo chức vụ mới
    @Transactional
    public PositionResponse createPosition(PositionRequest request) {
        log.info("Creating new position: {} for companyId: {}", request.getPositionName(), request.getCompanyId());

        // Kiểm tra tên chức vụ đã tồn tại trong công ty chưa
        if (positionRepository.existsByPositionNameAndCompanyId(request.getPositionName(), request.getCompanyId())) {
            throw new RuntimeException("Position name already exists in this company: " + request.getPositionName());
        }

        Position position = positionMapper.toEntity(request);


        Position saved = positionRepository.save(position);
        log.info("Position created successfully with id: {}", saved.getPositionId());

        return positionMapper.toResponse(saved);
    }

    // Cập nhật chức vụ
    @Transactional
    public PositionResponse updatePosition(UUID positionId, PositionRequest request) {
        log.info("Updating position: {}", positionId);

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("Position not found with id: " + positionId));

        // Kiểm tra tên mới có bị trùng không (nếu đổi tên)
        if (request.getPositionName() != null && !request.getPositionName().equals(position.getPositionName())) {
            if (positionRepository.existsByPositionNameAndCompanyId(request.getPositionName(), position.getCompanyId())) {
                throw new RuntimeException("Position name already exists in this company: " + request.getPositionName());
            }
        }

        positionMapper.updateEntity(request, position);

        Position saved = positionRepository.save(position);
        log.info("Position updated successfully: {}", positionId);

        return positionMapper.toResponse(saved);
    }

    // Xóa chức vụ
    @Transactional
    public void deletePosition(UUID positionId) {
        log.info("Deleting position: {}", positionId);

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("Position not found with id: " + positionId));

        positionRepository.delete(position);
        log.info("Position deleted successfully: {}", positionId);
    }

    // Cập nhật status chức vụ
    @Transactional
    public PositionResponse updatePositionStatus(UUID positionId, String status) {
        log.info("Updating position status: {} to {}", positionId, status);

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("Position not found with id: " + positionId));

        Position saved = positionRepository.save(position);

        return positionMapper.toResponse(saved);
    }
}