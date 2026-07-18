-- 引导系统管理员账号：授权体系落地后 /api/users 仅系统管理员可用，
-- 若库中无任何管理员将无法创建管理员，故预置一个默认账号。
-- 用户名: admin  密码: Admin@123（BCrypt，生产环境请登录后立即修改）
-- 幂等：仅当不存在同名用户时插入，重复执行/重建库安全。
INSERT INTO `sys_user` (`id`, `username`, `password`, `email`, `role`, `status`, `deleted`, `created_time`, `updated_time`)
SELECT 1, 'admin', '$2a$10$8sj1qu.PJ5ELfHDcv4OcOe2YWcevaPOfgD/Rm571cUUkmArJerfiG', 'admin@vehicle.local', 2, 1, 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'admin');
