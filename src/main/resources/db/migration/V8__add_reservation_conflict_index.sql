ALTER TABLE `vehicle_reservation`
ADD INDEX `idx_conflict_check` (`vehicle_id`, `deleted`, `status`, `start_time`, `end_time`);
