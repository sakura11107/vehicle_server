package com.vehicle.server.module.system.contact.controller;

import com.vehicle.server.common.api.ApiResponse;
import com.vehicle.server.module.system.contact.dto.ContactGroupResponse;
import com.vehicle.server.module.system.contact.dto.ContactItemResponse;
import com.vehicle.server.module.system.contact.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Tag(name = "通讯录", description = "用户通讯录，支持异步树结构和搜索")
public class ContactController {

    private final ContactService contactService;

    @GetMapping("/tree")
    @Operation(summary = "获取通讯录树结构", description = "返回角色分组：管理员、普通用户")
    public ApiResponse<List<ContactGroupResponse>> getTree() {
        return ApiResponse.success(contactService.getTree());
    }

    @GetMapping("/tree/{role}/users")
    @Operation(summary = "获取指定角色的用户列表", description = "管理员组(role=2)返回ADMIN和MANAGER，ADMIN在前")
    public ApiResponse<List<ContactItemResponse>> getUsersByRole(@PathVariable Integer role) {
        return ApiResponse.success(contactService.getUsersByRole(role));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索用户", description = "按用户名模糊搜索")
    public ApiResponse<List<ContactItemResponse>> search(@RequestParam String keyword) {
        return ApiResponse.success(contactService.search(keyword));
    }
}
