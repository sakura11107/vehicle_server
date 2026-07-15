package com.vehicle.server.common.api;

/**
 * 单个请求字段的校验结果，供前端定位字段并展示 i18n 文案。
 */
public record FieldError(String field, String code) {
}
