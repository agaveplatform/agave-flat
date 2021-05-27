############################################################################################
# Migration: V2.2.27.2__Alter_Systems_add_indexes_for_owner_and_uuid_by_tenant_id.sql
#
# Adding index for owner by tenant_id to systems table
#
# Database changes:
#
# Table changes:
#
# Index changes:
# + owner_tenant_id
# + uuid_tenant_id
#
# Column changes:
#
# Data changes:
#
##############################################################################################

SET @s = (SELECT IF((SELECT DISTINCT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'systems' AND index_name = 'owner_tenant_id' AND table_schema = DATABASE() ) > 0, "SELECT 1",
    "CREATE INDEX `owner_tenant_id` ON `systems` (`owner`, `tenant_id`);"));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT DISTINCT COUNT(*) FROM INFORMATION_SCHEMA.statistics
    WHERE table_name = 'systems' AND index_name = 'uuid_tenant_id' AND table_schema = DATABASE() ) > 0, "SELECT 1",
    "CREATE INDEX `uuid_tenant_id` ON `systems` (`uuid`, `tenant_id`);"));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;